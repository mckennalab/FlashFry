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

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack
import com.typesafe.scalalogging.LazyLogging

/**
  * our trait object for scoring guides -- any method that implements this trait can be used to
  * score on and off-target effects
  */
trait ScoreModel {

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  def scoreName(): String

  /**
    * @return the description of method for the header of the output file
    */
  def scoreDescription(): String

  /**
    * @return get a listing of the header columns for this score metric
    */
  def headerColumns(): Array[String]

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack)

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  def validOverScoreModel(enzyme: ParameterPack): Boolean

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  def parseScoringParameters(args: Seq[String]): Seq[String]

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  def bitEncoder(bitEncoding: BitEncoding): Unit

}

// sometimes it's easier to define scores over a single guide, not the collection of guides --
// this abstract class automates scoring each
abstract class SingleGuideScoreModel extends ScoreModel with LazyLogging {
  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]]

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guides the guide with it's off-targets
    * @param bitEnc the bit encoding
    * @param posEnc the position encoding
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    guides.zipWithIndex.foreach { case(hit,index) => {
      if ((index + 1) % 1000 == 0) {
        logger.info("For scoing metric " + this.scoreName() + " we're scoring our " + index + " guide")
      }

      if (this.validOverGuideSequence(pack, hit)) {
        val scores = scoreGuide(hit)
        this.headerColumns().zip(scores).foreach{ case(col,scores) =>
          hit.namedAnnotations(col) = scores
        }
      }
    }
    }
  }
}

object SingleGuideScoreModel {
  /**
    * the sequence stored with a guide can have additional 'context' on each side; find the guide position within that context,
    * handling conditions where the guide is repeated within the context.  In that case we choose the instance most centered
    * within the context
    *
    * @param guide the guide, including the sequence context
    */
  def findGuideSequenceWithinContext(guide: CRISPRSiteOT): Int = {
    if (!guide.target.sequenceContext.isDefined) return -1

    val guideLen = guide.target.bases.size
    (guide.target.sequenceContext.get.size - guideLen) / 2
  }
}