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

  def scoringAnnotations = scoringModels.map{mdl => mdl.scoreName()}.toArray

  scoringMetrics.foreach { modelParameter => {
    val model = ScoringManager.getRegisteredScoringMetric(modelParameter, bitEncoder)

    model.foreach{mdl => {
      mdl.parseScoringParameters(commandLineArgs)
      if (mdl.validOverScoreModel(bitEncoder.mParameterPack)) {
        logger.info("adding score: " + mdl.scoreName())
        mdl.bitEncoder(bitEncoder)
        scoringModels :+= mdl
      } else
        logger.error("DROPPING SCORING METHOD: " + mdl.scoreName() + "; it's not valid over enzyme parameter pack: " + bitEncoder.mParameterPack.enzyme)
    }}
  }}


  def scoreGuides(guides: Array[CRISPRSiteOT]): Array[CRISPRSiteOT] = {
    var newGuides = guides
    scoringModels.foreach{model => {
      newGuides = model.scoreGuides(newGuides,bitEncoder,posEncoder)
    }}
    guides
  }
}

object ScoringManager {

  def getRegisteredScoringMetric(name: String, bitEncoder: BitEncoding): Option[ScoreModel] = name.toLowerCase() match {
    case "crisprmit" => {
      val sc = new CrisprMitEduOffTarget()
      sc.bitEncoder(bitEncoder)
      Some(sc)
    }
    case "annotate" => {
      Some(new BedAnnotation())
    }
    case "doench2014ontarget" => {
      Some(new Doench2014OnTarget())
    }
    case "doench2016cdf" => {
      Some(new Doench2016CFDScore())
    }
    case _ => {
      throw new IllegalArgumentException("Unknown scoring metric: " + name)
    }

  }
}
