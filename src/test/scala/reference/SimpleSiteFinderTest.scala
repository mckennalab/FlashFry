package reference

import crispr.GuideStorage
import org.scalatest.{FlatSpec, Matchers}
import crispr.filter.SequencePreFilter
import standards.{Cas9ParameterPack, Cpf1ParameterPack}

/**
  * Created by aaronmck on 3/9/17.
  */
class SimpleSiteFinderTest extends FlatSpec with Matchers {
  "SimpleSiteFinder" should "find a Cas9 pam correctly" in {
    val string = "ATTTA AAAAA CCCCC AAAAA GGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9ParameterPack,Array[SequencePreFilter](),0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }
}
