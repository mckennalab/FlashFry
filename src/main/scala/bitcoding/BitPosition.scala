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

package bitcoding

import bitcoding.BitPosition.PositionLong
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

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
    assert(nextSeqId < (BitPosition.contigMask >> BitPosition.shiftToIntContig),
      "Contig count exceeds the current capacity of " + (BitPosition.contigMask >> BitPosition.shiftToIntContig))

  }

  def encode(refName: String, position: Int, targetLength: Int, forwardStrand: Boolean): Long = {
    assert(contigMap contains refName, "Unknown contig: " + refName + "; our current contig list is " + contigMap.map{case(c,t) => c + "=" + t}.mkString(","))
    assert(position >= 0, "Position should be positive, we saw: " + position)
    assert(targetLength < 256, "Target length is too large, should be less than 128: " + targetLength)

    val contigShifted   = contigMap(refName).toLong << BitPosition.shiftToIntContig
    val positionShifted = position.toLong << BitPosition.shiftToIntPos
    val strandShifted   = if (forwardStrand) BitPosition.forwardStrand else BitPosition.reverseStrand << BitPosition.shiftToIntStrand
    val sizeShifted     = targetLength.toLong << BitPosition.shiftToIntSize

    contigShifted | positionShifted | strandShifted | sizeShifted
  }

  def decode(encoding: PositionLong): PositionInformation = {

    val contig           = indexToContig(((encoding & BitPosition.contigMask) >> BitPosition.shiftToIntContig).toInt)
    val start            = ((encoding & BitPosition.positionMask) >> BitPosition.shiftToIntPos).toInt
    val size             = ((encoding & BitPosition.sizeMask) >> BitPosition.shiftToIntSize).toInt
    val forwardStrand    = ((encoding & BitPosition.strandMask) >> BitPosition.shiftToIntStrand).toInt == 0

    PositionInformationImpl(contig,start,size,forwardStrand)
  }

}




object BitPosition {
  // there's a dependency between these 8 values, dont change unless you look at effect on all values
  val strandMask   = 0xF000000000000000L
  val sizeMask     = 0x0FF0000000000000L
  val contigMask   = 0x000FFFFF00000000L
  val positionMask = 0x00000000FFFFFFFFL

  val shiftToIntStrand = 60
  val shiftToIntSize   = 52
  val shiftToIntContig = 32
  val shiftToIntPos    =  0

  val forwardStrand = 0L
  val reverseStrand = 1L

  type PositionLong = Long
}

