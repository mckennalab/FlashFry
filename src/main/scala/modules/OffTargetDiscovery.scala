package modules

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, GuideMemoryStorage}
import utils.BaseCombinationGenerator
import output.TargetOutput
import reference.traverser.{LinearTraverser, SeekTraverser, Traverser}
import reference.{CRISPRSite, ReferenceEncoder}
import reference.binary.BinaryHeader
import reference.traversal.{LinearTraversal, OrderedBinTraversalFactory}
import reference.traverser.SeekTraverser._
import reference.traverser.parallel.ParallelTraverser
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source

/**
  * Scan a fasta file for targets and tally their off-targets against the genome
  */
class OffTargetDiscovery(args: Array[String]) extends LazyLogging {

  // parse the command line arguments
  val parser = new OffTargetBaseOptions()

  parser.parse(args, DiscoverConfig()) map {
    config => {
      val initialTime = System.nanoTime()

      val formatter = java.text.NumberFormat.getIntegerInstance

      // get our enzyme's (cas9, cpf1) settings
      val params = ParameterPack.nameToParameterPack(config.enzyme)

      // load up their input file, and scan for any potential targets
      val guideHits = new GuideMemoryStorage()
      val encoders = ReferenceEncoder.findTargetSites(new File(config.inputFasta), guideHits, params, config.flankingSequence)

      logger.info("Reading the header....")
      val header = BinaryHeader.readHeader(config.binaryOTFile + BinaryHeader.headerExtension)

      // transform our targets into a list for off-target collection
      logger.info("Setting up the guide recording....")
      val guideOTStorage = guideHits.guideHits.map {
        guide => new CRISPRSiteOT(guide, header.bitCoder.bitEncodeString(StringCount(guide.bases, 1)), config.maximumOffTargets)
      }.toArray

      logger.info("Determine how many bins we'll traverse....")
      var traversalFactory: Option[OrderedBinTraversalFactory] = None

      if (!config.forceLinear)
        traversalFactory = Some(new OrderedBinTraversalFactory(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideOTStorage))

      logger.info("scanning against the known targets from the genome with " + guideHits.guideHits.toArray.size + " guides")

      val isTraversalSaturdated = if (traversalFactory.isDefined) traversalFactory.get.saturated else false

      (config.forceLinear, isTraversalSaturdated, config.numberOfThreads) match {
        case (fl, sat, threads) if ((fl | sat) & threads == 1) => {
          val lTrav = new LinearTraversal(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideOTStorage)
          logger.info("Starting linear traversal")
          LinearTraverser.scan(new File(config.binaryOTFile), header, lTrav, guideOTStorage, config.maxMismatch, params, header.bitCoder, header.bitPosition)
        }
        case (fl, sat, threads) if (!fl & !sat & threads == 1) => {
          logger.info("Starting seek traversal")
          SeekTraverser.scan(new File(config.binaryOTFile), header, traversalFactory.get.iterator, guideOTStorage, config.maxMismatch, params, header.bitCoder, header.bitPosition)
        }
        case (fl, sat, threads) if (!fl & !sat & threads > 1) => {
          logger.info("Starting parallel traversal")
          ParallelTraverser.numberOfThreads = threads
          ParallelTraverser.scan(new File(config.binaryOTFile), header, traversalFactory.get.iterator, guideOTStorage, config.maxMismatch, params, header.bitCoder, header.bitPosition)
        }
        case (fl, sat, threads) if (!fl & threads > 1) => {
          logger.info("Starting parallel linear traversal")
          val lTrav = new LinearTraversal(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideOTStorage)
          ParallelTraverser.numberOfThreads = threads
          ParallelTraverser.scan(new File(config.binaryOTFile), header, lTrav, guideOTStorage, config.maxMismatch, params, header.bitCoder, header.bitPosition)
        }
        case (fl, sat, threads) => {
          throw new IllegalStateException("We don't have a run type when --forceLinear=" + fl + ", binSaturation=" + sat + ", and --numberOfThreads=" + threads)
        }
      }

      logger.info("Performed a total of " + formatter.format(Traverser.allComparisions) + " guide to target comparisons")
      logger.info("Writing final output for " + guideHits.guideHits.toArray.size + " guides")

      // now output the scores per site
      val tgtOutput = TargetOutput(config.outputFile,
        guideOTStorage,
        config.includePositionOutputInformation,
        config.markTargetsWithExactGenomeHits,
        header.bitCoder,
        header.bitPosition)

      println("Total runtime " + ((System.nanoTime() - initialTime) / 1000000000.0) + " seconds")
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
                          maxMismatch: Int = 5,
                          includePositionOutputInformation: Boolean = false,
                          markTargetsWithExactGenomeHits: Boolean = false,
                          flankingSequence: Int = 10,
                          maximumOffTargets: Int = 2000,
                          forceLinear: Boolean = false,
                          numberOfThreads: Int = 1)


class OffTargetBaseOptions extends scopt.OptionParser[DiscoverConfig]("DiscoverOTSites") {
  head("DiscoverOTSites", "1.0")

  // *********************************** Inputs *******************************************************
  opt[String]("analysis") required() valueName ("<string>") action {
  (x, c) => c.copy(analysisType = Some(x))
} text ("The run type: one of: discovery, score")

  // *********************************** Inputs *******************************************************
  opt[String]("inputFasta") required() valueName ("<string>") action { (x, c) => c.copy(inputFasta = x) } text ("the reference file to scan for putitive targets")
  opt[String]("binaryOTFile") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
  opt[String]("outputFile") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
  opt[Unit]("positionalOTOutput") valueName ("<string>") action { (x, c) => c.copy(includePositionOutputInformation = true) } text ("include the position information of off-target hits")
  opt[Unit]("forceLinear") valueName ("<string>") action { (x, c) => c.copy(forceLinear = true) } text ("force the run to use a linear traversal of the bins; really only good for testing")
  opt[Unit]("markExactGenomeHits") valueName ("<string>") action { (x, c) => c.copy(markTargetsWithExactGenomeHits = true) } text ("should we add a column to indicate that a target has a exact genome hit")
  opt[Int]("maxMismatch") valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches we allow")
  opt[Int]("flankingSequence") valueName ("<int>") action { (x, c) => c.copy(flankingSequence = x) } text ("number of bases we should save on each side of the target, used in some scoring schemes (default is 10 on each side)")
  opt[Int]("maximumOffTargets") valueName ("<int>") action { (x, c) => c.copy(maximumOffTargets = x) } text ("the maximum number of off-targets for a guide, after which we stop adding new off-targets")
  // opt[Int]("numberOfThreads") valueName ("<int>") action { (x, c) => c.copy(numberOfThreads = x) } text ("the number of threads to use")
  opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("prints the usage information you see here")
}