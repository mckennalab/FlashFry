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

import java.io.File

import bitcoding.BitEncoding
import crispr.{CRISPRHit, CRISPRSiteOT}
import standards.{Cas9Type, ParameterPack}


/**
  * for more information about this scoring scheme check out their documentation:
  *
  * http://crispr.mit.edu/about
  *
  * This website has the reputation for missing some proportion of the off-targets in the genome,
  * so our unit tests rely on only verified lists where all discovered off-target sequences are the same
  *
  */
class CrisprMitEduOffTarget() extends SingleGuideScoreModel with RankedScore {

  var countOnTargetInScore: Boolean = false

  // the coefficients for each position in the guide
  val offtargetCoeff = Array[Double](
    0.0, 0.0, 0.014, 0.0, 0.0,
    0.395, 0.317, 0.0, 0.389, 0.079,
    0.445, 0.508, 0.613, 0.851, 0.732,
    0.828, 0.615, 0.804, 0.685, 0.583)

  // do we consider on-target sequences (same exact sequence, just located in the genome) in the off-target score calculation?
  var considerOnTarget = false

  // estimated from Doench et al. We use this to down-weight non-NGG PAM sequences
  val pamToAdjustment = Map("GG" -> 1.0, "AG" -> 0.26, "CG" -> 0.11, "TG" -> 0.01)

  // has to be setup before we do anything -- check with asserts
  var bitEncoder: scala.Option[BitEncoding] = scala.None

  override def scoreName(): String = "Hsu2013"

  def scoreGuide(cRISPRHit: CRISPRSiteOT): Array[Array[String]] = Array[Array[String]](Array[String](score_crispr(cRISPRHit).toString))

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
    getScore(scores)
  }

  /**
    * score the matches and mismatches with the score matrix
    *
    * for more details, reference the help webpage on http://crispr.mit.edu/about
    * @param cRISPRHit the crispr hit information
    * @return the score tuples
    */
  def scoreAgainstOffTargets(cRISPRHit: CRISPRSiteOT): Array[Double] = {
    assert(bitEncoder.isDefined,"We don't have a valid bit encoding to work with")

    var scores = Array[Double]()
    cRISPRHit.offTargets.foreach { offTarget => {
      if (considerOnTarget | bitEncoder.get.mismatches(cRISPRHit.longEncoding,offTarget.sequence) != 0) {
        scores :+= scoreOffTarget(cRISPRHit, offTarget)
      }
    }}
    scores
  }

  /**
    * get the final score
    *
    * @param scores
    * @return
    */
  def getScore(scores: Array[Double]): Double = {
    (100.0 / (100.0 + scores.sum)) * 100.0
  }

  def scoreOffTarget(crispr: CRISPRSiteOT, offTarget: CRISPRHit): Double = {

    var mismatches = 0
    var distances = Array[Int]()
    var lastMismatch: scala.Option[Int] = scala.None
    val stringOTSeq = bitEncoder.get.bitDecodeString(offTarget.sequence)
    var equationPartOne = 1.0

    // first part of the equation -- score each mismatch from the target compared to the off target with their scoring matrix
    stringOTSeq.str.slice(0,CrisprMitEduOffTarget.guideSize).zip(crispr.target.bases.slice(0,CrisprMitEduOffTarget.guideSize)).zipWithIndex.foreach {
      case (bases, index) => {

      if (bases._1 != bases._2) {
        equationPartOne = equationPartOne * (1.0 - offtargetCoeff(index))

        mismatches += 1
        lastMismatch.map { tk =>
          distances :+= index - tk
        }
        lastMismatch = scala.Some(index)
      }
    }}

    // the second part -- penalize based on the average distance between mismatches
    // couldn't get this right, and has to find this first part in the CRISPOR code
    val equationPartTwo = if (mismatches < 2) {1.0} else {
      val avgDist = distances.sum.toDouble / distances.size.toDouble
      1.0 / ((((19 - avgDist)/19.0) * 4.0) + 1.0)
    }

    // the last part is a 'damper' according to the website, to penalize pairings with lots of mismatches
    val equationPartThree = if (mismatches == 0) { 1.0 } else { 1.0 / math.pow(mismatches.toDouble,2) }

    val totalScore = equationPartOne * equationPartTwo * equationPartThree * 100.0
    val pamAdj = if (pamToAdjustment contains stringOTSeq.str.slice(CrisprMitEduOffTarget.pamStart, CrisprMitEduOffTarget.pamStop)) {
      pamToAdjustment(stringOTSeq.str.slice(CrisprMitEduOffTarget.pamStart, CrisprMitEduOffTarget.pamStop))
    } else {
      CrisprMitEduOffTarget.defaultValue
    }

    totalScore * pamAdj
  }

  /**
    * are we valid over the enzyme of interest?
    *
    * @param params the enzyme
    * @return truth about our ability to score this enzyme
    */
  override def validOverEnzyme(params: ParameterPack): Boolean = {
    params.enzyme.enzymeParent == Cas9Type && params.totalScanLength == ParameterPack.cas9ScanLength20mer
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    */
  override def setup(): Unit = {
    // just parse the options
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
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = validOverEnzyme(enzyme)

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String](scoreName)

  /**
    * @return true, a high score is good
    */
  override def highScoreIsGood: Boolean = true
}

object CrisprMitEduOffTarget {
  val guideSize = 20
  val pamStart = 21 // for the NGG pams
  val pamStop = 23
  val defaultValue = 0.01
}