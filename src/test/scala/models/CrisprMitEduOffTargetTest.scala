package models

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRHit, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import reference.CRISPRSite
import standards.Cas9ParameterPack


/**
  * Created by aaronmck on 2/22/17.
  */
class CrisprMitEduOffTargetTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("1", "GACTTGCATCCGAAGCCGGTGGG", true, 1, None)
  val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 0)))

  // now add the known off-target sites
  /*
    GAAATGCATCCTAAGCCGCTGGG _1_4
    GAAATGCATCCTAAGCCGTTGGG _2_4
    GACCTGCATACGAAGCCTTTGGG _1_4
    GACCTGCATCCAAAGCCAGAGGG _1_4
    GACGTGCATCCGCTGCTGGTGGG _1_4

    GACTGGCATCTGAAGTTGGTTGG _1_4
    GACTTACATCCGAAGGAGTTGGG _1_4
    GACTTGCAGCTGTAGCCGTTTGG _1_4
    GACTTGCATCAGAAGCACATAGG _1_4
    GACTTGCATCAGCAGCCTTTTGG _5_4

    GACTTGCATCCGAAGCCGGTGGG _1_0 *
    GACTTGCATCCTAATGTGGTGGG _1_4
    GACTTGCATCTGAATCCATTTGG _1_4
    GACTTGCATTTGAAGGGGGTGGG _1_4
    GACTTGCCTCCCAAACCTGTTGG _1_4

    GACTTGGAGCCGAAGTCGCTGGG _1_4
    GACTTGGGTCCGAGGCCTGTGGG _1_4
    GACTTTCATACAAAGCCGGTAGG _1_3
    GAGCTGCATCCTAAGCTGGTTGG _1_4
    GATGTGCATCAGAAGCCGGCAGG _1_4

    GCCTTGCCTCCGAAGCTGGGTGG _1_4
    GGCTGGCATCCCAAGCCAGTAGG _1_4
    GTCTTGCCTCCTAAGCCAGTTGG _1_4
    TTCTTGCATCAGAAGCCGCTGGG _1_4
    CACTAGCATCCCAGGCCGGTGGG _1_4
   */
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAAATGCATCCTAAGCCGCTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAAATGCATCCTAAGCCGTTGGG",2)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACCTGCATACGAAGCCTTTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACCTGCATCCAAAGCCAGAGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACGTGCATCCGCTGCTGGTGGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTGGCATCTGAAGTTGGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTACATCCGAAGGAGTTGGG",2)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCAGCTGTAGCCGTTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGAAGCACATAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCAGCAGCCTTTTGG",5)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCCGAAGCCGGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCCTAATGTGGTGGG",2)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATCTGAATCCATTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCATTTGAAGGGGGTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGCCTCCCAAACCTGTTGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGGAGCCGAAGTCGCTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTGGGTCCGAGGCCTGTGGG",2)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GACTTTCATACAAAGCCGGTAGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GAGCTGCATCCTAAGCTGGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GATGTGCATCAGAAGCCGGCAGG",1)),Array[Long]()))

  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GCCTTGCCTCCGAAGCTGGGTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GGCTGGCATCCCAAGCCAGTAGG",2)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("GTCTTGCCTCCTAAGCCAGTTGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("TTCTTGCATCAGAAGCCGCTGGG",1)),Array[Long]()))
  crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))

  "CrisprMitEduOffTargetTest" should "correctly score according to the MIT scoring model" in {
    val mit = new CrisprMitEduOffTarget()
    mit.bitEncoder(bitEncoder)
    (mit.score_crispr(crisprOT)) should be (97.0 +- 1.0) // they report 97, not sure what direction it was rounded, or how. we get 96.4 or so

  }
}
