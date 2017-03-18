package test.scala.util

import utils.BaseCombinationGenerator
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Matchers._

import scala.collection.mutable


/**
 * Created by aaronmck on 8/3/15.
 */
class BaseCombinationGeneratorTest extends FlatSpec with Matchers {
  "BaseCombinations" should " generate the complete set of unique kmers of the specified length (7)" in {
    val sizeOfIter = 7

    val events = new mutable.HashMap[String,Boolean]()
    val baseIterator = new BaseCombinationGenerator(sizeOfIter)

    baseIterator.foreach{iter => {
      assert(!(events contains iter))
      events(iter) = true
    }}

    assert(events.size == 16384 /* 4^7 */)
  }
}
