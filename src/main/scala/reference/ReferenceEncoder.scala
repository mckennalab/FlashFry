package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import org.slf4j.LoggerFactory
import reference.gprocess.{BinWriter, GuideWriter}
import standards.ParameterPack

import scala.io._

/**
  * encode a reference file to a binary file
  */
object ReferenceEncoder {
  val logger = LoggerFactory.getLogger("ReferenceEncoder")

  def findTargetSites(reference: File, binWriter: GuideWriter, params: ParameterPack): Tuple2[BitEncoding,BitPosition] = {

    val bitEncoder = new BitEncoding(params.totalScanLength, params.comparisonBitEncoding)
    val posEncoder = new BitPosition()
    val cls = CRISPRCircle(binWriter, params)

    Source.fromFile(reference).getLines().foreach{line => {
      if (line.startsWith(">")) {
        logger.info("Switching to chromosome " + line)
        posEncoder.addReference(line.stripPrefix(">").split(" ")(0))
        cls.reset(line.split(" ")(0).slice(1,line.split(" ")(0).length))
      } else {
        cls.addLine(line.toUpperCase)
      }
    }}
    logger.info("Done looking for targets")
    (bitEncoder,posEncoder)
  }
}






// --------------------------------------------------------------------------------------------
// this class finds potential CRISPR cutsites in the genome -- we use a rolling (circular)
// buffer to speed things up
// --------------------------------------------------------------------------------------------
case class CRISPRCircle(binWriter: GuideWriter, params: ParameterPack) {

  val revCompPam = reverseCompString(params.pam)
  val cutSiteFromEnd = 6
  var stack = new Array[Char](params.totalScanLength)
  var currentPos = 0
  var contig = "UNKNOWN"

  def addLine(line: String) {line.foreach{chr => {addBase(chr)}}}

  /**
    * add a base to our circular buffer
    */
  def addBase(chr: Char) {
    stack(currentPos % params.totalScanLength) = chr
    currentPos += 1
    if (currentPos >= params.totalScanLength)
      checkCRISPR().foreach{ct => {
        binWriter.addHit(ct)
      }}
  }

  def reset(cntig: String) {
    contig = cntig
    currentPos = 0
  }

  def checkCRISPR(): Array[CRISPRSite] = {
    val str = toTarget()
    if (str.map{base => if (base == 'A' || base == 'C' || base == 'G' || base == 'T') 0 else 1}.sum > 0)
      return Array[CRISPRSite]()

    var ret = Array[CRISPRSite]()

    if (!params.fivePrimePam) {
      if (str.endsWith(params.pam))
        ret :+= CRISPRSite(contig, str, true, (currentPos - params.totalScanLength), (currentPos - cutSiteFromEnd))
      if (str.startsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, (currentPos - params.totalScanLength), (currentPos - params.totalScanLength) + cutSiteFromEnd)
      ret
    } else {
      if (str.startsWith(params.pam))
        ret :+= CRISPRSite(contig, str, true, (currentPos - params.totalScanLength), (currentPos - cutSiteFromEnd))
      if (str.endsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, (currentPos - params.totalScanLength), (currentPos - params.totalScanLength) + cutSiteFromEnd)
      ret
    }
  }

  // utilities to reverse complement
  def reverseComp(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
  def reverseCompString(str: String): String = str.map{reverseComp(_)}.reverse.mkString

  // create a target string from the buffer
  def toTarget(): String = stack.slice(currentPos % params.totalScanLength,params.totalScanLength).mkString + stack.slice(0,currentPos % params.totalScanLength).mkString.toUpperCase()

}

