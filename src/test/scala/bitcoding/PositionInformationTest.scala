package bitcoding

import org.scalatest.{FlatSpec, Matchers}
import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSite}
import standards.ParameterPack
import standards.Cas9ParameterPack
import bitcoding._

/**
  * Created by aaronmck on 6/6/17.
  */
class PositionInformationTest extends FlatSpec with Matchers {

  "PositionInformation" should "find an overlap correctly" in {
    val posInfo1 = PositionInformationImpl("chr8",1,100,true)
    val posInfo2 = PositionInformationImpl("chr8",100,100,true)

    (posInfo1.overlap(posInfo2)) should be (true)
  }

  "PositionInformation" should "correctly indicate that two sites dont overlap" in {
    val posInfo1 = PositionInformationImpl("chr8",1,100,true)
    val posInfo2 = PositionInformationImpl("chr8",200,100,true)

    (posInfo1.overlap(posInfo2)) should be (false)
  }

  "PositionInformation" should "correctly indicate that two sites dont overlap since they have different contigs" in {
    val posInfo1 = PositionInformationImpl("chr7",1,100,true)
    val posInfo2 = PositionInformationImpl("chr8",100,100,true)

    (posInfo1.overlap(posInfo2)) should be (false)
  }

  "PositionInformation" should "correctly indicate that two sites dont overlap when sites are reversed" in {
    val posInfo2 = PositionInformationImpl("chr8",1,100,true)
    val posInfo1 = PositionInformationImpl("chr8",200,100,true)

    (posInfo1.overlap(posInfo2)) should be (false)
  }
}

