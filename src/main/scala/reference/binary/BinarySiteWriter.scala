package reference.binary

import java.io.{DataOutputStream, FileOutputStream, RandomAccessFile}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import main.scala.util.BaseCombinationGenerator
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source
import scala.math._

/**
  * write a somewhat compact (and indexed) storage for our genomic targets
  */
object BinarySiteWriter {

  // write the data out to a binned file
  def writeToBinnedFile(inputSortedBed: String,
                        output: String,
                        bitEncoder: BitEncoding,
                        positionEncoder: BitPosition,
                        binGenerator: BaseCombinationGenerator,
                        parameterPack: ParameterPack,
                        maxGenomicLocationsPerTarget: Int = 500): Unit = {

    val oStream = new DataOutputStream(new FileOutputStream(output))

    // some header information
    oStream.writeLong(BinaryConstants.magicNumber)
    oStream.writeLong(BinaryConstants.version)

    // the number of bins is the 4^binSize
    val binCount = pow(4, binGenerator.width).toLong
    oStream.writeLong(binCount)

    // write a number of longs equal to the block lookup table size -- we'll come back and fill these in later
    (0 until binCount.toInt).foreach { bin => oStream.writeLong(0l) }

    // now go read our sorted file. Our sequences should be sorted, and match the order we have from our bin generator.
    // record the number of targets in each bin, so we can set the table later
    val binIterator = binGenerator.iterator
    val inputFile = Source.fromFile(inputSortedBed).getLines()
    val guidesPerBin = new mutable.HashMap[String, Int]()
    val longsPerBin = new mutable.HashMap[String, Int]()

    var currentBin = binIterator.next() // unprotected call, but we should have at least one bin

    var lastGuide = 0l
    var positions = mutable.ArrayBuilder.make[Long]()
    var guideEncoding = 0l
    var positionEncoding = 0l

    // iterate over every line in the input file, assigning it to the right bin
    var totalGuides = 0

    inputFile.foreach{line => {
      totalGuides += 1
      val sp = line.split("\t")
      //val justTargetNoPam = if (parameterPack.fivePrimePam) sp(3).slice(parameterPack.pam.size,sp(3).size) else sp(3).slice(0,sp(3).size - parameterPack.pam.size)
      //val binSeqInTarget = justTargetNoPam.slice(0,binGenerator.width)
      val justTarget = sp(3)
      val binSeqInTarget = if (parameterPack.fivePrimePam) justTarget.slice(parameterPack.pam.size,parameterPack.pam.size + binGenerator.width) else justTarget.slice(0,binGenerator.width)

      guideEncoding = bitEncoder.bitEncodeString(StringCount(justTarget,1))
      positionEncoding = positionEncoder.encode(sp(0),sp(1).toInt)

      // are we the first guide
      if (lastGuide == 0l) {
        lastGuide = guideEncoding

      }

      // are we a repeat? then add to the tally
      if (guideEncoding == lastGuide) {
        positions += positionEncoding
      }
      // we're not a repeated guide, so output the last guide and reset our counters. are we still in the bin?
      else {
        var positionsRendered = positions.result()
        if (positionsRendered.size > maxGenomicLocationsPerTarget)
          positionsRendered = positionsRendered.slice(0,maxGenomicLocationsPerTarget)
        val longsUsed = 1 + positionsRendered.size

        // write to our binary file
        lastGuide = bitEncoder.bitEncodeString(StringCount(bitEncoder.bitDecodeString(lastGuide).str,positionsRendered.size.toShort))

        oStream.writeLong(lastGuide)
        positionsRendered.foreach{pos => oStream.writeLong(pos)}

        // clear our set our position storage and last guide
        lastGuide = guideEncoding
        positions.clear()
        positions += positionEncoding

        // update the byte offsets
        guidesPerBin(currentBin) = guidesPerBin.getOrElse(currentBin,0) + 1
        longsPerBin(currentBin) = longsPerBin.getOrElse(currentBin,0) + longsUsed
      }

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
    }}

    // output the last guide too
    if (lastGuide != 0l) {
      var positionsRendered = positions.result()
      if (positionsRendered.size > maxGenomicLocationsPerTarget)
        positionsRendered = positionsRendered.slice(0,maxGenomicLocationsPerTarget)
      val longsUsed = 1 + positionsRendered.size

      // write to our binary file
      lastGuide = bitEncoder.bitEncodeString(StringCount(bitEncoder.bitDecodeString(lastGuide).str,positionsRendered.size.toShort))

      oStream.writeLong(lastGuide)
      positionsRendered.foreach{pos => oStream.writeLong(pos)}

      // clear our set our position storage and last guide
      lastGuide = guideEncoding
      positions.clear()
      positions += positionEncoding

      // update the byte offsets
      guidesPerBin(currentBin) = guidesPerBin.getOrElse(currentBin,0) + 1
      longsPerBin(currentBin) = longsPerBin.getOrElse(currentBin,0) + longsUsed
    }

    println("Total guides " + totalGuides)

    // if we finished before we ran out of bins, add those as zeros
    while(binIterator.hasNext) {
      val finalBin = binIterator.next()
      guidesPerBin(finalBin) = 0
      longsPerBin(finalBin) = 0
    }


    // close the file, and reopen to set the bin locations
    oStream.close()

    val reopenStream = new RandomAccessFile(output, "rw")

    // the forth long in the file should be
    reopenStream.seek(24)

    // now for each bin, write it out the binary offset
    val binIterator2 = binGenerator.iterator
    while (binIterator2.hasNext) {
      val bn = binIterator2.next()
      reopenStream.writeLong(longsPerBin(bn))

    }

    // close the stream
    reopenStream.close()
  }
}
