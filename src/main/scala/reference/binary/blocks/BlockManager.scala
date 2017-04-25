package reference.binary.blocks

import bitcoding.{BinAndMask, BitEncoding}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, GuideIndex, ResultsAggregator}
import reference.binary.{BlockDescriptor, TargetPos}
import reference.binary.blocks.BlockManager.{compareIndexedBlock, compareLinearBlock}
import reference.traverser.Traverser.{allComparisons, allTargets, allTargetsAndPositions}
import utils.{BaseCombinationGenerator, Utils}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * This class handles determining the correct structure of a retrieved block from our off-target database,
  * and handles comparisons between a guide and that block. We allow block files to have interleaved blocks
  * of index and linear traversals in any combination
  */
class BlockManager(offset: Int, width: Int = 4, bitEncoding: BitEncoding) extends LazyLogging {

  // setup a bunch of things we'll only want to create once
  var combinedLongFilter = bitEncoding.compBitmaskForBin(offset + width)

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
        compareLinearBlock(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          numberOfTargets, guides, aggregator, bitEncoding, maxMismatches, bin)
      }
      case 2 => {
        compareIndexedBlock(blockOfTargetsAndPositions.slice(1, blockOfTargetsAndPositions.size),
          numberOfTargets, guides, aggregator, bitEncoding, maxMismatches, bin, blockDescriptorLookup)
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
    * @param numberOfTargets            the number of targets encoded in the block
    * @param guides                     the array guides
    * @param bitEncoding                how to encode values
    * @param maxMismatches              the maximum number of mismatches
    * @param parentBin                  the parent bin sequence
    * @param blockDescriptorLookup      a description of the underlying bin structure
    *
    */
  def compareIndexedBlock(blockOfTargetsAndPositions: Array[Long],
                          numberOfTargets: Int,
                          guides: Array[GuideIndex],
                          aggregator: ResultsAggregator,
                          bitEncoding: BitEncoding,
                          maxMismatches: Int,
                          parentBin: BinAndMask,
                          blockDescriptorLookup: Array[BinToLongComp]) {

    // for each sub-bin, slice the array of longs to the right size, find the guides that could match this
    // sub grouping, and run the standard linear comparison
    val slicedBlock = blockOfTargetsAndPositions.slice(0, blockDescriptorLookup.size)
    var lastPos = 0
    var lastSize = 0

    // one more while loop speed-up: we get a 2-3X improvement here by switching to a while loop
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
          val linearResults = compareLinearBlock(blockSlice, numberOfTargets, newGuides.toArray, aggregator, bitEncoding, maxMismatches, parentBin)
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
                         numberOfTargets: Int,
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

      val currentTarget = blockOfTargetsAndPositions(offset)
      val count = bitEncoding.getCount(currentTarget)

      assert(count > 0, "Encoded position count should be greater than zero: " + bitEncoding.bitDecodeString(currentTarget).toStr + " targets seen " + allTargets)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size + " bin " + bin)

      val positions = blockOfTargetsAndPositions.slice(offset + 1, (offset + 1) + count)

      allTargetsAndPositions += positions.size

      var guideIndex = 0
      while (guideIndex < guides.size) {
        allComparisons += 1
        val mismatches = bitEncoding.mismatches(guides(guideIndex).guide, currentTarget)
        if (mismatches <= maxMismatches) {
          aggregator.updateOT(guides(guideIndex),CRISPRHit(currentTarget, positions))
        }
        guideIndex += 1
      }
      offset += count + 1
    }
  }


  /**
    * create an indexed block from a set of targets
    *
    * @param targetsAndPositions the targets
    * @param prefix              the bin string
    * @param bitEncoding         the bit encoder
    * @param lookupBinSize       the size of the inner bin lookup
    * @return a block of longs
    */
  def createIndexedBlock(targetsAndPositions: Array[TargetPos],
                         prefix: String,
                         bitEncoding: BitEncoding,
                         lookupBinSize: Int): Array[Long] = {


    val block = mutable.ArrayBuilder.make[Long]()


    // now make the lookup table
    // --------------------------------------------
    val binLookup = new mutable.LinkedHashMap[String, Int]()
    val binLookupSize = new mutable.LinkedHashMap[String, Int]()

    new BaseCombinationGenerator(lookupBinSize).iterator.foreach { case (bin) => {
      binLookup(bin) = -1
      binLookupSize(bin) = 0
    }
    }

    var currentPos = 0

    targetsAndPositions.zipWithIndex.foreach { case (targetAndPos, index) => {
      val bn = bitEncoding.bitDecodeString(targetAndPos.target).str.slice(prefix.size, prefix.size + lookupBinSize)
      if (binLookup(bn) >= currentPos | binLookup(bn) < 0) {
        binLookup(bn) = currentPos
      }

      currentPos += (1 + targetAndPos.positions.size)
      binLookupSize(bn) = binLookupSize.getOrElse(bn, 0) + (1 + targetAndPos.positions.size)

      block += targetAndPos.target
      block ++= targetAndPos.positions
    }
    }

    val arrayOfOffsetsAndSizes = mutable.ArrayBuilder.make[Long]()
    arrayOfOffsetsAndSizes += 2l // our block type

    var blockSumCheck = 0
    binLookup.foreach { case (key, value) => {
      arrayOfOffsetsAndSizes += (binLookup(key).toLong << 32 | binLookupSize(key).toLong)
      blockSumCheck += binLookupSize(key)
    }
    }


    val res = arrayOfOffsetsAndSizes.result() ++ block.result()
    blockSumCheck += arrayOfOffsetsAndSizes.result().size
    assert(blockSumCheck == res.size, "Our sum of targets and positions doesn't add up the total block size (minus our header long): " + blockSumCheck + " vrs " + (res.size))
    res
  }


  /**
    * create a linear bin representation of this set of targets
    *
    * @param targetsAndPositions the targets and positions
    * @param prefix              the bin prefix
    * @param bitEncoding         the bit encoding
    * @return array of longs
    */
  def createLinearBlock(targetsAndPositions: Array[TargetPos],
                        prefix: String,
                        bitEncoding: BitEncoding): Array[Long] = {


    val block = mutable.ArrayBuilder.make[Long]()
    var blockSum = 0
    block += 1l // our block ID
    targetsAndPositions.zipWithIndex.foreach { case (targetAndPos, index) => {
      block += targetAndPos.target
      block ++= targetAndPos.positions
      blockSum += (1 + targetAndPos.positions.size)
    }
    }

    val res = block.result()
    assert(blockSum == res.size - 1, "Our sum of targets and positions doesn't add up the total block size (minus our header long): " + blockSum + " vrs " + (res.size - 1))
    res
  }
}

