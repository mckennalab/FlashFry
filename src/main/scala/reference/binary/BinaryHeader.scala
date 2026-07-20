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

package reference.binary

import java.io.PrintWriter

import bitcoding.{BitEncoding, BitPosition, TargetCodec}
import com.typesafe.scalalogging.LazyLogging
import standards.{NucleaseProfileSerialization, ParameterPack}
import utils.BaseCombinationGenerator

import scala.collection.mutable
import scala.io.Source
import scala.math.pow

/**
  * Parsed metadata required to traverse a FlashFry binary database.
  *
  * @param inputBinGenerator enumerates the fixed-width outer spacer bins
  * @param inputParameterPack targeting profile stored in the header
  * @param bitCoder sequence codec configured for that profile
  * @param bitPosition contig/position codec reconstructed from the header dictionary
  * @param blockOffsets compressed-file location and size for each outer bin
  * @param formatVersion header format version read from disk, or the current version for a new database
  */
case class BinaryHeader(inputBinGenerator: BaseCombinationGenerator,
                        inputParameterPack: ParameterPack,
                        bitCoder: BitEncoding,
                        bitPosition: BitPosition,
                        blockOffsets: mutable.HashMap[String,BlockOffset],
                        formatVersion: Long = BinaryConstants.version) {

  def binWidth: Int = inputBinGenerator.width

  def binMask: Long = bitCoder.compBitmaskForBin(binWidth)

  def parameterPack: ParameterPack = inputParameterPack

  def binGenerator: BaseCombinationGenerator = inputBinGenerator
}

/** Location and logical size of one compressed database block. */
case class BlockOffset(blockPosition: Long, uncompressedSize: Int, numberOfTargets: Int) {
  def prettyString: String = "BLOCKPOS:" + blockPosition + ",UNCOMPSIZE:" + uncompressedSize + ",NUMTARGET:" + numberOfTargets
}


object BinaryHeader extends LazyLogging {
  val headerExtension = ".header"
  val unitBase = 4


  /** Write a version-2 text header for a binary database. */
  def writeHeader(header: BinaryHeader, writer: PrintWriter) {

    writer.write(BinaryConstants.magicNumber.toString + "\n")
    writer.write(BinaryConstants.version.toString + "\n")

    writer.write("profile=" + NucleaseProfileSerialization.serialize(header.parameterPack) + "\n")
    writer.write("profileFingerprint=" + NucleaseProfileSerialization.fingerprint(header.parameterPack) + "\n")
    writer.write("targetCodec=" + header.bitCoder.targetCodec.id + "\n")

    // The bin count is 4^width; the width is reconstructed from this value when reading.
    writer.write(pow(4, header.binGenerator.width).toLong.toString + "\n")

    // One line per lexicographically generated DNA bin.
    header.binGenerator.iterator.zipWithIndex.foreach { case(bin,index) => {
      if (header.blockOffsets contains bin) {
        writer.write(bin + "=" +
          header.blockOffsets(bin).blockPosition + "," +
          header.blockOffsets(bin).uncompressedSize + "," +
          header.blockOffsets(bin).numberOfTargets + "\n"
        )
      } else {
        writer.write(bin + "=0,0,0,0\n")
      }
    }}

    (1 until header.bitPosition.nextSeqId).foreach{ index => {
      writer.write(header.bitPosition.indexToContig(index) + "=" + index + "\n")
    }}
  }

  /**
    * Read either supported text-header format.
    *
    * Common prefix:
    *
    *  1. magic number
    *  2. format version
    *
    * Version 1 then stores a numeric legacy profile ID and the bin count. Version 2 stores `profile=`,
    * `profileFingerprint=`, and `targetCodec=` lines followed by the bin count. Both formats continue with one
    * `BIN=compressedOffset,uncompressedBytes,targetCount` line per bin and then `contig=id` dictionary lines.
    *
    * The version-2 fingerprint validates targeting identity, and the codec ID is checked against the decoded profile
    * before the returned object can be used.
    *
    * @param filename path to the `.header` file
    * @return validated metadata and matching codecs
    */
  def readHeader(filename: String): BinaryHeader = {

    val inputText = Source.fromFile(filename).getLines()

    assert(inputText.next.toLong == BinaryConstants.magicNumber,
      "Binary file " + filename + " doesn't have the magic number expected at the top of the file")
    val formatVersion = inputText.next.toLong
    require(formatVersion == BinaryConstants.legacyVersion || formatVersion == BinaryConstants.version,
      "Binary file " + filename + " has unsupported format version " + formatVersion)

    val (enzymeType, codecId, binCount) = if (formatVersion == BinaryConstants.legacyVersion) {
      val pack = ParameterPack.indexToParameterPack(inputText.next.toInt)
      (pack, TargetCodec.legacyId, inputText.next.toLong)
    } else {
      val profileLine = inputText.next
      require(profileLine.startsWith("profile="), "Version-2 header is missing its profile definition")
      val pack = NucleaseProfileSerialization.deserialize(profileLine.stripPrefix("profile="))

      val fingerprintLine = inputText.next
      require(fingerprintLine.startsWith("profileFingerprint="), "Version-2 header is missing its profile fingerprint")
      require(fingerprintLine.stripPrefix("profileFingerprint=") == NucleaseProfileSerialization.fingerprint(pack),
        "Version-2 header profile fingerprint does not match its targeting definition")

      val codecLine = inputText.next
      require(codecLine.startsWith("targetCodec="), "Version-2 header is missing its target codec")
      (pack, codecLine.stripPrefix("targetCodec="), inputText.next.toLong)
    }
    logger.info("Loading header: enzyme type is " + enzymeType.enzyme)

    val binWidth = (math.log(binCount) / math.log(unitBase)).toInt
    logger.debug("Number of characters used to generate this lookup file: " + binWidth)

    val binGenerator = BaseCombinationGenerator(binWidth)
    val blockInformation     = new mutable.HashMap[String, BlockOffset]()

    val binRegex = """(\w+)=(\d+),(\d+),(\d+)""".r
    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      val binLine = inputText.next
      val inputStrMatch = binRegex.findAllIn(binLine)

      assert(inputStrMatch.hasNext,"Missing line for bin " + bin + " from line: " + binLine)
      val inputStr = inputStrMatch.matchData.next()

      assert(bin == inputStr.group(1), "Failed to verify bin name, expected: " + bin + " isn't what we got " + inputStr.group(1))
      blockInformation(bin) = BlockOffset(inputStr.group(2).toLong, inputStr.group(3).toInt, inputStr.group(4).toInt)
    }}

    val positionEncoder = new BitPosition()
    inputText.foreach(remainingLine => {
      val line = remainingLine.split("=")
      positionEncoder.addReference(line(0))
    })

    val binEncoder = new BitEncoding(enzymeType)
    require(binEncoder.targetCodec.id == codecId,
      "Header target codec " + codecId + " is incompatible with profile " + enzymeType.profile.id +
        "; expected " + binEncoder.targetCodec.id)

    BinaryHeader(binGenerator, enzymeType, binEncoder, positionEncoder, blockInformation, formatVersion)
  }
}
