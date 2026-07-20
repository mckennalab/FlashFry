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

/** Exports selected targets from a FlashFry binary database as FASTA. */
object DumpAllGuides extends LazyLogging {

  /**
    * Streams database targets to a FASTA file, filtering by occurrence count
    * and optionally retaining a random sample.
    *
    * Each retained record is named `<sequence>_<occurrences>`. The
    * `subsampleProportion` is compared with a uniform random value for every
    * target; callers should normally supply a value in the range `[0.0, 1.0]`.
    *
    * @param binaryFile database file to read
    * @param header parsed header for `binaryFile`
    * @param configuration nuclease configuration associated with the database
    * @param bitCoder sequence and record codec selected from the header
    * @param posCoder genomic-position codec selected from the header
    * @param outputFile destination FASTA path
    * @param minOccurrences minimum occurrence count to emit, inclusive
    * @param maxOccurrences maximum occurrence count to emit, inclusive
    * @param subsampleProportion probability of emitting a target that passes
    *                            the occurrence filters
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
          val count = target.getOffTargetCount
          if (count <= maxOccurrences && count >= minOccurrences)
            if (random.nextDouble() <= subsampleProportion) {
              outputTargets.write(">" + sequence.str + "_" + count + "\n" + sequence.str + "\n")
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
    * Reads one uncompressed bin payload from the current stream position.
    *
    * @param blockCompressedInput the block compressed stream to pull from
    * @param blockInformation metadata describing the block to read
    * @param file database file containing the block
    * @param bin sequence prefix identifying the block
    * @param bitCoder codec used for sequence records in the block
    * @return the block payload decoded into 64-bit words
    */
  private def fillBlock(blockCompressedInput: BlockCompressedInputStream, blockInformation: BlockOffset, file: File, bin: String, bitCoder: BitEncoding): (Array[Long]) = {

    assert(blockInformation.uncompressedSize >= 0, "Bin sizes must be positive (or zero)")

    val readToBlock = new Array[Byte](blockInformation.uncompressedSize)
    val read = blockCompressedInput.read(readToBlock)

    Utils.byteArrayToLong(readToBlock)
  }


}
