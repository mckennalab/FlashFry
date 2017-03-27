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
    binToWriter(bin) = new PrintWriter(binToFile(bin).getAbsolutePath)
  }}

  def addHit(cRISPRSite: CRISPRSite): Unit = {
    val putToBin = cRISPRSite.bases.slice(0,binGenerator.width)
    binToWriter(putToBin).write(cRISPRSite.to_output + "\n")
  }

  def close(outputFile: File): Unit = {
    val totalOutput = new PrintStream(Utils.gos(outputFile.getAbsolutePath)) // PrintWriter(outputFile)
    binGenerator.iterator.foreach{bin => {
      binToWriter(bin).close()

      val toSort = new ArrayBuffer[CRISPRSite]()
      Source.fromFile(binToFile(bin).getAbsolutePath).getLines().foreach{line => {
        toSort += CRISPRSite.fromLine(line)
      }}

      val toSortResults = toSort.toArray
      scala.util.Sorting.quickSort(toSortResults)
      toSortResults.foreach{hit => {
        totalOutput.println(hit.to_output)
      }}

      // remove the files when we shut down
      binToFile(bin).deleteOnExit()
    }}
    totalOutput.close()
  }

}
