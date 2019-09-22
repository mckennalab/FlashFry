package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9ParameterPack

/**
  * test the Doench 2016 off-target score against test examples run with the python code
  */
class JoistAndSantosCRISPRiTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val dScore = new JostAndSantosCRISPRi()
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
}
