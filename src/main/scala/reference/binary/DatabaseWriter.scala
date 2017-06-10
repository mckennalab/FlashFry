/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
                           maxTargetsPerLinearBin: Int = 500) {


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

      // save the start of the block, the block size uncompressed in bytes, and the number of targets
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

