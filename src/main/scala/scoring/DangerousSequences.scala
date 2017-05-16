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
  * - GC > 80% or less than 20%: hard to close
  * - palindromic guides: hard to clone in / PCR? Not implemented yet
  * - is the site within the genome?
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

    if (Utils.gcContent(guide.target.bases) < .2 || Utils.gcContent(guide.target.bases) > .8)
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
