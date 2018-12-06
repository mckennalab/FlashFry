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

import java.io.{File}

import com.typesafe.scalalogging.LazyLogging
import picocli.CommandLine.{Command, Option}
import reference.binary.{BinaryHeader}
import reference.traverser.dump.DumpAllGuides


@Command(name = "extract", description = Array("Dump targets from the database, given the filter parameters"))
class DumpDatabase extends Runnable with LazyLogging {

  @Option(names = Array("-binaryOTFile", "--binaryOTFile"), required = true, paramLabel = "FILE",
    description = Array("the binary off-target database to read from"))
  private var binaryOTFile: File = new File("UNKNOWN")

  @Option(names = Array("-outputFile", "--outputFile"), required = true, paramLabel = "FILE",
    description = Array("the output file (in bed format)"))
  private var outputFile: File = new File("UNKNOWN")

  @Option(names = Array("-minInGenome", "--minInGenome"), required = false, paramLabel = "INT",
    description = Array("filter out in-genome sequences that have less than minInGenome count"))
  private var minInGenome: Int = 0

  @Option(names = Array("-maxInGenome", "--maxInGenome"), required = false, paramLabel = "INT",
    description = Array("filter out in-genome sequences that have more than maxInGenome count"))
  private var maxInGenome: Int = Int.MaxValue

  @Option(names = Array("-subsampleProportion", "--subsampleProportion"), required = false, paramLabel = "INT",
    description = Array("randomly subsample the matching entries to this proportion of the total"))
  private var subsampleProportion: Double = 1.0

  def run(): Unit = {
    val formatter = java.text.NumberFormat.getIntegerInstance

    // load up their input file, and scan for any potential targets
    logger.info("Reading the header....")
    val header = BinaryHeader.readHeader(binaryOTFile + BinaryHeader.headerExtension)

    DumpAllGuides.toFile(binaryOTFile,
      header,
      header.inputParameterPack,
      header.bitCoder,
      header.bitPosition,
      outputFile.getAbsolutePath,
      minInGenome,
      maxInGenome,
      subsampleProportion)
  }
}
