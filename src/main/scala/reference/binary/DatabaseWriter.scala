package reference.binary

import java.io._

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import htsjdk.samtools.util.{BlockCompressedFilePointerUtil, BlockCompressedOutputStream}
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
    * @param inputSortedBed               the input bed file
    * @param output                       the output file we're writing
    * @param bitEncoder                   the bit encoder
    * @param positionEncoder              the position encoder
    * @param binGenerator                 the iterator over bins
    * @param parameterPack                all the details about an enzyme
    * @param maxGenomicLocationsPerTarget the maximum number of genomic locations we record per target
    */
  def writeToBinnedFileSet(inputSortedBed: File,
                           output: String,
                           bitEncoder: BitEncoding,
                           positionEncoder: BitPosition,
                           binGenerator: BaseCombinationGenerator,
                           parameterPack: ParameterPack,
                           maxGenomicLocationsPerTarget: Int = 1000) {


    val blockStream = new BlockCompressedOutputStream(output)

    val blockIterator = new BlockOutputIterator(inputSortedBed, binGenerator.iterator, bitEncoder, positionEncoder)

    // record the number of targets in each bin, so we can set the table later
    val binIterator = binGenerator.iterator

    val compressedBlockInfo = new mutable.HashMap[String,BlockOffset]()

    blockIterator.zipWithIndex.foreach{case(blockContainer,index) => {

      val oldPos = blockStream.getPosition // BlockCompressedFilePointerUtil.getBlockAddress(blockStream.getPosition)
      blockStream.write(Utils.longArrayToByteArray(blockContainer.block))

      val newBlockPos = blockStream.getPosition // BlockCompressedFilePointerUtil.getBlockAddress(blockStream.getPosition)

      if (index % 10000 == 0) {
        val nextBlock = if (blockContainer.block.size > 0) bitEncoder.bitDecodeString(blockContainer.block(0)) + " --> " + Utils.longToBitString(blockContainer.block(0)) else "none"
        logger.info("Writing bin " + blockContainer.bin + " with size " + blockContainer.block.size + " oldPos " + oldPos + " new pos " + newBlockPos  + " encoding " + nextBlock)
      }

      // save the start of the block, the block size in bytes (compressed), the block size uncompressed, and the number of targets
      val comppressedSize = (BlockCompressedFilePointerUtil.getBlockAddress(newBlockPos) - BlockCompressedFilePointerUtil.getBlockAddress(oldPos)).toInt
      assert(comppressedSize < Int.MaxValue, "A compressed block will be too large, please raise the block size")

      compressedBlockInfo(blockContainer.bin) = BlockOffset(oldPos,comppressedSize,blockContainer.block.size * bytesInLong, blockContainer.numberOfTargets)
    }}

    blockStream.flush()
    blockStream.close()

    val header = BinaryHeader(binGenerator,
      parameterPack,
      bitEncoder,
      positionEncoder,
      compressedBlockInfo)

    val headerOutputFile = new PrintWriter(new FileOutputStream(output + ".header"))
    BinaryHeader.writeHeader(header, headerOutputFile)
    headerOutputFile.close()

  }

}

