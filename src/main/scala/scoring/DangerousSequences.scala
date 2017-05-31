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

package scoring
import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
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

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuide(guide: CRISPRSiteOT): String = {
    var problems = Array[String]()

    if (Utils.gcContent(guide.target.bases) < .25 || Utils.gcContent(guide.target.bases) > .75)
      problems :+= "GC_" + Utils.gcContent(guide.target.bases)

    if (guide.target.bases.contains("TTTT"))
      problems :+= "PolyT"

    if (guide.offTargets.size > 0) {
      val inGenomeCount = guide.offTargets.map{ot => if (bitEncoder.get.mismatches(ot.sequence,guide.longEncoding) == 0) 1 else 0}.sum
      if (inGenomeCount > 0) problems :+= "IN_GENOME=" + inGenomeCount
    }

    if (problems.size == 0)
      "NONE"
    else
      problems.mkString(",")


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
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = args

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {this.bitEncoder = Some(bitEncoding)}
}
