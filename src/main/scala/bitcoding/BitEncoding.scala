package bitcoding

import java.lang.{Long => JavaLong}
import java.math.BigInteger

import bitcoding.BitEncoding.TargetLong
import utils.Utils
import standards.ParameterPack

import scala.annotation.switch

/**
  * perform high-speed encoding and decoding of strings to longs, supporting comparison functions
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
    require(strEncoding.str.size <= 24, "String " + strEncoding.str + " is too long to be encoded (" + strEncoding.str.size  + " > 24)")
    require(strEncoding.count >= 1, "String count " + strEncoding.toStr + " has a count <= 0")

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
  def bitDecodeString(encoding: TargetLong, actualSize: Int = mParameterPack.totalScanLength): StringCount = {
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
  def updateCount(encodedTarget: TargetLong, count: Short): Long = {
    // now shift the counts to the top of the 64 bit encoding
    (encodedTarget & BitEncoding.stringMask) | ((count.toLong << 48))
  }

  // return the current count of a long-encoded string
  def getCount(encoding: TargetLong): Short = (encoding >> 48).toShort


  /**
    * return the number of mismatches between two strings (encoded in longs), given both our set comparison mask
    * as well as an additional mask (optional). This uses the Java bitCount, which on any modern platform should call out
    * to the underlying POPCNT instructios. With bitshifting it's about 20X faster than any loop you'll come up with
    *
    * @param encoding1 the first string encoded as a long
    * @param encoding2 the second string
    * @param additionalMask consider only the specified bases
    * @return their differences, as a number of bases
    */
  def mismatches(encoding1: TargetLong, encoding2: TargetLong, additionalMask: Long = BitEncoding.stringMask): Int = {
    BitEncoding.allComparisons += 1
    val firstComp = ((encoding1 ^ encoding2) & additionalMask & mParameterPack.comparisonBitEncoding)
    java.lang.Long.bitCount((firstComp & BitEncoding.upperBits) | ((firstComp << 1) & BitEncoding.upperBits))

  }

  /**
    * given a bin sequence -- a short sequence we've used for binning targets, find the number of mismatches
    * between this bin and the guide of interest. Count values are not considered
    *
    * @param bin the bin sequence as a string
    * @param guide the guide sequences
    * @return a number of mismatches
    */
  def mismatchBin(bin: BinAndMask, guide: TargetLong): Int = {
    mismatches(bin.binLong, (guide & bin.guideMask))
  }

  /**
    * given a bin -- a subsequence we've used for binning targets, find the number of mismatches
    * between this bin and the guide of interest. Count values are not considered
    *
    * @param bin the bin sequence as a string
    * @return a number of mismatches
    */
  def binToLongComparitor(bin: String): BinAndMask = {
    val longBin = bitEncodeString(StringCount(bin,1))
    val binSize = bin.size

    if (parameterPack.fivePrimePam) {
      // the bin starts after the pam -- make space for it
      val shiftAmount = ((parameterPack.totalScanLength - (binSize - 1)) - parameterPack.pam.size) * 2
      BinAndMask(bin, (longBin << shiftAmount) &  BitEncoding.stringMask,(BitEncoding.stringMask << shiftAmount))
    } else {
      val shiftAmount = ((parameterPack.totalScanLength - (binSize)) * 2)
      BinAndMask(bin, (longBin << shiftAmount) &  BitEncoding.stringMask,(BitEncoding.stringMask << shiftAmount))
    }
  }



  def compBitmaskForBin(binSize: Int): Long = {
    if (parameterPack.fivePrimePam) {
      val shiftAmount = ((parameterPack.totalScanLength - (binSize - 1)) - parameterPack.pam.size) * 2
      (BitEncoding.stringMask << shiftAmount)
    } else {
      val shiftAmount = ((parameterPack.totalScanLength - (binSize)) * 2)
      (BitEncoding.stringMask << shiftAmount)
    }
  }

}

/**
  * handle encoding and decoding strings into packed bit vectors (currently a long -- 64 bits)
  */
object BitEncoding {
  var allComparisons = 0l

  val encodeA = 0x0
  val encodeC = 0x1
  val encodeG = 0x2
  val encodeT = 0x3

  val characterMask = 0x3

  val stringLimit = 24

  val stringMask         = 0xFFFFFFFFFFFFl
  val upperBits          = 0xAAAAAAAAAAAAl
  val stringMaskHighBits = 0xAAAAAAAAAAAAl
  val stringMaskLowBits =  0x555555555555l

  type TargetLong = Long
}

/**
  * a case class for passing target strings and their counts back and forth
  * @param str the base string
  * @param count it's count
  */
case class StringCount(str: String, count: Short) {
  require (str.size <= BitEncoding.stringLimit, "string size is too large for encoding (limit " + BitEncoding.stringLimit + ", size is: " + str.size + " for string " + str + ")")
  require (count >= 1, "the count for a string should be greater than 0, not " + count + " for string " + str)
  def toStr: String = str + " - " + count
}


case class BinAndMask(bin: String, binLong: Long, guideMask: Long)
