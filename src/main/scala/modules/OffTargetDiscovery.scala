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

import bitcoding.{StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, GuideMemoryStorage, ResultsAggregator}
import picocli.CommandLine.{Command, Option}
import reference.traverser.{LinearTraverser, SeekTraverser, Traverser}
import reference.ReferenceEncoder
import reference.binary.BinaryHeader
import reference.traversal.{LinearTraversal, OrderedBinTraversalFactory}

import scoring.ScoreModel
import targetio.TabDelimitedOutput

/**
  * Scan a fasta file for targets and tally their off-targets against the genome
  */
@Command(name = "discover", description = Array("Discover off-targets for the specified sequences within the genome of interest"))
class OffTargetDiscovery extends Runnable with LazyLogging {

  @Option(names = Array("-fasta", "--fasta"), required = true, paramLabel = "FILE", description = Array("the reference file to scan for putative targets"))
  private var inputFasta: String = ""

  @Option(names = Array("-database", "--database"), required = true, paramLabel = "FILE", description = Array("the binary off-target file"))
  private var binaryOTFile: String = ""

  @Option(names = Array("-output", "--output"), required = true, paramLabel = "FILE", description = Array("the output file (in bed format)"))
  private var outputFile: String = ""

  @Option(names = Array("-positionOutput", "--positionOutput"), required = false, paramLabel = "FLAG",
    description = Array("include the position information of off-target hits"))
  private var includePositionOutputInformation: Boolean = false

  @Option(names = Array("-forceLinear", "--forceLinear"), required = false, paramLabel = "FLAG",
    description = Array("force the run to use a linear traversal of the bins; really only good for testing"))
  private var forceLinear: Boolean = false

  @Option(names = Array("-maxMismatch", "--maxMismatch"), required = false, paramLabel = "INT",
    description = Array("the maximum number of mismatches we allow"))
  private var maxMismatch: Int = 4

  @Option(names = Array("-flankingSequence", "--flankingSequence"), required = false, paramLabel = "INT",
    description = Array("number of bases we should save on each side of the target, used in some scoring schemes"))
  private var flankingSequence: Int = 6

  @Option(names = Array("-maximumOffTargets", "--maximumOffTargets"), required = false, paramLabel = "INT",
    description = Array("the maximum number of off-targets for a guide, after which we stop adding new off-targets"))
  private var maximumOffTargets: Int = 2000

  @Option(names = Array("-minGC", "--minGC"), required = false, paramLabel = "DOUBLE",
    description = Array("the minimum GC needed to include a guide, from 0.0 (all A/T) to 1.0 (all G/C); default 0.0"))
  private var minGC: Double = 0.0

  @Option(names = Array("-maxGC", "--maxGC"), required = false, paramLabel = "DOUBLE",
    description = Array("the maximum GC needed to include a guide, from 0.0 (all A/T) to 1.0 (all G/C); default 1.0"))
  private var maxGC: Double = 1.0

  def run() {

    assert(minGC >= 0 && minGC <= 1.0)
    assert(maxGC >= 0 && maxGC <= 1.0)

    val stopSmartTraversalProp = 0.95

    val formatter = java.text.NumberFormat.getIntegerInstance

    logger.info("Reading the header....")
    val header = BinaryHeader.readHeader(binaryOTFile + BinaryHeader.headerExtension)

    // load up the input file, and scan for any potential targets
    val guideHits = new GuideMemoryStorage()
    val encoders = ReferenceEncoder.findTargetSites(new File(inputFasta), guideHits, header.inputParameterPack, flankingSequence)
    logger.info("Setting up the guide recording for our " + guideHits.guideHits.size + " candidate guides....")

    val filteredGuides = GuideMemoryStorage.filter_by_GC(guideHits,minGC,maxGC)

    // transform our targets into a list for off-target collection
    logger.info("Filtered GC guide count " + filteredGuides.guideHits.size)
    val guideOTs = filteredGuides.guideHits.map {
      guide => new CRISPRSiteOT(guide, header.bitCoder.bitEncodeString(StringCount(guide.bases, 1)), maximumOffTargets)
    }.toArray

    val guideStorage = new ResultsAggregator(guideOTs)

    var mTraversalFactory: scala.Option[OrderedBinTraversalFactory] = None

    logger.info("Precomputing traversal over bins....")
    if (!forceLinear)
      mTraversalFactory = Some(new OrderedBinTraversalFactory(header.binGenerator, maxMismatch, header.bitCoder, stopSmartTraversalProp, guideStorage))

    logger.info("scanning against the known targets from the genome with " + filteredGuides.guideHits.toArray.size + " guides")

    val isTraversalSaturated = if (mTraversalFactory.isDefined) mTraversalFactory.get.saturated else false

    /*
    handle the various configurations -- forced linear traversal, saturated traversal, multithreaded (not supported currently)
     */
    (forceLinear, isTraversalSaturated) match {
      case (fl, sat) if (fl | sat) => {
        val lTrav = new LinearTraversal(header.binGenerator, maxMismatch, header.bitCoder, 0.90, guideStorage)
        guideStorage.setTraversalOverFlowCallback(lTrav.overflowGuide)
        logger.info("Starting linear traversal")
        LinearTraverser.scan(new File(binaryOTFile), header, lTrav, guideStorage, maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
      }
      case (fl, sat) if (!fl & !sat) => {
        logger.info("Starting seek traversal")
        val traversal = mTraversalFactory.get.iterator
        guideStorage.setTraversalOverFlowCallback(traversal.overflowGuide)
        SeekTraverser.scan(new File(binaryOTFile), header, traversal, guideStorage, maxMismatch, header.inputParameterPack, header.bitCoder, header.bitPosition)
      }
      case (fl, sat) => {
        throw new IllegalStateException("We don't have a run type when --forceLinear=" + fl + ", binSaturation=" + sat)
      }
    }

    logger.info("Performed a total of " + formatter.format(Traverser.allComparisons) + " guide to target comparisons")
    logger.info("Writing final output for " + filteredGuides.guideHits.toArray.size + " guides")

    // now output the scores per site
    val output = new TabDelimitedOutput(new File(outputFile),
      header.bitCoder,
      header.bitPosition,
      Array[ScoreModel](),
      true,
      includePositionOutputInformation)

    guideStorage.wrappedGuides.foreach { gd => {
      output.write(gd.otSite)
    }
    }
    output.close()
  }
}
