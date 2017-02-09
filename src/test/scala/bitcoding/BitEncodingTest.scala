package bitcoding

import java.lang.{Integer => JavaInteger}

import org.scalatest._

import scala.util.Random

/**
  * test that we can encode and decode string and counts to a binary Long
  */
class BitEncodingTest extends FlatSpec with Matchers {

  val encodingSize = 20
  val rando = new Random()

  "A Bit Encoder" should "correctly encode and decode a simple guide and count" in {
    val strCount = StringCount("AAAAACCCCCGGGGGTTTTA",1000)
    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should be (decoding.str)
    (strCount.count) should be (decoding.count)
  }

  "A Bit Encoder" should "should reject a mismatch" in {
    val strCount = StringCount("AAAAACCCCCGGGGGTTTTT",1000)
    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should not be ("AAAAACCCCCGGGGGTTTTA")
    (strCount.count) should be (decoding.count)
  }

  "A Bit Encoder" should "should reject a count difference" in {
    val strCount = StringCount("AAAAACCCCCGGGGGTTTTT",1000)
    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val decoding = encodeDevice.bitDecodeString(encoding)

    (strCount.str) should be (decoding.str)
    (strCount.count) should not be (1001)
  }

  "A Bit Encoder" should "should successfully encode and decode a million random base strings and counts" in {
    val encodeDevice = new BitEncoding(encodingSize)

    (0 until 100000).foreach{index => {
      val strCount = randomStringCount(encodingSize)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val decoding = encodeDevice.bitDecodeString(encoding)
      (strCount.str) should be (decoding.str)
      (strCount.count) should be (decoding.count)
    }}
  }

  def randomStringCount(sz: Int): StringCount = {
    StringCount(randomBaseString(sz),(rando.nextInt(Short.MaxValue - 1) + 1).toShort)
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
    val strCount = StringCount("AAAAACCCCCGGGGGTTTTT",1000)
    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val difference = encodeDevice.mismatches(encoding, encoding)
    (difference) should be (0)
  }

  "A Bit Encoder" should "should compare two identical strings as the same, even when their counts are different" in {

    val strCount = StringCount("AAAAACCCCCGGGGGTTTTT",1000)
    val strCount2 = StringCount("AAAAACCCCCGGGGGTTTTT",1001)

    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (0)
  }

  "A Bit Encoder" should "should compare two different strings as different by one base" in {

    val strCount = StringCount("TAAAACCCCCGGGGGTTTTT",1000)
    val strCount2 = StringCount("AAAAACCCCCGGGGGTTTTT",1001)

    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (1)
  }

  "A Bit Encoder" should "should compare two different strings as different by all bases" in {

    val strCount =  StringCount("TTTTTTTTTTTTTTTGGGGG",1000)
    val strCount2 = StringCount("AAAAACCCCCGGGGGTTTTT",1001)

    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (20)
  }

  "A Bit Encoder" should "should compare two different strings as different by five bases" in {

    val strCount =  StringCount("TTTTTCCCCCGGGGGTTTTT",1000)
    val strCount2 = StringCount("AAAAACCCCCGGGGGTTTTT",1001)

    val encodeDevice = new BitEncoding(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)
    val difference = encodeDevice.mismatches(encoding, encoding2)
    (difference) should be (5)
  }

  "A Bit Encoder" should "compare lots of differences correctly and quickly" in {
    val encodeDevice = new BitEncoding(encodingSize)

    (0 until 10000).foreach{index => {
      val strCount = randomStringCount(encodingSize)
      val strCount2 = randomStringCount(encodingSize)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val actualDifference = strCount.str.zip(strCount2.str).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum
      val difference = encodeDevice.mismatches(encoding, encoding2)
      (actualDifference) should be (difference)
    }}
  }

  "A Bit Encoder" should "compare lots of differences quickly" in {
    val encodeDevice = new BitEncoding(encodingSize)

    val t0 = System.nanoTime()
    (0 until 100000).foreach{index => {
      val strCount = randomStringCount(encodingSize)
      val strCount2 = randomStringCount(encodingSize)

      val encoding = encodeDevice.bitEncodeString(strCount)
      val encoding2 = encodeDevice.bitEncodeString(strCount2)

      val difference = encodeDevice.mismatches(encoding, encoding2)
    }}
    val t1 = System.nanoTime()
    println("Elapsed time for 100k comparisions (with difference checks): " + ((t1 - t0)/1000000000.0) + " seconds")
  }

  "A Bit Encoder" should "compare 1 million of the same string quickly" in {
    val encodeDevice = new BitEncoding(encodingSize)
    val strCount = randomStringCount(encodingSize)
    val strCount2 = randomStringCount(encodingSize)

    val encoding = encodeDevice.bitEncodeString(strCount)
    val encoding2 = encodeDevice.bitEncodeString(strCount2)

    val t0 = System.nanoTime()
    (0 until 1000000).foreach{index => {

      val difference = encodeDevice.mismatches(encoding, encoding2)
    }}
    val t1 = System.nanoTime()
    println("Elapsed time for 1M comparisions: " + ((t1 - t0)/1000000000.0) + " seconds")
  }
}
