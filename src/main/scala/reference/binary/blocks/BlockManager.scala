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

package reference.binary.blocks

import bitcoding.{BinAndMask, BitEncoding}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSite, GuideIndex, ResultsAggregator}
import reference.binary.{BlockDescriptor, TargetPos}
import reference.binary.blocks.BlockManager.{compareIndexedBlock, compareLinearBlock}
import reference.traverser.Traverser.{allComparisons, allTargets, allTargetsAndPositions}
import utils.{BaseCombinationGenerator, Utils}

import scala.annotation._
import elidable._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * This class handles determining the correct structure of a retrieved block from our off-target database,
  * and handles comparisons between a guide and that block. We allow block files to have interleaved blocks
  * of index and linear traversals in any combination.
  */
class BlockManager(offset: Int, width: Int = 4, bitEncoding: BitEncoding, useGPU: Boolean = true) extends LazyLogging {

  // our filter for index blocks
  var combinedLongFilter = bitEncoding.compBitmaskForBin(offset + width)

  // convert the current bin width to bin comparisons for the indexed blocks
  val blockDescriptorLookup = (new BaseCombinationGenerator(width)).map { case (bin) => {
    BinToLongComp(bin, bitEncoding.binToLongComparitor(bin, offset))
  }
  }.toArray

  /**
    * given a block, determine it's type, and then compare targets to guides, returning any hits
    *
    * @param blockOfTargetsAndPositions the block of targets and positions
    * @param numberOfTargets            the number of targets within the block
    * @param guides                     the guides to use in comparison
    * @param bitEncoding                bit encoding manager
    * @param maxMismatches              the maximum number of mismatches to consider in an off-target
    * @param bin                        the sequence of this parent bin
    * @return an array of arrays. Each sub array corresponds one-to-one to the guide list, and individual lists can be empty
    */

  def compareBlock(blockOfTargetsAndPositions: Array[Long],
                   numberOfTargets: Int,
                   guides: Array[GuideIndex],
                   aggregator: ResultsAggregator,
                   bitEncoding: BitEncoding,
                   maxMismatches: Int,
                   bin: BinAndMask) {

    // check the block type, and choose the right block conversion
    val firstLong = blockOfTargetsAndPositions(0)

    firstLong match {
      case 1 => {
        // we have a linear block, return a linear traversal over that block
        BlockManager.compareLinearBlock(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          guides, aggregator, bitEncoding, maxMismatches, bin)
      }
      case 2 => {
        // an index block
        BlockManager.compareIndexedBlock(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          guides, aggregator, bitEncoding, maxMismatches, bin, blockDescriptorLookup, useGPU)
      }
      case _ => {
        throw new IllegalStateException("Invalid bin type, unknown value: " + firstLong)
      }
    }

  }

  /**
    * get all the guides from an encoded block
    *
    * @param blockOfTargetsAndPositions the block of targets and positions
    * @param numberOfTargets            the number of targets within the block
    * @param bin                        the sequence of this parent bin
    * @return an array of arrays. Each sub array corresponds one-to-one to the guide list, and individual lists can be empty
    */

  def decodeToTargets(blockOfTargetsAndPositions: Array[Long],
                   numberOfTargets: Int,
                   bitEncoding: BitEncoding,
                   bin: BinAndMask): Array[CRISPRHit] = {

    // check the block type, and choose the right block conversion
    val firstLong = blockOfTargetsAndPositions(0)

    firstLong match {
      case 1 => {
        BlockManager.linearBlockToGuides(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          bitEncoding)
      }
      case 2 => {
        BlockManager.indexedBlockToGuides(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          bitEncoding, bin, blockDescriptorLookup)
      }
      case _ => {
        throw new IllegalStateException("Invalid bin type, unknown value: " + firstLong)
      }
    }

  }
}

// describes a bin, it's long encoding, and it's mask
case class BinToLongComp(bin: String, binAndMask: BinAndMask)


object BlockManager extends LazyLogging {

  /**
    * compare an indexed block, which has a set lookup table at the top of the block
    *
    * @param blockOfTargetsAndPositions the array of targets
    * @param guides                     the array guides
    * @param bitEncoding                how to encode values
    * @param maxMismatches              the maximum number of mismatches
    * @param parentBin                  the parent bin sequence
    * @param blockDescriptorLookup      a description of the underlying bin structure
    *
    */
  def compareIndexedBlock(blockOfTargetsAndPositions: Array[Long],
                          guides: Array[GuideIndex],
                          aggregator: ResultsAggregator,
                          bitEncoding: BitEncoding,
                          maxMismatches: Int,
                          parentBin: BinAndMask,
                          blockDescriptorLookup: Array[BinToLongComp],
                          useGPU: Boolean) {

    // for each sub-bin, slice the array of longs to the right size, find the guides that could match this
    // sub grouping, and run the standard linear comparison
    val slicedBlock = blockOfTargetsAndPositions.slice(0, blockDescriptorLookup.size)
    var lastPos = 0
    var lastSize = 0

    // one more while-loop speed-up: we get a 2-3X improvement here
    var blockIndex = 0
    while (blockIndex < blockDescriptorLookup.size) {
      val blkDesc = blockDescriptorLookup(blockIndex)
      val positionAndSize = slicedBlock(blockIndex)

      val pos = (positionAndSize >> 32).toInt
      val size = ((positionAndSize << 32) >> 32).toInt

      if (lastPos != 0 && pos >= 0)
        assert(pos == lastPos + lastSize, "the last position: " + lastPos + " plus it's size: " + lastSize + " does not equal the current pos: " + pos)

      lastPos = math.max(0, pos)
      lastSize = size

      // make sure we even need to check -- if this is set to -1 (or a better rule, < 0), don't split and run
      if (pos >= 0 && size > 0) {

        val blockSlice = blockOfTargetsAndPositions.slice(blockDescriptorLookup.size + pos, blockDescriptorLookup.size + pos + size)

        // filter the guides
        val fullBinMask = blkDesc.binAndMask.guideMask | parentBin.guideMask

        val newGuidesBuilder = new mutable.ArrayBuffer[GuideIndex]()
        newGuidesBuilder.sizeHint((guides.size * 0.05).toInt) // suggest a reasonable size -- this seems like a good guess

        // again this is a tight loop that gets hit a lot, so we switched from a idiomatic scala filter to a while loop --
        // this is about ~3X faster than the filter approach in some ad-hoc testing. Always Builder over buffer for primitive types
        var guideIndex = 0
        while (guideIndex < guides.size) {
          if (bitEncoding.mismatches(guides(guideIndex).guide, parentBin.binLong | blkDesc.binAndMask.binLong, fullBinMask) <= maxMismatches)
            newGuidesBuilder += guides(guideIndex)
          guideIndex += 1
        }
        val newGuides = newGuidesBuilder.result()

        if (newGuides.size > 0) {
          BlockManager.compareLinearBlock(blockSlice, newGuides.toArray, aggregator, bitEncoding, maxMismatches, parentBin)
        }
      }

      blockIndex += 1
    }
  }


  /**
    * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
    * each guide it matches
    *
    * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
    * @param guides                     the guides
    * @return an mapping from a potential guide to it's discovered off target sequences
    */
  def compareLinearBlock(blockOfTargetsAndPositions: Array[Long],
                         guides: Array[GuideIndex],
                         aggregator: ResultsAggregator,
                         bitEncoding: BitEncoding,
                         maxMismatches: Int,
                         bin: BinAndMask) {

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l
    var targetIndex = 0

    // process each target in the block
    while (offset < blockOfTargetsAndPositions.size) {
      targetIndex += 1
      allTargets += 1

      assert(offset < blockOfTargetsAndPositions.size, "we were about to fetch the " + targetIndex + " target and failed because " + offset + " is  >= to total block " + blockOfTargetsAndPositions.size)

      val record = bitEncoding.decodeTargetRecord(blockOfTargetsAndPositions, offset)
      val currentTarget = record.target
      val count = record.count

      assert(count > 0, "Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr + " targets seen " + allTargets)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size + " bin " + bin)

      val positions = record.positions

      allTargetsAndPositions += positions.size

      var guideIndex = 0
      while (guideIndex < guides.size) {
        allComparisons += 1
        val mismatches = bitEncoding.mismatches(guides(guideIndex).guide, currentTarget)
        if (mismatches <= maxMismatches) {
          aggregator.updateOT(guides(guideIndex), new CRISPRHit(currentTarget, positions))
        }
        guideIndex += 1
      }
      offset = record.nextOffset
    }
  }

  /**
    * compare an indexed block, which has a set lookup table at the top of the block
    *
    * @param blockOfTargetsAndPositions the array of targets
    * @param numberOfTargets            the number of targets encoded in the block
    * @param bitEncoding                how to encode values
    * @param parentBin                  the parent bin sequence
    * @param blockDescriptorLookup      a description of the underlying bin structure
    *
    */
  def indexedBlockToGuides(blockOfTargetsAndPositions: Array[Long],
                           bitEncoding: BitEncoding,
                           parentBin: BinAndMask,
                           blockDescriptorLookup: Array[BinToLongComp]): Array[CRISPRHit] = {


    val returnGuides = new ArrayBuffer[CRISPRHit]()

    val slicedBlock = blockOfTargetsAndPositions.slice(0, blockDescriptorLookup.size)
    var lastPos = 0
    var lastSize = 0

    // one more while-loop speed-up: we get a 2-3X improvement here
    var blockIndex = 0
    while (blockIndex < blockDescriptorLookup.size) {
      val blkDesc = blockDescriptorLookup(blockIndex)
      val positionAndSize = slicedBlock(blockIndex)

      val pos = (positionAndSize >> 32).toInt
      val size = ((positionAndSize << 32) >> 32).toInt

      if (lastPos != 0 && pos >= 0)
        assert(pos == lastPos + lastSize, "the last position: " + lastPos + " plus it's size: " + lastSize + " does not equal the current pos: " + pos)

      lastPos = math.max(0, pos)
      lastSize = size

      // make sure we even need to check -- if this is set to -1 (or a better rule, < 0), don't split and run
      if (pos >= 0 && size > 0) {

        val blockSlice = blockOfTargetsAndPositions.slice(blockDescriptorLookup.size + pos, blockDescriptorLookup.size + pos + size)

        returnGuides ++= BlockManager.linearBlockToGuides(blockSlice, bitEncoding)

      }

      blockIndex += 1
    }

    returnGuides.toArray
  }

  /**
    * convert a encoded linear block to CRISPRHit objects
    *
    * @param blockOfTargetsAndPositions the block of longs
    * @param numberOfTargets            the number of targets within the block
    * @param bitEncoding                the encoding scheme for these longs
    * @return an array of CRISPR hits
    */
  def linearBlockToGuides(blockOfTargetsAndPositions: Array[Long],
                          bitEncoding: BitEncoding): Array[CRISPRHit] = {

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l
    var targetIndex = 0

    val returnGuides = new ArrayBuffer[CRISPRHit]()

    // process each target in the block
    while (offset < blockOfTargetsAndPositions.size) {
      targetIndex += 1
      allTargets += 1

      assert(offset < blockOfTargetsAndPositions.size, "we were about to fetch the " + targetIndex + " target and failed because " + offset + " is  >= to total block " + blockOfTargetsAndPositions.size)

      val record = bitEncoding.decodeTargetRecord(blockOfTargetsAndPositions, offset)
      val currentTarget = record.target
      val count = record.count

      assert(count > 0, "Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr + " targets seen " + allTargets)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = record.positions

      allTargetsAndPositions += positions.size
      returnGuides += new CRISPRHit(currentTarget, positions)

      offset = record.nextOffset
    }

    returnGuides.toArray

  }

  /**
    * Serialize targets as a two-level indexed block.
    *
    * Layout: `[2][lookup entry x 4^lookupBinSize][grouped target records]`. Each lookup entry packs the relative `Long`
    * offset into its high 32 bits and group size into its low 32 bits; `-1` marks an empty group. Records are grouped by
    * spacer sequence explicitly because full-target sort order begins with the PAM for 5-prime-PAM profiles.
    *
    * @param targetsAndPositions distinct targets assigned to the outer bin
    * @param prefix              outer spacer-bin sequence
    * @param bitEncoding         the bit encoder
    * @param lookupBinSize       number of additional spacer bases in the inner index
    * @return complete block including the block-type marker
    */
  def createIndexedBlock(targetsAndPositions: Array[TargetPos],
                         prefix: String,
                         bitEncoding: BitEncoding,
                         lookupBinSize: Int): Array[Long] = {

    val block = mutable.ArrayBuilder.make[Long]()
    val binOrder = new BaseCombinationGenerator(lookupBinSize).toVector
    val binLookup = new mutable.LinkedHashMap[String, Int]()
    val binLookupSize = new mutable.LinkedHashMap[String, Int]()
    val targetsByBin = new mutable.LinkedHashMap[String, ArrayBuffer[TargetPos]]()

    binOrder.foreach { bin =>
      binLookup(bin) = -1
      binLookupSize(bin) = 0
      targetsByBin(bin) = new ArrayBuffer[TargetPos]()
    }

    // Full target ordering begins with the PAM for 5-prime-PAM nucleases, so it cannot be used to infer that
    // spacer sub-bins are contiguous. Group explicitly before serializing the indexed block.
    targetsAndPositions.foreach { targetAndPos =>
      val decoded = bitEncoding.bitDecodeString(targetAndPos.target).str
      val bn = bitEncoding.mParameterPack.profile.layout.binSequence(decoded, lookupBinSize, prefix.size)
      targetsByBin(bn) += targetAndPos
    }

    var currentPos = 0
    binOrder.foreach { bin =>
      if (targetsByBin(bin).nonEmpty) {
        binLookup(bin) = currentPos
      }

      targetsByBin(bin).foreach { targetAndPos =>
        val encodedRecord = targetAndPos.toLongBuffer
        currentPos += encodedRecord.length
        binLookupSize(bin) += encodedRecord.length
        block ++= encodedRecord
      }
    }

    val arrayOfOffsetsAndSizes = mutable.ArrayBuilder.make[Long]()
    arrayOfOffsetsAndSizes += 2l // our block type

    var blockSumCheck = 0
    binOrder.foreach { bin =>
      arrayOfOffsetsAndSizes += (binLookup(bin).toLong << 32 | binLookupSize(bin).toLong)
      blockSumCheck += binLookupSize(bin)
    }

    val header = arrayOfOffsetsAndSizes.result()
    val res = header ++ block.result()
    blockSumCheck += header.size
    assert(blockSumCheck == res.size, "Our sum of targets and positions doesn't add up to the total block size: " + blockSumCheck + " vs. " + res.size)
    res
  }


  /**
    * Serialize targets as `[1][target record 1]...[target record n]`.
    *
    * @param targetsAndPositions the targets and positions
    * @param prefix              outer bin prefix; retained for API symmetry with indexed blocks
    * @param bitEncoding         active encoding; each `TargetPos` already retains the same encoder
    * @return complete block including the block-type marker
    */
  def createLinearBlock(targetsAndPositions: Array[TargetPos],
                        prefix: String,
                        bitEncoding: BitEncoding): Array[Long] = {


    val block = mutable.ArrayBuilder.make[Long]()
    var blockSum = 0
    block += 1l // our block ID
    targetsAndPositions.zipWithIndex.foreach { case (targetAndPos, index) => {
      val encodedRecord = targetAndPos.toLongBuffer
      block ++= encodedRecord
      blockSum += encodedRecord.length
    }
    }

    val res = block.result()
    assert(blockSum == res.size - 1, "Our sum of targets and positions doesn't add up to the total block size (minus our header long): " + blockSum + " vs. " + (res.size - 1))
    res
  }

}
