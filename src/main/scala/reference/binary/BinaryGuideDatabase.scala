package reference.binary

import java.io._
import java.nio.{ByteBuffer, ByteOrder, LongBuffer}
import java.nio.channels.FileChannel

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import main.scala.util.BaseCombinationGenerator
import reference.CRISPRSite
import standards.ParameterPack
import java.io.RandomAccessFile

import scala.collection.mutable.{LinkedHashMap, _}
import scala.math._

/**
  * scan a binary encoded file for hits against a specific list of putative targets.  This code really needs
  * to be cleaned up.
  */
object BinaryGuideDatabase extends LazyLogging {

  def scanAgainst(binaryFile: File,
                  guides: Array[CRISPRSiteOT],
                  configuration: ParameterPack,
                  bitCoder: BitEncoding,
                  maxMismatch: Int,
                  posCoder: BitPosition) {

    require(maxMismatch > 0)

    var comparisons = 0l
    val formatter = java.text.NumberFormat.getInstance()
    var t0 = System.nanoTime()

    // setup our input file
    val raf = new RandomAccessFile(binaryFile, "rw")

    val binLookup = readHeader(raf, binaryFile)

    require(binLookup.longsPerBin.size == math.pow(4,binLookup.binWidth).toInt, "the count of bins needs to match up against the header value")

    val binGenerator = BaseCombinationGenerator(binLookup.binWidth)
    val binaryTraversalIterator = new OrderedBinTraversal(binGenerator,maxMismatch, bitCoder, guides)

    binaryTraversalIterator.iterator.zipWithIndex.foreach { case (binDescription,index) => {

      // get the next bin
      val sizeToFetch = binLookup.longsPerBin(binDescription.bin)
      val offsetIntoFile = binLookup.offsetIntoFile(binDescription.bin)

      if (sizeToFetch > 0) {

        val input = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, offsetIntoFile, sizeToFetch * 8)

        // read the header of our file into a buffer
        var offset = 0
        var mismatchesAdded = 0
        var targetsLookedAt = 0

        // while we still have targets to read
        while (offset < sizeToFetch) {

          val target = input.getLong
          targetsLookedAt += 1

          val coordinatesSize = bitCoder.getCount(target)
          val coordinatesArray = new Array[Long](coordinatesSize)
          var ind = 0
          while (ind < coordinatesSize) {
            coordinatesArray(ind) = input.getLong
            ind += 1
          }

          var pos_index = 0

          // for each target that matches
          while (pos_index < binDescription.guides.size) {
            val mismatches = bitCoder.mismatches(binDescription.guides(pos_index), target)
            comparisons += 1
            if (mismatches <= maxMismatch) {
              binDescription.guides(index) //.addOT(CRISPRHit(target, coordinatesArray))
              mismatchesAdded += 1
            }
            pos_index += 1
          }
          offset += 1 + coordinatesSize
        }
      }
      if (index % 1000 == 0) {
        logger.info("Comparing the " + formatter.format(index) + "th bin of " + formatter.format(binLookup.longsPerBin.size) + " total, off-targets with prefix " +
          binDescription.bin + ", ran " + formatter.format(comparisons) + " guide comparisons so far. " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/1K bins")
        t0 = System.nanoTime()
      }
    }}
  }


  /**
    * read the header from the top of the file
    * @param raf the data input stream
    * @param bf the file
    * @return a bin-lookup object
    */
  private def readHeader(raf: RandomAccessFile, bf: File): BinLookup = {

    // some header information
    if (raf.readLong() != BinaryConstants.magicNumber)
      throw new IllegalStateException("Binary file " + bf.getAbsolutePath + " doesn't have the magic number expected at the top of the file")

    val version = raf.readLong()
    if (version != BinaryConstants.version)
      throw new IllegalStateException("Binary file " + bf.getAbsolutePath + " doesn't have the correct version: " + version + " expecting " + BinaryConstants.version)

    val binCount = raf.readLong()
    val binWidth = (math.log(binCount) / math.log(4)).toInt
    logger.info("Number of characters used for binning " + binWidth)

    val longCounts = new LinkedHashMap[String, Long]()
    val binOffsets = new LinkedHashMap[String, Long]()
    val binGenerator = BaseCombinationGenerator(binWidth)

    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      longCounts(bin) = raf.readLong()
      binOffsets(bin) = raf.readLong()
    }}

    BinLookup(binWidth, longCounts, binOffsets)
  }

  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum
}

case class BinLookup(binWidth: Int, longsPerBin: LinkedHashMap[String, Long], offsetIntoFile: LinkedHashMap[String, Long])
