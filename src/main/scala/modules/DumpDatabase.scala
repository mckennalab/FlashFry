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
import reference.binary.{BinaryHeader, DatabaseWriter}
import reference.traversal.{LinearTraversal, OrderedBinTraversalFactory}
import reference.traverser.SeekTraverser._
import reference.traverser.dump.DumpAllGuides
import reference.traverser.parallel.ParallelTraverser
import standards.ParameterPack

import scala.collection.mutable
import scala.io.Source
import scopt._

/**
  * Created by aaronmck on 4/27/17.
  */
class DumpDatabase extends LazyLogging with Module {

  def runWithOptions(remainingOptions: Seq[String]) {
    // parse the command line arguments
    val parser = new DatabaseDumpBaseOptions()

    parser.parse(remainingOptions, DatabaseDumpConfig()) map {
      case(config,remainingParameters) => {
        val formatter = java.text.NumberFormat.getIntegerInstance

        // load up their input file, and scan for any potential targets
        logger.info("Reading the header....")
        val header = BinaryHeader.readHeader(config.binaryOTFile + BinaryHeader.headerExtension)

        DumpAllGuides.toFile(new File(config.binaryOTFile),header,header.inputParameterPack,header.bitCoder,header.bitPosition,config.outputFile)
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

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("prints the usage information you see here")
}