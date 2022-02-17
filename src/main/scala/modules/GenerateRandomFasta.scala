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

import bitcoding.BitEncoding
import crispr.CRISPRSite
import standards.ParameterPack
import utils.RandoCRISPR

import scala.collection.mutable
import picocli.CommandLine.{Command, Option}
import com.typesafe.scalalogging.LazyLogging


/**
  * Given the enyzme of interest, generate a series of random target sequences that pass our filtering criteria
  */
@Command(name = "random", description = Array("Given the enyzme of interest, generate a series of random target sequences that pass our initial filtering criteria"))
class GenerateRandomFasta extends Runnable with LazyLogging {

  @Option(names = Array("-outputFile", "--outputFile"), required = true, paramLabel = "FILE", description = Array("the output file"))
  private var outputFile: File = new File("")

  @Option(names = Array("-enzyme", "--enzyme"), required = false, paramLabel = "STRING", description = Array("the CRISPR enzyme; Currently supported enzymes: SPCAS9,SPCAS9NGG,SPCAS9NAG,CPF1"))
  private var enzyme: String = ""

  @Option(names = Array("-namePrefix", "--namePrefix"), required = false, paramLabel = "STRING", description = Array("the prefix for the random target name; default is 'random'"))
  private var randomPrefix: String = "random"

  @Option(names = Array("-onlyUnidirectional", "--onlyUnidirectional"), required = false, paramLabel = "FLAG", description = Array("should we ensure that the guides only work in one direction?"))
  private var onlyUnidirectional: Boolean = false

  @Option(names = Array("-randomCount", "--randomCount"), required = true, paramLabel = "INT", description = Array("how many surviving random sequences should we have"))
  private var randomCount: Int = 1000

  @Option(names = Array("-sequenceContextLeft", "--sequenceContextLeft"), required = false, paramLabel = "INT", description = Array("sequence context to add on the left"))
  private var randomFront: Int = 0

  @Option(names = Array("-sequenceContextRight", "--sequenceContextRight"), required = false, paramLabel = "INT", description = Array("sequence context to add on the right"))
  private var randomBack: Int = 0

  @Option(names = Array("-patterned", "--patterned"), required = false, paramLabel = "STRING", description = Array("draw bases using an extended FASTA format"))
  private var patterned: String = ""

  @Option(names = Array("-duplicatesAllowed", "--duplicatesAllowed"), required = false, paramLabel = "BOOL", description = Array("should we allow duplicate guides in our output"))
  private var duplicatesAllowed: Boolean = false

  @Option(names = Array("-maxSuccessiveDesignFailures", "--maxSuccessiveDesignFailures"), required = false, paramLabel = "INT", description = Array("Maximum number of design failures before we quit"))
  private var maxSuccessiveDesignFailures: Int = 50

  def run() {

    // get our enzyme's (cas9, cpf1) settings
    val params = ParameterPack.nameToParameterPack(enzyme)

    // get our position encoder and bit encoder setup
    val bitEcoding = new BitEncoding(params)

    // our collection of random CRISPR sequences
    val sequences = new mutable.HashMap[String, Array[CRISPRSite]]()

    val crisprMaker = new RandoCRISPR(params.totalScanLength - params.pamLength,
      params.paddedPam,
      params.fivePrimePam,
      "",
      randomFront,
      randomBack,
      if (patterned == "") None else Some(patterned))


    // watch to make sure we haven't exhausted our possible guides
    var successiveFailures = 0

    while ((sequences.size < randomCount) && (successiveFailures <= maxSuccessiveDesignFailures)) {
      val randomSeq = crisprMaker.next()

      if (!(sequences contains randomSeq.guide) || duplicatesAllowed) {
        val crisprSeq = new CRISPRSite(randomSeq.fullTarget, randomSeq.fullTarget, true, 0, None)

        // it's valid, also now check to make sure there's only one hit if they asked for the filter
        if (!onlyUnidirectional ||
          (onlyUnidirectional && (params.fwdRegex.findAllIn(randomSeq.fullTarget).size + params.revRegex.findAllIn(randomSeq.fullTarget).size == 1))) {
          val existing = sequences.getOrElse(randomSeq.guide,Array[CRISPRSite]())
          sequences(randomSeq.guide) = existing ++ Array[CRISPRSite](crisprSeq)
        } else {
          logger.debug("Tossing " + crisprSeq.bases + " as its contains more than one CRISPR site")
        }
        successiveFailures = 0
      } else {
        successiveFailures += 1
      }
    }

    if (successiveFailures >= maxSuccessiveDesignFailures)
      logger.info("Stopping random CRISPR generation as we're struggling to find new, unique guides")

    logger.info("Writing final output for " + sequences.size + " guides")
    val outputFasta = new PrintWriter(outputFile)
    sequences.foreach { case(guide,sequence_set) =>
      sequence_set.foreach { sequence =>
        outputFasta.write(">" + randomPrefix + sequence.contig + "\n" + sequence.bases + "\n")
      }
    }
    outputFasta.close()
  }
}