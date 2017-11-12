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
    } else {
      logger.error("DROPPING SCORING METHOD: " + model.scoreName() + "; it's not valid over enzyme parameter pack: " + bitEncoder.mParameterPack.enzyme)
    }
  }}

  // feed any aggregate scoring metrics the full list of other metrics
  val nonAggregate = scoringModels.filter{case(m) => m.isInstanceOf[RankedScore]}.map{e => e.asInstanceOf[RankedScore]}
  val aggregate    = scoringModels.filter{case(m) => m.isInstanceOf[AggregateScore]}
  aggregate.foreach{case(e) => e.asInstanceOf[AggregateScore].initializeScoreNames(nonAggregate)}


  def scoreGuides(guides: Array[CRISPRSiteOT], maxMismatch: Int, header: BinaryHeader) {

    scoringModels.foreach {
      model => {
        logger.info("Scoring with model " + model.scoreName())
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
      case "rank" => {
        new AggregateRankedScore()
      }
      case _ => {
        throw new IllegalArgumentException("Unknown scoring metric: " + name)
      }
    }
  }
}
