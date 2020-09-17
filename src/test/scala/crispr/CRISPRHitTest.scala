package crispr

import org.scalatest.{FlatSpec, Matchers}


class CRISPRHitTest extends FlatSpec with Matchers {

  "CRISPRHit" should "correctly generate a scoring key-value output" in {
    val chit = new CRISPRHit(0L, Array[Long](0L), true)

    chit.addScore("test","value")

    (chit.toOutputScores().get) should be ("{test=value}")
  }

  "CRISPRHit" should "correctly generate a multi-scoring key-value output" in {
    val chit = new CRISPRHit(0L, Array[Long](0L), true)

    chit.addScore("test","value")
    chit.addScore("test2","value2")

    (chit.toOutputScores().get) should be ("{test2=value2!test=value}")
  }

}
