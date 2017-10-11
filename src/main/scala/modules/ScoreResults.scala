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

import java.io.File

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.ResultsAggregator
import reference.binary.BinaryHeader
import scoring.{ScoreModel, ScoringManager}
import standards.ParameterPack
import scopt._
import targetio.{TabDelimitedInput, TabDelimitedOutput}

/**
  * Given a results bed file from off-target discovery, annotate it with scores using established scoring schemes
  */
class ScoreResults extends LazyLogging with Module {

  def runWithOptions(args: Seq[String]) {
    // parse the command line arguments
    val parser = new ScoreBaseOptions()

    parser.parse(args, ScoreConfig()) map {
      case(config,remainingParameters) => {

        // get our settings
        val header = BinaryHeader.readHeader(config.binaryOTFile + BinaryHeader.headerExtension)

        // make ourselves a bit encoder
        val bitEnc = new BitEncoding(header.parameterPack)

        // load up the scored sites into a container
        logger.info("Loading CRISPR objects.. ")
        val guides = (new TabDelimitedInput(new File(config.inputBED),bitEnc,header.bitPosition,config.maxMismatch)).guides.toArray

        // get a scoring manager
        val scoringManager = new ScoringManager(bitEnc, header.bitPosition, config.scoringMetrics, args.toArray)

        // score all the sites
        logger.info("Scoring all guides...")
        scoringManager.scoreGuides(guides, config.maxMismatch, header)

        logger.info("Aggregating results...")
        val results = new ResultsAggregator(guides)

        // output a new data file with the scored results
        logger.info("Writing annotated guides to the output file...")

        // now output the scores per site
        val output = new TabDelimitedOutput(new File(config.outputBED),
          header.bitCoder,
          header.bitPosition,
          scoringManager.scoringModels.toArray,
          config.includeOTs,
          true)

        results.wrappedGuides.foreach{gd => {
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
case class ScoreConfig(inputBED: String = "",
                       outputBED: String = "",
                       binaryOTFile: String = "",
                       maxMismatch: Int = Int.MaxValue,
                       scoringMetrics: Seq[String] = Seq(),
                        includeOTs: Boolean = false)


class ScoreBaseOptions extends PeelParser[ScoreConfig]("score") {
  // *********************************** Inputs *******************************************************
  opt[String]("input") required() valueName ("<string>") action { (x, c) => c.copy(inputBED = x) } text ("the reference file to scan for putitive targets")
  opt[String]("output") required() valueName ("<string>") action { (x, c) => c.copy(outputBED = x) } text ("the output file (in bed format)")
  opt[Seq[String]]("scoringMetrics") required() valueName("<scoringMethod1>,<scoringMethod1>...") action{ (x,c) => c.copy(scoringMetrics = x) } text ("scoring methods to include")
  opt[Int]("maxMismatch") valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("only consider off-targets that have a maximum mismatch the guide of X")
  opt[String]("database") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
  opt[Unit]("includeOTs") valueName ("<string>") action { (x, c) => c.copy(includeOTs = true) } text ("include the off-target hits")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("match off-targets for the specified guides to the genome of interest\n")
}