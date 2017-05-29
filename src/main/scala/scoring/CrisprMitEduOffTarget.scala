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

import bitcoding.BitEncoding
import crispr.{CRISPRSiteOT}
import standards.{Cas9Type, ParameterPack}
import standards.ParameterPack._

import scala.util.Random

/**
  * for more information about this scoring scheme check out their documentation:
  *
  * http://crispr.mit.edu/about
  *
  * This website has the reputation for missing some proportion of the off-targets in the genome,
  * so our unit tests rely on only verified lists where all discovered off-target sequences are the same
  *
  */
class CrisprMitEduOffTarget() extends SingleGuideScoreModel {

  // the coefficients for each position in the guide
  val offtargetCoeff = Array[Double](
    0.0, 0.0, 0.014, 0.0, 0.0,
    0.395, 0.317, 0.0, 0.389, 0.079,
    0.445, 0.508, 0.613, 0.851, 0.732,
    0.828, 0.615, 0.804, 0.685, 0.583)

  // has to be setup before we do anything -- check with asserts
  var bitEncoder: Option[BitEncoding] = None

  override def scoreName(): String = "MITOffTarget"

  def scoreGuide(cRISPRHit: CRISPRSiteOT): String = score_crispr(cRISPRHit).toString

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "The scoring scheme from crispr.mit.edu"

  /**
    * score the crispr site using the MIT scheme
    *
    * @param cRISPRHit the guide details
    * @return a double score value
    */
  def score_crispr(cRISPRHit: CRISPRSiteOT): Double = {
    val scores = scoreAgainstOffTargets(cRISPRHit)
    val distances = get_distances(cRISPRHit)
    val meanDist = distances.sum.toDouble / distances.length.toDouble
    getScore(scores, meanDist)
  }

  /**
    * find all the pairwise distances between our off-targets
    *
    * @param cRISPRHit the guide / off-target object
    * @return the pairwise distances between all values
    *
    */
  private def get_distances(cRISPRHit: CRISPRSiteOT, maximumSitesToConsider: Int = 100): Array[Double] = {
    val otList =
      if (cRISPRHit.offTargets.size > maximumSitesToConsider)
        Random.shuffle(cRISPRHit.offTargets).slice(0,maximumSitesToConsider)
      else cRISPRHit.offTargets

    otList.combinations(2).map { case (ots) => {
      bitEncoder.get.mismatches(ots(0).sequence, ots(1).sequence).toDouble
    }}.toArray
  }

  /**
    * score the matches and mismatches with the score matrix
    * @param cRISPRHit the crispr hit information
    * @return the score tuples
    */
  def scoreAgainstOffTargets(cRISPRHit: CRISPRSiteOT): Array[Tuple2[Double, Int]] = {
    assert(bitEncoder.isDefined,"We don't have a valid bit encoding to work with")

    var scores = Array[Tuple2[Double, Int]]()
    cRISPRHit.offTargets.foreach { offTarget => {
      var score = 1.0
      var mismatches = 0

      val stringOTSeq = bitEncoder.get.bitDecodeString(offTarget.sequence)

      stringOTSeq.str.slice(0,20).zip(cRISPRHit.target.bases.slice(0,20)).zipWithIndex.foreach { case (bases, index) => {
        if (bases._1 == bases._2)
          score = score * 1.0
        else {
          score = score * (1.0 - offtargetCoeff(index))
          mismatches += 1
        }
      }}
      if (mismatches > 0) {
        scores :+= (score, mismatches)
      }
    }}
    scores
  }

  /**
    * get the final score
    *
    * @param scores
    * @param meanDistance
    * @return
    */
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

  /**
    * are we valid over the enzyme of interest?
    *
    * @param params the enzyme
    * @return truth about our ability to score this enzyme
    */
  override def validOverScoreModel(params: ParameterPack): Boolean = {
    params.enzyme.enzymeParent == Cas9Type
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param Args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    // we don't have any command line args
    args
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {this.bitEncoder = Some(bitEncoding)}

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = validOverScoreModel(enzyme)
}
