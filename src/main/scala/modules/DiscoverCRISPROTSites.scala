package modules

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition}
import crispr.PAMBuffer
import main.scala.util.BaseCombinationGenerator
import org.slf4j.LoggerFactory
import reference.{CRISPRSite, ReferenceEncoder}
import reference.binary.ScanAgainstBinary
import reference.gprocess.GuideStorage
import standards.StandardScanParameters

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/16/15.
 */
class DiscoverCRISPROTSites(args: Array[String]) {
  // parse the command line arguments
  val parser = new scopt.OptionParser[DiscoverConfig]("DiscoverOTSites") {
    head("DiscoverOTSites", "1.0")

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

  val logger = LoggerFactory.getLogger("DiscoverCRISPRSites")

  parser.parse(args, DiscoverConfig()) map {
    config => {

      // get our enzyme (cas9, cpf1) settings
      val params = StandardScanParameters.nameToParameterPack(config.enzyme)

      // first load up their input file, and scan for any potential targets
      val guideHits = new GuideStorage()
      val encoders = ReferenceEncoder.findTargetSites(new File(config.inputFasta), guideHits, params)

      // get our position encoder and bit encoder setup
      val positionEncoder = BitPosition.fromFile(config.binaryOTFile + BitPosition.positionExtension)
      val bitEcoding = new BitEncoding(params)

      // take this target list and tally against the known binary file
      println("scanning..")
      val mapping = ScanAgainstBinary.scanAgainst(new File(config.binaryOTFile),guideHits.guideHits.toArray,config.maxMismatch,params,bitEcoding,positionEncoder)

      // now given the array of scoring critia, score those sites, and output each scoring schemes normalized score, plus the total score output
      //val outputScoreArray =

      // now output the scores per site
      val outputToScore = new PrintWriter(config.outputFile)
      mapping.foreach{case(tgt,otList) => {
        outputToScore.write(tgt.to_output + "\t" + otList.offTargets.map{ot => {
          bitEcoding.bitDecodeString(ot.sequence).str + "_" + bitEcoding.bitDecodeString(ot.sequence).count
        }}.mkString(",")
        + "\n"
        )
      }}

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