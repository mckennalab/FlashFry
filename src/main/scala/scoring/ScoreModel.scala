package scoring

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack

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
  def parseScoringParameters(args: Array[String])

}

// sometimes it's easier to define scores over a single guide, not the collection of guides -- this abstract class makes that simple
abstract class SingleGuideScoreModel extends ScoreModel {
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
    guides.map{hit => {
      hit.namedAnnotations(this.scoreName()) = Array[String](scoreGuide(hit).toString)
      hit
    }}
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  def bitEncoder(bitEncoding: BitEncoding): Unit
}