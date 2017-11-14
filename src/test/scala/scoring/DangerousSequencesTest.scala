package scoring

import bitcoding.{BitEncoding, StringCount}
import crispr.{CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cas9ParameterPack, Cpf1ParameterPack}

import scala.util.Random

class DangerousSequencesTest extends FlatSpec with Matchers {

  val rando = new Random()


  "DangerousSequences" should "catch polyT sequences" in {
    val parameterPack = Cas9ParameterPack
    val encodeDevice = new BitEncoding(parameterPack)

    val danger = new DangerousSequences()
    danger.bitEncoder(encodeDevice)

    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter { c => c != ' ' }.mkString(""), 1000)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val tgt = new CRISPRSiteOT(CRISPRSite("test",strCount.str,true,0,None),encoding,1000)

    val eval = danger.scoreGuide(tgt)
    (eval.map{t => t.mkString("")}.mkString(",")) should be("NONE,PolyT,NONE")
  }


  "DangerousSequences" should "not include polyT sequences that rely on the pam" in {
    val parameterPack = Cas9ParameterPack
    val encodeDevice = new BitEncoding(parameterPack)

    val danger = new DangerousSequences()
    danger.bitEncoder(encodeDevice)

    val strCount = StringCount("AAAAA CCCCC GGGGG TATTT TGG".filter { c => c != ' ' }.mkString(""), 1000)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val tgt = new CRISPRSiteOT(CRISPRSite("test",strCount.str,true,0,None),encoding,1000)

    val eval = danger.scoreGuide(tgt)
    (eval.map{t => t.mkString("")}.mkString(",")) should be("NONE,NONE,NONE")
  }

  "DangerousSequences" should "catch polyT sequences (cpf1)" in {
    val parameterPack = Cpf1ParameterPack
    val encodeDevice = new BitEncoding(parameterPack)

    val danger = new DangerousSequences()
    danger.bitEncoder(encodeDevice)

    val strCount = StringCount("TTTG AAAAA CCCCC GGGGG TTTTA".filter { c => c != ' ' }.mkString(""), 1000)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val tgt = new CRISPRSiteOT(CRISPRSite("test",strCount.str,true,0,None),encoding,1000)

    val eval = danger.scoreGuide(tgt)
    (eval.map{t => t.mkString("")}.mkString(",")) should be("NONE,PolyT,NONE")
  }


  "DangerousSequences" should "not include polyT sequences that rely on the pam (cpf1)" in {
    val parameterPack = Cpf1ParameterPack
    val encodeDevice = new BitEncoding(parameterPack)

    val danger = new DangerousSequences()
    danger.bitEncoder(encodeDevice)
    
    val strCount = StringCount("TTTT CCCCC GGGGG TATTT".filter { c => c != ' ' }.mkString(""), 1000)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val tgt = new CRISPRSiteOT(CRISPRSite("test",strCount.str,true,0,None),encoding,1000)

    val eval = danger.scoreGuide(tgt)
    (eval.map{t => t.mkString("")}.mkString(",")) should be("NONE,NONE,NONE")
  }
}