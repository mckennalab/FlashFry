package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9ParameterPack


/**
  * Created by aaronmck on 2/22/17.
  */
class CrisprMitEduOffTargetTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("1", "GACTTGCATCCGAAGCCGGTGGG", true, 1, None)
  val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACCTGCATACGAAGCCTTTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("TTCTTGCATCAGAAGCCGCTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACCTGCATCCAAAGCCAGAGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GGCTGGCATCCCAAGCCAGTAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCCTCCCAAACCTGTTGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGAAGCACATAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGGGTCCGAGGCCTGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAAATGCATCCTAAGCCGTTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAAATGCATCCTAAGCCGTTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GTCTTGCCTCCTAAGCCAGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAAATGCATCCTAAGCCGCTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAGCTGCATCCTAAGCTGGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTACATCCGAAGGAGTTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGGAGCCGAAGTCGCTGGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCAGCTGTAGCCGTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACGTGCATCCGCTGCTGGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GATGTGCATCAGAAGCCGGCAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTGGCATCTGAAGTTGGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GCCTTGCCTCCGAAGCTGGGTGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCTGAATCCATTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCCTAATGTGGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATTTGAAGGGGGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTTCATACAAAGCCGGTAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTGGCATCTGAAGCCGGTGAG",1)),Array[Long]()))

  "CrisprMitEduOffTargetTest" should "correctly score according to the MIT scoring model" in {
    val mit = new CrisprMitEduOffTarget()
    mit.bitEncoder(bitEncoder)
    (mit.score_crispr(crisprOT)) should be (96.0 +- 1.0) // they report 97, not sure how it was rounded. we get 96.4 or so
  }


  "CrisprMitEduOffTargetTest" should "score TTGTTTCCAGGTCAATGTGACGG to correctly TTGTCTTCAAGTCAATATGATGG" in {
    val target = new CRISPRSite("1", "TTGTTTCCAGGTCAATGTGACGG", true, 1, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)

    val otHit = new CRISPRHit(bitEncoder.bitEncodeString(StringCount("TTGTCTTCAAGTCAATATGATGG",1)),Array[Long]())

    val mit = new CrisprMitEduOffTarget()
    mit.bitEncoder(bitEncoder)
    (mit.scoreOffTarget(crisprOT,otHit)) should be (0.36403873 +- 0.1) // they report 97, not sure how it was rounded. we get 96.4 or so
  }
}
