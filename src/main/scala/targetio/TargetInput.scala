package targetio

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import reference.CRISPRSite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * this object contains methods that take bed files containing off-targets
  * and produce arrays of targets
  */
object TargetInput {

  def inputBedToTargetArray(inputFile: File, bitEnc: BitEncoding, overflowVal: Int, maxOTMismatch: Int): Tuple2[BitPosition, Array[CRISPRSiteOT]] = {

    val posEncoder = new BitPosition()

    val guides = new ArrayBuffer[CRISPRSiteOT]()

    val annotations = new mutable.HashMap[String,Int]()

    Source.fromFile(inputFile.getAbsolutePath).getLines().foreach{ln => {
      if (ln.startsWith("# contig:"))
        posEncoder.addReference(ln.split(":")(1))
      else if (ln.startsWith("# annotation:"))
        annotations(ln.split(":")(1)) = ln.split(":")(3).toInt
      else {
        val guide = GuideEncodingTools.bedLineToCRISPRSiteOT(ln, bitEnc, posEncoder, overflowVal, annotations.toArray.sortBy(ki => ki._2).map{i => i._1})

        guides += guide
      }
    }}

    (posEncoder,guides.toArray)
  }

}


