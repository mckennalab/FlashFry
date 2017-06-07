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

import java.io.File

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.ResultsAggregator
import reference.binary.BinaryHeader
import scoring.ScoringManager
import standards.ParameterPack
import targetio.{TargetInput, TargetOutput}
import scopt._

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
        logger.info("Loading input BED file into CRISPR objects.. ")
        val posEncoderAndOffTargets = TargetInput.inputBedToTargetArray(new File(config.inputBED), bitEnc, 2000, config.maxMismatch)

        // get a scoring manager
        val scoringManager = new ScoringManager(bitEnc, posEncoderAndOffTargets._1, config.scoringMetrics, args.toArray)

        // score all the sites
        logger.info("Scoring all guides...")
        scoringManager.scoreGuides(posEncoderAndOffTargets._2, config.maxMismatch, header)

        logger.info("Aggregating results...")
        val results = new ResultsAggregator(posEncoderAndOffTargets._2)

        // output a new data file with the scored results
        logger.info("Writing annotated guides to the output file...")
        TargetOutput.output(config.outputBED, results, true, false, bitEnc, posEncoderAndOffTargets._1, scoringManager.scoringAnnotations)

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
                       scoringMetrics: Seq[String] = Seq())


class ScoreBaseOptions extends PeelParser[ScoreConfig]("score") {
  // *********************************** Inputs *******************************************************
  opt[String]("input") required() valueName ("<string>") action { (x, c) => c.copy(inputBED = x) } text ("the reference file to scan for putitive targets")
  opt[String]("output") required() valueName ("<string>") action { (x, c) => c.copy(outputBED = x) } text ("the output file (in bed format)")
  opt[Seq[String]]("scoringMetrics") required() valueName("<scoringMethod1>,<scoringMethod1>...") action{ (x,c) => c.copy(scoringMetrics = x) } text ("scoring methods to include")
  opt[Int]("maxMismatch") valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("only consider off-targets that have a maximum mismatch the guide of X")
  opt[String]("database") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("match off-targets for the specified guides to the genome of interest\n")
}