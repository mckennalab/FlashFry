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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
//import reference.binary.BinaryGuideReader.ComparisonActor.{ComparisonActorReturnData, ComparisonActorSearchData}

import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap, Map, _}

/**
  * scan a binary encoded file for putitive off-targets against a specific list of guide sequences.  This code really needs
  * to be cleaned up a bit more...
  */
object BinaryGuideReader extends LazyLogging {

  /**
    * given a block of longs representing the targets and their positions, add any potential off-targets to
    * the guide objects
    *
    * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
    * @param guides                     the guides
    * @return an mapping from a potential guide to it's discovered off target sequences
    */
  def compareBlock(blockOfTargetsAndPositions: Array[Long],
                   guides: Array[Long],
                   bitEncoding: BitEncoding,
                   maxMismatches: Int,
                   bin: Long,
                   binMask: Long): Map[Long, Array[CRISPRHit] ] = {

    val returnMap = new mutable.HashMap[Long, ArrayBuilder[CRISPRHit]]()

    // filter down the guides we actually want to look at
    val lookAtGuides = guides.filter(gd => bitEncoding.mismatches(gd,bin,binMask) <= maxMismatches)

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l

    while (offset < blockOfTargetsAndPositions.size) {
      val currentTarget = blockOfTargetsAndPositions(offset)
      val count = bitEncoding.getCount(currentTarget)

      require(blockOfTargetsAndPositions.size > offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = blockOfTargetsAndPositions.slice(offset + 1, (offset + 1) + count)

      var guideOffset = 0

      while (guideOffset < lookAtGuides.size) {
        val mismatches = bitEncoding.mismatches(lookAtGuides(guideOffset), currentTarget)
        if (mismatches <= maxMismatches) {
          returnMap(lookAtGuides(guideOffset)) = returnMap.getOrElse(lookAtGuides(guideOffset), mutable.ArrayBuilder.make[CRISPRHit]) +=
            CRISPRHit(blockOfTargetsAndPositions(offset),blockOfTargetsAndPositions.slice(offset, offset + count + 1))
        }
        guideOffset += 1
      }
      offset += count + 1
    }
    returnMap.map { case (guide, ots) => (guide, ots.result()) }
  }


  /**
    * read the header from the top of the file
    *
    * @param raf the data input stream
    * @param bf  the file
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

    // read in the bins and their sizes
    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      longCounts(bin) = raf.readLong()
      binOffsets(bin) = raf.readLong()
    }
    }

    BinLookup(binWidth, longCounts, binOffsets)
  }

  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum


  /**
    * scan against the binary database of off-target sites
    *
    * @param binaryFile    the file we're scanning from
    * @param targets       the array of candidate guides we have
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @return a guide to OT hit array
    */
  def scanAgainstLinear(binaryFile: File,
                        targets: Array[CRISPRSiteOT],
                        maxMismatch: Int,
                        configuration: ParameterPack,
                        bitCoder: BitEncoding,
                        posCoder: BitPosition): Array[CRISPRSiteOT] = {

    var comparisons = 0l
    val formatter = java.text.NumberFormat.getInstance()

    // setup our input file
    val fos = new FileInputStream(binaryFile)
    val bos = new BufferedInputStream(fos)
    val dos = new DataInputStream(bos)

    // make sure the header is intact
    require(dos.readLong() == BinaryConstants.magicNumber, "Binary file " + binaryFile.getAbsolutePath + " doesn't have the magic number expected at the top of the file")
    require(dos.readLong() == BinaryConstants.version, "Binary file " + binaryFile.getAbsolutePath + " doesn't have the correct version, we were expecting " + BinaryConstants.version)

    val binCount = dos.readLong()
    require(binCount > 0, "Invalid number of bins: " + binCount)

    val binWidth = (math.log(binCount) / math.log(4)).toInt
    val binMask = bitCoder.compBitmaskForBin(binWidth)
    logger.info("Number of characters used for binning " + binWidth)

    val binSizeLookup = new mutable.LinkedHashMap[String, Long]()
    val binOffsetLookup = new mutable.LinkedHashMap[String, Long]()
    val binGenerator = BaseCombinationGenerator(binWidth)

    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      binSizeLookup(bin) = dos.readLong()
      binOffsetLookup(bin) = dos.readLong()
    }}

    // where we collect the off-target hits
    val siteSequenceToSite = new mutable.LinkedHashMap[Long, CRISPRSiteOT]()
    val guideList = new Array[Long](targets.size)

    targets.zipWithIndex.foreach { case(tgt,index) => {
      guideList(index) = tgt.longEncoding
      siteSequenceToSite(tgt.longEncoding) = tgt
    } }

    // do the look analysis here
    var t0 = System.nanoTime()

    logger.info("Beginning search against off-targets")
    val binIterator = binGenerator.iterator
    var binIndex = 0

    while(binIterator.hasNext) {
      val bin = binIterator.next()
      binIndex += 1
      // make a buffer that will hold the bin's longs
      var index = 0
      //logger.info("Loading block of size " + binSizeLookup(bin).toInt)
      var longBuffer = new Array[Long](binSizeLookup(bin).toInt)
      while (index < binSizeLookup(bin).toInt) {
        longBuffer(index) = dos.readLong()
        index += 1
      }

      //logger.info("Comparing to our guides")
      compareBlock(longBuffer,guideList,bitCoder,maxMismatch,bitCoder.binToLongComparitor(bin),binMask).foreach{case(guide,ots) => {
        siteSequenceToSite(guide).addOTs(ots)
      }}

      if (binIndex % 1000 == 0) {
        logger.info("Comparing the " + formatter.format(binIndex) + "th bin of " + formatter.format(binSizeLookup.size) + " total, off-targets with prefix " +
          bin + ". " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/1K bins")
        t0 = System.nanoTime()
      }
    }


    siteSequenceToSite.values.toArray
  }

/*
  // simple actor -- get message to process a list of off-target, and then... do that
  class ComparisonActor(master: ActorRef) extends Actor with LazyLogging {
    def receive = {
      case searchData: ComparisonActorSearchData => {
        val results = compareBlock(searchData.targetBlock,searchData.guides, searchData.bitEncoding, searchData.numMismatches)
        master ! ComparisonActorReturnData(results)
      }
      case _ => logger.info("received unknown message")
    }
  }
  object ComparisonActor {
    case class ComparisonActorSearchData(guides: Array[Long], targetBlock: Array[Long], bitEncoding: BitEncoding, numMismatches: Int)
    case class ComparisonActorReturnData(guidesToOTs: Map[Long, Array[Long]])
  }

  // simple actor -- get message to process off-target, and then do that
  class MasterActor extends Actor with LazyLogging {
    def receive = {
      case searchData: ComparisonActorSearchData => {
        val results = compareBlock(searchData.targetBlock,searchData.guides, searchData.bitEncoding, searchData.numMismatches)
        sender() ! ComparisonActorReturnData(results)
      }
      case _ => logger.info("received unknown message")
    }
  }
  object MasterActor {
  }
*/
}




case class BinLookup(binWidth: Int, longsPerBin: LinkedHashMap[String, Long], offsetIntoFile: LinkedHashMap[String, Long])
