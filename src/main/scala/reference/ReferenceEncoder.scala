/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.GuideContainer
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
        //logger.info("Switching to chromosome " + line)
        posEncoder.addReference(line.stripPrefix(">").split(" ")(0))
        cls.reset(line.split(" ")(0).slice(1, line.split(" ")(0).length))
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
        var site = CRISPRSite(currentContig.get, subStr, true, start, Some(context))
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
        var site = CRISPRSite(currentContig.get, subStr, false, start, Some(context))
        binWriter.addHit(site)
        targetCount += 1

      }
      }
      logger.info("Done looking for targets on chromosome " + currentContig.getOrElse("UKNOWN") + "...")

    }
    // now handle the reset part -- change the contig to the the new sequence name and clear the buffer
    currentContig = Some(contig)
    currentBuffer.clear()
  }

  override def close(): Unit = {
    reset("")
  }

}

