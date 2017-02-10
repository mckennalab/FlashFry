package modules

import java.io.{File, PrintWriter}

import bitcoding.BitPosition
import crispr.PAMBuffer
import main.scala.util.BaseCombinationGenerator
import org.slf4j.LoggerFactory
import reference.CRISPRSite
import reference.binary.ScanAgainstBinary
import standards.StandardScanParameters

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/16/15.
 */
class DiscoverCRISPRSites(args: Array[String]) {
  // parse the command line arguments
  val parser = new scopt.OptionParser[DiscoverConfig]("DiscoverSites") {
    head("DiscoverSites", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")

    // *********************************** Inputs *******************************************************
    opt[String]("inputFasta") required() valueName ("<string>") action { (x, c) => c.copy(inputFasta = x) } text ("the reference file to scan for putitive targets")
    opt[String]("binaryOTFile") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
    opt[String]("outputFile") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
    opt[Int]("maxMismatch")  valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches we allow")
    opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")

    // some general command-line setup stuff
    note("match off-targets for the specified guides to the genome of interest\n")
    help("help") text ("prints the usage information you see here")
  }

  val logger = LoggerFactory.getLogger("Main")

  parser.parse(args, DiscoverConfig()) map {
    config => {

      // get our enzyme (cas9, cpf1) settings
      val params = StandardScanParameters.nameToParameterPack(config.enzyme)

      // first load up their input file, and scan for any potential targets

      /**
        * def scanAgainst(binaryFile: File,
                  targets: Array[CRISPRSite],
                  maxMismatch: Int,
                  configuration: ParameterPack,
                  bitCoder: BitEncoding,
                  posCoder: BitPosition): Map[CRISPRSite,CRISPRSiteOT] = {
        */

      val positionEncoder = BitPosition.fromFile(config.binaryOTFile + BitPosition.positionExtension)

      // take this target list and score against the known binary file
      //ScanAgainstBinary.scanAgainst(new File(config.binaryOTFile),Array[CRISPRSite](),config.maxMismatch,params,)

      }
  }

}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class DiscoverConfig(analysisType: Option[String] = None,
                          inputFasta: String = "",
                          binaryOTFile: String = "",
                          outputFile: String = "",
                          enzyme: String = "cas9",
                          maxMismatch: Int = 5)