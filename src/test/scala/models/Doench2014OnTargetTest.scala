package models

import bitcoding.{BitEncoding, StringCount}
import crispr.CRISPRSiteOT
import org.scalatest.{FlatSpec, Matchers}
import reference.CRISPRSite
import standards.Cas9ParameterPack

import scala.collection.mutable
import scala.io.Source

/**
  * test the Doench 2014 et al on-target prediction scores
  */
class Doench2014OnTargetTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("1", "GACTTGCATCCGAAGCCGGTGGG", true, 1, None)
  val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 0)))
  val dScore = new Doench2014OnTarget()
  dScore.bitEncoder(bitEncoder)

  val offTargetToScore = new mutable.HashMap[CRISPRSiteOT, Double]()

  // load up the supplemental table 7 for Doench 2014, which contains on-target scores for a thousand+ guides. Unforuntely the code
  // and the scores there don't currently line up -- we'll follow-up with the authors.  So weve' currently chosen to go with
  // the code version, and validate against that.

  /*
  Source.fromFile("test_data/doench_2014_on_target_scores.csv").getLines().foreach { line => {
    val sp = line.split(",")
    val score = sp(8).toDouble

    val crisprSite = new CRISPRSiteOT(CRISPRSite("testContig", sp(1).slice(4, 27), true, 0, Some(sp(1))), bitEncoder.bitEncodeString(StringCount(sp(0), 1)))
    offTargetToScore(crisprSite) = score
  }
  }


  "Doench2014OnTarget" should "correctly score supplemental table 7 correctly" in {
    offTargetToScore.foreach { case (otHit, score) => {
      (dScore.scoreGuide(otHit).toDouble) should be(score +- 0.1)
    }
    }
  }
*/

  "Doench2014OnTarget" should "correctly a simple guide right according to the python code" in {
    val crispr = CRISPRSite("test", "GTCAGCTGCCCCCACCTCCCTGG", true, 0, Some("GGTTGTCAGCTGCCCCCACCTCCCTGGGCCC"))
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("GTCAGCTGCCCCCACCTCCCTGG", 1)))
    (dScore.scoreGuide(otHit).toDouble) should be(0.011271132331539457 +- 0.001)

  }
}
