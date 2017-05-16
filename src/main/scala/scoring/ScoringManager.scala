package scoring

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT

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


  def scoreGuides(guides: Array[CRISPRSiteOT]) {

    scoringModels.foreach {
      model => {
        model.scoreGuides(guides, bitEncoder, posEncoder)
      }
    }
  }
}

object ScoringManager {

  def getRegisteredScoringMetric(name: String, bitEncoder: BitEncoding): ScoreModel = {
    name.toLowerCase() match {
      case "crisprmit" => {
        val sc = new CrisprMitEduOffTarget()
        sc.bitEncoder(bitEncoder)
        sc
      }
      case "doench2014ontarget" => {
        new Doench2014OnTarget()
      }
      case "doench2016cdf" => {
        new Doench2016CFDScore()
      }
      case "bedannotator" => {
        new BedAnnotation()
      }
      case "dangerous" => {
        new DangerousSequences()
      }
      case _ => {
        throw new IllegalArgumentException("Unknown scoring metric: " + name)
      }
    }
  }
}
