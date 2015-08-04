package crispr.models

import crispr.CRISPRGuide
import scala.util.Random

/**
 * Created by aaronmck on 6/18/15.
 */
class OffTarget extends ScoreModel {
  val offtargetCoeff = Array[Double](0.0, 0.0, 0.014, 0.0, 0.0,
    0.395, 0.317, 0.0, 0.389, 0.079,
    0.445, 0.508, 0.613, 0.851, 0.732,
    0.828, 0.615, 0.804, 0.685, 0.583)

  override def scoreName(): String = "off-target"

  override def scoreGuide(cRISPRHit: CRISPRGuide): String = score_crispr(cRISPRHit).toString


  def scoreAgainstOffTargets(cRISPRHit: CRISPRGuide): Array[Tuple2[Double, Int]] = {
    var scores = Array[Tuple2[Double, Int]]()
    cRISPRHit.offBases.foreach { offTarget => {
      var score = 1.0
      var mismatches = 0

      cRISPRHit.name.zip(offTarget).zipWithIndex.foreach { case (bases, index) => {
        if (bases._1 == bases._2)
          score = score * 1.0
        else {
          score = score * (1.0 - offtargetCoeff(index))
          mismatches += 1
        }
      }
      }
      if (mismatches > 0) {
        scores :+=(score, mismatches)
      }
    }
    }
    scores
  }


  def getScore(scores: Array[Tuple2[Double, Int]], meanDistance: Double): Double = {
    var finalScore = 100.0
    scores.foreach { case (score, mismatch) => {
      val termTwo = (1.0 / (((19.0 - meanDistance) / 19.0) * 4.0 + 1.0))
      val termThree = ((1.0 / (mismatch)) * (1.0 / (mismatch)))
      val thisScore = score * termTwo * termThree
      finalScore += 100.0 * thisScore

    }
    }
    return ((100.0 / (finalScore)) * 100.0)
  }


  def score_crispr(cRISPRHit: CRISPRGuide): Double = {
    val scores = scoreAgainstOffTargets(cRISPRHit)
    val distances = get_distances(cRISPRHit)
    val meanDist = distances.sum.toDouble / distances.length.toDouble
    getScore(scores, meanDist)
  }


  def get_distances(cRISPRHit: CRISPRGuide, maxTargetsToSample: Int = 100): Array[Double] = {
    var distances = Array[Double]()

    if (cRISPRHit.offBases.size > maxTargetsToSample) {
      val newSubset = Random.shuffle(cRISPRHit.offBases).slice(0,maxTargetsToSample)
      newSubset.combinations(2).foreach { case (strings) => {
        distances :+= CRISPRGuide.editDistance(strings(0), strings(1),strings(0).length).toDouble
      }}
    }
    else {
      cRISPRHit.offBases.combinations(2).foreach { case (strings) => {
        distances :+= CRISPRGuide.editDistance(strings(0), strings(1),strings(0).length).toDouble
      }}
    }
    return (distances)
  }
}
