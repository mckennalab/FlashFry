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

package reference.traverser.dump

import reference.traverser.Traverser
import java.io._
import java.nio.ByteBuffer
import java.nio.channels.{Channels, FileChannel, SeekableByteChannel}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import htsjdk.samtools.util.{BlockCompressedFilePointerUtil, BlockCompressedInputStream, BlockGunzipper}

import scala.annotation._
import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, ResultsAggregator}
import reference.binary.blocks.BlockManager
import utils.{BaseCombinationGenerator, Utils}
import reference.binary.{BinaryHeader, BlockOffset}
import standards.ParameterPack

import scala.util.Random

/**
  * Created by aaronmck on 5/9/17.
  */
object DumpAllGuides extends LazyLogging {

  /**
    * scan against the binary database of off-target sites in an implmenetation specific way
    *
    * @param binaryFile    the file we're scanning from
    * @param header        we have to parse the header ahead of time so that we know
    * @param aggregator    guides
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @param minOccurances emitted targets can have this minimum number of occurrences
    * @param maxOccurrences emitted targets can have this maximum number of occurrences
    * @param subsampleProportion the downsampling proportion
    * @return a guide to OT hit array
    */
  def toFile(binaryFile: File,
             header: BinaryHeader,
             configuration: ParameterPack,
             bitCoder: BitEncoding,
             posCoder: BitPosition,
             outputFile: String,
             minOccurrences: Int,
             maxOccurrences: Int,
             subsampleProportion: Double
            ) {

    val outputTargets = new PrintWriter(outputFile)

    val formatter = java.text.NumberFormat.getInstance()

    val blockCompressedInput = new BlockCompressedInputStream(binaryFile)

    // setup our input file
    val filePath = Paths.get(binaryFile.getAbsolutePath)
    val channel = FileChannel.open(filePath, StandardOpenOption.READ)
    val inputStream = Channels.newInputStream(channel)

    val blockManager = new BlockManager(header.binWidth, 4, bitCoder)

    var t0 = System.nanoTime()
    var binIndex = 0

    var totalKeptTargets = 0
    var totalTargets = 0

    val random = new Random()

    // ------------------------------------------ traversal ------------------------------------------
    header.inputBinGenerator.iterator.foreach {
      binString => {

        // get the bin information, fetch that block, and get the underlying data
        assert(header.blockOffsets contains binString)

        val binPositionInformation = header.blockOffsets(binString)

        if (binPositionInformation.blockPosition != 0) // we can't do this before we start moving in the file, if we do the BlockCompressedStream will throw a null pointer exception
          assert(blockCompressedInput.getPosition == binPositionInformation.blockPosition, "Positions don't match up, expecting " + binPositionInformation.blockPosition + " but we're currently at " + blockCompressedInput.getPosition)
        val longBuffer = fillBlock(blockCompressedInput, binPositionInformation, new File(binaryFile.getAbsolutePath), binString, bitCoder)

        val targets = blockManager.decodeToTargets(longBuffer,
          binPositionInformation.numberOfTargets,
          bitCoder,
          bitCoder.binToLongComparitor(binString))

        targets.foreach { target => {
          val sequence = bitCoder.bitDecodeString(target.sequence)
          if (sequence.count <= maxOccurrences && sequence.count >= minOccurrences)
            if (random.nextDouble() <= subsampleProportion) {
              outputTargets.write(">" + sequence.str + "_" + sequence.count + "\n" + sequence.str + "\n")
            }
        }}

        binIndex += 1
        if (binIndex % 1000 == 0) {
          logger.info("dumped bin " + binIndex + " taking " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/1K bins, executed ")
          t0 = System.nanoTime()
        }

      }
    }
    logger.info("Kept targets " + totalKeptTargets + " of a total " + totalTargets)
    outputTargets.close()

  }

  /**
    * fill a block of off-targets from the database
    *
    * @param blockCompressedInput the block compressed stream to pull from
    * @param blockInformation     information about the block we'd like to fetch
    * @param file                 file name to use
    * @return
    */
  private def fillBlock(blockCompressedInput: BlockCompressedInputStream, blockInformation: BlockOffset, file: File, bin: String, bitCoder: BitEncoding): (Array[Long]) = {

    assert(blockInformation.uncompressedSize >= 0, "Bin sizes must be positive (or zero)")

    val readToBlock = new Array[Byte](blockInformation.uncompressedSize)
    val read = blockCompressedInput.read(readToBlock)

    Utils.byteArrayToLong(readToBlock)
  }


}
