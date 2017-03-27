package scoring

import bitcoding.BitEncoding

/**
  * create the scoring metrics, and then run each scoring metric on the guide and their off-targets
  *
  */
class ScoringManager {

  // store our models
  var scoringModels = List[ScoreModel]()

  // parse out the scoring methods into a corresponding object list
  def findModelsFromCommandLine(parsedCommandLineTokens: Seq[String], bitEncoder: BitEncoding, commandLineArgs: Array[String]) = {
    assert(scoringModels.size == 0, "You can't reinitialize the scoring models")

    parsedCommandLineTokens.foreach{ model => {
      val model = ScoringManager.getRegisteredScoringMetric(model,bitEncoder)
      model.parseScoringParameters(commandLineArgs)
      scoringModels :+= model
    }}
  }

}

object ScoringManager {

  def getRegisteredScoringMetric(name: String, bitEncoder: BitEncoding): ScoreModel = name.toLowerCase() match {
    case "crisprmit" => {
      val sc = new CrisprMitEduOffTarget()
      sc.bitEncoder(bitEncoder)
      sc
    }
    case "annotate" => {
      new BedAnnotation()
    }
    case "Doench2014OnTarget" => {
      new Doench2014OnTarget()
    }
    case "Doench2016CDF" => {
      new Doench2016CDFScore()
    }
    case _ => {
      throw new IllegalStateException("Unknown scoring metric requested: " + name)
    }

  }
}
