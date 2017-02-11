package reference.binary

import java.io._
import java.nio.ByteOrder
import java.nio.channels.FileChannel

import bitcoding.{BitEncoding, BitPosition, StringCount}
import main.scala.util.BaseCombinationGenerator
import reference.CRISPRSite
import standards.ParameterPack

import scala.collection.mutable
import scala.math._

/**
  * scan a binary encoded file for hits against a specific list of putative targets
  */
object ScanAgainstBinary {
  def scanAgainst(binaryFile: File,
                  targets: Array[CRISPRSite],
                  maxMismatch: Int,
                  configuration: ParameterPack,
                  bitCoder: BitEncoding,
                  posCoder: BitPosition): Map[CRISPRSite,CRISPRSiteOT] = {

    val stream = new FileInputStream(binaryFile)
    val inChannel = stream.getChannel()

    // read the header of our file into a buffer
    val buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size())
    buffer.order( ByteOrder.BIG_ENDIAN )
    val longBuffer = buffer.asLongBuffer( )

    val headerResult = new Array[Long](3)


    longBuffer.get(headerResult)

    // some header information
    if (headerResult(0) != BinaryConstants.magicNumber)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the magic number expected at the top of the file")

    val version = headerResult(1)
    if (version != BinaryConstants.version)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the correct version: " + version + " expecting " + BinaryConstants.version)

    val binCount = headerResult(2)
    val binWidth = (math.log(binCount)/math.log(4)).toInt
    println("Number of characters used for binning " + binWidth)

    val binLookup = new mutable.LinkedHashMap[String,Long]()
    val binGenerator = BaseCombinationGenerator(binWidth)
    val binResult = new Array[Long](binCount.toInt)
    longBuffer.get(binResult)

    binGenerator.iterator.zipWithIndex.foreach{case(bin,index) => {
      binLookup(bin) = binResult(index)
    }}

    if (binLookup.size != binCount)
      throw new IllegalStateException("Bin lookup container of size " + binLookup.size + " != expected size " + binCount)


    // where we collect the off-target htis
    val siteSequenceToSite = targets.map{tgt => (bitCoder.bitEncodeString(StringCount(tgt.bases,1)),new CRISPRSiteOT(tgt))}.toMap
    println("scoring " + targets.size + " targets")
    var comparisons = 0l
    val formatter = java.text.NumberFormat.getInstance()
    var t0 = System.nanoTime()

    binGenerator.iterator.zipWithIndex.foreach{case(bin,index) => {
      // what targets do we want to score in this bin
      val targetsToScore =
        if (configuration.fivePrimePam)
          targets.filter{tgt => mismatches(bin,tgt.bases.slice(configuration.pam.size,configuration.pam.size + binWidth)) <= maxMismatch}.map{tgt => bitCoder.bitEncodeString(StringCount(tgt.bases,1))}
        else
          targets.filter{tgt => mismatches(bin,tgt.bases.slice(0,binWidth)) <= maxMismatch}.map{tgt => bitCoder.bitEncodeString(StringCount(tgt.bases,1))}

      // now seek to that bin, and read out the sequences there, and compare to our
      try {

        //println("Loading bin " + bin + " with count " + binLookup(bin))
        val result = new Array[Long](binLookup(bin).toInt)

        longBuffer.get(result)

        var offset = 0
        var mismatchesAdded = 0
        var targetsLookedAt = 0
        var counts = 0


        while (offset < binLookup(bin)) {

          val target = result(offset)
          targetsLookedAt += 1
          val coordinatesSize = bitCoder.getCount(target)
          counts += coordinatesSize
          val coordinatesArray = result.slice(offset + 1, offset + 1 + coordinatesSize)
          //println(bitCoder.bitDecodeString(target).str + " = " + bitCoder.bitDecodeString(target).count)

          // now check each target we have -- what's their distance

          var index = 0

          while (index < targetsToScore.size) {
            val mismatches = bitCoder.mismatches(targetsToScore(index), target)
            comparisons += 1
            if (mismatches <= maxMismatch) {
              siteSequenceToSite(targetsToScore(index)).addOT(CRISPRHit(target, coordinatesArray))
              mismatchesAdded += 1
            }

            index += 1
          }
          offset += 1 + coordinatesSize
        }

        //println("mismatchesAdded " + mismatchesAdded + " targets looked at " + targetsLookedAt + " counts " + counts + " size " + binLookup(bin).toInt)
        //var target = result(0)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          throw new IllegalStateException("Exception generated from processing binary file")
        }
      }
      if (index % 1000 == 0 ) {

        println("Scoring our " + formatter.format(index) + "th bin of " + formatter.format(binCount) + " total, off-targets with prefix " +
          bin + ", ran " + formatter.format(comparisons) + " guide comparisons so far. taking " + ((System.nanoTime() - t0)/1000000000.0) + " seconds")
        t0 = System.nanoTime()
      }
    }}

    // where we collect the off-target htis
    // val siteSequenceToSite = targets.map{tgt => (bitCoder.bitEncodeString(StringCount(tgt.bases,1)),new CRISPRSiteOT(tgt))}.toMap
    targets.map{tgt => (tgt,siteSequenceToSite(bitCoder.bitEncodeString(StringCount(tgt.bases,1))))}.toMap
  }

  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map{case(a,b) => if (a == b) 0 else 1}.sum

}
