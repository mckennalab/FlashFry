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

package scoring

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import reference.binary.BinaryHeader

/**
  * create the scoring metrics, and then run each scoring metric on the guide and their off-targets
  *
  */
class ScoringManager(bitEncoder: BitEncoding, posEncoder: BitPosition, scoringMetrics: Seq[String], commandLineArgs: Array[String]) extends LazyLogging {

  // store our models
  var scoringModels = List[ScoreModel]()

  def scoringAnnotations = scoringModels.map { mdl => mdl.scoreName() }.toArray

  scoringMetrics.foreach { modelParameter => {
    val model = ScoringManager.getRegisteredScoringMetric(modelParameter, bitEncoder)

    model.parseScoringParameters(commandLineArgs)

    if (model.validOverScoreModel(bitEncoder.mParameterPack)) {
      logger.info("adding score: " + model.scoreName())
      model.bitEncoder(bitEncoder)
      scoringModels :+= model
    } else
      logger.error("DROPPING SCORING METHOD: " + model.scoreName() + "; it's not valid over enzyme parameter pack: " + bitEncoder.mParameterPack.enzyme)
  }}


  def scoreGuides(guides: Array[CRISPRSiteOT], maxMismatch: Int, header: BinaryHeader) {

    logger.info("Scoring guides...")
    scoringModels.foreach {
      model => {
        model.scoreGuides(guides, bitEncoder, posEncoder, header.parameterPack)
      }
    }
  }
}

object ScoringManager {

  def getRegisteredScoringMetric(name: String, bitEncoder: BitEncoding): ScoreModel = {
    name.toLowerCase() match {
      case "hsu2013" => {
        val sc = new CrisprMitEduOffTarget()
        sc.bitEncoder(bitEncoder)
        sc
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
        new BedAnnotation()
      }
      case "dangerous" => {
        new DangerousSequences()
      }
      case "minot" => {
        new ClosestHit()
      }
      case "reciprocalofftargets" => {
        new ReciprocalOffTargets()
      }
      case _ => {
        throw new IllegalArgumentException("Unknown scoring metric: " + name)
      }
    }
  }
}
