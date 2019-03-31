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

import bitcoding.{BitEncoding}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSite}
import picocli.CommandLine.{Command, Option}

import standards.ParameterPack
import utils.RandoCRISPR

import scala.collection.mutable.ArrayBuffer



/**
  * Given the enyzme of interest, generate a series of random target sequences that pass our filtering criteria
  */
@Command(name = "random", description = Array("Given the enyzme of interest, generate a series of random target sequences that pass our initial filtering criteria"))
class GenerateRandomFasta extends Runnable with LazyLogging {

  @Option(names = Array("-outputFile", "--outputFile"), required = true, paramLabel = "FILE", description = Array("the output file"))
  private var outputFile: File = new File("")

  @Option(names = Array("-enzyme", "--enzyme"), required = false, paramLabel = "STRING", description = Array("the CRISPR enzyme; Currently supported enzymes: SPCAS9,SPCAS9NGG,SPCAS9NAG,CPF1"))
  private var enzyme: String = ""

  @Option(names = Array("-onlyUnidirectional", "--onlyUnidirectional"), required = false, paramLabel = "FLAG", description = Array("should we ensure that the guides only work in one direction?"))
  private var onlyUnidirectional: Boolean = false

  @Option(names = Array("-randomCount", "--randomCount"), required = true, paramLabel = "INT", description = Array("how many surviving random sequences should we have"))
  private var randomCount: Int = 1000

  @Option(names = Array("-sequenceContextLeft", "--sequenceContextLeft"), required = false, paramLabel = "INT", description = Array("sequence context to add on the left"))
  private var randomFront: Int = 0

  @Option(names = Array("-sequenceContextRight", "--sequenceContextRight"), required = false, paramLabel = "INT", description = Array("sequence context to add on the right"))
  private var randomBack: Int = 0

  def run() {

    // get our enzyme's (cas9, cpf1) settings
    val params = ParameterPack.nameToParameterPack(enzyme)

    // get our position encoder and bit encoder setup
    val bitEcoding = new BitEncoding(params)

    // our collection of random CRISPR sequences
    val sequences = new ArrayBuffer[CRISPRSite]()

    val crisprMaker = new RandoCRISPR(params.totalScanLength - params.pamLength,
      params.paddedPam,
      params.fivePrimePam,
      "",
      randomFront,
      randomBack)

    while (sequences.size < randomCount) {
      val randomSeq = crisprMaker.next()
      val crisprSeq = new CRISPRSite(randomSeq, randomSeq, true, 0, None)

      // it's valid, also now check to make sure there's only one hit if they asked for the filter
      if (!onlyUnidirectional ||
        (onlyUnidirectional && (params.fwdRegex.findAllIn(randomSeq).size + params.revRegex.findAllIn(randomSeq).size == 1))) {
        sequences.append(crisprSeq)
      } else {
        logger.debug("Tossing " + crisprSeq.bases + " as its contains more than one CRISPR site")
      }
    }

    logger.info("Writing final output for " + sequences.size + " guides")
    val outputFasta = new PrintWriter(outputFile)
    sequences.foreach { sequence =>
      outputFasta.write(">random" + sequence.contig + "\n" + sequence.bases + "\n")
    }
    outputFasta.close()
  }
}

