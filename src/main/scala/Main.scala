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

import com.typesafe.scalalogging.LazyLogging
import modules._
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command

@Command(name = "FlashFry", version = Array("1.9.0"), sortOptions = false,
  description = Array("@|bold FlashFry|@ Discover CRISPR targets within arbitrary","nucleotide sequences"))
class Main() extends Runnable with LazyLogging {

  def run(): Unit = {
  }
}

object Main {
  def main(args: Array[String]) {
    val commandLine = buildCommandLine()
    val logger = LoggerFactory.getLogger("Main")

    val initialTime = System.nanoTime()

    commandLine.parseWithHandler(new CommandLine.RunLast, args)

    // check to make sure we ran a subcommand
    if (!commandLine.getParseResult.hasSubcommand)
      System.err.println(commandLine.getUsageMessage)

    logger.info("Total runtime " + "%.2f".format((System.nanoTime() - initialTime) / 1000000000.0) + " seconds")
  }

  /** Construct the complete command hierarchy for production and CLI tests. */
  def buildCommandLine(): CommandLine = {
    val commandLine = new CommandLine(new Main())
    commandLine.addSubcommand("index", new BuildOffTargetDatabase())
    commandLine.addSubcommand("discover", new OffTargetDiscovery())
    commandLine.addSubcommand("random", new GenerateRandomFasta())
    commandLine.addSubcommand("score", new ScoreResults())
    commandLine.addSubcommand("extract", new DumpDatabase())

    val profileCommand = new CommandLine(new ProfileCommand())
    profileCommand.addSubcommand("validate", new ValidateNucleaseProfile())
    commandLine.addSubcommand("profile", profileCommand)
    commandLine
  }
}

trait CommandLineParser {
  def addSubCommands(cmdLine: CommandLine)
}
