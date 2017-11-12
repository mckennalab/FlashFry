package utils

import org.scalatest.{FlatSpec, Matchers}
import utils._
import scala.collection.mutable

/**
  * test that a base conversion works correctly
  */
class SchizeRankTest extends FlatSpec with Matchers {
  "SchizeRank" should "convert a base to an int correctly" in {
    val intArray = Array[Tuple2[Array[Int],Int]]((Array[Int](5,5,5,5,5),1), (Array[Int](2,2,2,2,2,1),1), (Array[Int](3,3,3,3,3),1), (Array[Int](4,4,4,4,4),1), (Array[Int](1,1,1,1,1),1))

    val rnk = new SchulzeRank[Int](intArray)

    (rnk.indexToRNS(0).rank) should be (0)
    (rnk.indexToRNS(0).score) should be (50)

  }
}