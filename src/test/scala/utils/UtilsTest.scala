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
}

