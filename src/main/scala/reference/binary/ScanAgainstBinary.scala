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

    val iStream = new DataInputStream(new FileInputStream(binaryFile))

    // some header information
    if (iStream.readLong() != BinaryConstants.magicNumber)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the magic number expected at the top of the file")

    val version = iStream.readLong()
    if (version != BinaryConstants.version)
      throw new IllegalStateException("Binary file " + binaryFile.getAbsolutePath + " doesn't have the correct version: " + version + " expecting " + BinaryConstants.version)

    val binCount = iStream.readLong()
    val binWidth = (math.log(binCount)/math.log(4)).toInt
    println("Number of characters used for binning " + binWidth)

    val binLookup = new mutable.LinkedHashMap[String,Long]()
    val binGenerator = BaseCombinationGenerator(binWidth)
    binGenerator.iterator.zipWithIndex.foreach{case(bin,index) => {
      binLookup(bin) = iStream.readLong()
    }}

    if (binLookup.size != binCount)
      throw new IllegalStateException("Bin lookup container of size " + binLookup.size + " != expected size " + binCount)
    iStream.close()

    // where we collect the off-target htis
    val siteSequenceToSite = targets.map{tgt => (bitCoder.bitEncodeString(StringCount(tgt.bases,1)),new CRISPRSiteOT(tgt))}.toMap

    binGenerator.iterator.zipWithIndex.foreach{case(bin,index) => {
      // what targets do we want to score in this bin
      val targetsToScore =
        if (configuration.fivePrimePam)
          targets.filter{tgt => mismatches(bin,tgt.bases.slice(configuration.pam.size,configuration.pam.size + binWidth)) <= maxMismatch}.map{tgt => bitCoder.bitEncodeString(StringCount(tgt.bases,1))}
        else
          targets.filter{tgt => mismatches(bin,tgt.bases.slice(0,binWidth)) <= maxMismatch}.map{tgt => bitCoder.bitEncodeString(StringCount(tgt.bases,1))}

      // now seek to that bin, and read out the sequences there, and compare to our
      try {
        val stream = new FileInputStream(binaryFile)
        val inChannel = stream.getChannel()

        val buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());

        val longsToGet = binLookup(bin) / BinaryConstants.bytesPerTarget
        val result = new Array[Long](longsToGet.toInt)

        buffer.order( ByteOrder.BIG_ENDIAN )
        val longBuffer = buffer.asLongBuffer( )
        longBuffer.get(result)

        var offset = 0
        while (offset < longsToGet) {
          val target = result(offset)
          val coordinatesSize = bitCoder.getCount(target)
          val coordinatesArray = result.slice(offset + 1, offset + 1 + coordinatesSize)

          // now check each target we have -- what's their distance
          targetsToScore.foreach{targetEncoding => {
            if (bitCoder.mismatches(targetEncoding,target) <= maxMismatch) {
              siteSequenceToSite(targetEncoding).addOT(CRISPRHit(target,coordinatesArray))
            }
          }}

          offset += 1 + coordinatesSize
        }
        var target = result(0)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          throw new IllegalStateException("UNable to work with binary file")
        }
      }
    }}

    // where we collect the off-target htis
    // val siteSequenceToSite = targets.map{tgt => (bitCoder.bitEncodeString(StringCount(tgt.bases,1)),new CRISPRSiteOT(tgt))}.toMap
    targets.map{tgt => (tgt,siteSequenceToSite(bitCoder.bitEncodeString(StringCount(tgt.bases,1))))}.toMap
  }

  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map{case(a,b) => if (a == b) 0 else 1}.sum

}
