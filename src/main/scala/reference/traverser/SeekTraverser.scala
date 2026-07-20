/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package reference.traverser

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.{Channels, FileChannel, SeekableByteChannel}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, ResultsAggregator}
import htsjdk.samtools.seekablestream.SeekableFileStream
import htsjdk.samtools.util.{BlockCompressedFilePointerUtil, BlockCompressedInputStream, BlockGunzipper}
import reference.binary.blocks.BlockManager
import utils.{BaseCombinationGenerator, Utils}
import reference.binary.{BinaryHeader, BlockOffset}
import reference.traversal.{BinToGuidesLookup, BinTraversal}
import standards.ParameterPack

import scala.collection.mutable

/** Traverser that seeks directly to every requested compressed block. */
object SeekTraverser extends Traverser with LazyLogging {

  /**
    * Scans selected bins by seeking to their indexed block offsets.
    *
    * Parameters follow the [[Traverser.scan]] contract. This implementation
    * is useful when the traversal visits only a subset of the database bins.
    */
  def scan(binaryFile: File,
           header: BinaryHeader,
           traversal: BinTraversal,
           aggregator: ResultsAggregator,
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition) {

    val formatter = java.text.NumberFormat.getInstance()

    val blockCompressedInput = new BlockCompressedInputStream(new File(binaryFile.getAbsolutePath))

    val blockManager = new BlockManager(header.binWidth, 4, bitCoder)

    // keep a timer and bin counter for output
    var t0 = System.nanoTime()
    var binIndex = 0

    // ------------------------------------------ traversal ------------------------------------------
    traversal.foreach { guidesToSeekForBin => {
      assert(header.blockOffsets contains guidesToSeekForBin.bin)

      val binPositionInformation = header.blockOffsets(guidesToSeekForBin.bin)

      val longBuffer = fillBlock(binPositionInformation, blockCompressedInput)

      blockManager.compareBlock(longBuffer,
        binPositionInformation.numberOfTargets,
        guidesToSeekForBin.guides,
        aggregator,
        bitCoder,
        maxMismatch,
        bitCoder.binToLongComparitor(guidesToSeekForBin.bin))

      if (binIndex % 1000 == 0) {
        //val totalGuidesScoring = traveralIterator.size
        logger.info("Comparing the " + formatter.format(binIndex) +
          "th bin (" + guidesToSeekForBin.bin + ") with " + binPositionInformation.numberOfTargets + " guides, of a total bin count " + formatter.format(traversal.traversalSize) + ". " + ((System.nanoTime() - t0) / 1000000000.0) +
          " seconds/10K bins, executed " + formatter.format(BitEncoding.allComparisons) + " comparisons")
        t0 = System.nanoTime()
      }
      binIndex += 1
    }
    }
    blockCompressedInput.close()
  }

  /**
    * Seeks to and reads one uncompressed bin payload.
    *
    * @param blockInformation metadata describing the block to read
    * @param blockCompressedInput the compressed seekable stream
    * @return the block payload decoded into 64-bit words
    */
  private def fillBlock(blockInformation: BlockOffset, blockCompressedInput: BlockCompressedInputStream): (Array[Long]) = {
    assert(blockInformation.uncompressedSize >= 0, "Bin sizes must be positive (or zero)")

    blockCompressedInput.seek(blockInformation.blockPosition)
    val readToBlock = new Array[Byte](blockInformation.uncompressedSize)
    val read = blockCompressedInput.read(readToBlock)

    Utils.byteArrayToLong(readToBlock)
  }


}
