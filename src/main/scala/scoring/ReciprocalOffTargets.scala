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
import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import scopt.PeelParser
import standards.ParameterPack

/**
  * You sometimes don't want targets within your scan to have reciprocal off-targets: where one guide
  * is a really close off-target for another guide in your results. This may lead to drop-out of a whole
  * region, which can be really confusing when looking at functional effects.
  */
class ReciprocalOffTargets() extends ScoreModel {
  var maxMismatch = 1

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "ReciprocalOffTargets"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Will guides within this design target one-anothers sites? "

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guides the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    guides.foreach{guide1 => {
      guides.foreach{guide2 => {
        if (bitEnc.mismatches(guide1.longEncoding,guide2.longEncoding) != 0 && bitEnc.mismatches(guide1.longEncoding,guide2.longEncoding) <= maxMismatch) {
          guide1.namedAnnotations(scoreName) = guide1.namedAnnotations.getOrElse(scoreName,Array[String]()) :+ guide2.target.bases
        }
      }}
    }}
  }

  /**
    * we're valid over all target types
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * always true for ReciprocalOffTargets
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
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    val parser = new ROTOptions()

    val remaining = parser.parse(args, ROTConfig()) map {
      case(config,remainingParameters) => {
        maxMismatch = config.maxMismatch
        remainingParameters
      }
    }
    remaining.getOrElse(Seq[String]())
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("ReciprocalOffTargets")
}


/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class ROTConfig(maxMismatch: Int = 2)

class ROTOptions extends PeelParser[ROTConfig]("") {
  opt[Int]("maxReciprocalMismatch") required() valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches between two targets with the region to be highlighted in the output")

}