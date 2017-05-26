package scoring

import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
import standards.ParameterPack
import utils.Utils

/**
  * what's the closest off-target in mismatch space? a convenience class
  * to make guide selection easier
  */
class ClosestHit extends SingleGuideScoreModel {
  var bitEncoder : Option[BitEncoding] = None

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuide(guide: CRISPRSiteOT): String = {
    var closest = Int.MaxValue
    var count = 0

    if (guide.offTargets.size > 0) {
      guide.offTargets.foreach { ot =>
        if (bitEncoder.get.mismatches(ot.sequence, guide.longEncoding) < closest) {
          closest = bitEncoder.get.mismatches(ot.sequence, guide.longEncoding)
          count = 1
        } else if (bitEncoder.get.mismatches(ot.sequence, guide.longEncoding) == closest)
          count += 1
      }
    }

    if (closest == Int.MaxValue)
      "MIN_MISMATCH=UNK,0"
    else
      "MIN_MISMATCH=" + closest + "," + count
  }

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "closest"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "recorded the minimum number of mismatches to the set of off-targets"

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
