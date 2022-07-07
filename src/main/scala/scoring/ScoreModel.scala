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

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack
import com.typesafe.scalalogging.LazyLogging

/**
  * our trait object for scoring guides -- any method that implements this trait can be used to
  * score on and off-target effects
  */
trait ScoreModel extends LazyLogging {

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
    */
  def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack)

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  def validOverEnzyme(enzyme: ParameterPack): Boolean

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return if we can score this target. Scoring methods should also log a warning that guides will be dropped, and why
    */
  def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  def setup()

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
    * @return an array of arrays (ugh). For each column the scoring method can return multiple pieces of information
    *         which are then merged at the output stage
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
      if ((index + 1) % 10000 == 0) {
        logger.info("For scoring metric " + this.scoreName() + " we're scoring our " + index + " guide")
      }

      if (this.validOverTargetSequence(pack, hit)) {
        val scores = scoreGuide(hit)
        assert(scores.size == this.headerColumns().size)
        this.headerColumns().zip(scores).foreach{ case(col,scores) =>
          hit.namedAnnotations(col) = scores
        }
      } else {
        this.headerColumns().foreach{ case(col) =>
          hit.namedAnnotations(col) = Array[String](SingleGuideScoreModel.missingAnnotation)
        }
      }
    }
    }
  }
}

object SingleGuideScoreModel {
  val missingAnnotation = "NA"

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