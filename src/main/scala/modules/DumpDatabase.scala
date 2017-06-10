/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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