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
import com.typesafe.scalalogging.LazyLogging
import crispr.BinWriter
import picocli.CommandLine.{Command, Option}
import utils.BaseCombinationGenerator
import reference.binary.DatabaseWriter
import reference.{ReferenceEncoder}
import standards.ParameterPack


/**
  * "give me a genome, and I'll give you ALL the potential off-targets, encoded into a binary representation" -this class
  *
  * Febuary 9th, 2017
  */
@Command(name = "index", description = Array("Build an off-target database from a reference file"))
class BuildOffTargetDatabase extends Runnable with LazyLogging {

  @Option(names = Array("-reference", "--reference"), required = true, paramLabel = "FILE", description = Array("the reference file"))
  private var reference: File = new File("UNKNOWN")

  @Option(names = Array("-database", "--database"), required = true, paramLabel = "FILE", description = Array("the output database file"))
  private var output: File = new File("UNKNOWN")

  @Option(names = Array("-tmpLocation", "--tmpLocation"), required = true, paramLabel = "DIRECTORY", description = Array("the temporary file output"))
  private var tmp: File = new File("UNKNOWN")

  @Option(names = Array("-enzyme", "--enzyme"), required = false, paramLabel = "STRING",
    description = Array("the CRISPR enzyme to use; Currently supported enzymes: SPCAS9,SPCAS9NGG,SPCAS9NAG,CPF1"))
  private var enzyme: String = "spCas9ngg"

  @Option(names = Array("-binSize", "--binSize"), required = false, paramLabel = "INT",
    description = Array("how many bins (and subsequent open files) should we use when sorting our genome"))
  private var binSize: Int = 7

  override def run() {
    // setup a decent sized iterator over bases patterns for finding sites
    val binGenerator = BaseCombinationGenerator(6)

    // get our settings
    val params = ParameterPack.nameToParameterPack(enzyme)

    // create our output bins
    val outputBins = BinWriter(tmp, binGenerator, params)

    logger.info("Discovering target sites in the input genome file...")

    // first discover sites in the target genome -- writing out in bins
    val encoders = ReferenceEncoder.findTargetSites(reference, outputBins, params, 0)

    logger.info("Closing the temporary binary output files...")

    val binToFile = outputBins.close()

    logger.info("Creating the final binary database file...")

    // setup our bin generator for the output
    val searchBinGenerator = BaseCombinationGenerator(binSize)

    // then process this total file into a binary file
    DatabaseWriter.writeToBinnedFileSet(binToFile,
      binGenerator.width,
      output.getAbsolutePath,
      encoders._1,
      encoders._2,
      searchBinGenerator,
      params)
  }
}
