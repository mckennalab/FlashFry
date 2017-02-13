package reference.binary

import java.io._
import java.nio.{ByteBuffer, ByteOrder, LongBuffer}
import java.nio.channels.FileChannel

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import main.scala.util.BaseCombinationGenerator
import reference.CRISPRSite
import standards.ParameterPack

import scala.collection.mutable
import scala.math._

/**
  * scan a binary encoded file for hits against a specific list of putative targets
  */
object ScanAgainstBinary extends LazyLogging {
  def scanAgainst(binaryFile: File,
                  targets: Array[CRISPRSite],
                  maxMismatch: Int,
                  configuration: ParameterPack,
                  bitCoder: BitEncoding,
                  posCoder: BitPosition): mutable.LinkedHashMap[CRISPRSite, CRISPRSiteOT] = {

    var comparisons = 0l
    val formatter = java.text.NumberFormat.getInstance()
    var t0 = System.nanoTime()

    // setup our input file
    val fos = new FileInputStream(binaryFile)
    val bos = new BufferedInputStream(fos)
    val dos = new DataInputStream(bos)

    // some header information
    if (dos.readLong() != BinaryConstants.magicNumber)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the magic number expected at the top of the file")

    val version = dos.readLong()
    if (version != BinaryConstants.version)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the correct version: " + version + " expecting " + BinaryConstants.version)

    val binCount = dos.readLong()
    val binWidth = (math.log(binCount) / math.log(4)).toInt
    logger.info("Number of characters used for binning " + binWidth)

    val binLookup = new mutable.LinkedHashMap[String, Long]()
    val binGenerator = BaseCombinationGenerator(binWidth)

    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {
      binLookup(bin) = dos.readLong()
    }}

    if (binLookup.size != binCount)
      throw new IllegalStateException("Bin lookup container of size " + binLookup.size + " != expected size " + binCount)

    // where we collect the off-target hits
    val siteSequenceToSite = new mutable.LinkedHashMap[Long,CRISPRSiteOT]()
    targets.foreach { tgt => siteSequenceToSite(bitCoder.bitEncodeString(StringCount(tgt.bases, 1))) = new CRISPRSiteOT(tgt,bitCoder.bitEncodeString(StringCount(tgt.bases, 1))) }

    binGenerator.iterator.zipWithIndex.foreach { case (bin, index) => {

      // what targets do we want to score in this bin
      val targetsToScore = if (configuration.fivePrimePam)
        targets.filter { tgt =>
          mismatches(bin, tgt.bases.slice(configuration.pam.size, configuration.pam.size + binWidth)) <= maxMismatch }.
          map { tgt => bitCoder.bitEncodeString(StringCount(tgt.bases, 1)) }
      else
        targets.filter { tgt =>
          mismatches(bin, tgt.bases.slice(0, binWidth)) <= maxMismatch }.
          map { tgt => bitCoder.bitEncodeString(StringCount(tgt.bases, 1)) }

      // now seek to that bin, and read out the sequences there, and compare to our
      try {

        if (binLookup(bin) > 0) {
          // read the header of our file into a buffer
          var offset = 0
          var mismatchesAdded = 0
          var targetsLookedAt = 0
          var counts = 0


          while (offset < binLookup(bin)) {

            val target = dos.readLong()
            targetsLookedAt += 1
            val coordinatesSize = bitCoder.getCount(target)
            counts += coordinatesSize
            val coordinatesArray = new Array[Long](coordinatesSize)
            var ind = 0
            while (ind < coordinatesSize) {
              coordinatesArray(ind) = dos.readLong()
              ind += 1
            }

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
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
          throw new IllegalStateException("Exception generated from processing binary file")
        }
      }
      if (index % 1000 == 0) {

        logger.info("Comparing the " + formatter.format(index) + "th bin of " + formatter.format(binCount) + " total, off-targets with prefix " +
          bin + ", ran " + formatter.format(comparisons) + " guide comparisons so far. " + ((System.nanoTime() - t0) / 1000000000.0) + " seconds/1K bins")
        t0 = System.nanoTime()
      }
    }
    }

    // where we collect the off-target htis
    // val siteSequenceToSite = targets.map{tgt => (bitCoder.bitEncodeString(StringCount(tgt.bases,1)),new CRISPRSiteOT(tgt))}.toMap
    val returnVal = new mutable.LinkedHashMap[CRISPRSite,CRISPRSiteOT]()
    targets.foreach { tgt => returnVal(tgt) = siteSequenceToSite(bitCoder.bitEncodeString(StringCount(tgt.bases, 1)))}
    returnVal
  }

  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum
}
