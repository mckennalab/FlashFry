package reference.binary

import java.io._

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import htsjdk.samtools.util.{BlockCompressedFilePointerUtil, BlockCompressedOutputStream}
import reference.binary.blocks.BlockManager
import utils.BaseCombinationGenerator
import utils.Utils
import standards.ParameterPack

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.math._

/**
  * write a somewhat compact (and indexed) storage for our genomic off-targets
  *
  */
object DatabaseWriter extends LazyLogging {

  // the number of bytes in a long
  val bytesInLong = 8

  /**
    * write a binary representation of our off-target file
    *
    * @param binsToFiles                  our mapping of bins to files
    * @param writenBinSize                the bin size we used when writing files
    * @param output                       the output file we're writing
    * @param bitEncoder                   the bit encoder
    * @param positionEncoder              the position encoder
    * @param outputGenerator              the iterator over bins
    * @param parameterPack                all the details about an enzyme
    * @param maxGenomicLocationsPerTarget the maximum number of genomic locations we record per target
    */
  def writeToBinnedFileSet(binsToFiles: mutable.HashMap[String,File],
                           writenBinSize: Int,
                           output: String,
                           bitEncoder: BitEncoding,
                           positionEncoder: BitPosition,
                           outputGenerator: BaseCombinationGenerator,
                           parameterPack: ParameterPack,
                           maxGenomicLocationsPerTarget: Int = 1000,
                           maxTargetsPerLinearBin: Int = 5000) {


    val blockStream = new BlockCompressedOutputStream(output)

    val blockReader = new BlockReader(binsToFiles,outputGenerator.width,writenBinSize,bitEncoder,positionEncoder)

    val binIterator = outputGenerator.iterator

    val compressedBlockInfo = new mutable.HashMap[String,BlockOffset]()

    binIterator.zipWithIndex.foreach{case(bin,index) => {

      val oldPos = blockStream.getPosition

      val nextBlock = blockReader.fetchBin(bin)

      val encodedBlock = if (nextBlock.size > maxTargetsPerLinearBin)
        BlockManager.createIndexedBlock(nextBlock,bin,bitEncoder,4)
      else
        BlockManager.createLinearBlock(nextBlock,bin,bitEncoder)

      blockStream.write(Utils.longArrayToByteArray(encodedBlock))

      // save the start of the block, he block size uncompressed in bytes, and the number of targets
      compressedBlockInfo(bin) = BlockOffset(oldPos,encodedBlock.size * bytesInLong, nextBlock.size)

      if ((index + 1 ) % 1000 == 0) logger.info("Writing bin " + bin + " our " + index + " bin")
    }}

    blockStream.flush()
    blockStream.close()

    val header = BinaryHeader(outputGenerator,
      parameterPack,
      bitEncoder,
      positionEncoder,
      compressedBlockInfo)

    val headerOutputFile = new PrintWriter(new FileOutputStream(output + ".header"))
    BinaryHeader.writeHeader(header, headerOutputFile)
    headerOutputFile.close()
  }

}

