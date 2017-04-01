package targetio

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import reference.CRISPRSite

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * this object contains methods that take bed files containing off-targets
  * and produce arrays of targets
  */
object TargetInput {

  def inputBedToTargetArray(inputFile: File, bitEnc: BitEncoding, overflowVal: Int): Tuple2[BitPosition, Array[CRISPRSiteOT]] = {

    val posEncoder = new BitPosition()

    val guides = new ArrayBuffer[CRISPRSiteOT]()

    Source.fromFile(inputFile.getAbsolutePath).getLines().foreach{ln => {
      if (ln.startsWith("# contig:"))
        posEncoder.addReference(ln.split(":")(1))
      else
        guides += GuideEncodingTools.bedLineToCRISPRSiteOT(ln, bitEnc,posEncoder, overflowVal)
    }}

    (posEncoder,guides.toArray)
  }

}


