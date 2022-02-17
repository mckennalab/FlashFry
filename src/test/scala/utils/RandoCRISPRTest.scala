package utils

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

/**
  * test that a base conversion works correctly
  */
class RandoCRISPRTest extends FlatSpec with Matchers {
  "RandoCRISPR" should "draw some easy bases correctly" in {
    (PatternedTarget.drawRandom('A')) should be ('A')
    (PatternedTarget.drawRandom('C')) should be ('C')
  }

  "RandoCRISPR" should "draw matching memorized bases" in {
    val pattern = "N1,T,N1"
    for (i <- 0 until 100) {
      val draw = PatternedTarget.patternedDraw(pattern, 3)
      (draw(0)) should be(draw(2))
    }
  }

  "RandoCRISPR" should "draw matching memorized bases in order" in {
    val pattern = "N1,N2,T,N1,N2"
    for (i <- 0 until 100) {
      val draw = PatternedTarget.patternedDraw(pattern, 5)
      (draw(0)) should be(draw(3))
      (draw(1)) should be(draw(4))
    }
  }

  "RandoCRISPR" should "draw matching memorized complemented bases in order" in {
    val pattern = "N1,N2,T,N1-,N2-"
    for (i <- 0 until 100) {
      val draw = PatternedTarget.patternedDraw(pattern, 5)
      (draw(0)) should be(Utils.compBase(draw(3)))
      (draw(1)) should be(Utils.compBase(draw(4)))
    }
  }
}