/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

/**
  * an object that traverses over a file of off-target blocks
  */
trait Traverser {
  /**
    * scan against the binary database of off-target sites in an implmenetation specific way
    *
    * @param binaryFile    the file we're scanning from
    * @param header        we have to parse the header ahead of time so that we know
    * @param traversal     the traversal over bins we'll use
    * @param aggregator    the array of candidate guides we have
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @return a guide to OT hit array
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

/**
  * common methods that any traverser may need -- unified here to help future parallelism
  */
object Traverser extends LazyLogging {
  // count all the targets we look at
  var allTargets = 0l

  // count all the targets we look at
  var allTargetsAndPositions = 0l

  // record the total number of comparisons we do
  var allComparisons = 0l

  // compute the mismatch count between two strings
  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum

  // format a number into a comma-seperated list
  val formatter = java.text.NumberFormat.getInstance()


  /**
    * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
    * each guide it matches
    *
    * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
    * @param guides                     the guides
    * @return an mapping from a potential guide to it's discovered off target sequences
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
      val currentTarget = blockOfTargetsAndPositions(offset)
      val count = bitEncoding.getCount(currentTarget)
      assert(count > 0,"Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = blockOfTargetsAndPositions.slice(offset + 1, (offset + 1) + count)
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
      offset += count + 1
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

      // get the target, and it's count of positions
      val target = bitEnc.bitDecodeString(larray(pointer))

      // validate that the target belongs in the block
      assert(bitEnc.mismatchBin(bin,larray(pointer)) == 0, "There were mismatches between the bin and the target: " + bin + " and " + target.str)

      pointer += 1
      assert(target.count + pointer <= larray.size, "Fetching the current block of positions would underflow our buffer: curpos = " +
        pointer + " positions = " + target.count + " arraysize = " + larray.size)

      ret += new CRISPRHit(larray(pointer - 1),larray.slice(pointer, pointer + target.count))

      // now move the counter the number of positions
      pointer += target.count

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
      val currentTarget = blockOfTargetsAndPositions(offset)
      val count = bitEncoding.getCount(currentTarget)
      assert(count > 0,"Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = blockOfTargetsAndPositions.slice(offset + 1, (offset + 1) + count)
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
      offset += count + 1
    }

    val returnMap = new mutable.HashMap[Long, Array[CRISPRHit]]()
    returnArray.map{case(offTargetsFound) => offTargetsFound.toArray}
  }
}
