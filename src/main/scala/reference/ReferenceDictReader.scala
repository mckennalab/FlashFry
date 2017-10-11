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

package reference

import bitcoding.BitPosition

import scala.io.Source

/**
  * give us an iterator over sequences
  */
case class ReferenceDictReader(ref: String) extends Iterable[ReferenceEntry] {
  var seqList = Array[ReferenceEntry]()

  val sequences = Source.fromFile(ref).getLines().foreach{line => {
    if (line startsWith "@SQ") {
      val sp = line.split("\t")
      seqList :+= ReferenceEntry(sp(1).stripPrefix("SN:"),sp(2).stripPrefix("LN:").toInt)
    }
  }}

  override def iterator: Iterator[ReferenceEntry] = seqList.toIterator

  def generateBitPosition(): BitPosition = {
    val ret = new BitPosition()
    iterator.foreach{refEntry => ret.addReference(refEntry.seqName)}
    ret
  }
}

case class ReferenceEntry(seqName: String, length: Int)