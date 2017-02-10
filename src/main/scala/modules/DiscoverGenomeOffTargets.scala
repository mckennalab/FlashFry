package modules

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import main.scala.util.BaseCombinationGenerator
import org.slf4j.LoggerFactory
import reference.binary.BinarySiteWriter
import reference.gprocess.BinWriter
import reference.{ReferenceDictReader, ReferenceEncoder}
import standards.StandardScanParameters

/**
  * Created by aaronmck on 2/9/17.
  */
class DiscoverGenomeOffTargets(args: Array[String]) {

  // parse the command line arguments
  val parser = new scopt.OptionParser[TallyConfig]("Tally") {
    head("TallyScores", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")

    // *********************************** Inputs *******************************************************
    opt[String]("reference") required() valueName ("<string>") action { (x, c) => c.copy(reference = x) } text ("the reference file")
    opt[String]("output") required() valueName ("<string>") action { (x, c) => c.copy(output = x) } text ("the output file")
    opt[String]("tmpLocation") required() valueName ("<string>") action { (x, c) => c.copy(tmp = x) } text ("the output file")
    opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")
  }

  // now use the parsed configuration options to run the tool
  parser.parse(args, TallyConfig()) map {
    config => {
      val logger = LoggerFactory.getLogger("DiscoverGenomeOffTargets")

      // setup a decent sized iterator over bases patterns for finding sites
      val binGenerator = BaseCombinationGenerator(5)

      // create out output bins
      val outputBins = BinWriter(new File(config.tmp),binGenerator)

      // get our settings
      val params = StandardScanParameters.nameToParameterPack(config.enzyme)

      // first discover sites in the target genome -- writing out in bins
      val encoders = ReferenceEncoder.findTargetSites(new File(config.reference), outputBins, params)

      // sort them into an output file
      val totalOutput = File.createTempFile(config.tmp + File.separator + "totalFile",".txt")
      println("Creating output file " + totalOutput)

      //totalOutput.deleteOnExit()
      outputBins.close(totalOutput)

      // setup our bin generator for the output
      val searchBinGenerator = BaseCombinationGenerator(config.binSize)

      // then process this total file into a binary file
      BinarySiteWriter.writeToBinnedFile(totalOutput.getAbsolutePath, config.output, encoders._1, encoders._2 , searchBinGenerator, params.pam, params.fivePrimePam)

      // now write the position encoder information to a companion file
      BitPosition.toFile(encoders._2, config.output + BitPosition.positionExtension)

    }}

}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class TallyConfig(analysisType: Option[String] = None,
                       reference: String = "",
                       output: String = "",
                       tmp: String = "/tmp/",
                       enzyme: String = "cas9",
                       binSize: Int = 9)
