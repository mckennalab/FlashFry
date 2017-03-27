package bitcoding

import java.io.{IOException, PrintWriter}

import bitcoding.BitPosition.PositionLong
import com.typesafe.scalalogging.LazyLogging
import utils.Utils

import scala.collection.mutable
import scala.io.Source

/**
  * the bit position mapping - encode a genomic position into a long value. We encode a position as a long value
  * using the following convention:
  * high 4 bits -- positive (0) or negitive strand (1)
  * second 4 bits -- length of the target / guide
  * bytes 2-4 -- contig encoding
  * bytes 5-8 -- position, as a 32 bit int
  *
  */
class BitPosition extends LazyLogging {

  val contigMap = new mutable.HashMap[String,Int]()
  val indexToContig = new mutable.HashMap[Int,String]()
  var nextSeqId = 1

  def addReference(refName: String): Unit = {
    contigMap(refName) = nextSeqId
    indexToContig(nextSeqId) = refName
    nextSeqId += 1
  }

  def encode(refName: String, position: Int, targetLength: Int, forwardStrand: Boolean): Long = {
    assert(contigMap contains refName, "Unknown contig: " + refName)
    assert(position >= 0, "Position should be positive, we saw: " + position)
    assert(targetLength < 256, "Target length is too large, should be less than 128: " + targetLength)

    val contigShifted   = contigMap(refName).toLong << BitPosition.shiftToIntContig
    val positionShifted = position.toLong << BitPosition.shiftToIntPos
    val strandShifted   = if (forwardStrand) 0l else 1l << BitPosition.shiftToIntStrand
    val sizeShifted     = targetLength.toLong << BitPosition.shiftToIntSize

    contigShifted | positionShifted | strandShifted | sizeShifted
  }

  def decode(encoding: PositionLong): PositionInformation = {

    val contig           = indexToContig(((encoding & BitPosition.contigMask) >> BitPosition.shiftToIntContig).toInt)
    val start            = ((encoding & BitPosition.positionMask) >> BitPosition.shiftToIntPos).toInt
    val size             = ((encoding & BitPosition.sizeMask) >> BitPosition.shiftToIntSize).toInt
    val forwardStrand    = ((encoding & BitPosition.strandMask) >> BitPosition.shiftToIntStrand).toInt == 0

    PositionInformation(contig,start,size,forwardStrand)
  }

}

// everything you might want to know about a target's position in the genome
case class PositionInformation(contig: String, start: Int, length: Int, forwardStrand: Boolean) {

  def overlap(oContig: String, startPos: Int, endPos: Int): Boolean = {
    if (contig != oContig) return false
    if (((start + length) < startPos) || (endPos < startPos)) return false
    true
  }
}


object BitPosition {
  // there's a dependency between these 8 values, dont change unless you look at effect on all values
  var strandMask   = 0xF000000000000000l
  var sizeMask     = 0x0FF0000000000000l
  var contigMask   = 0x000FFFFF00000000l
  var positionMask = 0x00000000FFFFFFFFl

  var shiftToIntStrand = 60
  var shiftToIntSize   = 52
  var shiftToIntContig = 32
  var shiftToIntPos    =  0

  type PositionLong = Long
}

