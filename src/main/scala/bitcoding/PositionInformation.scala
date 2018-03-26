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

/**
  * everything you might want to know about a target's position in the genome, with
  * some helper functions to determine overlap (used for instance with BED annotations)
  */
trait PositionInformation {
  def contig: String
  def start: Int
  def length: Int
  def forwardStrand: Boolean

  def overlap(oContig: String, startPos: Int, endPos: Int): Boolean = {
    if (contig != oContig) return false
    (start < startPos && startPos < (start + length) && (start < (endPos)) ||
      start >= startPos && start < (endPos) && (startPos < (start + length)))
  }

  def overlap(pInfo: PositionInformation): Boolean = {
    if (contig != pInfo.contig) return false
    (start < pInfo.start && pInfo.start < (start + length) && (start < (pInfo.start + pInfo.length)) ||
      start >= pInfo.start && start < (pInfo.start + pInfo.length) && (pInfo.start < (start + length)))

  }

}

case class PositionInformationImpl(contig: String, start: Int, length: Int, forwardStrand: Boolean) extends PositionInformation {
}
