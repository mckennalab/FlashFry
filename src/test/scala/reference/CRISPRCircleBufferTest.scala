package reference

import crispr.GuideStorage
import main.scala.util.Utils
import models.Doench2014OnTarget
import org.scalatest.{FlatSpec, Matchers}
import crispr.filter.SequencePreFilter
import standards.{Cas9ParameterPack, Cpf1ParameterPack}

/**
  * Created by aaronmck on 2/10/17.
  */
class CRISPRCircleBufferTest extends FlatSpec with Matchers {
  "CRISPRCircleTest" should "find a cpf1 pam correctly" in {
    val string = "TTTA AAAAA CCCCC GGGGG TTTTA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircleBuffer(guideStore,Cpf1ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "CRISPRCircleTest" should "find a reverse COMP cpf1 pam correctly" in {
    val string = "AAAAA CCCCC GGGGG TTTTA CAAA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircleBuffer(guideStore,Cpf1ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (Utils.reverseCompString(string))
  }

  "CRISPRCircleTest" should "find a cas9 pam correctly" in {
    val string = "AAAAA CCCCC GGGGG TTTTA CGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircleBuffer(guideStore,Cas9ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "CRISPRCircleTest" should "find a reverse COMP cas9 pam correctly" in {
    val string = "CCA AAAAA CCCCC GGGGG TTTTA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircleBuffer(guideStore,Cas9ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (Utils.reverseCompString(string))
  }
}
