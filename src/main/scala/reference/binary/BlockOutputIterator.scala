package reference.binary

import java.io.File

import bitcoding.{BinAndMask, BitEncoding, BitPosition, StringCount}
import utils.Utils

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * given an input file and a bin iterator, return blocks of guides and their genome positions as an array of longs
  */
class BlockOutputIterator(inputCompressedFile: File,
                          binIterator: Iterator[String],
                          bitEnc: BitEncoding,
                          posEnc: BitPosition) extends Iterator[BlockDescriptor] {

  // for each line in the sorted file, split and rerecord
  val input = Source.fromInputStream(Utils.gis(inputCompressedFile.getAbsolutePath)).getLines()

  // store the growing block before returning it to the user
  var currentBlock: Option[Tuple2[Array[Long],Int]] = None

  // we have to look-ahead to see if the next guide is outside our current bin
  var nextGuide: Option[TargetPos] = None

  // where we get bins from
  val binIter = binIterator

  // the current bin we're in
  var currentBinAndMask : Option[BinAndMask] = None

  // setup the first block (VERY IMPORTANT)
  loadNextBlock()

  /**
    * @return do we have
    */
  override def hasNext: Boolean = currentBlock.isDefined

  /**
    * @return the next block of the iterator -- can be a size 0 array
    */
  override def next(): BlockDescriptor = {
    val ret = BlockDescriptor(currentBinAndMask.get.bin, currentBlock.getOrElse( (Array[Long](),0) )._1, currentBlock.getOrElse( (Array[Long](),0) )._2)
    loadNextBlock()
    ret
  }

  /**
    * performs the actual fetching of blocks
    */
  private def loadNextBlock() {

    if (!binIter.hasNext) {
      currentBlock = None
      return
    }

    currentBinAndMask = Some(bitEnc.binToLongComparitor(binIter.next))

    val nextBinBuilder = mutable.ArrayBuilder.make[Long]
    var numberOfTargets = 0
    var numberOfLongs = 0

    if (!(nextGuide.isDefined) && input.hasNext) {
      nextGuide = Some(BlockOutputIterator.lineToTargetAndPosition(input.next(), bitEnc, posEnc))
    }

    while (input.hasNext && nextGuide.isDefined && bitEnc.mismatchBin(currentBinAndMask.get, nextGuide.get.target) == 0) {
      val guide = BlockOutputIterator.lineToTargetAndPosition(input.next(), bitEnc, posEnc)

      // they are the same guide, add their positions together
      if (bitEnc.mismatches(nextGuide.get.target, guide.target) == 0) {
        nextGuide = Some(guide.combine(nextGuide.get)) // combine off-targets
      }
      // if they're not the same sequence, add the old and setup the new as the nextGuide
      else {
        assert(bitEnc.getCount(nextGuide.get.target) == nextGuide.get.positions.size,"Size of position array and the count in the guide don't match")
        nextBinBuilder ++= nextGuide.get.toLongBuffer
        numberOfTargets += 1
        numberOfLongs += nextGuide.get.sz

        nextGuide = Some(guide)
      }
    }

    // handle the last guide in the file; we may have to try many bins before it matches
    if (nextGuide.isDefined && !input.hasNext && bitEnc.mismatchBin(currentBinAndMask.get, nextGuide.get.target) == 0) {
      nextBinBuilder ++= nextGuide.get.toLongBuffer
      numberOfTargets += 1
      numberOfLongs += nextGuide.get.sz

      nextGuide = None
    }

    val resultingLongs = nextBinBuilder.result()
    assert(resultingLongs.size == numberOfLongs,"Block size check: size " + resultingLongs.size + " should equal the number of packed longs " + numberOfLongs + " (targets " + numberOfTargets + " )")
    currentBlock = Some((resultingLongs,numberOfTargets))
  }

}



object BlockOutputIterator {
  /**
    * convert a line into a target and position pair
    *
    * @param line   the line to split
    * @param bitEnc encodes targets
    * @param posEnc encodes positions
    * @return a case class representing a paired target and it's position
    */
  def lineToTargetAndPosition(line: String, bitEnc: BitEncoding, posEnc: BitPosition): TargetPos = {
    val sp = line.split("\t")

    assert(sp.size == 5,"Each line must have 5 fields")
    assert(sp(3).size <= 24, sp(3) + " too long to be encoded in a 24 base long: " + sp(3).size)
    assert(sp(4) == "F" || sp(4) == "R", sp(4) + "should be either forward or reverse")


    val positionEncoded = posEnc.encode(sp(0), sp(1).toInt, sp(3).size, if (sp(4) == "F") true else false)

    TargetPos(bitEnc.bitEncodeString(StringCount(sp(3), 1)), Array[Long](positionEncoded),bitEnc)
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