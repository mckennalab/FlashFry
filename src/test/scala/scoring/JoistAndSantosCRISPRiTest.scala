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

  "JostAndSantosCRISPRi" should "correctly tally a simple guide" in {
    val target    = "AAAAA AAAAA AAAAA AAAAA GGG".replace(" ","")
    var offTarget = "TAAAA AAAAA AAAAA AAAAA GGG".replace(" ","")

    (dScore.calc_score(target, offTarget)) should be (1.0)

    offTarget = "ATAAA AAAAA AAAAA AAAAA GGG".replace(" ","")

    (dScore.calc_score(target, offTarget)) should be (0.7952747759038213)



    offTarget = "AAAAA AAAAA AAAAA AAAAT GGG".replace(" ","")

    (dScore.calc_score(target, offTarget)) should be (0.03182081449682617)

    offTarget = "ATAAA AAAAA AAAAA AAAAT GGG".replace(" ","")

    (dScore.calc_score(target, offTarget)) should be (0.7952747759038213 * 0.03182081449682617)
  }
}
