package reference

import crispr.GuideMemoryStorage
import org.scalatest.{FlatSpec, Matchers}
import standards._
import utils.Utils

/**
  * Created by aaronmck on 3/9/17.
  */
class SimpleSiteFinderTest extends FlatSpec with Matchers {

  "SimpleSiteFinder" should "find a NGG Cas9 site correctly" in {
    val string = "ATTTA AAAAA CCCCC AAAAA GGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)

    (guideStore.guideHits(0).sequenceContext.isDefined) should be (true)
  }

  "SimpleSiteFinder" should "find context correctly" in {
    val guideStr = "ATTTA AAAAA TTTTT AAAAA AGG".filter{c => c != ' '}.mkString("")
    val string = "ATA ATATA ATTTA AAAAA TTTTT AAAAA AGG AATTA AAT".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,8)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (guideStr)
    (guideStore.guideHits(0).sequenceContext.get) should be (string)
    (guideStore.guideHits(0).start) should be (8)
  }

  "SimpleSiteFinder" should "find a RC NGG Cas9 site correctly" in {
    val string = "CCTTA AAAAA CCCCC AAAAA AAA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (Utils.reverseCompString(string))
  }

  "SimpleSiteFinder" should "find two Cas9 sites together" in {

    // there are two sites here now -- one that starts with AA, and one that starts with AT
    val string = "A ATTTA AAAAA CCCCC AAAAA GGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (2)
    (guideStore.guideHits(0).bases) should be (string.slice(0,23))
    (guideStore.guideHits(1).bases) should be (string.slice(1,24))
  }

  "SimpleSiteFinder" should "find a NAG Cas9 site correctly" in {
    val string = "ATTTA AAAAA CCCCC AAAAA GAG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NAGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "SimpleSiteFinder" should "find a RC NAG Cas9 site correctly" in {
    val string = "CTTTA AAAAA CCCCC AAAAA AAA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NAGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (Utils.reverseCompString(string))
  }
  "SimpleSiteFinder" should "find two Cas9 N*G sites together" in {

    // there are two sites here now -- one that starts with AA, and one that starts with AT
    val string = "A ATTTA AAAAA CCCCC AAAAA AGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9ParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (2)
    (guideStore.guideHits(0).bases) should be (string.slice(0,23))
    (guideStore.guideHits(1).bases) should be (string.slice(1,24))
  }

  "SimpleSiteFinder" should "find a RC Cpf1 correctly" in {
    val string = "AAATA AAAAA CCCCC AAAAA GGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).bases) should be (string)
  }

  "SimpleSiteFinder" should "find two Cpf1 sites together" in {

    // there are two sites here now -- one that starts with AA, and one that starts with AT
    val string = "TTTTA ATTTA AAAAA CCCCC AATTT".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cpf1ParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (2)
    (guideStore.guideHits(0).bases) should be (string.slice(0,24))
    (guideStore.guideHits(1).bases) should be (string.slice(1,25))
  }

  "SimpleSiteFinder" should "find two RC Cpf1 sites together" in {

    // there are two sites here now -- one that starts with AA, and one that starts with AT
    val string = "TAATA ATTTA AAAAA CCCCC AAAAA".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cpf1ParameterPack,0)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (2)
    (guideStore.guideHits(0).bases) should be (Utils.reverseCompString(string.slice(0,24)))
    (guideStore.guideHits(1).bases) should be (Utils.reverseCompString(string.slice(1,25)))
  }


  "SimpleSiteFinder" should "not return content for a target without the required content" in {
    val string = "ATTTA AAAAA CCCCC AAAAA GGG".filter{c => c != ' '}.mkString("")

    val guideStore = new GuideMemoryStorage()
    val circ = SimpleSiteFinder(guideStore,Cas9NGGParameterPack,1)
    circ.reset("testContig")

    circ.addLine(string)
    circ.close()
    (guideStore.guideHits.size) should be (1)
    (guideStore.guideHits(0).sequenceContext.isDefined) should be (false)


  }

}
