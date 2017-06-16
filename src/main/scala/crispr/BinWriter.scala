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

package crispr

import java.io.{File, PrintStream, PrintWriter}

import utils.{BaseCombinationGenerator, Utils}
import org.slf4j.LoggerFactory
import reference.CRISPRSite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * handle writing off-target hits to sorted output files. When the BinWriter is closed, we merge all the off-targets
  * back into one master sorted file
  */
case class BinWriter(tempLocation: File, binGenerator: BaseCombinationGenerator) extends GuideContainer {

  val logger = LoggerFactory.getLogger("BinWriter")
  val binToFile = new mutable.HashMap[String, File]()
  val binToWriter = new mutable.HashMap[String, PrintWriter]()
  val binPrefix = "bin"
  val binSuffix = ".txt"

  binGenerator.iterator.foreach{bin => {
    binToFile(bin) = File.createTempFile(binPrefix + bin , binSuffix, tempLocation)
    binToWriter(bin) = new PrintWriter(Utils.gos(binToFile(bin).getAbsolutePath))
  }}

  /**
    * add a crispr site to the output bins
    * @param cRISPRSite the site to add
    */
  def addHit(cRISPRSite: CRISPRSite): Unit = {
    val putToBin = cRISPRSite.bases.slice(0,binGenerator.width)
    binToWriter(putToBin).write(cRISPRSite.to_output + "\n")
  }

  /**
    * close out the files
    * @return a mapping from the bin to a file
    */
  def close(): mutable.HashMap[String,File] = {
    binGenerator.iterator.foreach{bin => {
      binToWriter(bin).close()

      // remove the files when we shut down
      //binToFile(bin).deleteOnExit()
    }}
    binToFile
  }
}
