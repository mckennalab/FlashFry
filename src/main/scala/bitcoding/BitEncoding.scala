package bitcoding

import java.lang.{Long => JavaLong}

import standards.ParameterPack

import scala.annotation.switch

/**
  * handle encoding and decoding strings into packed bit vectors
  */

object BitEncoding {

  val encodeA = 0x0
  val encodeC = 0x1
  val encodeG = 0x2
  val encodeT = 0x3

  val characterMask = 0x3

  val stringLimit = 24

  val stringMask =         0xFFFFFFFFFFFFl

  val stringMaskHighBits = 0xAAAAAAAAAAAAl
  val stringMaskLowBits =  0x555555555555l


}

class BitEncoding(parameterPack: ParameterPack) {
  val encodingLen = parameterPack.totalScanLength
  val compMask = parameterPack.comparisonBitEncoding & BitEncoding.stringMask

  /**
    * encode our target string and count into a 64-bit Long
    * @param strEncoding the string and count to encode
    * @return the Long encoding of this string
    */
  def bitEncodeString(strEncoding: StringCount): Long = {
    var encoding: Long = 0l

    strEncoding.str.toUpperCase().foreach { ch => {
      encoding = encoding << 2

      (ch: @switch) match {
        case 'A' => encoding = BitEncoding.encodeA | encoding
        case 'C' => encoding = BitEncoding.encodeC | encoding
        case 'G' => encoding = BitEncoding.encodeG | encoding
        case 'T' => encoding = BitEncoding.encodeT | encoding
        case _ => throw new IllegalStateException("Unable to encode character " + ch)
      }
    }}

    // now shift the counts to the top of the 64 bit encoding
    encoding | ((strEncoding.count.toLong << 48))
  }

  def updateCount(encodedString: Long, count: Short): Long = {
    // now shift the counts to the top of the 64 bit encoding
    (encodedString & BitEncoding.stringMask) | ((count.toLong << 48))
  }

  def getCount(encoding: Long): Short = (encoding >> 48).toShort

  /**
    * decode the string and count into an object
    * @param encoding the encoding as a long
    * @return an object representation
    */
  def bitDecodeString(encoding: Long, actualSize: Int = encodingLen): StringCount = {
    val stringEncoding = new Array[Char](actualSize)
    val count: Short = (encoding >> 48).toShort

    (0 until actualSize).foreach{index => {
      (0x3 & (encoding >> (index * 2))) match {
        case BitEncoding.encodeA => stringEncoding(index) = 'A'
        case BitEncoding.encodeC => stringEncoding(index) = 'C'
        case BitEncoding.encodeG => stringEncoding(index) = 'G'
        case BitEncoding.encodeT => stringEncoding(index) = 'T'
      }
    }}
    StringCount(stringEncoding.mkString("").reverse,count)
  }

  /**
    * return the number of mismatches between two strings, encoded as long values, given our set comparison mask --
    * I wish we could use the underlying POPCNT of the system for speed but we need two bit bit tests
    *
    * @param encoding1 the first string encoded as a long
    * @param encoding2 the second string
    * @return their differences, as a number of bases
    */
  def mismatches(encoding1: Long, encoding2: Long): Int = {
    val xORed = (encoding1 & compMask) ^ (encoding2 & compMask) // ^ is XOR
    var diff = 0

    // this is 5X faster than a foreach lookp
    var index = 0
    while (index < BitEncoding.stringLimit) {
      if (((xORed >> (index * 2)) & 0x3) > 0) diff += 1
      index += 1
    }
    diff
  }
}


case class StringCount(str: String, count: Short) {
  require (str.size <= BitEncoding.stringLimit, {throw new IllegalStateException("string size is too large for encoding (limit " + BitEncoding.stringLimit + "), size is: " + str.size)})
  require (count > 0, {throw new IllegalStateException("the count for a string should be greater than 0, not " + count)})
}