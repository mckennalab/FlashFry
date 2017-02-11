package reference

import crispr.models.OnTarget
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import reference.gprocess.GuideStorage
import standards.StandardScanParameters

/**
  * Created by aaronmck on 2/10/17.
  */
class CRISPRCircleTest extends FlatSpec with ShouldMatchers {
  "CRISPRCircleTest" should "find a cpf1 pam correctly" in {
    val string = "TTTA AAAAA CCCCC GGGGG TTTTA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircle(guideStore,StandardScanParameters.cpf1ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "CRISPRCircleTest" should "find a reverse COMP cpf1 pam correctly" in {
    val string = "AAAAA CCCCC GGGGG TTTTA CAAA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircle(guideStore,StandardScanParameters.cpf1ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (circ.reverseCompString(string))
  }

  "CRISPRCircleTest" should "find a cas9 pam correctly" in {
    val string = "AAAAA CCCCC GGGGG TTTTA CGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircle(guideStore,StandardScanParameters.cas9ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "CRISPRCircleTest" should "find a reverse COMP cas9 pam correctly" in {
    val string = "CCA AAAAA CCCCC GGGGG TTTTA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = CRISPRCircle(guideStore,StandardScanParameters.cas9ParameterPack)

    circ.addLine(string)
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (circ.reverseCompString(string))
  }
}
