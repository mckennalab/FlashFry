package reference.binary

import java.io.{DataOutputStream, File, FileOutputStream, RandomAccessFile}
import java.util.zip.Deflater

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
  */
object BinaryTargetStorage extends LazyLogging {

  def writeToBinnedFile(inputSortedBed: String,
                       output: String,
                       bitEncoder: BitEncoding,
                       positionEncoder: BitPosition,
                       binGenerator: BaseCombinationGenerator,
                       parameterPack: ParameterPack,
                       maxGenomicLocationsPerTarget: Int = 500)  {

    val oStream = new DataOutputStream(new FileOutputStream(output))

    // now go read our sorted file. Our sequences should be sorted, and match the order we have from our bin generator.
    // record the number of targets in each bin, so we can set the table later
    val binIterator = binGenerator.iterator
    val inputFile = Source.fromFile(inputSortedBed).getLines()

    val guidesPerBin = new scala.collection.mutable.HashMap[String, Int]()
    val longsPerBin = new scala.collection.mutable.HashMap[String, Int]()

    var currentBin = binIterator.next() // unprotected call, but we should have at least one bin

    var lastGuide = 0l
    var positions = mutable.ArrayBuilder.make[Long]()
    var guideEncoding = 0l
    var positionEncoding = 0l

    // iterate over every line in the input file, assigning it to the right bin
    var totalGuides = 0

    // setup the header
    setupBinaryWriter(binGenerator, oStream)

    /**
      * write our binned file
      */
    {
      // for each line in the sorted file,
      inputFile.foreach { line => {
        totalGuides += 1

        val sp = line.split("\t")
        val justTarget = sp(3)

        val binSeqInTarget = if (parameterPack.fivePrimePam)
          justTarget.slice(parameterPack.pam.size, parameterPack.pam.size + binGenerator.width)
        else
          justTarget.slice(0, binGenerator.width)

        guideEncoding = bitEncoder.bitEncodeString(StringCount(justTarget, 1))
        positionEncoding = positionEncoder.encode(sp(0), sp(1).toInt)

        // are we the first guide
        if (lastGuide == 0l) {
          lastGuide = guideEncoding
        }

        // are we a repeat? then add to the tally
        if (guideEncoding == lastGuide)
          positions += positionEncoding
        else
          new_guide()

        // if our current bin not match our guide bin sequence anymore, move it to the next bin that matches, saving
        // results for any bin that gets skipped over
        while (binSeqInTarget != currentBin) {
          if (binIterator.hasNext)
            currentBin = binIterator.next()
          else
            throw new IllegalStateException("We've iterated off the end of the iterator, this shouldn't happen")

          guidesPerBin(currentBin) = 0
          longsPerBin(currentBin) = 0
        }
      }
      }

      // output the last guide too
      if (lastGuide != 0l)
        new_guide()

      logger.info("Total guides " + totalGuides)

      // if we finished before we ran out of bins, add those as zeros
      while (binIterator.hasNext) {
        val finalBin = binIterator.next()
        guidesPerBin(finalBin) = 0
        longsPerBin(finalBin) = 0
      }

      // close the file, and reopen to set the bin locations
      oStream.close()

      rewrite_bin_sizes
    }


    // ******************************************************************************
    //
    // helper methods
    //
    // ******************************************************************************

    /**
      * track back into the file, and rewrite our bin sizes
      */
    def rewrite_bin_sizes() {
      val reopenStream = new RandomAccessFile(output, "rw")

      // the forth long in the file should be the start of the bin offsets
      reopenStream.seek(BinaryConstants.headerSize)

      // now for each bin, write it out the binary offset
      val binIterator2 = binGenerator.iterator
      var totalByteOffset = BinaryConstants.headerSize + (pow(4, binGenerator.width).toLong * 16)

      while (binIterator2.hasNext) {
        val bn = binIterator2.next()
        reopenStream.writeLong(longsPerBin(bn))
        reopenStream.writeLong(totalByteOffset)
        totalByteOffset += ((longsPerBin(bn) * 8))
      }

      // close the stream
      reopenStream.close()
    }

    /**
      * update our tracking objects when we find a new guide
      */
    def new_guide() {
      var positionsRendered = positions.result()
      if (positionsRendered.size > maxGenomicLocationsPerTarget)
        positionsRendered = positionsRendered.slice(0, maxGenomicLocationsPerTarget)
      val longsUsed = 1 + positionsRendered.size

      // write to our binary file
      lastGuide = bitEncoder.bitEncodeString(StringCount(bitEncoder.bitDecodeString(lastGuide).str, positionsRendered.size.toShort))

      // logger.info("last guide = " + bitEncoder.bitDecodeString(lastGuide).str + " with count " + + bitEncoder.bitDecodeString(lastGuide).count)

      oStream.writeLong(lastGuide)
      positionsRendered.foreach { pos => oStream.writeLong(pos) }

      // clear our set our position storage and last guide
      lastGuide = guideEncoding
      positions.clear()
      positions += positionEncoding

      // update the byte offsets
      guidesPerBin(currentBin) = guidesPerBin.getOrElse(currentBin, 0) + 1
      longsPerBin(currentBin) = longsPerBin.getOrElse(currentBin, 0) + longsUsed
    }

    /**
      * write the header information to the binary format
      *
      * @param binGenerator how many bins we use
      * @param oStream      the output stream
      */
    def setupBinaryWriter(binGenerator: BaseCombinationGenerator, oStream: DataOutputStream): Unit = {
      // some header information
      oStream.writeLong(BinaryConstants.magicNumber)
      oStream.writeLong(BinaryConstants.version)

      // the number of bins is the 4^binSize
      val binCount = pow(4, binGenerator.width).toLong
      oStream.writeLong(binCount)

      // write a number of longs equal to the block lookup table size -- we'll come back and fill these in later
      (0 until binCount.toInt).foreach { bin => {
        oStream.writeLong(0l)
        oStream.writeLong(0l)
      }}
    }

  }
}
