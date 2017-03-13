package reference.binary

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import main.scala.util.BaseCombinationGenerator
import reference.traverser.Traverser._
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsValue, RootJsonFormat}
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
                        compressedBlockSize: Array[Long],
                        compressedBlockPosition: Array[Long],
                        uncompressedBlockSize: Array[Long]) {

  def binWidth = inputBinGenerator.width

  def binMask: Long = bitCoder.compBitmaskForBin(binWidth)

  def parameterPack = inputParameterPack

  def binGenerator: BaseCombinationGenerator = inputBinGenerator
}

object BinaryHeader extends LazyLogging {
  val headerExtension = ".header"

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
    writer.write(header.parameterPack.enzyme.index.toString + "\n")

    // the number of bins is the 4^binSize
    writer.write(pow(4, header.binGenerator.width).toLong.toString + "\n")

    // write a number of longs equal to the block lookup table size -- we'll come back and fill these in later
    header.binGenerator.iterator.zipWithIndex.foreach { case(bin,index) => {
      writer.write(bin + "=" + header.compressedBlockSize(index) + "," + header.compressedBlockPosition(index) + "," + header.uncompressedBlockSize(index) + "\n")
    }}

    (0 until header.bitPosition.nextSeqId).foreach{ index => {
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
  def readHeader(filename: String, bitCoder: BitEncoding): BinaryHeader = {

    // setup our input file
    val dos = Source.fromFile(filename).getLines()

    // some header checks
    assert(dos.next.toLong != BinaryConstants.magicNumber, "Binary file " + filename + " doesn't have the magic number expected at the top of the file")
    assert(dos.next.toLong != BinaryConstants.version, "Binary file " + filename + " doesn't have the correct version, expecting " + BinaryConstants.version)

    // get the enzyme type
    val enzymeType = Enzyme.indexToParameterPack(dos.next.toLong)

    // now process the bins
    val binCount = dos.next.toLong
    val binWidth = (math.log(binCount) / math.log(4)).toInt
    logger.debug("Number of characters used to generate this lookup file: " + binWidth)

    val binGenerator = BaseCombinationGenerator(binWidth)
    val compressedBlockSize     = mutable.ArrayBuilder.make[Long]()
    val compressedBlockPosition = mutable.ArrayBuilder.make[Long]()
    val uncompressedBlockSize   = mutable.ArrayBuilder.make[Long]()

    // read in the bins and their sizes
    val binRegex = """(\w+)=(\d+),(\d+),(\d+)""".r
    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      val inputStr = binRegex.findAllIn(dos.next).matchData.next()

      assert(bin == inputStr.group(0))
      compressedBlockSize += inputStr.group(1).toLong
      compressedBlockPosition += inputStr.group(2).toLong
      uncompressedBlockSize += inputStr.group(3).toLong
    }}

    val positionEncoder = new BitPosition()
    dos.foreach(remainingLine => {
      val line = remainingLine.split("=")
      positionEncoder.addReference(line(1))
    })

    val binMask = bitCoder.compBitmaskForBin(binWidth)

    BinaryHeader(binGenerator, enzymeType, bitCoder, positionEncoder, compressedBlockSize.result(), compressedBlockPosition.result(), uncompressedBlockSize.result() )
  }
}
