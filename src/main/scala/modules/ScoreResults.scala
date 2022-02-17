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
import crispr.ResultsAggregator
import reference.binary.BinaryHeader
import scoring._
import standards.ParameterPack
import targetio.{TabDelimitedInput, TabDelimitedOutput}
import picocli.CommandLine.{Command, Option, Parameters}
import com.typesafe.scalalogging.LazyLogging


/**
  * Given a results bed file from off-target discovery, annotate it with scores using established scoring schemes
  */
@Command(name = "score", description = Array("Score candidate targets with various community-developed metrics"))
class ScoreResults extends Runnable with LazyLogging {

  @Option(names = Array("-input", "--input"), required = true, paramLabel = "FILE", description = Array("the file from the discovery phase we'll score"))
  private var inputBED: String = ""

  @Option(names = Array("-output", "--output"), required = true, paramLabel = "FILE", description = Array("the output file"))
  private var outputBED: String = ""

  @Option(names = Array("-scoringMetrics", "--scoringMetrics"), required = true, paramLabel = "STRING",
    description = Array("scoring methods to include, separated by a comma (no spaces). " +
      "\nCurrently supported scoring metrics: hsu2013,doench2014ontarget,doench2016cfd,moreno2015,bedannotator,dangerous,minot,reciprocalofftargets,rank"))
  private var scoringMetrics = ""

  @Option(names = Array("-maxMismatch", "--maxMismatch"), required = false, paramLabel = "INT",
    description = Array("only consider off-targets that have this maximum mismatch the guide of X"))
  private var maxMismatch: Int = Int.MaxValue

  @Option(names = Array("-database", "--database"), required = true, paramLabel = "FILE", description = Array("the database of off-targets"))
  private var binaryOTFile: String = ""

  @Option(names = Array("-includeOTs", "--includeOTs"), required = false, paramLabel = "FLAG",
    description = Array("include the off-target hits in the output file"))
  private var writeOTsToOutput: Boolean = false

  @Option(names = Array("-numericOutput", "--numericOutput"), required = false, paramLabel = "FLAG",
    description = Array("Write counts for dangerous components without annotations (no IN_GENOME=)"))
  private var numericOutput: Boolean = false

  // parameters inherited from scoring modules
  // -----------------------------------------
  @Option(names = Array("-inputAnnotationBed", "--inputAnnotationBed"), required = false, paramLabel = "FILE",
    description = Array("the bed file to annotate overlapping targets with in the format name:bedfile"))
  var inputBed = ""

  // -----------------------------------------
  @Option(names = Array("-shortestGuideEnergy", "--shortestGuideEnergy"), required = false, paramLabel = "FILE",
    description = Array("the RNAFold will be calculated on subsets of the guides from the specified length to the full guide"))
  var shortestGuideEnergy = -1

  @Option(names = Array("-transformPositions", "--transformPositions"), required = false, paramLabel = "FILE",
    description = Array("attempt to annotate each target with its genomic location by using matching (zero-mismatch) in-genome targets"))
  var genomeTransform = ""

  @Option(names = Array("-countOnTargetInScore", "--countOnTargetInScore"), required = false, paramLabel = "FILE",
    description = Array("we consider exact matches when calculating the off-target scores, default is false to stay consistent with Hsu 2013"))
  private var countOnTargetInScore: Boolean = false

  @Option(names = Array("-maxReciprocalMismatch", "--maxReciprocalMismatch"), required = false, paramLabel = "INT",
    description = Array("the maximum number of mismatches between two targets to be marked reciprocal in the output"))
  private var maxReciprocalMismatch = 1


  def run() {
    val header = BinaryHeader.readHeader(binaryOTFile + BinaryHeader.headerExtension)
    val bitEnc = new BitEncoding(header.parameterPack)

    logger.info("Loading CRISPR objects (filtering out overflow guides).. ")
    val guides = (new TabDelimitedInput(new File(inputBED), bitEnc, header.bitPosition, maxMismatch, writeOTsToOutput, true)).guides.toArray

    var scoringModels = List[ScoreModel]()

    def scoringAnnotations = scoringModels.map { mdl => mdl.scoreName() }.toArray

    scoringMetrics.split(",").foreach { modelParameter => {
      val model = ScoreResults.getRegisteredScoringMetric(modelParameter,
        bitEnc,
        inputBed,
        header.parameterPack,
        shortestGuideEnergy,
        genomeTransform,
        countOnTargetInScore,
        numericOutput,
        maxReciprocalMismatch)

      if (model.validOverEnzyme(bitEnc.mParameterPack)) {
        logger.info("adding score: " + model.scoreName())
        model.bitEncoder(bitEnc)
        model.setup()
        scoringModels :+= model
      } else {
        logger.error("DROPPING SCORING METHOD: " + model.scoreName() + "; it's not valid over enzyme parameter pack: " + bitEnc.mParameterPack)
      }
    }
    }

    // feed any aggregate scoring metrics the full list of other metrics
    val nonAggregate = scoringModels.filter { case (m) => m.isInstanceOf[RankedScore] }.map { e => e.asInstanceOf[RankedScore] }
    val aggregate = scoringModels.filter { case (m) => m.isInstanceOf[AggregateScore] }
    aggregate.foreach { case (e) => e.asInstanceOf[AggregateScore].initializeScoreNames(nonAggregate) }

    logger.info("Scoring all guides...")
    scoringModels.foreach {
      model => {
        logger.info("Scoring with model " + model.scoreName())
        model.scoreGuides(guides, bitEnc, header.bitPosition, header.parameterPack)
      }
    }

    logger.info("Aggregating results...")
    val results = new ResultsAggregator(guides)


    // now output the scores per site
    logger.info("Writing annotated guides to the output file...")
    val output = new TabDelimitedOutput(new File(outputBED),
      header.bitCoder,
      header.bitPosition,
      scoringModels.toArray,
      writeOTsToOutput,
      true)

    results.wrappedGuides.foreach { gd => {
      output.write(gd.otSite)
    }
    }
    output.close()
  }
}

object ScoreResults {

  def getRegisteredScoringMetric(name: String,
                                 bitEncoder: BitEncoding,
                                 inputBed: String,
                                 parameterPack: ParameterPack,
                                 shortestGuideEnergy: Int,
                                 genomeTransform: String,
                                 countOnTargetInScore: Boolean,
                                 numericOutput: Boolean,
                                 maxReciprocalMismatch: Int): ScoreModel = {

    val newMetric: ScoreModel = name.toLowerCase() match {
      case "hsu2013" => {
        val sm = new CrisprMitEduOffTarget()
        sm.bitEncoder(bitEncoder)
        sm.countOnTargetInScore = countOnTargetInScore
        sm
      }
      case "doench2014ontarget" => {
        new Doench2014OnTarget()
      }
      case "doench2016cfd" => {
        new Doench2016CFDScore()
      }
      case "moreno2015" => {
        new CRISPRscan()
      }
      case "bedannotator" => {
        val sm = new BedAnnotation()
        sm.inputBed = inputBed
        sm.genomeTransform = genomeTransform
        sm
      }
      case "dangerous" => {
        val dng = new DangerousSequences()
        if (numericOutput) {
          dng.cleanOutput = true
        }
        dng
      }
      case "minot" => {
        new ClosestHit()
      }
      case "reciprocalofftargets" => {
        val sm = new ReciprocalOffTargets()
        sm.maxMismatch = maxReciprocalMismatch
        sm
      }
      case "rank" => {
        new AggregateRankedScore()
      }
      case "jostandsantos" => {
        new JostAndSantosCRISPRi()
      }
      case "folding" => {
        val fold = new RNAFold4j()
        if (shortestGuideEnergy >= 0)
          fold.smallestGuide = Some(shortestGuideEnergy)
        else
          fold.smallestGuide = Some(math.abs(parameterPack.guideRange._1 - parameterPack.guideRange._2))
        fold
      }

      case _ => {
        throw new IllegalArgumentException("Unknown scoring metric: " + name)
      }
    }
    newMetric
  }
}