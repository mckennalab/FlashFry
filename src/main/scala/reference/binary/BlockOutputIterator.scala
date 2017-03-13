package reference.binary

import java.io.File

import bitcoding.{BitEncoding, BitPosition, StringCount}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * given an input file and a bin iterator, return blocks of guides and their genome positions as an array of longs
  */
class BlockOutputIterator(inputFile: File,
                          binIterator: Iterator[String],
                          bitEnc: BitEncoding,
                          posEnc: BitPosition) extends Iterator[Array[Long]] {

  // for each line in the sorted file, split and rerecord
  val input = Source.fromFile(inputFile).getLines()

  // store the growing block before returning it to the user
  var currentBlock: Option[Array[Long]] = None

  // we have to look-ahead to see if the next guide is outside our current bin
  var nextGuide: Option[TargetPos] = if (input.hasNext) Some(BlockOutputIterator.lineToTargetAndPosition(input.next(), bitEnc, posEnc)) else None

  // where we get bins from
  val binIter = binIterator

  /**
    * @return do we have
    */
  override def hasNext: Boolean = currentBlock.isDefined

  /**
    * @return the next block of the iterator -- can be a size 0 array
    */
  override def next(): Array[Long] = {
    val ret = currentBlock.getOrElse(Array[Long]())
    loadNextBlock()
    ret
  }

  // performs the actual fetching of blocks
  private def loadNextBlock() {

    if (!binIter.hasNext) {
      currentBlock = None
      return
    }
    val currentBin = binIter.next

    val nextBinBuilder = mutable.ArrayBuilder.make[Long]

    if (!nextGuide.isDefined && input.hasNext)
      nextGuide = Some(BlockOutputIterator.lineToTargetAndPosition(input.next(), bitEnc, posEnc))

    while (nextGuide.isDefined && bitEnc.mismatchBin(currentBin, nextGuide.get.target) == 0) {
      val guide = BlockOutputIterator.lineToTargetAndPosition(input.next(), bitEnc, posEnc)

      if (bitEnc.mismatches(nextGuide.get.target, guide.target) == 0) {
        nextGuide = Some(guide.combine(nextGuide.get, bitEnc)) // combine off-targets
      } else {
        nextBinBuilder += nextGuide.get.target
        nextBinBuilder ++= nextGuide.get.positions
        nextGuide = Some(guide)
      }
    }
    currentBlock = Some(nextBinBuilder.result())
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

    TargetPos(bitEnc.bitEncodeString(StringCount(sp(3), 1)), Array[Long](posEnc.encode(sp(0), sp(1).toInt)))
  }
}

case class TargetPos(target: Long, positions: Array[Long]) {

  def combine(other: TargetPos, bitEnc: BitEncoding) = {
    assert(bitEnc.mismatches(other.target, target) == 0)
    TargetPos(target, positions ++ other.positions)
  }
}