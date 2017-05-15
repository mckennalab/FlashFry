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
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition): Array[CRISPRSiteOT]

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
  def scoreGuide(guide: CRISPRSiteOT): String

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
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition): Array[CRISPRSiteOT] = {
    guides.zipWithIndex.map { case(hit,index) => {
      if ((index + 1) % 1000 == 0) {
        logger.info("For scoing metric " + this.scoreName() + " we're scoring our " + index + " guide")
      }
      hit.namedAnnotations(this.scoreName()) = Array[String](scoreGuide(hit).toString)
      hit
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

    // find the guide within the context, and determine if we have enough flanking
    // sequence for the scoring metric. We need to look ahead to catch embedded instances of the guide
    val guideRegex = ("""(?=""" + guide.target.bases + """)""").r

    val guidePos = guideRegex.findAllIn(guide.target.sequenceContext.get).matchData.toArray

    println("guide =====" + guide.target.bases + " size " + guidePos.size)
    guidePos.size match {
      case n if n <= 0 => return -1 // we have no matches, return negitive one
      case 1 => return guidePos(0).start
      case n if n > 1 => {
        // here we need to figure out which instance to pick as the guide. Our goal will be to
        // pick the target most centered in the window
        val distanceToCenter = guidePos.map { tgt =>
          math.abs((tgt.start + (guide.target.bases.size / 2.0)) - guide.target.sequenceContext.get.size / 2.0)
        }
        val minIndex = distanceToCenter.zipWithIndex.min._2
        return guidePos(minIndex).start
      }
    }
  }
}