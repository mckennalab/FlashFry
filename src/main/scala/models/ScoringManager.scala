package models

import bitcoding.BitEncoding

/**
  * create the scoring metrics, and then run each scoring metric on the guide and their off-targets
  *
  */
class ScoringManager {

  // store our models
  var scoringModels = List[ScoreModel]()

  // parse out the scoring methods into a corresponding object list
  def findModelsFromCommandLine(parsedCommandLineTokens: Seq[String]) = {
    assert(scoringModels.size == 0, "You can't reinitialize the scoring models")


  }

}

object ScoringManager {

  def getRegisteredScoringMetric(name: String, bitEncoder: BitEncoding): ScoreModel = name.toLowerCase() match {
    case "crisprmit" => {
      val sc = new CrisprMitEduOffTarget()
      sc.bitEncoder(bitEncoder)
      sc
    }
    case _ => {
      throw new IllegalStateException("Unknown scoring metric requested: " + name)
    }

  }
}
