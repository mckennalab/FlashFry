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

package main.scala

import java.io.{File, PrintWriter}

import modules._
import org.slf4j._
import scopt._

object Main extends App {
  // parse the command line arguments
  val parser = new PeelParser[Config]("FlashFry") {
    head("FlashFry", "1.2")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: index (create database), discover (characterization), or score (your guides) or random (create random sequences)")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome. Specify individual analysis options for more detailed help information\n")
    help("help") text ("prints the usage information you see here")
  }

  val logger = LoggerFactory.getLogger("Main")

  parser.parse(args, Config()) map {
    case(config, remainingArgs) => {

      val initialTime = System.nanoTime()

      config.analysisType.get match {
        case "index" => {
          (new BuildOffTargetDatabase()).runWithOptions(args)
        }
        case "discover" => {
          (new OffTargetDiscovery()).runWithOptions(args)
        }
        case "random" => {
          (new GenerateRandomFasta()).runWithOptions(args)
        }
        case "score" => {
          (new ScoreResults()).runWithOptions(args)
        }
        case "dump" => {
          (new DumpDatabase()).runWithOptions(args)
        }
        case _ => {
          throw new IllegalStateException("")
        }
      }

      logger.info("Total runtime " + "%.2f".format((System.nanoTime() - initialTime) / 1000000000.0) + " seconds")
    }
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class Config(analysisType: Option[String] = None, scoringMetrics: Seq[File] = Seq())