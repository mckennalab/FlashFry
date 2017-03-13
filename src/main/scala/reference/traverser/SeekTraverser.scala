package reference.traverser

import java.io.{BufferedInputStream, DataInputStream, File, FileInputStream}
import java.nio.ByteBuffer
import java.nio.channels.{Channels, FileChannel, SeekableByteChannel}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import main.scala.util.BaseCombinationGenerator
import reference.traversal.{BinToGuides, BinTraversal, OrderedBinTraversal}
import standards.ParameterPack

import scala.collection.mutable

/**
  * traverse a binary database file, seeking to the correct bins
  */
object SeekTraverser extends Traverser with LazyLogging {

  /**
    * scan against the binary database of off-target sites in an implmenetation specific way
    *
    * @param binaryFile    the file we're scanning from
    * @param header        we have to parse the header ahead of time so that we know
    * @param traversal     the traversal over bins we'll use
    * @param targets       the array of candidate guides we have
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @return a guide to OT hit array
    */
  def scan(binaryFile: File,
           header: BinLookup,
           traversal: BinTraversal,
           targets: Array[CRISPRSiteOT],
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition): Array[CRISPRSiteOT] = {

    val formatter = java.text.NumberFormat.getInstance()

    // setup our input file
    val filePath = Paths.get(binaryFile.getAbsolutePath)
    val channel = FileChannel.open(filePath, StandardOpenOption.READ)
    val inputStream = Channels.newInputStream(channel)

    Traverser.readHeader(inputStream,binaryFile.getAbsolutePath, bitCoder)

    // where we collect the off-target hits
    val siteSequenceToSite = new mutable.LinkedHashMap[Long, CRISPRSiteOT]()
    val guideList = new Array[Long](targets.size)

    targets.zipWithIndex.foreach { case(tgt,index) => {
      guideList(index) = tgt.longEncoding
      siteSequenceToSite(tgt.longEncoding) = tgt
    } }

    // do the look analysis here
    var t0 = System.nanoTime()
    var binIndex = 0

    logger.info("Beginning search against off-targets with " + traversal.traversalSize)
    val traveralIterator = traversal.iterator

    // ------------------------------------------ traversal ------------------------------------------
    while(traveralIterator.hasNext) {
      val binDescription = traveralIterator.next()
      val longBuffer = fillBlock(header, filePath, binDescription)

      Traverser.compareBlock(longBuffer,binDescription.guides,bitCoder,maxMismatch,bitCoder.binToLongComparitor(binDescription.bin),header.binMask).foreach{case(guide,ots) => {
        siteSequenceToSite(guide).addOTs(ots)

        // if we're done with a guide, tell our traverser to remove it
        if (siteSequenceToSite(guide).full) {
          traversal.overflowGuide(guide)
          logger.info("Guide " + bitCoder.bitDecodeString(guide).str + " has overflowed, and will no longer collect off-targets (total " + siteSequenceToSite(guide).offTargets.result().size + " and other " + siteSequenceToSite(guide).currentTotal)
        }
      }}

      if (binIndex % 10000 == 0) {
        //val totalGuidesScoring = traveralIterator.size
        logger.info("Comparing the " + formatter.format(binIndex) + "th bin of " + formatter.format(traversal.traversalSize) + ". " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/10K bins")
        t0 = System.nanoTime()
      }
      binIndex += 1
    }
    siteSequenceToSite.values.toArray
  }

  /**
    * fill a block of off-targets from the database
    * @param header the header object
    * @param filePath our file path
    * @param binDescription the size of the bin
    * @return
    */
  private def fillBlock(header: BinLookup, filePath: Path, binDescription: BinToGuides): (Array[Long]) = {

    val binSize = (header.longsPerBin(binDescription.bin) * 8).toInt
    assert(binSize > 0,"We hit a bin that's larger than the maximum integer")

    val bf = ByteBuffer.allocate(binSize)
    val sbc = Files.newByteChannel(filePath, StandardOpenOption.READ)
    sbc.position(header.offsetIntoFile(binDescription.bin))
    sbc.read(bf)
    bf.flip()
    bf.asLongBuffer()

    val longBuffer = new Array[Long](header.longsPerBin(binDescription.bin).toInt)
    var index = 0
    while (index < header.longsPerBin(binDescription.bin)) {
      longBuffer(index) = bf.getLong()
      index += 1
    }
    sbc.close()
    (longBuffer)
  }
}
