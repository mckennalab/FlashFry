package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import reference.filter.HitFilter
import reference.gprocess.{GuideWriter}
import standards.ParameterPack

import scala.io._

/**
  * encode a reference file to a binary file
  */
object ReferenceEncoder extends LazyLogging {
  def findTargetSites(reference: File, binWriter: GuideWriter, params: ParameterPack, filters: Array[HitFilter]): Tuple2[BitEncoding, BitPosition] = {

    val bitEncoder = new BitEncoding(params)
    val posEncoder = new BitPosition()
    val cls = CRISPRCircle(binWriter, params, filters)

    Source.fromFile(reference).getLines().foreach { line => {
      if (line.startsWith(">")) {
        logger.info("Switching to chromosome " + line)
        posEncoder.addReference(line.stripPrefix(">").split(" ")(0))
        cls.reset(line.split(" ")(0).slice(1, line.split(" ")(0).length))
      } else {
        cls.addLine(line.toUpperCase)
      }
    }
    }
    logger.info("Done looking for targets")
    (bitEncoder, posEncoder)
  }
}


// --------------------------------------------------------------------------------------------
// this class finds potential CRISPR cutsites in the genome -- we use a rolling (circular)
// buffer to speed things up. This class is really ugly in places, but it morphed from canonical
// scala for speed reasons
// --------------------------------------------------------------------------------------------
case class CRISPRCircle(binWriter: GuideWriter, params: ParameterPack, filters: Array[HitFilter]) extends LazyLogging {

  val revCompPam = reverseCompString(params.pam)
  val compPam = compString(params.pam)
  val cutSiteFromEnd = 6
  var stack = new Array[Char](params.totalScanLength)
  var currentPos = 0
  var contig = "UNKNOWN"

  def addLine(line: String) {
    line.foreach { chr => {
      addBase(chr)
    }
    }
  }

  /**
    * add a base to our circular buffer
    */
  def addBase(chr: Char) {
    stack(currentPos % params.totalScanLength) = chr
    currentPos += 1
    if (currentPos >= params.totalScanLength)
      checkCRISPR().foreach { ct => {
        if (filters.size == 0) {
          binWriter.addHit(ct)
        } else {
          val keep = filters.map{ft => if (ft.filter(ct)) 0 else 1}.sum == 0
          if (keep)
            binWriter.addHit(ct)
          else
            logger.info("Rejecting guide " + ct.bases)
        }
      }
      }
  }

  def reset(cntig: String) {
    contig = cntig
    currentPos = 0
  }

  /**
    * check both ends of a string for the specified pam sequence
    * @param pam the pam sequence
    * @param compPam the complement of the pam
    * @return true if we match on either strand
    */
  def checkForPAM(pam: String, compPam: String): Boolean = {
    {
      var hits = 0
      var bases = 0
      while (bases < pam.size) {
        if (stack((currentPos - params.totalScanLength + bases) % params.totalScanLength) == pam(bases))
          hits += 1
        bases += 1
      }
      hits == params.pam.size
    } || {
      var rhits = 0
      var rbases = 1
      while (rbases <= compPam.size) {
        if (stack((currentPos - rbases) % params.totalScanLength) == compPam(params.pam.size - rbases))
          rhits += 1
        rbases += 1
      }
      rhits == params.pam.size
    }

  }


  def checkCRISPR(): Array[CRISPRSite] = {

    val matched = if (params.fivePrimePam) checkForPAM(params.pam, compPam) else checkForPAM(compPam, params.pam)

    if (!matched || stack.map { base => if (base == 'A' || base == 'C' || base == 'G' || base == 'T') 0 else 1 }.sum > 0)
      return Array[CRISPRSite]()


    val str = toTarget()
    var ret = Array[CRISPRSite]()

    if (!params.fivePrimePam) {
      if (str.endsWith(params.pam))
        ret :+= CRISPRSite(contig, str, true, currentPos - params.totalScanLength, currentPos)
      if (str.startsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, currentPos - params.totalScanLength, currentPos)
      ret
    } else {
      if (str.startsWith(params.pam))
        ret :+= CRISPRSite(contig, str, true, (currentPos - params.totalScanLength), (currentPos))
      if (str.endsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, currentPos - params.totalScanLength, currentPos)
      ret
    }
  }

  // utilities to reverse complement
  def compBase(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c

  def compString(str: String): String = str.map {
    compBase(_)
  }.mkString

  def reverseCompString(str: String): String = compString(str).reverse

  // create a target string from the buffer
  def toTarget(): String = stack.slice(currentPos % params.totalScanLength, params.totalScanLength).mkString + stack.slice(0, currentPos % params.totalScanLength).mkString.toUpperCase()

}

