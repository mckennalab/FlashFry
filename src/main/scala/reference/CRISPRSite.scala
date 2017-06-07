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

package reference

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
case class CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, sequenceContext: Option[String]) extends Ordered[CRISPRSite] with PositionInformation {
  val sep = "\t"
  val strandOutput = if (forwardStrand) CRISPRSite.forwardStrandEncoding else CRISPRSite.reverseStrandEncoding

  def to_output = contig + sep + position + sep + (position + bases.length) + sep + bases + sep + strandOutput

  def compare(that: CRISPRSite): Int = (this.bases) compare (that.bases)

  def start = position

  def length = bases.size

}

object CRISPRSite {
  val forwardStrandEncoding = "F"
  val reverseStrandEncoding = "R"
  def header = "contig\tposition\tcutsite\tbases\tstrand\n"

  def fromLine(line: String): CRISPRSite = {
    val sp = line.split("\t")
    val strandEncoding = sp(4) match {
      case `forwardStrandEncoding` => true
      case `reverseStrandEncoding` => false
      case _ => throw new IllegalStateException("Unable to parse strand encoding: " + sp(4))
    }
    CRISPRSite(sp(0), sp(3), strandEncoding, sp(1).toInt, None)
  }
}