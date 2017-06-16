package utils

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by aaronmck on 3/15/17.
  */
class UtilsTest extends FlatSpec with Matchers {

  "Utils" should "should calculate GC content correctly" in {
    (Utils.gcContent("AACC")) should be (0.5)
    (Utils.gcContent("aaCC")) should be (0.5)
    (Utils.gcContent("CaCC")) should be (0.75)
    (Utils.gcContent("GaCC")) should be (0.75)
    (Utils.gcContent("AaCt")) should be (0.25)
    (Utils.gcContent("aaaa")) should be (0.00)
    (Utils.gcContent("GGGG")) should be (1.00)
  }

  "Utils" should "Covert a long buffer to a byte array and back again successfully" in {
    val longArray = Array[Long](21394812034120l,1l,80808080l,1234234234234l)
    val bytes     = Utils.longArrayToByteArray(longArray)
    val newLongs  = Utils.byteArrayToLong(bytes)

    (newLongs(0)) should be (longArray(0))
    (newLongs(1)) should be (longArray(1))
    (newLongs(3)) should be (longArray(3))
    (newLongs(1)) should not be (longArray(3))
  }

  "Utils" should "should covert a long to a bit string correctly" in {
    val longValue = 0xABCDEFABCDEFABCDl
    val expectedBitString = "1010 1011 1100 1101 1110 1111 1010 1011 1100 1101 1110 1111 1010 1011 1100 1101"

    (Utils.longToBitString(longValue)) should be (expectedBitString)
  }

  "Utils" should "should convert a long array to a byte array correctly" in {
    val longValueArray = Array[Long](0x0BCDEFABCDEFABCDl,0x0l,0x1l)

    val byteArray = Utils.longArrayToByteArray(longValueArray)

    (byteArray.size) should be (8 * 3)
    (byteArray(7))   should be (0x0B)
    (byteArray(16))  should be (0x1)
  }

  "Utils" should "should convert a byte array into a long array" in {
    val longValueArray = Array[Long](0xABCDEFABCDEFABCDl,0x0l,0x1l)

    val byteArray = Utils.longArrayToByteArray(longValueArray)
    val longAgain = Utils.byteArrayToLong(byteArray)

    (longAgain.size) should be (3)
    (longAgain(0))   should be (0xABCDEFABCDEFABCDl)
    (longAgain(1))   should be (0x0l)
    (longAgain(2))  should be (0x1l)
  }

  "Utils" should "find the longest homopolymer run correctly" in {
    val seq1 = Utils.longestHomopolymerRun("AAAAATTCC")
    val seq2 = Utils.longestHomopolymerRun("ACTGACGT")
    val seq3 = Utils.longestHomopolymerRun("AACCTTGG")
    val seq4 = Utils.longestHomopolymerRun("AATTTTTGG")

    (seq1) should be (5)
    (seq2) should be (1)
    (seq3) should be (2)
    (seq4) should be (5)
  }

  "Utils" should "find the entropy correctly" in {
    val seq1 = Utils.sequenceEntropy("AAAAATTCC")

    (seq1) should be (1.435521 +- 0.01)
  }

  "Utils" should "reverse comp something correctly" in {
    val seq1 = Utils.reverseCompString("AG")

    (seq1) should be ("CT")
  }
}

