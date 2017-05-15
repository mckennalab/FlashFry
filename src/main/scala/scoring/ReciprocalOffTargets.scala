package scoring
import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack

/**
  * You sometimes don't want targets within your scan to have reciprocal off-targets: where one guide
  * is a really close off-target for another guide in your results. This may lead to drop-out of a whole
  * region, which can be really confusing when looking at functional effects.
  */
class ReciprocalOffTargets(maximumMismatch: Int = 2) extends ScoreModel {
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
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition) {
    guides.foreach{guide1 => {
      guides.foreach{guide2 => {
        if (bitEnc.mismatches(guide1.longEncoding,guide2.longEncoding) <= maximumMismatch) {
          guide1.namedAnnotations(scoreName) = guide1.namedAnnotations.getOrElse(scoreName,Array[String]()) :+ guide2.target.bases
        }
      }}
    }}
    guides
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
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {args}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}
}
