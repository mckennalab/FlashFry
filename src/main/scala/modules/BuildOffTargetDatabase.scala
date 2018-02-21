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

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.BinWriter
import utils.BaseCombinationGenerator
import reference.binary.DatabaseWriter
import reference.{ReferenceDictReader, ReferenceEncoder}
import standards.ParameterPack
import scopt._

/**
  * "give me a genome, and I'll give you ALL the potential off-targets, encoded into a binary representation" -this class
  *
  * Febuary 9th, 2017
  */
class BuildOffTargetDatabase extends LazyLogging {

  def runWithOptions(remainingOptions: Seq[String]) {
    // parse the command line arguments
    val parser = new OptionParser[TallyConfig]("index") {
      head("index", "1.3")

      // *********************************** Inputs *******************************************************
      opt[String]("analysis") required() valueName ("<string>") action {
        (x, c) => c.copy(analysisType = Some(x))
      } text ("The run type: one of: discovery, score")

      // *********************************** Inputs *******************************************************
      opt[String]("reference") required() valueName ("<string>") action { (x, c) => c.copy(reference = x) } text ("the reference file")
      opt[String]("database") required() valueName ("<string>") action { (x, c) => c.copy(output = x) } text ("the output file")
      opt[String]("tmpLocation") required() valueName ("<string>") action { (x, c) => c.copy(tmp = x) } text ("the output file")
      opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, spcas9, spcas9ngg)")
      opt[Int]("binSize") valueName ("<int>") action { (x, c) => c.copy(binSize = x) } text ("how many bins (and subsequent open files) should we use when sorting our genome")
    }

    // discover off-targets
    parser.parse(remainingOptions, TallyConfig()) map {
      case(config,remainingParameters) => {
        // setup a decent sized iterator over bases patterns for finding sites
        val binGenerator = BaseCombinationGenerator(6)

        // get our settings
        val params = ParameterPack.nameToParameterPack(config.enzyme)

        // create out output bins
        val outputBins = BinWriter(new File(config.tmp), binGenerator, params)

        // first discover sites in the target genome -- writing out in bins
        val encoders = ReferenceEncoder.findTargetSites(new File(config.reference), outputBins, params, 0)

        logger.info("Closing the temporary binary output files...")

        val binToFile = outputBins.close()

        logger.info("Creating the final binary database file...")

        // setup our bin generator for the output
        val searchBinGenerator = BaseCombinationGenerator(config.binSize)

        // then process this total file into a binary file
        DatabaseWriter.writeToBinnedFileSet(binToFile, binGenerator.width, config.output, encoders._1, encoders._2, searchBinGenerator, params)

      }
    }
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class TallyConfig(analysisType: Option[String] = None,
                       reference: String = "",
                       output: String = "",
                       tmp: String = "/tmp/",
                       enzyme: String = "spCas9ngg",
                       binSize: Int = 7)
