package targetio

import bitcoding.{BitEncoding, BitPosition}
import crispr.{CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9ParameterPack
import java.io.File

import scoring.ScoreModel

class TabDelimitedHanderTest extends FlatSpec with Matchers {
  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("fakeChrom", "GACTTGCATCCGAAGCCGGTGGG", true, 150, None)
  val posEnc = new BitPosition()
  TabDelimitedHanderTest.hg19ChromeOrder.foreach{t => {posEnc.addReference(t)}}

  "TabDelimitedHander" should "regex a position correctly" in {

    val ot = new CRISPRSiteOT(target, 0L, 100, false)
    "AAAACCAGGCCAGCTGACAGTGG_1_4<1:55796894^R>"

    val positions = TabDelimitedOutput.extractPositionBlock.
      findAllIn("AAAACCAGGCCAGCTGACAGTGG_1_4{SCORE=VALUE}<1:55796894^R>").group(1)

    (positions) should be ("1:55796894^R")
  }

  "TabDelimitedHander" should "regex a score correctly" in {

    val ot = new CRISPRSiteOT(target, 0L, 100, false)
    "AAAACCAGGCCAGCTGACAGTGG_1_4<1:55796894^R>"

    val positions = TabDelimitedOutput.extractScoreBlock.
      findAllIn("AAAACCAGGCCAGCTGACAGTGG_1_4{SCORE=VALUE}<1:55796894^R>").group(1).split("=")

    (positions(0)) should be ("SCORE")
    (positions(1)) should be ("VALUE")
  }

  "TabDelimitedHander" should "read the same file it just wrote" in {
    val inputFile = new File("test_data/fake.sites")
    val outputTemp = new File("test_data/fake.sites_temp")
    //outputTemp.deleteOnExit()

    val input = new TabDelimitedInput(inputFile,bitEncoder,posEnc,4, false, true)
    val output = new TabDelimitedOutput(outputTemp,bitEncoder,posEnc,Array[ScoreModel](),true,true)

    input.guides.foreach{g => output.write(g)}
    output.close()
    (computeHash(inputFile.getAbsolutePath)) should be (computeHash(outputTemp.getAbsolutePath))
  }

  // from https://stackoverflow.com/questions/41642595/scala-file-hashing
  import java.security.{MessageDigest, DigestInputStream}
  import java.io.{File, FileInputStream}

  // Compute a hash of a file
  // The output of this function should match the output of running "md5 -q <file>"
  def computeHash(path: String): String = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(new File(path)), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }


}
object TabDelimitedHanderTest {
  val hg19ChromeOrder = Array[String]("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17",
    "18","19","20","21","22","X","Y","MT","GL000207.1","GL000226.1","GL000229.1","GL000231.1","GL000210.1","GL000239.1",
    "GL000235.1","GL000201.1","GL000247.1","GL000245.1","GL000197.1","GL000203.1","GL000246.1","GL000249.1","GL000196.1",
    "GL000248.1","GL000244.1","GL000238.1","GL000202.1","GL000234.1","GL000232.1","GL000206.1","GL000240.1","GL000236.1",
    "GL000241.1","GL000243.1","GL000242.1","GL000230.1","GL000237.1","GL000233.1","GL000204.1","GL000198.1","GL000208.1",
    "GL000191.1","GL000227.1","GL000228.1","GL000214.1","GL000221.1","GL000209.1","GL000218.1","GL000220.1","GL000213.1",
    "GL000211.1","GL000199.1","GL000217.1","GL000216.1","GL000215.1","GL000205.1","GL000219.1","GL000224.1","GL000223.1",
    "GL000195.1","GL000212.1","GL000222.1","GL000200.1","GL000193.1","GL000194.1","GL000225.1","GL000192.1","NC_007605")
}
