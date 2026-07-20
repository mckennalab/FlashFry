/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package bitcoding

import bitcoding.BitEncoding.TargetLong
import standards.ParameterPack

import scala.annotation.switch

/**
  * Two-bit target encoding and spacer-aware comparison operations for one nuclease profile.
  *
  * Bases are right-aligned in one `Long` (`A=00`, `C=01`, `G=10`, `T=11`). Targets up to 24 bases use the historical
  * packed codec, whose upper 16 bits contain an occurrence count. Targets of 25 through 32 bases use all 64 bits for
  * sequence and store occurrence counts in target records instead. Persisted data must therefore go through
  * `encodeTargetRecord` and `decodeTargetRecord`.
  *
  * @param parameterPack profile-backed targeting configuration
  */
class BitEncoding(parameterPack: ParameterPack) {

  val mParameterPack = parameterPack
  val targetCodec: TargetCodec = TargetCodec.forParameterPack(parameterPack)
  private val layout = parameterPack.profile.layout

  /**
    * Encode a DNA sequence and compatibility count into one `Long`.
    *
    * For the wide codec the returned value contains sequence only; the count is retained only when a full target record
    * is created.
    *
    * @param strEncoding the string and count to encode
    * @return two-bit sequence value, with an embedded count only under the legacy codec
    */
  def bitEncodeString(strEncoding: StringCount): Long = {
    require(strEncoding.str.size <= targetCodec.maximumBases, "String " + strEncoding.str + " is too long to be encoded (" + strEncoding.str.size + " > " + targetCodec.maximumBases + ")")
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
    }
    }

    targetCodec.addCount(encoding, strEncoding.count.toInt)
  }

  /**
    * Encode a DNA sequence with a compatibility count of one.
    *
    * @param str the string
    * @return the Long encoding of this string
    */
  def bitEncodeString(str: String): Long = {
    bitEncodeString(StringCount(str, 1))
  }

  /**
    * Decode a sequence value.
    *
    * Wide sequence values do not carry their record count, so the returned `StringCount` has count one. Use
    * `decodeTargetRecord` when the occurrence count is required.
    *
    * @param encoding the encoding as a long
    * @return decoded DNA string and embedded compatibility count
    */
  def bitDecodeString(encoding: TargetLong, actualSize: Int = mParameterPack.totalScanLength): StringCount = {
    val stringEncoding = new Array[Char](actualSize)
    val count = targetCodec.countFromSequence(encoding)

    (0 until actualSize).foreach { index => {
      (0x3 & (encoding >> (index * 2))) match {
        case BitEncoding.encodeA => stringEncoding(index) = 'A'
        case BitEncoding.encodeC => stringEncoding(index) = 'C'
        case BitEncoding.encodeG => stringEncoding(index) = 'G'
        case BitEncoding.encodeT => stringEncoding(index) = 'T'
      }
    }
    }
    StringCount(stringEncoding.mkString("").reverse, math.min(count, Short.MaxValue).toShort)
  }

  /**
    * Replace the embedded compatibility count under the legacy codec.
    *
    * The wide codec validates `count` and returns the unchanged sequence because its count belongs in the record.
    *
    * @param encodedTarget the currently encoded target
    * @param count         the count to add
    * @return updated packed value, or the unchanged wide sequence value
    */
  def updateCount(encodedTarget: TargetLong, count: Short): Long = {
    targetCodec.addCount(encodedTarget, count.toInt)
  }

  /** Return the embedded compatibility count; wide sequence values return one. */
  def getCount(encoding: TargetLong): Short = math.min(targetCodec.countFromSequence(encoding), Short.MaxValue).toShort

  /** Encode a persisted record containing the sequence, count, and genomic positions. */
  def encodeTargetRecord(sequence: TargetLong, positions: Array[Long]): Array[Long] =
    targetCodec.encodeRecord(sequence, positions)

  /** Decode a persisted target record at the supplied block offset. */
  def decodeTargetRecord(block: Array[Long], offset: Int): EncodedTargetRecord =
    targetCodec.decodeRecord(block, offset)


  /**
    * Count spacer mismatches between two encoded targets.
    *
    * PAM bits are always excluded by the profile's comparison mask. `additionalMask` can narrow the comparison further,
    * as indexed traversal does for bin prefixes. Occurrence-count bits are never compared.
    *
    * @param encoding1      the first string encoded as a long
    * @param encoding2      the second string
    * @param additionalMask consider only the specified bases
    * @return their differences, as a number of bases
    */
  def mismatches(encoding1: TargetLong, encoding2: TargetLong, additionalMask: Long = -1L): Int = {
    BitEncoding.allComparisons += 1
    val firstComp = ((encoding1 ^ encoding2) & additionalMask & layout.comparisonMask)
    java.lang.Long.bitCount((firstComp & BitEncoding.upperBits) | ((firstComp << 1) & BitEncoding.upperBits))

  }

  /**
    * Count mismatches between an encoded spacer bin and a target.
    *
    * @param bin   the bin sequence as a string
    * @param guide the guide sequences
    * @return a number of mismatches
    */
  def mismatchBin(bin: BinAndMask, guide: TargetLong): Int = {
    mismatches(bin.binLong, (guide & bin.guideMask))
  }

  /**
    * Align a spacer substring with its position in the full target encoding.
    *
    * @param bin              the bin sequence as a string
    * @param rightShiftXBases offset in spacer bases, not bits
    * @return a bin and mask object
    */
  def binToLongComparitor(bin: String, rightShiftXBases: Int = 0): BinAndMask = {
    val binLong = binShift(bin.size, bitEncodeString(bin), rightShiftXBases)

    BinAndMask(bin, binLong, compBitmaskForBin(bin.size, rightShiftXBases))
  }


  /**
    * Create a comparison mask for a spacer substring.
    *
    * @param binSize          the size of the bin
    * @param rightShiftXBases offset in spacer bases, not bits
    * @return a comparison mask for the bin of interest, which you can use to compare a bin to a target site
    */
  def compBitmaskForBin(binSize: Int, rightShiftXBases: Int = 0): Long = {
    require(binSize > 0 && binSize <= layout.spacerLength,
      "Bin size must be between 1 and spacer length " + layout.spacerLength)
    val bitCount = binSize * 2
    val base = if (bitCount == 64) -1L else (1L << bitCount) - 1L
    binShift(binSize, base, rightShiftXBases)
  }

  /**
    * Shift a right-aligned value into a spacer-relative position in the full target.
    *
    * @param binSize the size of the bin
    * @param base the base bit vector to shift into the correct place; we assume this is currently right-justified (lowest bits)
    * @param rightShiftXBases adjust to the right by X bases
    * @return shifted value restricted to the current target layout
    */
  def binShift(binSize: Int, base: Long, rightShiftXBases: Int = 0): Long = {
    require(rightShiftXBases >= 0 && rightShiftXBases + binSize <= layout.spacerLength,
      "Bin offset and width exceed spacer length " + layout.spacerLength)
    val targetStart = layout.spacerRange._1 + rightShiftXBases
    val shift = 2 * (layout.totalLength - (targetStart + binSize))
    (base << shift) & layout.fullSequenceMask
  }

}

/**
  * Constants for the two-bit DNA representation.
  */
object BitEncoding {
  var allComparisons = 0L

  val encodeA = 0x0
  val encodeC = 0x1
  val encodeG = 0x2
  val encodeT = 0x3

  val characterMask = 0x3

  val stringLimit = 32

  val legacyStringMask = 0xFFFFFFFFFFFFL
  val stringMask = legacyStringMask
  val upperBits = 0xAAAAAAAAAAAAAAAAL
  val stringMaskHighBits = upperBits
  val stringMaskLowBits = 0x5555555555555555L

  type TargetLong = Long
}

/**
  * Compatibility value for a DNA string and a short occurrence count.
  *
  * Wide database records can represent larger counts independently; this type remains short-valued for source
  * compatibility with the original sequence API.
  *
  * @param str   the base string
  * @param count positive compatibility count
  */
case class StringCount(str: String, count: Short) {
  require(str.size <= BitEncoding.stringLimit, "string size is too large for encoding (limit " +
    BitEncoding.stringLimit + ", size is: " + str.size + " for string " + str + ")")
  
  require(count >= 1, "the count for a string should be greater than 0, not " + count + " for string " + str)

  def toStr: String = str + " - " + count
}


/** Spacer bin text together with its aligned two-bit value and comparison mask. */
case class BinAndMask(bin: String, binLong: Long, guideMask: Long)
