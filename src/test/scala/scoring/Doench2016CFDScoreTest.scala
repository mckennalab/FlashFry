package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRHit, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import reference.CRISPRSite
import standards.Cas9ParameterPack

import scala.collection.mutable

/**
  * test the Doench 2016 off-target score against test examples run with the python code
  */
class Doench2016CFDScoreTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val dScore = new Doench2016CFDScore()
  dScore.bitEncoder(bitEncoder)

  val offTargetToScore = new mutable.HashMap[CRISPRSiteOT, Double]()

  val offTargetList = Array[String]("AAAAAGGTTGGGGATATTGCTGG","AAAACTGCTTGGGATATAGCAGG","AAAAGGATTTGGGATATGGGTGG","AAAAGGGCTTGGAGTATAGCTGG",
    "AAAAGGGTATGGGATAAAACAGG","AAAAGGGTCTGGCATACAGCAGG","AAAAGGGTTGGGGATAGTGCTGG","AAAAGGGTTTGGGAAGTAACAGG","AAAAGGTCTTGGGATATAGGAGG","AAAAGGTTTTGGAATATAGATGG",
    "AAAAGTGATTGGGATATAGTAGG","AAAAGTGTTTGGGATATGGAAGG", "AACAAGGTTTGTGATATAGCAGG","AATAGGGTCGGGGATATAGCAGG","AGAAAGGTTTGGGATATTGCTGG","AGAAGGCTTTGGGATATGGCTGG",
    "CAAAGGGATTGGGACATAGCTGG","CAAAGGGTTTGGCATATAGATGG","GAAAGGGTTTGGGATATCTCTGG")
  
  val otListLong = offTargetList.map{ot => bitEncoder.bitEncodeString(ot)}

  "Doench2016CFDScore" should "correctly a simple guide right according to the python code" in {
    val crispr = CRISPRSite("test", "AAAAGGGTTTGGGATATAGCTGG", true, 0, Some("GGTTGTCAGCTAAAAGGGTTTGGGATATAGCTGGCCTCCCTGGGCCC"))
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount("AAAAGGGTTTGGGATATAGCTGG", 1)),1000)
    otListLong.foreach{ot => otHit.addOT(new CRISPRHit(ot,Array[Long](0l)))}
    (dScore.scoreGuide(otHit).toDouble) should be(7.51176486888e-16 +- 1e-16)
  }
}
