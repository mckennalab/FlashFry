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

package scoring
import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
import picocli.CommandLine.Command
import standards.ParameterPack
import utils.Utils

/**
  * annotate sites with any 'dangerous' sequences
  *
  * Our current list:
  * - TTTT (or longer): could prematurely terminate pol3 transcription
  * - GC > 75% or less than 15%: high GC guides have been shown to generate additional off-targets, and high/low GC can make cloning harder
  * - How many times does this guide perfectly match a genome location
  *
  * TODO: Checks that could be implemented in the future:
  * - palindromic guides: hard to clone in / PCR? Not implemented yet
  *
  */
class DangerousSequences extends SingleGuideScoreModel {
  var bitEncoder : Option[BitEncoding] = None
  var cleanOutput = false

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    var problems = if (cleanOutput) Array.fill[String](3)("0") else Array.fill[String](3)("NONE")

    if (cleanOutput)
      problems(0) = Utils.gcContent(guide.target.bases).toString
    else if (Utils.gcContent(guide.target.bases) < .25 || Utils.gcContent(guide.target.bases) > .75)
      problems(0) = "GC_" + Utils.gcContent(guide.target.bases)

    // thanks to D. Simeonov for the bug catch here
    if (guide.target.bases.slice(bitEncoder.get.mParameterPack.guideRange._1,bitEncoder.get.mParameterPack.guideRange._2).contains("TTTT"))
      problems(1) = if (cleanOutput) "1" else "PolyT"

    if (guide.offTargets.size > 0) {
      val inGenomeCount = guide.offTargets.map{ot => if (bitEncoder.get.mismatches(ot.sequence,guide.longEncoding) == 0) bitEncoder.get.getCount(ot.sequence) else 0}.sum
      if (inGenomeCount > 0)
        problems(2) = if (cleanOutput) inGenomeCount.toString else "IN_GENOME=" + inGenomeCount
    }

    problems.map{prob => Array[String](prob)}
  }

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "dangerous"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "flag sequences that will be hard to create, or could confound analysis"

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def setup() {}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {this.bitEncoder = Some(bitEncoding)}

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("dangerous_GC","dangerous_polyT","dangerous_in_genome")
}
