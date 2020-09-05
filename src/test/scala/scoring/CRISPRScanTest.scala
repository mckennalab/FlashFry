package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9ParameterPack

import scala.collection.mutable

/**
  * Test the Moreno-Mateos et. al. 2015 Nature Methods paper implementation
  */
class CRISPRScanTest extends FlatSpec with Matchers {

  // always needed - encode guides
  val bitEncoder = new BitEncoding(Cas9ParameterPack)

  // setup the scoring scheme
  val mmScore = new CRISPRscan()
  mmScore.bitEncoder(bitEncoder)

  val offTargetToScore = new mutable.HashMap[CRISPRSiteOT, Double]()

  "CRISPRScan" should "correctly a guide right according to the python code" in {

    val crispr = CRISPRSite("test", "GGTGGCGCTGCTGGATGGACGGG", true, 0, Some("TCCTCTGGTGGCGCTGCTGGATGGACGGGACTGTA"))

    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("GGTGGCGCTGCTGGATGGACGGG", 1)),1000)

    (mmScore.scoreGuide(otHit)(0)(0).toDouble) should be(0.77 +- 0.005)

  }

  "CRISPRScan" should "correctly a second guide right according to the python code" in {

    val crispr = CRISPRSite("test", "GGTGGGGCTGAAAGATGGACGGG", true, 0, Some("TCCTCTGGTGGGGCTGAAAGATGGACGGGTTTGTA"))

    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("GGTGGGGCTGAAAGATGGACGGG", 1)),1000)

    (mmScore.scoreGuide(otHit)(0)(0).toDouble) should be(0.68 +- 0.005)

  }

  "CRISPRScan" should "validate that we have enough context on each side" in {

    val crispr = CRISPRSite("test", "GGTGGGGCTGAAAGATGGACGGG", true, 0, Some("TCCTCTGGTGGGGCTGAAAGATGGACGGGTTTGTA"))
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("GGTGGGGCTGAAAGATGGACGGG", 1)),1000)

    (mmScore.validOverTargetSequence(Cas9ParameterPack, otHit)) should be(true)

    val crispr2 = CRISPRSite("test", "GGTGGGGCTGAAAGATGGACGGG", true, 0, Some("CCTCTGGTGGGGCTGAAAGATGGACGGGTTTGTA"))
    val otHit2 = new CRISPRSiteOT(crispr2, bitEncoder.bitEncodeString(StringCount("GGTGGGGCTGAAAGATGGACGGG", 1)),1000)

    (mmScore.validOverTargetSequence(Cas9ParameterPack, otHit2)) should be(false)

    val crispr3 = CRISPRSite("test", "GGTGGGGCTGAAAGATGGACGGG", true, 0, Some("TCCTCTGGTGGGGCTGAAAGATGGACGGGTTTGT"))
    val otHit3 = new CRISPRSiteOT(crispr3, bitEncoder.bitEncodeString(StringCount("GGTGGGGCTGAAAGATGGACGGG", 1)),1000)

    (mmScore.validOverTargetSequence(Cas9ParameterPack, otHit2)) should be(false)

    val crispr4 = CRISPRSite("test", "GGTGGGGCTGAAAGATGGACGGG", true, 0, Some("CCTCTGGTGGGGCTGAAAGATGGACGGGTTTGT"))
    val otHit4 = new CRISPRSiteOT(crispr4, bitEncoder.bitEncodeString(StringCount("GGTGGGGCTGAAAGATGGACGGG", 1)),1000)

    (mmScore.validOverTargetSequence(Cas9ParameterPack, otHit2)) should be(false)

  }
}
