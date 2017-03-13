package reference.traverser

import java.io.{FileInputStream, RandomAccessFile, _}

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import htsjdk.samtools.util.BlockCompressedInputStream
import main.scala.util.BaseCombinationGenerator
import reference.binary.{BinaryConstants, BinaryHeader}
import reference.traversal.{BinTraversal, OrderedBinTraversal}
import standards.ParameterPack

import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap, Map, _}

/**
  * scan, in a linear fashion,  a binary encoded file for putitive off-targets against a specific list of guide sequences.
  */
object LinearTraverser with LazyLogging {

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
           header: BinaryHeader,
           traversal: BinTraversal,
           targets: Array[CRISPRSiteOT],
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition): Array[CRISPRSiteOT] = {


    // time a linear traverse over the file
    var t0 = System.nanoTime()

    val dos = new DataInputStream(new BlockCompressedInputStream(new FileInputStream(binaryFile)))

    val siteSequenceToSite = new mutable.LinkedHashMap[Long, CRISPRSiteOT]()
    var guideList = new Array[Long](targets.size)

    // convert the guides to a list of longs
    targets.zipWithIndex.foreach { case (tgt, index) => {
      guideList(index) = tgt.longEncoding
      siteSequenceToSite(tgt.longEncoding) = tgt
    }}

    traversal.foreach{bin => {

      // make a buffer that will hold the bin's longs
      var index = 0
      //logger.info("Loading block of size " + binSizeLookup(bin).toInt)
      var longBuffer = new Array[Long](header.longsPerBin(bin).toInt)
      while (index < header.longsPerBin(bin).toInt) {
        longBuffer(index) = dos.readLong()
        index += 1
      }

      //logger.info("Comparing to our guides")
      Traverser.compareBlock(longBuffer, guideList, bitCoder, maxMismatch, bitCoder.binToLongComparitor(bin), header.binMask).foreach { case (guide, ots) => {
        siteSequenceToSite(guide).addOTs(ots)

        // if we're done, tell our traverser
        if (siteSequenceToSite(guide).full) {
          guideList = guideList.filter { case (gd) => guide != gd }
          logger.info("Guide " + bitCoder.bitDecodeString(guide).str + " has overflowed, and will no longer collect off-targets (total " + siteSequenceToSite(guide).offTargets.result().size + " and other " + siteSequenceToSite(guide).currentTotal)
        }
      }
      }

      if (binIndex % 10000 == 0) {
        logger.info("Comparing the " + formatter.format(binIndex) + "th bin of " + formatter.format(header.longsPerBin.size) + " total, off-targets with prefix " +
          bin + ". " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/10K bins")
        t0 = System.nanoTime()
      }
    }
    siteSequenceToSite.values.toArray
  }

}

