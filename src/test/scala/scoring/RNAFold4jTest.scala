import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import scoring.RNAFold4j
import standards.Cas9ParameterPack

import scala.collection.mutable

/**
  * test the Doench 2014 et al on-target prediction scores
  */
class RNAFold4jTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("1", "GACTTGCATCCGAAGCCGGTGGG", true, 1, None)
  val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
  val dScore = new RNAFold4j()
  dScore.validOverEnzyme(Cas9ParameterPack)
  dScore.bitEncoder(bitEncoder)

  val offTargetToScore = new mutable.HashMap[CRISPRSiteOT, Double]()

  "RNAFold4j" should "correctly score a 17bp guide-like sequence" in {
    (dScore.scoreSequence("AGTACTCGAGTACTTCC")) should be(-3.5 +- 0.001)
    (dScore.scoreSequence("AAGTACTCGAGTACTTCC")) should be(-4.4 +- 0.001)
    (dScore.scoreSequence("GGAAGTACTCGAGTACTTCC")) should be(-10.5 +- 0.001)
    (dScore.scoreSequence("GCCAGGAAGTACTCGAGTACTTCC")) should be(-10.7 +- 0.001)
  }

}
