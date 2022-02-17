package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cas9ParameterPack, Cas9ParameterPack19bp}

/**
  * test the Doench 2016 off-target score against test examples run with the python code
  */
class JoistAndSantosCRISPRiTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val dScore = new JostAndSantosCRISPRi()
  dScore.validOverEnzyme(Cas9ParameterPack)
  dScore.bitEncoder(bitEncoder)

  "JostAndSantosCRISPRi" should "correctly tally a set of simple one-mismatch off-targets" in {
    val target = "AAAAA AAAAA AAAAA AAAAA GGG".replace(" ", "")
    var offTarget = "TAAAA AAAAA AAAAA AAAAA GGG".replace(" ", "")

    (dScore.calc_score(target, offTarget)) should be(1.0)

    offTarget = "ATAAA AAAAA AAAAA AAAAA GGG".replace(" ", "")

    (dScore.calc_score(target, offTarget)) should be(0.7952747759038213)
  }


  "JostAndSantosCRISPRi" should "correctly tally a set of more complex one-mismatch off-targets" in {
    val target =    "AAAAA AAAAA AAAAA AAAAA GGG".replace(" ", "")
    var offTarget = "AAAAT AAAAT AAAAG AAAAA GGG".replace(" ", "")

    (dScore.calc_score(target, offTarget)) should be(0.6947382165440157 * 0.31016952886752025 * 0.26865890093507167)

    offTarget = "ATAAA AAAAA AAAAA AAAAT GGG".replace(" ", "")

    (dScore.calc_score(target, offTarget)) should be(0.7952747759038213 * 0.03182081449682617)
  }

  "JostAndSantosCRISPRi" should "not consider an exact match in the scoring scheme" in {
    val crispr = CRISPRSite("test", "AAAAAAAAAAAAAAAAAAAAGGG", true, 0, None)
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount(crispr.bases, 1)),1000)
    val otHit2 = new CRISPRHit(bitEncoder.bitEncodeString(StringCount("AAAAAAAAAAAAAAAAAAAAGGG", 1)),Array[Long](1L))
    otHit.offTargets.append(otHit2)

    (dScore.scoreGuide(otHit)(0)) should be(Array[String]((0.00).toString))
  }

  "JostAndSantosCRISPRi" should "consider a couple of mismatches correctly in the scoring scheme" in {
    val crispr = CRISPRSite("test", "AAAAAAAAAAAAAAAAAAAAGGG", true, 0, None)
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount(crispr.bases, 1)),1000)
    val otHit2 = new CRISPRHit(bitEncoder.bitEncodeString(StringCount("AAAATAAAATAAAAGAAAAAGGG", 1)),Array[Long](1L))
    otHit.offTargets.append(otHit2)

    (dScore.scoreGuide(otHit)(0)) should be(Array[String]((0.6947382165440157 * 0.31016952886752025 * 0.26865890093507167).toString))
  }

  "JostAndSantosCRISPRi" should "consider targets with a single base change at the first position to be a perfect match" in {
    val crispr = CRISPRSite("test", "AAAAAAAAAAAAAAAAAAAAAGG", true, 0, None)
    val otHit = new CRISPRSiteOT(crispr, bitEncoder.bitEncodeString(StringCount(crispr.bases, 1)),1000)
    val otHit2 = new CRISPRHit(bitEncoder.bitEncodeString(StringCount("TAAAAAAAAAAAAAAAAAAAAGG", 1)),Array[Long](1L))
    otHit.offTargets.append(otHit2)

    (dScore.scoreGuide(otHit)(0)) should be(Array[String]((1.0).toString))
  }

  "JostAndSantosCRISPRi" should "consider a couple of mismatches correctly in the scoring scheme using a 19mer target" in {

    val bitEncoder19 = new BitEncoding(Cas9ParameterPack19bp)
    val dScore19 = new JostAndSantosCRISPRi()
    dScore19.validOverEnzyme(Cas9ParameterPack19bp)
    dScore19.bitEncoder(bitEncoder19)



    val crispr = CRISPRSite("test", "AAAAAAAAAAAAAAAAAAAGGG", true, 0, None)
    val otHit = new CRISPRSiteOT(crispr, bitEncoder19.bitEncodeString(StringCount(crispr.bases, 1)),1000)
    val otHit2 = new CRISPRHit(bitEncoder19.bitEncodeString(StringCount("AAATAAAATAAAAGAAAAAGGG", 1)),Array[Long](1L))
    otHit.offTargets.append(otHit2)

    (dScore19.scoreGuide(otHit)(0)) should be (Array[String]((0.6947382165440157 * 0.31016952886752025 * 0.26865890093507167).toString))
  }


}
