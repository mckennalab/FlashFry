package reference.traverser

import java.io.{BufferedInputStream, DataInputStream, File, FileInputStream}
import java.nio.ByteBuffer
import java.nio.channels.{Channels, FileChannel, SeekableByteChannel}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import htsjdk.samtools.util.{BlockCompressedInputStream, BlockGunzipper}
import main.scala.util.BaseCombinationGenerator
import reference.binary.{BinaryHeader, BlockOffset}
import reference.traversal.{BinToGuidesLookup, BinTraversal}
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
           header: BinaryHeader,
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

    // where we collect the off-target hits
    val siteSequenceToSite = new mutable.LinkedHashMap[Long, CRISPRSiteOT]()
    val guideList = new Array[Long](targets.size)

    targets.zipWithIndex.foreach { case (tgt, index) => {
      guideList(index) = tgt.longEncoding
      siteSequenceToSite(tgt.longEncoding) = tgt
    }
    }

    // do the look analysis here
    var t0 = System.nanoTime()
    var binIndex = 0

    logger.info("Beginning search against off-targets with " + traversal.traversalSize)
    // ------------------------------------------ traversal ------------------------------------------
    traversal.foreach { guidesToSeekForBin => {
      assert(header.blockOffsets contains guidesToSeekForBin.bin)

      val binPositionInformation = header.blockOffsets(guidesToSeekForBin.bin)

      val longBuffer = fillBlock(binPositionInformation, filePath)

      Traverser.compareBlock(longBuffer,
        guidesToSeekForBin.guides,
        bitCoder,
        maxMismatch,
        bitCoder.binToLongComparitor(guidesToSeekForBin.bin),
        header.binMask).foreach { case (guide, ots) => {

        siteSequenceToSite(guide).addOTs(ots)

        // if we're done with a guide, tell our traverser to remove it
        if (siteSequenceToSite(guide).full) {
          traversal.overflowGuide(guide)
          logger.info("Guide " + bitCoder.bitDecodeString(guide).str + " has overflowed, and will no longer collect off-targets (total " + siteSequenceToSite(guide).offTargets.result().size + " and other " + siteSequenceToSite(guide).currentTotal)
        }
      }
      }

      if (binIndex % 10000 == 0) {
        //val totalGuidesScoring = traveralIterator.size
        logger.info("Comparing the " + formatter.format(binIndex) + "th bin of " + formatter.format(traversal.traversalSize) + ". " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/10K bins")
        t0 = System.nanoTime()
      }
      binIndex += 1
    }
    }

    siteSequenceToSite.values.toArray
  }

  /**
    * fill a block of off-targets from the database
    *
    * @param blockInformation information about the block we'd like to fetch
    * @param filePath         our file path
    * @return
    */
  private def fillBlock(blockInformation: BlockOffset, filePath: Path): (Array[Long]) = {
    assert(blockInformation.uncompressedSize > 0, "Bin sizes must be positive")
    val blockCompressedInput = new BlockCompressedInputStream(filePath)

    // read in the compressed block into an internal buffer
    val bf = ByteBuffer.allocate(blockInformation.compressedblockSize.toInt) // Dangerous downcast, but we're probably ok
    val sbc = Files.newByteChannel(filePath, StandardOpenOption.READ)
    sbc.position(blockInformation.blockPosition)
    sbc.read(bf)
    bf.flip()
    sbc.close()

    // uncompress into a buffer
    val uncompressedOutput = new Array[Byte](blockInformation.uncompressedSize)
    try {
      blockDecompressor.unzipBlock(bf.array(), uncompressedOutput, blockInformation.compressedblockSize)
    } catch {
      case e: htsjdk.samtools.SAMFormatException => {
        logger.error("Failed to fetch block starting at " + blockInformation.blockPosition + " with compressed size " + blockInformation.compressedblockSize)
        throw e
      }
    }

    // now copy the data into a final Long array
    val longArray = new Array[Long](blockInformation.uncompressedArraySize)
    var index = 0
    while (index < longArray.size) {
      longArray(index) = bf.getLong()
      index += 1
    }

    (longArray)
  }
}
