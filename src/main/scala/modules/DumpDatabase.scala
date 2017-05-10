package modules

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, GuideMemoryStorage, ResultsAggregator}
import utils.BaseCombinationGenerator
import targetio.TargetOutput
import reference.traverser.{LinearTraverser, SeekTraverser, Traverser}
import reference.{CRISPRSite, ReferenceEncoder}
import reference.binary.{BinaryHeader, DatabaseWriter}
import reference.traversal.{LinearTraversal, OrderedBinTraversalFactory}
import reference.traverser.SeekTraverser._
import reference.traverser.dump.DumpAllGuides
import reference.traverser.parallel.ParallelTraverser
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source
import scopt.options._

/**
  * Created by aaronmck on 4/27/17.
  */
class DumpDatabase extends LazyLogging with Module {

  def runWithOptions(remainingOptions: Seq[String]) {
    // parse the command line arguments
    val parser = new DatabaseDumpBaseOptions()

    parser.parse(remainingOptions, DatabaseDumpConfig()) map {
      config => {
        val formatter = java.text.NumberFormat.getIntegerInstance

        // get our enzyme's (cas9, cpf1) settings
        val params = ParameterPack.nameToParameterPack(config.enzyme)

        // load up their input file, and scan for any potential targets
        logger.info("Reading the header....")
        val header = BinaryHeader.readHeader(config.binaryOTFile + BinaryHeader.headerExtension)

        DumpAllGuides.toFile(new File(config.binaryOTFile),header,params,header.bitCoder,header.bitPosition,config.outputFile)
      }
    }
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class DatabaseDumpConfig(analysisType: Option[String] = None,
                              binaryOTFile: String = "",
                              outputFile: String = "",
                              enzyme: String = "cas9")


class DatabaseDumpBaseOptions extends OptionParser[DatabaseDumpConfig]("DiscoverOTSites") {
  head("DatabaseDumpConfig", "1.0")

  // *********************************** Inputs *******************************************************
  opt[String]("analysis") required() valueName ("<string>") action {
    (x, c) => c.copy(analysisType = Some(x))
  } text ("The run type: one of: discovery, score")

  // *********************************** Inputs *******************************************************
  opt[String]("binaryOTFile") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
  opt[String]("outputFile") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
  opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("prints the usage information you see here")
}