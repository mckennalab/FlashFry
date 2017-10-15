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

package crispr

import java.io._

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

  val bufferedHits = 1000

  // we buffer output, only writing to the output file when we've filled up a bin
  val binToBuffer = new mutable.HashMap[String, ArrayBuffer[CRISPRSite]]()

  val binPrefix = "bin"
  val binSuffix = ".txt"

  binGenerator.iterator.foreach{bin => {
    binToFile(bin) = File.createTempFile(binPrefix + bin , binSuffix, tempLocation)
    binToBuffer(bin) = new ArrayBuffer[CRISPRSite]()
  }}

  /**
    * add a crispr site to the output bins
    * @param cRISPRSite the site to add
    */
  def addHit(cRISPRSite: CRISPRSite): Unit = {
    val putToBin = cRISPRSite.bases.slice(0,binGenerator.width)
    binToBuffer(putToBin) += cRISPRSite
    if (binToBuffer(putToBin).size > bufferedHits) {
      val fw = new FileWriter(binToFile(putToBin), true)
      val bw = new BufferedWriter(fw)
      val output = new PrintWriter(bw)
      binToBuffer(putToBin).toArray.foreach{hit => output.write(hit.to_output + "\n")}
      binToBuffer(putToBin).clear()
      output.close()
    }
  }

  /**
    * close out the files
    * @return a mapping from the bin to a file
    */
  def close(): mutable.HashMap[String,File] = {
    binGenerator.iterator.foreach{bin => {

      val fw = new FileWriter(binToFile(bin), true)
      val bw = new BufferedWriter(fw)
      val output = new PrintWriter(bw)
      binToBuffer(bin).toArray.foreach{hit => output.write(hit.to_output + "\n")}
      output.close()

      // remove the files when we shut down
      binToFile(bin).deleteOnExit()
    }}
    binToFile
  }
}
