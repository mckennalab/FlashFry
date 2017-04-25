package utils

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

/**
  * test that a base conversion works correctly
  */
class BaseTest extends FlatSpec with Matchers {
  "Base" should "convert a base to an int correctly" in {
    (Base.baseToInt(Base.A)) should be (0)
    (Base.baseToInt(Base.C)) should be (1)
    (Base.baseToInt(Base.G)) should be (2)
    (Base.baseToInt(Base.T)) should be (3)
  }

  "Base" should "convert an int to a a base correctly" in {
    (Base.intToBase(0)) should be (Base.A)
    (Base.intToBase(1)) should be (Base.C)
    (Base.intToBase(2)) should be (Base.G)
    (Base.intToBase(3)) should be (Base.T)
  }
}