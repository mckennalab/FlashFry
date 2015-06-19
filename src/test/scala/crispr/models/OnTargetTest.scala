package crispr.models

import main.scala.CRISPROnTarget
import main.scala.trie.CRISPRPrefixMap
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.matchers.ShouldMatchers

/**
 * Created by aaronmck on 6/18/15.
 */
class OnTargetTest extends FlatSpec with ShouldMatchers {
  "OnTargetTest" should "score on-target scores correctly" in {
    val ontarget = new OnTarget()

    ontarget.calc_score("TCTTAAGCAGAACAAGGGCA")shouldEqual 0.08621383652119444 +- 0.001

  }
}
