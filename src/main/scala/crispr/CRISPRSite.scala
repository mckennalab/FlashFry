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

package crispr

import bitcoding.PositionInformation

/**
  * This class describes a specific sequence we've found in the genome that matches our target criteria (has the appropriate
  * PAM sequence, etc).
 *
  * @param contig the chromosome or contig we found it in
  * @param bases the bases that make up the target, in the orientation expected (i.e. reverse complement negative strand options)
  * @param forwardStrand are we on the forward or reverse strand?
  * @param position the position we found this sequence at within the contig
  * @param sequenceContext add a broader context to the candidate target. We expect the target to approximately centered (or at least embedded with) the context
  */
case class CRISPRSite(contig: String,
                      bases: String,
                      forwardStrand: Boolean,
                      position: Int,
                      sequenceContext: Option[String]) extends Ordered[CRISPRSite] with PositionInformation {
  
  val sep = "\t"
  val strandOutput = if (forwardStrand) CRISPRSite.forwardStrandEncoding else CRISPRSite.reverseStrandEncoding

  def toOutputString(): String = {
    contig + sep + position + sep + (position + bases.length) + sep + bases + sep + strandOutput
  }

  def compare(that: CRISPRSite): Int = (this.bases) compare (that.bases)

  def start: Int = position

  def length: Int  = bases.size

}

object CRISPRSite {
  val forwardStrandEncoding = "F"
  val reverseStrandEncoding = "R"
  val contigPosition = 0
  val offsetPosition = 1
  val baseStringPosition = 3
  val forwardReversePosition = 4

  def header: String = "contig\tposition\tcutsite\tbases\tstrand\n"

  def fromLine(line: String): CRISPRSite = {
    val sp = line.split("\t")
    try {
      val strandEncoding = sp(forwardReversePosition) match {
        case `forwardStrandEncoding` => true
        case `reverseStrandEncoding` => false
        case _ => throw new IllegalStateException("Unable to parse strand encoding: " + sp(forwardReversePosition))
      }
      CRISPRSite(sp(contigPosition), sp(baseStringPosition), strandEncoding, sp(offsetPosition).toInt, None)
    } catch {
      case e: Exception => {
        throw new IllegalStateException("Unable to parse line: " + line)
      }
    }
  }
}