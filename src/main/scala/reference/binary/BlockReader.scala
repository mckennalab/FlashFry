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

package reference.binary

import java.io.File

import bitcoding.{BitEncoding, BitPosition, StringCount}
import reference.CRISPRSite
import utils.{BaseCombinationGenerator, Utils}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * fetches a block of targets on-demand from files on disk
  */
class BlockReader(binsToFile: mutable.HashMap[String, File],
                  fetchingBlockSize: Int,
                  outputFileBlockSize: Int,
                  bitEncoding: BitEncoding,
                  bitPosition: BitPosition) {

  assert(fetchingBlockSize >= outputFileBlockSize, "The fetching block size must be larger or equal to the output file block size")

  var activeBlock: Option[String] = None

  var targets = Array[TargetPos]()

  val binMask = bitEncoding.compBitmaskForBin(fetchingBlockSize, 0)

  /**
    * get the bin
    * @param bin the bin, as a string
    * @return a sorted block of longs that represent this bin
    */
  def fetchBin(bin: String): Array[TargetPos] = {
    val binSlice = bin.slice(0, outputFileBlockSize)

    val binComparitor = bitEncoding.binToLongComparitor(bin)

    // if we need to, load the block in question
    if (!activeBlock.isDefined || activeBlock.get != binSlice)
      loadBlock(binsToFile(binSlice),binSlice)

    val ret = new mutable.ArrayBuffer[TargetPos]()

    // while-loop for speed
    var index = 0
    while (index < targets.size) {
      if (bitEncoding.mismatches(targets(index).target, binComparitor.binLong, binMask) == 0)
        ret += targets(index)
      index += 1
    }
    ret.toArray
  }

  /**
    * load a block of data, sort it, and convert to longs
    * @param file the file to load
    */
  def loadBlock(file: File, bin: String): Unit = {
    val newTargets = ArrayBuffer[TargetPos]()

    val toSort = new ArrayBuffer[CRISPRSite]()
    Source.fromInputStream(Utils.gis(file.getAbsolutePath)).getLines().foreach { line => {
      toSort += CRISPRSite.fromLine(line)
    }}

    val toSortResults = toSort.toArray
    scala.util.Sorting.quickSort(toSortResults)

    var lastTarget : Option[TargetPos] = None
    var lastTargetSeq : Option[String] = None

    toSortResults.foreach { hit => {
      val encoded = bitEncoding.bitEncodeString(hit.bases)
      val position = bitPosition.encode(hit.contig,hit.position,hit.bases.size,hit.forwardStrand)

      val fullTarget = new TargetPos(encoded,Array[Long](position),bitEncoding)

      if (lastTargetSeq.isDefined && lastTargetSeq.get == hit.bases) {
        lastTarget = Some(lastTarget.get.combine(fullTarget))
      } else if (lastTargetSeq.isDefined) {
        newTargets += lastTarget.get

        lastTarget = Some(fullTarget)
        lastTargetSeq = Some(hit.bases)
      } else {
        lastTarget = Some(fullTarget)
        lastTargetSeq = Some(hit.bases)
      }
    }}

   if (lastTargetSeq.isDefined) {
      newTargets += lastTarget.get
    }

    activeBlock = Some(bin)
    targets = newTargets.toArray

  }
}

case class TargetPos(target: Long, positions: Array[Long], bitEnc: BitEncoding) {
  assert(bitEnc.bitDecodeString(target).str.size <= 24,"Size of decoded string is too long. The string was " + bitEnc.bitDecodeString(target).str)

  def combine(other: TargetPos) = {
    assert(bitEnc.mismatches(other.target, target) == 0)
    assert(bitEnc.bitDecodeString(target).str.size <= 24,"Size of decoded string is too long. The string was " + bitEnc.bitDecodeString(target).str)
    assert(bitEnc.bitDecodeString(other.target).str.size <= 24,"Size of other decoded string is too long. The string was " + bitEnc.bitDecodeString(other.target).str)


    var newTotal = bitEnc.getCount(target) + bitEnc.getCount(other.target)
    if (newTotal > Short.MaxValue)
      newTotal = Short.MaxValue


    TargetPos(bitEnc.bitEncodeString(StringCount(bitEnc.bitDecodeString(target).str,newTotal.toShort)),
      (positions ++ other.positions).slice(0,Short.MaxValue), bitEnc)
  }

  def sz = 1 + positions.size

  def toLongBuffer: Array[Long] = Array[Long](target) ++ positions
}

case class BlockDescriptor(bin: String, block: Array[Long], numberOfTargets: Int)

object BlockDescriptor {

  def toBlockDescriptor(bin: String, targets: Array[TargetPos]): BlockDescriptor = {
    val blockBuilder = mutable.ArrayBuilder.make[Long]()
    targets.foreach{case(tgt) => {
      blockBuilder += tgt.target
      blockBuilder ++= tgt.positions
      assert(tgt.bitEnc.getCount(tgt.target) == tgt.positions.size)
    }}
    BlockDescriptor(bin,blockBuilder.result(),targets.size)
  }
}