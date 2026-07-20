/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package reference.traverser

import java.io._

import bitcoding.{BinAndMask, BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT, GuideIndex, ResultsAggregator}
import utils.BaseCombinationGenerator
import reference.binary.{BinaryConstants, BinaryHeader}
import reference.traversal.BinTraversal
import standards.ParameterPack

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ArrayBuilder, LinkedHashMap, Map}

/** Strategy for scanning off-target blocks in a binary database. */
trait Traverser {
  /**
    * Scans the bins selected by `traversal` and adds matching off-targets to
    * `aggregator`.
    *
    * @param binaryFile database file to scan
    * @param header parsed metadata and block index for `binaryFile`
    * @param traversal iterator selecting bins and guides to compare
    * @param aggregator destination for discovered guide/off-target matches
    * @param maxMismatch maximum number of spacer mismatches to retain
    * @param configuration nuclease configuration associated with the database
    * @param bitCoder sequence and target-record codec selected from the header
    * @param posCoder genomic-position codec selected from the header
    */
  def scan(binaryFile: File,
           header: BinaryHeader,
           traversal: BinTraversal,
           aggregator: ResultsAggregator,
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition)

}

/** Shared comparison routines and diagnostic counters for traversers. */
object Traverser extends LazyLogging {
  /** Number of distinct encoded targets examined by [[compareBlock]]. */
  var allTargets = 0l

  /** Number of encoded genomic positions examined by [[compareBlock]]. */
  var allTargetsAndPositions = 0l

  /** Number of guide-to-target sequence comparisons performed. */
  var allComparisons = 0l

  /** Counts differing characters at corresponding positions. */
  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum

  /** Locale-aware formatter used for traversal progress messages. */
  val formatter = java.text.NumberFormat.getInstance()


  /**
    * Compares an encoded target block with candidate guides.
    *
    * The returned array follows the input `guides` order. Each nested array
    * contains the off-target hits discovered for that guide in this block.
    *
    * @param blockOfTargetsAndPositions encoded target records and positions
    * @param numberOfTargets number of target records in the block
    * @param guides encoded guide sequences to compare
    * @param bitEncoding codec used by the block
    * @param maxMismatches maximum spacer mismatches to retain
    * @param bin comparison mask and encoded bin prefix
    * @return discovered hits grouped by input guide
    */
  def compareBlock(blockOfTargetsAndPositions: Array[Long],
                   numberOfTargets: Int,
                   guides: Array[Long],
                   bitEncoding: BitEncoding,
                   maxMismatches: Int,
                   bin: BinAndMask): Array[Array[CRISPRHit]] = {

    val returnArray = Array.fill[ArrayBuffer[CRISPRHit]](guides.size)(new ArrayBuffer[CRISPRHit]())

    // filter down the guides we actually want to look at
    val lookAtGuides = guides.filter(gd => bitEncoding.mismatchBin(bin, gd) <= maxMismatches)

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l
    var targetIndex = 0

    // process each target in the block
    while (targetIndex < numberOfTargets) {
      targetIndex += 1
      allTargets += 1

      assert(offset < blockOfTargetsAndPositions.size, "we were about to fetch the " + targetIndex + " target and failed because " + offset + " is  >= to total block " + blockOfTargetsAndPositions.size)
      val record = bitEncoding.decodeTargetRecord(blockOfTargetsAndPositions, offset)
      val currentTarget = record.target
      val count = record.count
      assert(count > 0,"Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = record.positions
      assert(positions.size > 0,"Position count should be greater than zero")

      allTargetsAndPositions += positions.size

      var guideIndex = 0
      while (guideIndex < lookAtGuides.size) {
        allComparisons += 1
        val mismatches = bitEncoding.mismatches(lookAtGuides(guideIndex), currentTarget)
        if (mismatches <= maxMismatches) {
          //logger.info("Hit " + bitEncoding.bitDecodeString(lookAtGuides(guideIndex)) +
          //  " and " + bitEncoding.bitDecodeString(currentTarget) + " + positions " + positions.size + " " + bitEncoding.getCount(currentTarget))
          returnArray(guideIndex) += new CRISPRHit(currentTarget, positions)
        }
        guideIndex += 1
      }
      offset = record.nextOffset
    }

    val returnMap = new mutable.HashMap[Long, Array[CRISPRHit]]()
    returnArray.map{case(offTargetsFound) => offTargetsFound.toArray}
  }


  /**
    * given a block retrieved from the disk, make sure all the targets are there, and there's no extra space -- generally this is used for
    * validation of the encoding and decoding of blocks
    *
    * @param larray the block of stored targets (longs)
    * @param numberOfTargets the number of targets we expect, from the header
    * @param numberOfLongs how big this block is, according to the header
    */
  def validateBlock(larray: Array[Long], numberOfTargets: Int, numberOfLongs: Int, bitEnc: BitEncoding, bin: BinAndMask): Array[CRISPRHit] = {
    assert(larray.size == numberOfLongs,"The block contains an incorrect number of longs (" + larray.size + " != " + numberOfLongs + ")")

    var pointer = 0
    var targetsSeen = 0

    val ret = new ArrayBuffer[CRISPRHit]()

    while (pointer < numberOfLongs) {

      val record = bitEnc.decodeTargetRecord(larray, pointer)
      val target = bitEnc.bitDecodeString(record.target)

      // validate that the target belongs in the block
      assert(bitEnc.mismatchBin(bin,record.target) == 0, "There were mismatches between the bin and the target: " + bin + " and " + target.str)

      ret += new CRISPRHit(record.target, record.positions)
      pointer = record.nextOffset

      // record we processed a target
      targetsSeen += 1
    }

    // now check that we used the whole buffer, and that the target count matches up
    assert(pointer == larray.size, "We did not use the exact size of the buffer: used = " + pointer + " and buffer size " + larray.size)
    assert(targetsSeen == numberOfTargets, "We didn't see exactly the right number of targets, seen = " + targetsSeen + " and expected " + numberOfTargets)

    // this is a bit redundant (or completely) but worth checking if the above conditions change at all)
    assert(numberOfLongs == pointer, "We did not use the expected number of longs: used = " + pointer + " and buffer size " + numberOfLongs)

    ret.toArray
  }

  /**
    * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
    * each guide it matches
    *
    * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
    * @param guides                     the guides
    * @return an mapping from a potential guide to it's discovered off target sequences
    */
  def compareHeapBlock(blockOfTargetsAndPositions: Array[Long],
                   numberOfTargets: Int,
                   guides: Array[Long],
                   bitEncoding: BitEncoding,
                   maxMismatches: Int,
                   bin: BinAndMask): Array[Array[CRISPRHit]] = {

    val returnArray = Array.fill[ArrayBuffer[CRISPRHit]](guides.size)(new ArrayBuffer[CRISPRHit]())

    // filter down the guides we actually want to look at
    val lookAtGuides = guides.filter(gd => bitEncoding.mismatchBin(bin, gd) <= maxMismatches)

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l
    var targetIndex = 0

    // process each target in the block
    while (targetIndex < numberOfTargets) {
      targetIndex += 1
      allTargets += 1

      assert(offset < blockOfTargetsAndPositions.size, "we were about to fetch the " + targetIndex + " target and failed because " + offset + " is  >= to total block " + blockOfTargetsAndPositions.size)
      val record = bitEncoding.decodeTargetRecord(blockOfTargetsAndPositions, offset)
      val currentTarget = record.target
      val count = record.count
      assert(count > 0,"Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = record.positions
      assert(positions.size > 0,"Position count should be greater than zero")

      allTargetsAndPositions += positions.size

      var guideIndex = 0
      while (guideIndex < lookAtGuides.size) {
        allComparisons += 1
        val mismatches = bitEncoding.mismatches(lookAtGuides(guideIndex), currentTarget)
        if (mismatches <= maxMismatches) {
          //logger.info("Hit " + bitEncoding.bitDecodeString(lookAtGuides(guideIndex)) +
          //  " and " + bitEncoding.bitDecodeString(currentTarget) + " + positions " + positions.size + " " + bitEncoding.getCount(currentTarget))
          returnArray(guideIndex) += new CRISPRHit(currentTarget, positions)
        }
        guideIndex += 1
      }
      offset = record.nextOffset
    }

    val returnMap = new mutable.HashMap[Long, Array[CRISPRHit]]()
    returnArray.map{case(offTargetsFound) => offTargetsFound.toArray}
  }
}
