package scoring

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
  val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
  val dScore = new Doench2014OnTarget()
  dScore.bitEncoder(bitEncoder)

  val offTargetToScore = new mutable.HashMap[CRISPRSiteOT, Double]()

  "Doench2014OnTarget" should "correctly a simple guide right according to the python code" in {
    val crispr = CRISPRSite("test", "GTCAGCTGCCCCCACCTCCCTGG", true, 0, Some("GGTTGTCAGCTGCCCCCACCTCCCTGGGCCC"))
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("GTCAGCTGCCCCCACCTCCCTGG", 1)),1000)
    (dScore.scoreGuide(otHit).toDouble) should be(0.011271132331539457 +- 0.001)

  }
}
