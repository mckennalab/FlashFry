/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSite, GuideContainer}
import utils.Utils
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

    val cls: CRISPRDiscovery = SimpleSiteFinder(binWriter, params, flankingSequence)

    fileToSource(reference).getLines().foreach { line => {
      if (line.startsWith(">")) {

        val convertedReferenceName = line.stripPrefix(">").replace(' ','_').replace('\t','_')

        logger.info("Switching to chromosome " + line)

        posEncoder.addReference(convertedReferenceName)
        cls.reset(convertedReferenceName)
      } else {
        cls.addLine(line.toUpperCase)
      }
    }
    }
    cls.close()
    logger.info("Done looking for targets...")
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
case class SimpleSiteFinder(binWriter: GuideContainer, params: ParameterPack, flankingSequence: Int) extends LazyLogging with CRISPRDiscovery {

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
      (params.fwdRegex findAllMatchIn contigBuffer).foreach { fwdMatch => {
        val start = fwdMatch.start
        val end = fwdMatch.start + params.totalScanLength
        val subStr = contigBuffer.slice(start, end)

        val context = contigBuffer.slice(math.max(0, start - flankingSequence), end + flankingSequence)

        // add the site, ONLY if it's as large as the target plus the context they ask for
        var site = CRISPRSite(currentContig.get,
          subStr,
          true,
          start,
          if (context.size == params.totalScanLength + (2 * flankingSequence))
            Some(context)
          else
            None)

        binWriter.addHit(site)
        targetCount += 1

      }
      }

      // check reverse
      (params.revRegex findAllMatchIn contigBuffer).foreach { revMatch => {
        val start = revMatch.start
        val end = revMatch.start + params.totalScanLength

        val subStr = Utils.reverseCompString(contigBuffer.slice(start, end))

        val context = Utils.reverseCompString(contigBuffer.slice(math.max(0, start - flankingSequence), end + flankingSequence))
        var site = CRISPRSite(currentContig.get,
          subStr,
          false,
          start,
          if (context.size == params.totalScanLength + (2 * flankingSequence))
            Some(context)
          else
            None)
        binWriter.addHit(site)
        targetCount += 1

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

