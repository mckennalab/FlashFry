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

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import utils.BaseCombinationGenerator
import reference.traverser.Traverser._
import standards.{Enzyme, ParameterPack}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}
import scala.io.Source
import scala.math._

/**
  * Container for information about the sequence bin and their locations within the file
  *
  */
case class BinaryHeader(inputBinGenerator: BaseCombinationGenerator,
                        inputParameterPack: ParameterPack,
                        bitCoder: BitEncoding,
                        bitPosition: BitPosition,
                        blockOffsets: mutable.HashMap[String,BlockOffset]) {

  def binWidth: Int = inputBinGenerator.width

  def binMask: Long = bitCoder.compBitmaskForBin(binWidth)

  def parameterPack: ParameterPack = inputParameterPack

  def binGenerator: BaseCombinationGenerator = inputBinGenerator
}

case class BlockOffset(blockPosition: Long, uncompressedSize: Int, numberOfTargets: Int) {
  def prettyString: String = "BLOCKPOS:" + blockPosition + ",UNCOMPSIZE:" + uncompressedSize + ",NUMTARGET:" + numberOfTargets
}


object BinaryHeader extends LazyLogging {
  val headerExtension = ".header"
  val unitBase = 4


  /**
    * write the header information to the binary format
    *
    * @param header the header to convert into a binary block for output
    */
  def writeHeader(header: BinaryHeader, writer: PrintWriter) {

    // some header information
    writer.write(BinaryConstants.magicNumber.toString + "\n")
    writer.write(BinaryConstants.version.toString + "\n")

    // our enzyme type
    writer.write(ParameterPack.parameterPackToIndex(header.parameterPack) + "\n")

    // the number of bins is the 4^binSize
    writer.write(pow(4, header.binGenerator.width).toLong.toString + "\n")

    // write a number of longs equal to the block lookup table size -- we'll come back and fill these in later
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
    * read the header from the top of the file
    * Headers are defined as:
    *
    * magic version - long
    * version - long
    * enzyme index - long
    * bin count - long
    *
    * for each bin:
    * number of longs in the bin - long
    * offset in the file - long
    *
    * @param filename the file name, just to print in error messages
    * @return a bin-lookup object
    */
  def readHeader(filename: String): BinaryHeader = {

    // setup our input file
    val inputText = Source.fromFile(filename).getLines()

    // some header checks
    assert(inputText.next.toLong == BinaryConstants.magicNumber,
      "Binary file " + filename + " doesn't have the magic number expected at the top of the file")
    assert(inputText.next.toLong == BinaryConstants.version,
      "Binary file " + filename + " doesn't have the correct version, expecting " + BinaryConstants.version)

    // get the enzyme type
    val enzymeType = ParameterPack.indexToParameterPack(inputText.next.toInt)
    logger.info("Loading header: enzyme type is " + enzymeType.enzyme)

    // now process the bins
    val binCount = inputText.next.toLong
    val binWidth = (math.log(binCount) / math.log(unitBase)).toInt
    logger.debug("Number of characters used to generate this lookup file: " + binWidth)

    val binGenerator = BaseCombinationGenerator(binWidth)
    val blockInformation     = new mutable.HashMap[String, BlockOffset]()

    // read in the bins and their sizes
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

    BinaryHeader(binGenerator, enzymeType, binEncoder, positionEncoder, blockInformation)
  }
}
