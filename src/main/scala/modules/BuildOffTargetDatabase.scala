package modules

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.BinWriter
import utils.BaseCombinationGenerator
import reference.binary.DatabaseWriter
import reference.{ReferenceDictReader, ReferenceEncoder}
import standards.ParameterPack

/**
  * "give me a genome, and I'll give you ALL the potential off-targets, encoded into a binary representation" -this class
  *
  * Febuary 9th, 2017
  */
class BuildOffTargetDatabase(args: Array[String]) extends LazyLogging {

  // parse the command line arguments
  val parser = new scopt.OptionParser[TallyConfig]("Tally") {
    head("TallyScores", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")

    // *********************************** Inputs *******************************************************
    opt[String]("reference") required() valueName ("<string>") action { (x, c) => c.copy(reference = x) } text ("the reference file")
    opt[String]("targetio") required() valueName ("<string>") action { (x, c) => c.copy(output = x) } text ("the output file")
    opt[String]("tmpLocation") required() valueName ("<string>") action { (x, c) => c.copy(tmp = x) } text ("the output file")
    opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")
    opt[Int]("binSize")  valueName ("<int>") action { (x, c) => c.copy(binSize = x) } text ("how many bins (and subsequent open files) should we use when sorting our genome")
  }

  // discover off-targets
  parser.parse(args, TallyConfig()) map {
    config => {
      // setup a decent sized iterator over bases patterns for finding sites
      val binGenerator = BaseCombinationGenerator(6)

      // create out output bins
      val outputBins = BinWriter(new File(config.tmp),binGenerator)

      // get our settings
      val params = ParameterPack.nameToParameterPack(config.enzyme)

      // first discover sites in the target genome -- writing out in bins
      val encoders = ReferenceEncoder.findTargetSites(new File(config.reference), outputBins, params, 0)

      val binToFile = outputBins.close()

      logger.info("Creating the binary file...")

      // setup our bin generator for the output
      val searchBinGenerator = BaseCombinationGenerator(config.binSize)

      // then process this total file into a binary file
      DatabaseWriter.writeToBinnedFileSet(binToFile, binGenerator.width, config.output, encoders._1, encoders._2 , searchBinGenerator, params)


    }}

}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class TallyConfig(analysisType: Option[String] = None,
                       reference: String = "",
                       output: String = "",
                       tmp: String = "/tmp/",
                       enzyme: String = "spCas9",
                       binSize: Int = 9)
