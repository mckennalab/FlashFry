package crispr

import java.io.{File, PrintWriter}

import main.scala.util.BaseCombinationGenerator
import org.slf4j.LoggerFactory
import reference.CRISPRSite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * handle writing hits to sorted output files.  When we go to close, we merge them
  * all back into one master sorted file
  */
case class BinWriter(tempLocation: File, binGenerator: BaseCombinationGenerator) extends GuideContainer {
  val logger = LoggerFactory.getLogger("BinWriter")
  val binToFile = new mutable.HashMap[String, File]()
  val binToWriter = new mutable.HashMap[String, PrintWriter]()
  val binPrefix = "bin"
  val binSuffix = ".txt"

  binGenerator.iterator.foreach{bin => {
    binToFile(bin) = File.createTempFile(binPrefix + bin , binSuffix, tempLocation)
    binToWriter(bin) = new PrintWriter(binToFile(bin).getAbsolutePath)
  }}

  def addHit(cRISPRSite: CRISPRSite): Unit = {
    val putToBin = cRISPRSite.bases.slice(0,binGenerator.width)
    binToWriter(putToBin).write(cRISPRSite.to_output + "\n")
  }

  def close(outputFile: File): Unit = {
    val totalOutput = new PrintWriter(outputFile)
    binGenerator.iterator.foreach{bin => {
      //logger.info("Merging back bin " + bin)
      binToWriter(bin).close()

      val toSort = new ArrayBuffer[CRISPRSite]()
      Source.fromFile(binToFile(bin).getAbsolutePath).getLines().foreach{line => {
        toSort += CRISPRSite.fromLine(line)
      }}

      val toSortResults = toSort.toArray
      scala.util.Sorting.quickSort(toSortResults)
      toSortResults.foreach{hit => {
        totalOutput.write(hit.to_output + "\n")
      }}

      // remove the files when we shut down
      binToFile(bin).deleteOnExit()
    }}
    totalOutput.close()
  }

}
