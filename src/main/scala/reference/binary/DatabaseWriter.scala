package reference.binary

import java.io._

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import htsjdk.samtools.util.BlockCompressedOutputStream
import main.scala.util.BaseCombinationGenerator
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source
import scala.math._

/**
  * write a somewhat compact (and indexed) storage for our genomic targets
  *
  */
object DatabaseWriter extends LazyLogging {

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
    val oStream = new DataOutputStream(blockStream)

    val blockIterator = new BlockOutputIterator(inputSortedBed, binGenerator.iterator, bitEncoder, positionEncoder)

    // record the number of targets in each bin, so we can set the table later
    val binIterator = binGenerator.iterator

    val compressedBlockSize     = mutable.ArrayBuilder.make[Long]()
    val compressedBlockPosition = mutable.ArrayBuilder.make[Long]()
    val uncompressedBlockSize   = mutable.ArrayBuilder.make[Long]()

    blockIterator.foreach{block => {
      val oldPos = blockStream.getPosition

      uncompressedBlockSize   += block.size
      compressedBlockPosition += oldPos

      blockStream.write(block)

      compressedBlockSize     +=  blockStream.getPosition - oldPos
    }}


    blockStream.close()

    val header = BinaryHeader(binGenerator,
      parameterPack,
      bitEncoder,
      positionEncoder,
      compressedBlockSize.result(),
      compressedBlockPosition.result(),
      uncompressedBlockSize.result())

    BinaryHeader.writeHeader(header, new PrintWriter(new FileOutputStream(output + ".header")))

  }

  /**
    * add an implicit to convert a byte array to a long array - just to hide the uglyness of block compression
    * streams only taking byte arrays
    * @param larray the array of longs
    * @return an array of bytes
    */
  implicit def longArrayToByteArray(larray: Array[Long]): Array[Byte] = {
    val bbuf = java.nio.ByteBuffer.allocate(8*larray.length)
    bbuf.order(java.nio.ByteOrder.nativeOrder)
    bbuf.asLongBuffer.put(larray)
    bbuf.flip()
    bbuf.array()
  }
}

