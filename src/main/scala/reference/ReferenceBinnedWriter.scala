package reference

import java.io.{DataOutputStream, FileOutputStream, RandomAccessFile}

import bitcoding.{BitEncoding, BitPosition, StringCount}

import scala.collection.mutable
import scala.io.Source
import scala.math._

/**
  * write a somewhat compact (and indexed) storage for our genomic targets
  */
object ReferenceBinnedWriter {

  val magicNumber = 0x1234ABCDE123890l // looks magic enough -- just there to make sure the file what we expect
  val version = 1l
  val bytesPerTarget = 8
  val bytesPerGenomeLocation = 8

  // write the data out to a binned file
  def writeToBinnedFile(inputSortedBed: String,
                        output: String,
                        bitEncoder: BitEncoding,
                        positionEncoder: BitPosition,
                        binSize: Int = 9,
                        pam: String,
                        fivePrimePam: Boolean = false): Unit = {

    val oStream = new DataOutputStream(new FileOutputStream(output))

    // some header information
    oStream.writeLong(magicNumber)
    oStream.writeLong(version)

    // the number of bins is the 4^binSize
    val binCount = pow(4, binSize).toLong
    oStream.writeLong(binCount)

    // write a number of longs equal to the block lookup table size -- we'll come back and fill these in later
    (0 until binCount.toInt).foreach { bin => oStream.writeLong(0l) }

    // now go read our sorted file. Our sequences should be sorted, and match the order we have from our bin generator.
    // record the number of targets in each bin, so we can set the table later
    val binGenerator = new BaseCombinations(binSize)
    val inputFile = Source.fromFile(inputSortedBed).getLines()
    val guidesPerBin = new mutable.HashMap[String, Int]()
    val bytesPerBin = new mutable.HashMap[String, Int]()

    var currentBin = binGenerator.next() // unprotected call, but we should have at least one bin

    var lastGuide = 0l
    var positions = mutable.ArrayBuilder.make[Long]()


    // iterate over every line in the input file, assigning it to the right bin
    inputFile.foreach{line => {
      val sp = line.split("\t")
      val justTargetNoPam = if (fivePrimePam) sp(3).slice(pam.size,sp(3).size) else sp(3).slice(0,sp(3).size - pam.size)
      val binSeqInTarget = justTargetNoPam.slice(0,binSize)

      val guideEncoding = bitEncoder.bitEncodeString(StringCount(justTargetNoPam,1))
      val positionEncoding = positionEncoder.encode(sp(0),sp(1).toInt)

      // are we a repeat?
      if (guideEncoding == lastGuide) {
        positions += positionEncoding
      }
      // we're not a repeated guide, so output the last guide and reset our counters. are we still in the bin?
      else {
        val positionsRendered = positions.result()
        val bytesUsed = bytesPerTarget + (bytesPerGenomeLocation * positionsRendered.size)

        // write to our binary file
        oStream.writeLong(lastGuide)
        positionsRendered.foreach{pos => oStream.writeLong(pos)}

        // clear our set our position storage and last guide
        lastGuide = guideEncoding
        positions.clear()
        positions += positionEncoding

        // update the byte offsets
        guidesPerBin(currentBin) = guidesPerBin.getOrElse(currentBin,0) + 1
        bytesPerBin(currentBin) = bytesPerBin.getOrElse(currentBin,0) + bytesUsed

      }

      // if our current bin not match our guide bin sequence anymore, move it to the next bin that matches, saving
      // results for any bin that gets skipped over
      while (binSeqInTarget != currentBin) {
        println("Current bin " + currentBin)
        if (binGenerator.hasNext)
          currentBin = binGenerator.next()
        else
          throw new IllegalStateException("We've iterated off the end of the iterator, this shouldn't happen")

        guidesPerBin(currentBin) = 0
        bytesPerBin(currentBin) = 0
      }
    }}

    // if we finished before we ran out of bins, add those as zeros
    while(binGenerator.hasNext) {
      val finalBin = binGenerator.next()
      guidesPerBin(finalBin) = 0
      bytesPerBin(finalBin) = 0
    }


    // close the file, and reopen to set the bin locations
    oStream.close()

    val reopenStream = new RandomAccessFile(output, "w")

    // the forth long in the file should be
    reopenStream.seek(24)

    // now for each bin, write it out the binary offset
    val binGenerator2 = new BaseCombinations(binSize)
    var totalOffset = (24 + (binCount * 8)).toLong
    while (binGenerator2.hasNext) {
      reopenStream.writeLong(totalOffset)
      totalOffset += bytesPerBin(binGenerator2.next())
    }

    // close the stream
    reopenStream.close()
  }
}
