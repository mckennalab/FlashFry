package bitcoding

import java.lang.{Integer => JavaInteger}

import utils.Utils
import org.scalatest._
import standards.{Cas9NGGParameterPack, Cas9ParameterPack, Cpf1ParameterPack, ParameterPack}
import standards.ParameterPack._

import scala.util.Random

/**
  * test that we can encode and decode string and counts to a binary Long
  */
class BitEncodingTest extends FlatSpec with Matchers {

  val rando = new Random()
  val parameterPack = Cas9ParameterPack

  "A Bit Encoder" should "correctly encode and decode a simple guide and count" in {
    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should be (decoding.str)
    (strCount.count) should be (decoding.count)
  }

  "A Bit Encoder" should "should reject a mismatch" in {
    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should not be ("AAAAA CCCCC GGGGG TTTTT GGG".filter{c => c != ' '}.mkString(""))
    (strCount.count) should be (decoding.count)
  }

  "A Bit Encoder" should "should reject a count difference" in {
    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should be (decoding.str)
    (strCount.count) should not be (1001)
  }

  "A Bit Encoder" should "should successfully encode and decode a million random base strings and counts" in {
    val encodeDevice = new BitEncoding(parameterPack)

    (0 until 100000).foreach{index => {
      val strCount = randomStringCount(parameterPack.totalScanLength)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val decoding = encodeDevice.bitDecodeString(encoding)
      (strCount.str) should be (decoding.str)
      (strCount.count) should be (decoding.count)
    }}
  }

  def randomStringCount(sz: Int, prefix: String = "", suffix: String = ""): StringCount = {
    StringCount(prefix + randomBaseString(sz - (prefix.size + suffix.size)) + suffix,(rando.nextInt(Short.MaxValue - 1) + 1).toShort)
  }

  def randomBaseString(len: Int): String = {
    (0 until len).map{ln => rando.nextInt(4) match {
      case 0 => 'A'
      case 1 => 'C'
      case 2 => 'G'
      case 3 => 'T'
    }}.mkString("")
  }

  "A Bit Encoder" should "should compare two identical strings as the same" in {
    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val difference = encodeDevice.mismatches(encoding, encoding)
    (difference) should be (0)
  }

  "A Bit Encoder" should "should compare two identical strings as the same, even when their counts are different" in {

    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val strCount2 = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (0)
  }

  "A Bit Encoder" should "should compare two different strings as different by one base" in {

    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val strCount2 = StringCount("TAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (1)
  }

  "A Bit Encoder" should "should compare two different strings as different by all bases that actually get compared with the set mask" in {

    val strCount =  StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1000)
    val strCount2 = StringCount("TTTTT TTTTT AAAAA GGGGG GGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (20)
  }

  "A Bit Encoder" should "should compare two different strings as different by five bases even when the pam degenerate base would add a 6th mismatch" in {

    val strCount =  StringCount("AAAAA CCCCC GGGGG AAAAT AGG".filter{c => c != ' '}.mkString(""),1000)
    val strCount2 = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (5)
  }

  "A Bit Encoder" should "should compare two different strings as different by five bases even when the second pam base is degenerate" in {

    val strCount =  StringCount("AAAAA CCCCC GGGGG AAAAT AAG".filter{c => c != ' '}.mkString(""),1000)
    val strCount2 = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (5)
  }

  "A Bit Encoder" should "compare lots of differences correctly and quickly" in {
    val encodeDevice = new BitEncoding(parameterPack)

    (0 until 10000).foreach{index => {
      val strCount = randomStringCount(parameterPack.totalScanLength)
      val strCount2 = randomStringCount(parameterPack.totalScanLength)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val actualDifference = strCount.str.slice(0,20).zip(strCount2.str.slice(0,20)).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum
      val difference = encodeDevice.mismatches(encoding, encoding2)
      (actualDifference) should be (difference)
    }}
  }

  "A Bit Encoder" should "compare lots of differences correctly and quickly for Cpf1" in {
    val encodeDevice = new BitEncoding(Cpf1ParameterPack)

    (0 until 10000).foreach{index => {
      val strCount = randomStringCount(Cpf1ParameterPack.totalScanLength, "TTTT")
      val strCount2 = randomStringCount(Cpf1ParameterPack.totalScanLength, "TTTT")

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val actualDifference = strCount.str.slice(4,24).zip(strCount2.str.slice(4,24)).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum
      val difference = encodeDevice.mismatches(encoding, encoding2)
      (actualDifference) should be (difference)
    }}
  }


  "A Bit Encoder" should "compare lots of differences correctly and quickly for Cpf1, regardless of their pam" in {
    val encodeDevice = new BitEncoding(Cpf1ParameterPack)

    (0 until 10000).foreach{index => {
      val strCount = randomStringCount(Cpf1ParameterPack.totalScanLength)
      val strCount2 = randomStringCount(Cpf1ParameterPack.totalScanLength)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val actualDifference = strCount.str.slice(4,24).zip(strCount2.str.slice(4,24)).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum
      val difference = encodeDevice.mismatches(encoding, encoding2)
      (actualDifference) should be (difference)
    }}
  }

  "A Bit Encoder" should "compare lots of differences quickly" in {
    val encodeDevice = new BitEncoding(parameterPack)

    val t0 = System.nanoTime()
    (0 until 100000).foreach{index => {
      val strCount = randomStringCount(parameterPack.totalScanLength)
      val strCount2 = randomStringCount(parameterPack.totalScanLength)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val difference = encodeDevice.mismatches(encoding, encoding2)
    }}
    val t1 = System.nanoTime()
    println("Elapsed time for 100k comparisons (with difference checks): " + ((t1 - t0)/1000000000.0) + " seconds")
  }

  "A Bit Encoder" should "compare 1 million of the same string quickly" in {
    val encodeDevice = new BitEncoding(parameterPack)
    val strCount = randomStringCount(parameterPack.totalScanLength)
    val strCount2 = randomStringCount(parameterPack.totalScanLength)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)

    val t0 = System.nanoTime()
    (0 until 1000000).foreach{index => {

      val difference = encodeDevice.mismatches(encoding, encoding2)
    }}
    val t1 = System.nanoTime()
    println("Elapsed time for 1M comparisons: " + ((t1 - t0)/1000000000.0) + " seconds")
  }

  "A Bit Encoder" should "compare a bin correctly to a guide that's a perfect match" in {

    val strCount = StringCount("AAAAA CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1)
    val bin = "AAAAA"

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    println(strCount + " " + Utils.longToBitString(encoding))

    val binMask = encodeDevice.binToLongComparitor(bin)
    val differences = encodeDevice.mismatchBin(binMask,encoding)

    (differences) should be (0)
  }

  "A Bit Encoder" should "compare a bin correctly to a guide that's non-perfect match" in {

    val strCount = StringCount("TTAAT CCCCC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1)
    val bin = "TTTTT"

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val binMask = encodeDevice.binToLongComparitor(bin)
    val differences = encodeDevice.mismatchBin(binMask,encoding)

    (differences) should be (2)
  }

  "A Bit Encoder" should "find a perfect bin match" in {

    val strCount = StringCount("AAAAA AAAAC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1)
    val bin = "AAAAAAAAA"

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val binMask = encodeDevice.binToLongComparitor(bin)
    val differences = encodeDevice.mismatchBin(binMask,encoding)

    (differences) should be (0)
  }

  "A Bit Encoder" should "calculate distances from two bins with a guide correctly" in {

    val strCount = StringCount("AAAAA AAAAC GGGGG TTTTA GGG".filter{c => c != ' '}.mkString(""),1)
    val bin = "AAAAAAAAA"
    val subBin = "CAG"

    val encodeDevice = new BitEncoding(parameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val binMask = encodeDevice.binToLongComparitor(bin)
    val differences = encodeDevice.mismatchBin(binMask,encoding)

    (differences) should be (0)
  }


  "A Bit Encoder" should "calculate distances between guides correctly" in {

    val strCount =  StringCount("GAGTC CGAGC AGAAG AAGAA GGG".filter{c => c != ' '}.mkString(""),1)
    val strCount2 = StringCount("GAATC ATAGC AGAAG ATGAA AGG".filter{c => c != ' '}.mkString(""),1001)

    val encodeDevice = new BitEncoding(Cas9NGGParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (4)
  }


  "A Bit Encoder" should "calculate a bin mismatch correctly" in {

    val strCount =  StringCount("GAGTC CGAGC AGAAG AAGAA GGG".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cas9NGGParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("GAGTCCG"), encoding)
    (difference) should be (0)
  }

  "A Bit Encoder" should "calculate a bin mismatch correctly for five-prime guides/targets (cpf1), where it drops the PAM" in {

    val strCount =  StringCount("TTTT CGAGC AGAAG AAGAA GGGAC".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cpf1ParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    var difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("CGAGCAG"), encoding)
    (difference) should be (0)

    difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("CAAGCAG"), encoding)
    (difference) should be (1)

    difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("AGAGCAA"), encoding)
    (difference) should be (2)
  }

  "A Bit Encoder" should "calculate another bin mismatch correctly" in {

    val strCount =  StringCount("GGCTC CGAGC AGAAG AAGAA GGG".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cas9NGGParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("GAGTCCG"), encoding)
    (difference) should be (2)
  }


  "A Bit Encoder" should "calculate very distant bin mismatch correctly" in {

    val strCount =  StringCount("GGCTC CGAGC AGAAG AAGAA GGG".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cas9NGGParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val difference = encodeDevice.mismatchBin(encodeDevice.binToLongComparitor("AAAAAAA"), encoding)
    (difference) should be (7)
  }

  "A Bit Encoder" should "filter a string by a comparison function correctly" in {

    val strCount =  StringCount("GGCTC CGAGC AGAAG AAGAA GGG".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cas9NGGParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val justTarget = encodeDevice.bitDecodeString(encoding).toStr.slice(Cas9NGGParameterPack.guideRange._1,Cas9NGGParameterPack.guideRange._2)

    (justTarget) should be ("GGCTCCGAGCAGAAGAAGAA")
  }

  "A Bit Encoder" should "filter a string by a comparison function correctly (cpf1)" in {

    val strCount =  StringCount("TTTG GGCTC CGAGC AGAAG AAGAA".filter{c => c != ' '}.mkString(""),1)

    val encodeDevice = new BitEncoding(Cpf1ParameterPack)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val justTarget = encodeDevice.bitDecodeString(encoding).toStr.slice(Cpf1ParameterPack.guideRange._1,Cpf1ParameterPack.guideRange._2)

    (justTarget) should be ("GGCTCCGAGCAGAAGAAGAA")
  }
}
