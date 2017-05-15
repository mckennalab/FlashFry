package scoring

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT}
import standards.{Cas9ParameterPack, ParameterPack, SpCAS9}

/**
  * Implementation of:
  *
  * Rational design of highly active sgRNAs for CRISPR-Cas9–mediated gene inactivation. John G Doench, et. al. 2014 Nature Biotech
  *
  * doi:10.1038/nbt.3026
  *
  * WARNING: the implementation below matches the supplemental info, implementations on the web, etc. IT DOES NOT MATCH
  * THE SCORES IN SUPPLEMENTAL TABLE 7. The reason is unclear.
  *
  */
class Doench2014OnTarget extends SingleGuideScoreModel with LazyLogging {

  private var bitEncoder: Option[BitEncoding] = None

  override def scoreName(): String = "Doench2014OnTarget"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "On target scoring metric described by John G Doench in the 2014 paper 'Rational design of highly active sgRNAs for CRISPR-Cas9–mediated gene inactivation'"

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuide(guide: CRISPRSiteOT): String = {
    require(validOverGuideSequence(Cas9ParameterPack,guide), "We're not a valid score over this guide")

    val guidePos = guide.target.sequenceContext.get.indexOf(guide.target.bases)
    (calc_score(guide.target.sequenceContext.get.slice(guidePos - Doench2014OnTarget.contextInFront, guidePos + guide.target.bases.length + Doench2014OnTarget.contextBehind))).toString
  }

  /**
    * this method is valid for Cas9
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = enzyme.enzyme match {
    case _ : SpCAS9.type => true
    case _ => false
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    // we have no specific parameters to parse out here
    args
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding the bit encoder to use
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {bitEncoder = Some(bitEncoding)}

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = {
    if (enzyme.enzyme != SpCAS9) return false
    if (!guide.target.sequenceContext.isDefined) return false

    // find the guide within the context, and determine if we have enough flanking sequence for the scoring metric
    val guidePos = SingleGuideScoreModel.findGuideSequenceWithinContext(guide)

    // do we have enough sequence?
    val enoughContextOnTheLeft = guidePos >= 4
    val enoughContextOnTheRight = guide.target.sequenceContext.get.size - (guidePos + guide.target.bases.size) >= 3

    enoughContextOnTheLeft & enoughContextOnTheRight
  }

  /**
    * the main function: here we calculate the on-target scores for specific guide sequences
    * @param guideAndContext the guide plus flanking context on each side
    * @return a score, converted to a string for output
    */
  def calc_score(guideAndContext: String): Double = {
    assert(guideAndContext.size == 30,"you provided " + guideAndContext.length + " bases and we require 30")
    val gc = guideAndContext.slice(4, 24).map { base => if (base == 'C' || base == 'G') 1 else 0 }.sum
    var gc_val = math.abs(gc - 10)

    var score: Double = Doench2014OnTarget.intercept + (gc_val * Doench2014OnTarget.gc_low)
    if (gc > 10) {
      gc_val = gc - 10
      score = Doench2014OnTarget.intercept + (gc_val * Doench2014OnTarget.gc_high)
    }
    guideAndContext.zipWithIndex.foreach { case (base, index) => {
      val key = base.toString + (index).toString
      var nuc_score = 0.0

      if (Doench2014OnTarget.scoreLookup contains key) {
        nuc_score = Doench2014OnTarget.scoreLookup(key)
      }

      score = score + nuc_score
      if (index < guideAndContext.length - 1) {
        val dinuc = base.toString + guideAndContext(index+1).toString + (index).toString //changed to offset for our 20 mer
        if (Doench2014OnTarget.scoreLookup contains dinuc) {
          score += Doench2014OnTarget.scoreLookup(dinuc)
        }
      }

    }
    }

    val partial_score = math.exp(-1.0 * score)
    val final_score = 1.0 / (1.0 + partial_score)
    return final_score
  }


}

// constants they use in the paper
object Doench2014OnTarget {

  // we need 4 bases upstream and 3 bases behind
  val contextInFront = 4
  val contextBehind = 3

  // various look-up hashes from the original python code

  val scoreLookup = collection.immutable.HashMap("G1"-> -0.2753771,
    "A2"->   -0.3238875,   "C2"->0.17212887,   "C3"-> -0.1006662,   "C4"-> -0.2018029, "G4"->0.24595663,
    "A5"->    0.03644004,  "C5"->0.09837684,   "C6"-> -0.7411813,   "G6"-> -0.3932644, "A11"-> -0.466099,"A14"->0.08537695,
    "C14"->  -0.013814,    "A15"->0.27262051,  "C15"-> -0.1190226,  "T15"-> -0.2859442,
    "A16"->   0.09745459,  "G16"-> -0.1755462, "C17"-> -0.3457955,  "G17"-> -0.6780964,
    "A18"->   0.22508903,  "C18"-> -0.5077941, "G19"-> -0.4173736,  "T19"-> -0.054307,
    "G20"->   0.37989937,  "T20"-> -0.0907126, "C21"->0.05782332,   "T21"-> -0.5305673,
    "T22"->  -0.8770074,   "C23"-> -0.8762358, "G23"->0.27891626,   "T23"-> -0.4031022,
    "A24"->  -0.0773007,   "C24"->0.28793562,  "T24"-> -0.2216372,  "G27"-> -0.6890167,
    "T27"->   0.11787758,  "C28"-> -0.1604453, "G29"->0.38634258,   "GT1"-> -0.6257787,
    "GC4"->   0.30004332,  "AA5"-> -0.8348362, "TA5"->0.76062777,   "GG6"-> -0.4908167,
    "GG11"-> -1.5169074,   "TA11"->0.7092612,  "TC11"->0.49629861,  "TT11"-> -0.5868739,
    "GG12"-> -0.3345637,   "GA13"->0.76384993, "GC13"-> -0.5370252, "TG16"-> -0.7981461,
    "GG18"-> -0.6668087,   "TC18"->0.35318325, "CC19"->0.74807209,  "TG19"-> -0.3672668,
    "AC20"->  0.56820913,  "CG20"->0.32907207, "GA20"-> -0.8364568, "GG20"-> -0.7822076,
    "TC21"-> -1.029693,    "CG22"->0.85619782, "CT22"-> -0.4632077, "AA23"-> -0.5794924,
    "AG23"->  0.64907554,  "AG24"-> -0.0773007,"CG24"->0.28793562,  "TG24"-> -0.2216372,
    "GT26"->  0.11787758,  "GG28"-> -0.69774)

  private val gc_low = -0.202625894
  private val gc_high = -0.166587752
  private var intercept = 0.597636154

}