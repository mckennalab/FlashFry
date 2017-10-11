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

import scala.annotation._
import elidable._
import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, ResultsAggregator}
import htsjdk.samtools.util.{BlockCompressedFilePointerUtil, BlockCompressedInputStream, BlockGunzipper}
import reference.binary.blocks.BlockManager
import utils.{BaseCombinationGenerator, Utils}
import reference.binary.{BinaryHeader, BlockOffset}
import reference.traversal.{BinToGuidesLookup, BinTraversal}
import standards.ParameterPack

import scala.collection.mutable

/**
  * traverse a binary database file, seeking to the correct bins
  */
object LinearTraverser extends Traverser with LazyLogging {

  /**
    * scan against the binary database of off-target sites in an implmenetation specific way
    *
    * @param binaryFile    the file we're scanning from
    * @param header        we have to parse the header ahead of time so that we know
    * @param traversal     the traversal over bins we'll use
    * @param aggregator    guides
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @return a guide to OT hit array
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

    val blockCompressedInput = new BlockCompressedInputStream(binaryFile)

    // setup our input file
    val filePath = Paths.get(binaryFile.getAbsolutePath)
    val channel = FileChannel.open(filePath, StandardOpenOption.READ)
    val inputStream = Channels.newInputStream(channel)

    val blockManager = new BlockManager(header.binWidth,4,bitCoder)

    var t0 = System.nanoTime()
    var binIndex = 0

    // ------------------------------------------ traversal ------------------------------------------
    traversal.foreach { guidesToSeekForBin => {
      assert(header.blockOffsets contains guidesToSeekForBin.bin)

      val binPositionInformation = header.blockOffsets(guidesToSeekForBin.bin)

      if (binPositionInformation.blockPosition != 0) // we can't do this before we start moving in the file, if we do the BlockCompressedStream will throw a null pointer exception
        assert(blockCompressedInput.getPosition == binPositionInformation.blockPosition, "Positions don't match up, expecting " + binPositionInformation.blockPosition+ " but we're currently at " + blockCompressedInput.getPosition)
      val longBuffer = fillBlock(blockCompressedInput, binPositionInformation, new File(binaryFile.getAbsolutePath), guidesToSeekForBin.bin, bitCoder)

      blockManager.compareBlock(longBuffer,
        binPositionInformation.numberOfTargets,
        guidesToSeekForBin.guides,
        aggregator,
        bitCoder,
        maxMismatch,
        bitCoder.binToLongComparitor(guidesToSeekForBin.bin))

      binIndex += 1
      if (binIndex % 1000 == 0) {
        logger.info(formatter.format(binIndex) + "/" + formatter.format(traversal.traversalSize) + " bins; guides: " + guidesToSeekForBin.guides.size + "; targets: " + binPositionInformation.numberOfTargets + "; " + ((System.nanoTime() - t0) / 1000000000.0) +
          " seconds/1K bins, executed " + formatter.format(BitEncoding.allComparisons) + " comparisons")
        t0 = System.nanoTime()
      }

    }
    }

  }

  /**
    * fill a block of off-targets from the database
    *
    * @param blockCompressedInput the block compressed stream to pull from
    * @param blockInformation information about the block we'd like to fetch
    * @param file             file name to use
    * @return
    */
  private def fillBlock(blockCompressedInput: BlockCompressedInputStream, blockInformation: BlockOffset, file: File, bin: String, bitCoder: BitEncoding): (Array[Long]) = {

    assert(blockInformation.uncompressedSize >= 0, "Bin sizes must be positive (or zero)")

    val readToBlock = new Array[Byte](blockInformation.uncompressedSize)
    val read = blockCompressedInput.read(readToBlock)

    Utils.byteArrayToLong(readToBlock)
  }


}
