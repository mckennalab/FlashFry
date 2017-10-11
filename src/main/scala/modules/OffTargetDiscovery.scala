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

package modules

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, GuideMemoryStorage, ResultsAggregator}
import utils.BaseCombinationGenerator
import reference.traverser.{LinearTraverser, SeekTraverser, Traverser}
import reference.{CRISPRSite, ReferenceEncoder}
import reference.binary.BinaryHeader
import reference.traversal.{LinearTraversal, OrderedBinTraversalFactory}
import reference.traverser.SeekTraverser._
import reference.traverser.parallel.ParallelTraverser
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source
import scopt._
import scoring.ScoreModel
import targetio.TabDelimitedOutput

/**
  * Scan a fasta file for targets and tally their off-targets against the genome
  */
class OffTargetDiscovery extends LazyLogging with Module {

  def runWithOptions(remainingOptions: Seq[String]) {
    // parse the command line arguments
    val parser = new OffTargetBaseOptions()

    parser.parse(remainingOptions, DiscoverConfig()) map {
      case(config,remainingParameters) => {
        val formatter = java.text.NumberFormat.getIntegerInstance

        logger.info("Reading the header....")
        val header = BinaryHeader.readHeader(config.binaryOTFile + BinaryHeader.headerExtension)

        // load up their input file, and scan for any potential targets
        val guideHits = new GuideMemoryStorage()
        val encoders = ReferenceEncoder.findTargetSites(new File(config.inputFasta), guideHits, header.inputParameterPack, config.flankingSequence)

        // transform our targets into a list for off-target collection
        logger.info("Setting up the guide recording for our " + guideHits.guideHits.size + " candidate guides....")
        val guideOTs = guideHits.guideHits.map {
          guide => new CRISPRSiteOT(guide, header.bitCoder.bitEncodeString(StringCount(guide.bases, 1)), config.maximumOffTargets)
        }.toArray

        val guideStorage = new ResultsAggregator(guideOTs)

        var traversalFactory: Option[OrderedBinTraversalFactory] = None

        logger.info("Precomputing traversal over bins....")
        if (!config.forceLinear)
          traversalFactory = Some(new OrderedBinTraversalFactory(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideStorage))

        logger.info("scanning against the known targets from the genome with " + guideHits.guideHits.toArray.size + " guides")

        val isTraversalSaturated = if (traversalFactory.isDefined) traversalFactory.get.saturated else false

        /*
        handle the various configurations -- forced linear traversal, saturated traversal, multithreaded (not supported currently)
         */
        (config.forceLinear, isTraversalSaturated, config.numberOfThreads) match {
          case (fl, sat, threads) if ((fl | sat) & threads == 1) => {
            val lTrav = new LinearTraversal(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideStorage)
            guideStorage.setTraversalOverFlowCallback(lTrav.overflowGuide)
            logger.info("Starting linear traversal")
            LinearTraverser.scan(new File(config.binaryOTFile), header, lTrav, guideStorage, config.maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
          }
          case (fl, sat, threads) if (!fl & !sat & threads == 1) => {
            logger.info("Starting seek traversal")
            val traversal = traversalFactory.get.iterator
            guideStorage.setTraversalOverFlowCallback(traversal.overflowGuide)
            SeekTraverser.scan(new File(config.binaryOTFile), header, traversal, guideStorage, config.maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
          }
          case (fl, sat, threads) if (!fl & !sat & threads > 1) => {
            logger.info("Starting parallel traversal")
            ParallelTraverser.numberOfThreads = threads
            val traversal = traversalFactory.get.iterator
            guideStorage.setTraversalOverFlowCallback(traversal.overflowGuide)
            ParallelTraverser.scan(new File(config.binaryOTFile), header, traversal, guideStorage, config.maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
          }
          case (fl, sat, threads) if (!fl & threads > 1) => {
            logger.info("Starting parallel linear traversal")
            val lTrav = new LinearTraversal(header.binGenerator, config.maxMismatch, header.bitCoder, 0.90, guideStorage)
            guideStorage.setTraversalOverFlowCallback(lTrav.overflowGuide)
            ParallelTraverser.numberOfThreads = threads
            ParallelTraverser.scan(new File(config.binaryOTFile), header, lTrav, guideStorage, config.maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
          }
          case (fl, sat, threads) => {
            throw new IllegalStateException("We don't have a run type when --forceLinear=" + fl + ", binSaturation=" + sat + ", and --numberOfThreads=" + threads)
          }
        }

        logger.info("Performed a total of " + formatter.format(Traverser.allComparisons) + " guide to target comparisons")
        logger.info("Writing final output for " + guideHits.guideHits.toArray.size + " guides")

        // now output the scores per site
        val output = new TabDelimitedOutput(new File(config.outputFile),
          header.bitCoder,
          header.bitPosition,
          Array[ScoreModel](),
          true,
          config.includePositionOutputInformation)

        guideStorage.wrappedGuides.foreach{gd => {
          output.write(gd.otSite)
        }}
        output.close()
      }
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
                          maxMismatch: Int = 4,
                          includePositionOutputInformation: Boolean = false,
                          markTargetsWithExactGenomeHits: Boolean = false,
                          flankingSequence: Int = 6,
                          maximumOffTargets: Int = 2000,
                          forceLinear: Boolean = false,
                          numberOfThreads: Int = 1)


class OffTargetBaseOptions extends OptionParser[DiscoverConfig]("discover") {
  head("discover", "1.3")

  // *********************************** Inputs *******************************************************
  opt[String]("analysis") required() valueName ("<string>") action {
    (x, c) => c.copy(analysisType = Some(x))
  } text ("The run type: one of: discovery, score")

  // *********************************** Inputs *******************************************************
  opt[String]("fasta") required() valueName ("<string>") action { (x, c) => c.copy(inputFasta = x) } text ("the reference file to scan for putitive targets")
  opt[String]("database") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
  opt[String]("output") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
  opt[Unit]("positionOutput") valueName ("<string>") action { (x, c) => c.copy(includePositionOutputInformation = true) } text ("include the position information of off-target hits")
  opt[Unit]("forceLinear") valueName ("<string>") action { (x, c) => c.copy(forceLinear = true) } text ("force the run to use a linear traversal of the bins; really only good for testing")
  opt[Unit]("markExactGenomeHits") valueName ("<string>") action { (x, c) => c.copy(markTargetsWithExactGenomeHits = true) } text ("should we add a column to indicate that a target has a exact genome hit")
  opt[Int]("maxMismatch") valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches we allow")
  opt[Int]("flankingSequence") valueName ("<int>") action { (x, c) => c.copy(flankingSequence = x) } text ("number of bases we should save on each side of the target, used in some scoring schemes (default is 10 on each side)")
  opt[Int]("maximumOffTargets") valueName ("<int>") action { (x, c) => c.copy(maximumOffTargets = x) } text ("the maximum number of off-targets for a guide, after which we stop adding new off-targets")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("prints the usage information you see here")
}