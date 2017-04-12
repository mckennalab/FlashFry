package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.GuideContainer
import utils.Utils
import crispr.filter.SequencePreFilter
import standards.ParameterPack

import scala.collection.mutable
import scala.io._

/**
  * encode a reference file to a binary file
  */
object ReferenceEncoder extends LazyLogging {

  /**
    * given a reference, find all of the potential target sequences and send them to the guide storage
    *
    * @param reference the reference file, either as plain text or with a gz extension
    * @param binWriter the output location for any guides we find
    * @param params    the parameter pack, defining the enzyme's parameters
    * @return the encoding schemes that this parameter pack and refenence use
    */
  def findTargetSites(reference: File, binWriter: GuideContainer, params: ParameterPack, flankingSequence: Int): Tuple2[BitEncoding, BitPosition] = {

    val bitEncoder = new BitEncoding(params)
    val posEncoder = new BitPosition()

    val cls: CRISPRDiscovery = if (flankingSequence == 0) {
      logger.info("Running with fast circle-buffer site finder due to no flanking sequence request")
      CRISPRCircleBuffer(binWriter, params)
    } else {
      logger.info("Running with simple site finder due to flanking sequence request")
      SimpleSiteFinder(binWriter, params, flankingSequence)
    }

    fileToSource(reference).getLines().foreach { line => {
      if (line.startsWith(">")) {
        //logger.info("Switching to chromosome " + line)
        posEncoder.addReference(line.stripPrefix(">").split(" ")(0))
        cls.reset(line.split(" ")(0).slice(1, line.split(" ")(0).length))
      } else {
        cls.addLine(line.toUpperCase)
      }
    }
    }
    logger.info("Done looking for targets")
    cls.close()

    (bitEncoder, posEncoder)
  }


  /**
    * @param file the input file, either gzipped (.gz) or plain text
    * @return a Source object for this file
    */
  def fileToSource(file: File): Source = {

    if (file.getAbsolutePath endsWith ".gz")
      Source.fromInputStream(Utils.gis(file.getAbsolutePath))
    else
      Source.fromFile(file.getAbsolutePath)
  }
}


/**
  *
  */
trait CRISPRDiscovery {
  def addLine(line: String)

  def reset(contig: String)

  def close()
}

/**
  * simple implementation of a site finder -- just concat the contigs and look for site in the final
  *
  * @param binWriter        where to send the site we've found
  * @param params           the parameters to look for
  * @param flankingSequence pull out X bases on either side of the putitive target
  */
case class SimpleSiteFinder(binWriter: GuideContainer, params: ParameterPack, flankingSequence: Int)
  extends LazyLogging with CRISPRDiscovery {

  val currentBuffer = mutable.ArrayBuilder.make[String]()
  var currentContig: Option[String] = None
  var targetCount = 0

  override def addLine(line: String): Unit = currentBuffer += line
  def getTotal: Int = targetCount

  override def reset(contig: String): Unit = {
    // first processes the sequence supplied, looking for guides
    val contigBuffer = currentBuffer.result().mkString("")

    if (currentContig.isDefined) {
      // check forward
      (params.fwdRegex findAllIn contigBuffer).matchData.foreach { fwdMatch => {
        val subStr = contigBuffer.slice(fwdMatch.start, fwdMatch.end)
        if (!subStr.contains("N")) {
          val context = contigBuffer.slice(math.max(0, fwdMatch.start - flankingSequence), fwdMatch.end + flankingSequence)

          var site = CRISPRSite(currentContig.get, subStr, true, fwdMatch.start, if (context.size == subStr.size + (flankingSequence * 2)) Some(context) else None)
          binWriter.addHit(site)
          targetCount += 1
        }
      }
      }

      // check reverse
      (params.revRegex findAllIn contigBuffer).matchData.foreach { revMatch => {
        val subStr = Utils.reverseCompString(contigBuffer.slice(revMatch.start, revMatch.end))
        if (!subStr.contains("N")) {
          val context = Utils.reverseCompString(contigBuffer.slice(math.max(0, revMatch.start - flankingSequence), revMatch.end + flankingSequence))

          var site = CRISPRSite(currentContig.get, subStr, false, revMatch.start, if (context.size == subStr.size + (flankingSequence * 2)) Some(context) else None)
          binWriter.addHit(site)
        }
      }
      }


    }
    // now handle the reset part -- change the contig to the the new sequence name and clear the buffer
    currentContig = Some(contig)
    currentBuffer.clear()
  }

  override def close(): Unit = {
    reset("")
  }

}


/**
  * this class finds potential CRISPR cutsites in the genome -- we use a rolling (circular)
  * buffer to speed things up. This class is really ugly in places, but it morphed from canonical
  * scala for speed reasons
  *
  * @param binWriter the output container we send hits to
  * @param params    what kind of guide we're looking for
  */
case class CRISPRCircleBuffer(binWriter: GuideContainer, params: ParameterPack) extends LazyLogging with CRISPRDiscovery {

  val revCompPams = params.pam.map { pm => Utils.reverseCompString(pm) }
  val compPams = params.pam.map { pm => Utils.compString(pm) }
  val cutSiteFromEnd = 6
  var stack = new Array[Char](params.totalScanLength)
  var currentPos = 0
  var targetsFound = 0
  var contig = "UNKNOWN"

  def getTotal: Int = targetsFound

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
        binWriter.addHit(ct)
        targetsFound += 1
      }
      }
  }

  def reset(cntig: String) {
    contig = cntig
    currentPos = 0
  }

  /**
    * check both ends of a string for the specified pam sequence
    *
    * @param pam     the pam sequence
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
      hits == pam.size
    } || {
      var rhits = 0
      var rbases = 1
      while (rbases <= compPam.size) {
        if (stack((currentPos - rbases) % params.totalScanLength) == compPam(pam.size - rbases))
          rhits += 1
        rbases += 1
      }
      rhits == pam.size
    }

  }


  def checkCRISPR(): Array[CRISPRSite] = {

    val matched =
      if (params.fivePrimePam)
        params.pam.zip(compPams).map { case (pm, cmp) => checkForPAM(pm, cmp) }.filter(t => t).size > 0
      else
        params.pam.zip(compPams).map { case (pm, cmp) => checkForPAM(cmp, pm) }.filter(t => t).size > 0

    if (!matched || stack.map { base => if (base == 'A' || base == 'C' || base == 'G' || base == 'T') 0 else 1 }.sum > 0)
      return Array[CRISPRSite]()


    val str = toTarget()
    var ret = Array[CRISPRSite]()

    if (!params.fivePrimePam) {
      params.pam.zip(revCompPams).foreach { case(pm,rcp) => {
        if (str.endsWith(pm))
          ret :+= CRISPRSite(contig, str, true, currentPos - params.totalScanLength, None)
        if (str.startsWith(rcp))
          ret :+= CRISPRSite(contig, Utils.reverseCompString(str), false, currentPos - params.totalScanLength, None)
        }}
        ret
      }
      else
      {
        params.pam.zip(revCompPams).foreach { case(pm,rcp) => {
          if (str.startsWith(pm))
            ret :+= CRISPRSite(contig, str, true, (currentPos - params.totalScanLength), None)
          if (str.endsWith(rcp))
            ret :+= CRISPRSite(contig, Utils.reverseCompString(str), false, currentPos - params.totalScanLength, None)
        }}
        ret
      }
    }


    // create a target string from the buffer
    def toTarget(): String = stack.slice(currentPos % params.totalScanLength, params.totalScanLength).mkString +
      stack.slice(0, currentPos % params.totalScanLength).mkString.toUpperCase()

    override def close(): Unit

    =
    {} // do nothing
  }

