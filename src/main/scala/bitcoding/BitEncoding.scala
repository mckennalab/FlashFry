package bitcoding

import java.lang.{Long => JavaLong}
import java.math.BigInteger

import main.scala.util.Utils
import standards.ParameterPack

import scala.annotation.switch

/**
  * given a type of CRISPR target search, setup a bit mask long to store sequences
  * @param parameterPack the parameter pack
  */
class BitEncoding(parameterPack: ParameterPack) {

  val mParameterPack = parameterPack

  /**
    * encode our target string and count into a 64-bit Long
    * @param strEncoding the string and count to encode
    * @return the Long encoding of this string
    */
  def bitEncodeString(strEncoding: StringCount): Long = {
    require(strEncoding.str.size <= 24)

    var encoding: Long = 0l

    strEncoding.str.toUpperCase.foreach { ch => {
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

  /**
    * decode the string and count into an object
    * @param encoding the encoding as a long
    * @return an object representation
    */
  def bitDecodeString(encoding: Long, actualSize: Int = mParameterPack.totalScanLength): StringCount = {
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
    * add a counter value this this long's encoding
    * @param encodedTarget the currently encoded target
    * @param count the count to add
    * @return
    */
  def updateCount(encodedTarget: Long, count: Short): Long = {
    // now shift the counts to the top of the 64 bit encoding
    (encodedTarget & BitEncoding.stringMask) | ((count.toLong << 48))
  }

  // return the current count of a long-encoded string
  def getCount(encoding: Long): Short = (encoding >> 48).toShort


  /**
    * return the number of mismatches between two strings (encoded in longs), given both our set comparison mask
    * as well as an additional mask (optional)
    *
    * TODO: I wish we could use the underlying POPCNT of the system for speed but we need two bit bit tests,
    * probably a popcnt (original & original >> 1) could be faster
    *
    * @param encoding1 the first string encoded as a long
    * @param encoding2 the second string
    * @param additionalMask consider only the specified bases
    * @return their differences, as a number of bases
    */
  def mismatches(encoding1: Long, encoding2: Long, additionalMask: Long = BitEncoding.stringMask): Int = {
    val xORed = ((encoding1 & mParameterPack.comparisonBitEncoding) ^ (encoding2 & mParameterPack.comparisonBitEncoding)) & additionalMask
    var diff = 0

    // this is 5X faster than a foreach lookup
    var index = 0
    while (index < BitEncoding.stringLimit) {
      if (((xORed >> (index * 2)) & 0x3) > 0) diff += 1
      index += 1
    }
    diff
  }

  /**
    * given a bin -- a subsequence we've used for binning targets, find the number of mismatches
    * between this bin and the guide of interest. Count values are not considered
    *
    * @param bin the bin sequence as a string
    * @param guide the guide sequences
    * @return a number of mismatches
    */
  def mismatchBin(bin: String, guide: Long): Int = {
    // bit shift the bin to the correct location and return the mismatch count
    val longBin = bitEncodeString(StringCount(bin,1))

    if (parameterPack.fivePrimePam) {
      // the bin starts after the pam -- make space for it
      val shiftAmount = ((parameterPack.totalScanLength - (bin.size - 1)) - parameterPack.pam.size) * 2

      mismatches(longBin << shiftAmount, (guide & BitEncoding.stringMask) & (BitEncoding.stringMask << shiftAmount))
    } else {
      val shiftAmount = ((parameterPack.totalScanLength - (bin.size)) * 2)
      mismatches(longBin << shiftAmount, (guide & BitEncoding.stringMask) & (BitEncoding.stringMask << shiftAmount) )
    }
  }

}

/**
  * handle encoding and decoding strings into packed bit vectors (currently a long -- 64 bits)
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

/**
  * a case class for passing target strings and their counts back and forth
  * @param str the base string
  * @param count it's count
  */
case class StringCount(str: String, count: Short) {
  require (str.size <= BitEncoding.stringLimit, {throw new IllegalStateException("string size is too large for encoding (limit " + BitEncoding.stringLimit + "), size is: " + str.size)})
  require (count >= 1, {throw new IllegalStateException("the count for a string should be greater than 0, not " + count)})
}

