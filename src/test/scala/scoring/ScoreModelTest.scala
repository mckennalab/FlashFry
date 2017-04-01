package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.CRISPRSiteOT
import org.scalatest.{FlatSpec, Matchers}
import reference.CRISPRSite
import standards.{Cas9ParameterPack, ParameterPack, SpCAS9}

/**
  * Created by aaronmck on 3/31/17.
  */
class ScoreModelTest extends FlatSpec with Matchers {

  val bitEnc = new BitEncoding(Cas9ParameterPack)

  "Scoring model" should "find the position of a guide correctly" in {
    val guideSeq = "GTCAGCTGCCCCCACCTCCCTGG"
    val leftFlank = "GGGGGG"
    val rightFlank = "AAAAAA"

    val crispr = CRISPRSite("test", guideSeq, true, 0, Some(leftFlank + guideSeq + rightFlank))
    val cOT = new CRISPRSiteOT(crispr,bitEnc.bitEncodeString(guideSeq),2000)

    (SingleGuideScoreModel.findGuideSequenceWithinContext(cOT)) should be(6)

  }


  "Scoring model" should "find the position of a guide correctly 2" in {
    val guideSeq = "GTCAGCTGCCCCCACCTCCCTGG"
    val leftFlank = ""
    val rightFlank = "AAAAAA"

    val crispr = CRISPRSite("test", guideSeq, true, 0, Some(leftFlank + guideSeq + rightFlank))
    val cOT = new CRISPRSiteOT(crispr,bitEnc.bitEncodeString(guideSeq),2000)

    (SingleGuideScoreModel.findGuideSequenceWithinContext(cOT)) should be(0)

  }

  "Scoring model" should "find the correct position of a repeated guide" in {
    val guideSeq = "AGAGAGAGGGAGAGAGAGGG"
    val leftFlank = "AGAGAGAGGG"
    val rightFlank = "AGAGAGAGGG"
    // full: AGAGAGAGGGAGAGAGAGGGAGAGAGAGGGAGAGAGAGGG
    val crispr = CRISPRSite("test", guideSeq, true, 0, Some(leftFlank + guideSeq + rightFlank))
    val cOT = new CRISPRSiteOT(crispr,bitEnc.bitEncodeString(guideSeq),2000)

    (SingleGuideScoreModel.findGuideSequenceWithinContext(cOT)) should be(10)

  }
}
