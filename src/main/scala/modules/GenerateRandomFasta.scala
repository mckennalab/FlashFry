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
import crispr.{CRISPRSite, CRISPRSiteOT, GuideMemoryStorage}
import utils.BaseCombinationGenerator
import reference.traverser.SeekTraverser
import reference.ReferenceEncoder
import standards.ParameterPack
import utils.RandoCRISPR

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scopt._

/**
  * Given the enyzme of interest, generate a series of random target sequences that pass our filtering criteria
  */
class GenerateRandomFasta extends LazyLogging with Module {

  def runWithOptions(remainingOptions: Seq[String]) {
    // parse the command line arguments
    val parser = new OptionParser[RanomdFastaConfig]("DiscoverOTSites") {
      head("DiscoverOTSites", "1.0")

      // *********************************** Inputs *******************************************************
      opt[String]("analysis") required() valueName ("<string>") action {
        (x, c) => c.copy(analysisType = Some(x))
      } text ("The run type: one of: discovery, score")

      // *********************************** Inputs *******************************************************
      opt[String]("outputFile") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
      opt[String]("enzyme") required() valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")
      opt[Unit]("onlyUnidirectional") valueName ("<string>") action { (x, c) => c.copy(onlyUnidirectional = true) } text ("should we ensure that the guides only work in one direction?")
      opt[Int]("randomCount") required() valueName ("<int>") action { (x, c) => c.copy(randomCount = x) } text ("how many surviving random sequences should we have")

      // some general command-line setup stuff
      note("Given the enyzme of interest, generate a series of random target sequences that pass our initial filtering criteria\n")
      help("help") text ("prints the usage information you see here")
    }

    parser.parse(remainingOptions, RanomdFastaConfig()) map {
      case(config,remainingParameters) => {

        // get our enzyme's (cas9, cpf1) settings
        val params = ParameterPack.nameToParameterPack(config.enzyme)

        // get our position encoder and bit encoder setup
        val bitEcoding = new BitEncoding(params)

        // our collection of random CRISPR sequences
        val sequences = new ArrayBuffer[CRISPRSite]()

        val crisprMaker = new RandoCRISPR(params.totalScanLength - params.pamLength, params.pam, params.fivePrimePam)

        while (sequences.size < config.randomCount) {
          val randomSeq = crisprMaker.next()
          val crisprSeq = new CRISPRSite(randomSeq, randomSeq, true, 0, None)

          // it's valid, and check to make sure there's only one hit, and not a secret reverse sequence hit
          if ((!config.onlyUnidirectional || (config.onlyUnidirectional && (params.fwdRegex.findAllIn(randomSeq).size + params.revRegex.findAllIn(randomSeq).size == 1)))) {
            sequences.append(crisprSeq)
          }
        }

        logger.info("Writing final output for " + sequences.size + " guides")
        val outputFasta = new PrintWriter(config.outputFile)
        sequences.foreach { sequence =>
          outputFasta.write(">random" + sequence.contig + "\n" + sequence.bases + "\n")
        }
        outputFasta.close()
      }
    }
  }

}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class RanomdFastaConfig(analysisType: Option[String] = None,
                             inputFasta: String = "",
                             outputFile: String = "",
                             enzyme: String = "",
                             onlyUnidirectional: Boolean = false,
                             randomCount: Int = 1000)