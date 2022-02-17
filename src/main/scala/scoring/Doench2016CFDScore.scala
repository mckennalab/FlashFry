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
import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
import picocli.CommandLine.Command
import utils.Utils
import standards.{Cas9Type, ParameterPack}

import scala.collection.mutable.ArrayBuffer

/**
  * implementation of the Doench 2016 CFD score from their python code
  * doi:10.1038/nbt.3437
  */
class Doench2016CFDScore extends SingleGuideScoreModel with RankedScore {
  var bitCode: Option[BitEncoding] = None


  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "Doench2016CFDScore"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Score off-target effects from Doench 'Optimized sgRNA design to maximize activity and minimize off-target effects of CRISPR-Cas9'"

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    assert(guide.target.bases.size == 23, "We saw an unexpected guide size of " + guide.target.bases.size)
    assert(bitCode.isDefined, "Our bitEncoder has not been set")

    val bases = guide.target.bases
    val scores = new ArrayBuffer[Tuple2[Double,Int]]()

    guide.offTargets.foreach{ ot => {

      // find the maximum CFD score over our off-targets
      val otScore = bitCode.get.bitDecodeString(ot.sequence)

      // we exclude any on-targets that are listed as off-targets
      // since we're only dealing with Cas9 we can slice the first 20 bases
      if (otScore.str.slice(0,20) != guide.target.bases.slice(0,20)) {

        var pam = Doench2016CFDScore.pamLookup(otScore.str.slice(otScore.str.length - 2, otScore.str.length))

        val candidateScore = scoreCFD(bases.slice(0, 20), otScore.str.slice(0, 20))
        ot.addScore(this.scoreName(),(pam * candidateScore).toString)
        scores += Tuple2[Double,Int](pam * candidateScore,bitCode.get.getCount(ot.sequence))
      }
      //val candidateScore = scoreCFD(bases.slice(0,20), otScore.str.slice(0,20))
      //totalScore *= candidateScore
    }}

    val specificity_score = if (scores.size > 0) 1.0 / (1.0 + scores.map { case (score, count) => score * count }.sum) else 1.0
    val maxScore = if (scores.size > 0) scores.map{case(score,count) => score}.max else 0.0

    // guided by CRISPOR paper -- thresholding at 0.023
    if (maxScore >= Doench2016CFDScore.cfdMinimumThreshold) {
      Array[Array[String]](Array[String](maxScore.toString),Array[String](specificity_score.toString))
    } else {
      Array[Array[String]](Array[String]("0.0"),Array[String](specificity_score.toString))
    }
  }

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = {
    enzyme.enzyme.enzymeParent == Cas9Type && enzyme.totalScanLength == ParameterPack.cas9ScanLength20mer
  }

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = {
    (enzyme.totalScanLength == 23) && enzyme.enzyme.enzymeParent == Cas9Type
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def setup(){}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {bitCode = Some(bitEncoding)}

  /**
    * score the CFD
    * @param guide the guide, as 20 bases
    * @param otString the string we want to score against
    * @return the score, as a double
    */
  def scoreCFD(guide: String, otString: String): Double = {
    assert(guide.size == 20, "guide size is not 20")
    assert(otString.size == 20, "Wildtype string size is not 20")

    val guideWithUs = guide.toUpperCase().replace('T','U')
    val offTString = otString.toUpperCase().replace('T','U')

    var score = 1.0
    guideWithUs.zip(offTString).zipWithIndex.foreach{case((gBase,otBase),index) => {
      if (gBase != otBase) {
        val key = "r" + gBase + ":d" + specialReverseCompBase(otBase) + "," + (index + 1)
        //println("key = " + key)
        assert(Doench2016CFDScore.mmLookup contains key,"Missing key " + key + " in mm Lookup table")
        // println(key + "-> " + Doench2016CFDScore.mmLookup(key) + " score " + score)
        score *= Doench2016CFDScore.mmLookup(key)
      }
    }}
    // println("Final " + score)
    score
  }

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("DoenchCFD_maxOT","DoenchCFD_specificityscore")


  def specialReverseCompBase(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'U') 'A' else c

  /**
    * @return a low CFD score is good
    */
  override def highScoreIsGood: Boolean = false
}

// constants for the CDF score --- taken from their Python Pickle files
object Doench2016CFDScore {

  // guided by CRISPOR paper -- thresholding at 0.023
  val cfdMinimumThreshold = 0.023

  val mmLookup = Map("rC:dC,9" -> 0.619047619, "rC:dC,8" -> 0.642857143, "rG:dA,8" -> 0.625, "rG:dG,19" -> 0.448275862, "rG:dG,18" -> 0.476190476,
    "rG:dG,15" -> 0.272727273, "rG:dG,14" -> 0.428571429, "rG:dG,17" -> 0.235294118, "rG:dG,16" -> 0.0, "rC:dC,20" -> 0.058823529, "rG:dT,20" -> 0.9375,
    "rG:dG,13" -> 0.421052632, "rG:dG,12" -> 0.529411765, "rC:dT,13" -> 0.384615385, "rC:dT,18" -> 0.538461538, "rC:dC,3" -> 0.5, "rU:dG,12" -> 0.947368421,
    "rA:dG,13" -> 0.210526316, "rA:dG,12" -> 0.263157895, "rA:dG,11" -> 0.4, "rA:dG,10" -> 0.333333333, "rA:dA,19" -> 0.538461538, "rA:dA,18" -> 0.5,
    "rA:dG,15" -> 0.272727273, "rA:dG,14" -> 0.214285714, "rA:dA,15" -> 0.2, "rA:dA,14" -> 0.533333333, "rA:dA,17" -> 0.133333333, "rA:dA,16" -> 0.0,
    "rA:dA,11" -> 0.307692308, "rA:dA,10" -> 0.882352941, "rA:dA,13" -> 0.3, "rA:dA,12" -> 0.333333333, "rG:dA,13" -> 0.3, "rG:dA,12" -> 0.384615385,
    "rG:dA,11" -> 0.384615385, "rG:dA,10" -> 0.8125, "rG:dA,17" -> 0.25, "rG:dA,16" -> 0.0, "rG:dA,15" -> 0.142857143, "rG:dA,14" -> 0.266666667,
    "rG:dT,10" -> 0.933333333, "rG:dA,19" -> 0.666666667, "rG:dA,18" -> 0.666666667, "rC:dT,15" -> 0.222222222, "rA:dC,4" -> 0.625, "rA:dG,9" -> 0.571428571,
    "rA:dG,8" -> 0.428571429, "rC:dA,3" -> 0.6875, "rC:dA,2" -> 0.909090909, "rC:dA,1" -> 1.0, "rC:dA,7" -> 0.8125, "rC:dA,6" -> 0.928571429, "rC:dA,5" -> 0.636363636,
    "rC:dA,4" -> 0.8, "rC:dA,9" -> 0.875, "rC:dA,8" -> 0.875, "rA:dG,20" -> 0.227272727, "rA:dG,3" -> 0.428571429, "rA:dG,2" -> 0.785714286, "rG:dA,20" -> 0.7,
    "rC:dT,20" -> 0.5, "rC:dT,12" -> 0.714285714, "rG:dT,17" -> 0.933333333, "rC:dA,17" -> 0.466666667, "rC:dA,16" -> 0.307692308, "rC:dA,15" -> 0.066666667,
    "rC:dA,14" -> 0.733333333, "rC:dA,13" -> 0.7, "rC:dA,12" -> 0.538461538, "rC:dA,11" -> 0.307692308, "rC:dA,10" -> 0.941176471, "rG:dG,11" -> 0.428571429,
    "rA:dC,20" -> 0.764705882, "rC:dA,19" -> 0.461538462, "rG:dG,10" -> 0.4, "rU:dG,17" -> 0.705882353, "rU:dG,16" -> 0.666666667, "rU:dG,15" -> 0.272727273,
    "rU:dG,14" -> 0.285714286, "rU:dG,13" -> 0.789473684, "rU:dC,20" -> 0.176470588, "rU:dG,11" -> 0.666666667, "rU:dG,10" -> 0.533333333, "rG:dA,7" -> 0.571428571,
    "rG:dA,6" -> 0.666666667, "rG:dA,5" -> 0.3, "rG:dA,4" -> 0.363636364, "rG:dA,3" -> 0.5, "rG:dA,2" -> 0.636363636, "rG:dA,1" -> 1.0, "rG:dT,7" -> 1.0,
    "rG:dT,4" -> 0.9, "rG:dG,6" -> 0.681818182, "rU:dT,20" -> 0.5625, "rC:dC,15" -> 0.05, "rC:dC,14" -> 0.0, "rC:dC,17" -> 0.058823529, "rC:dC,16" -> 0.153846154,
    "rC:dC,11" -> 0.25, "rC:dC,10" -> 0.388888889, "rC:dC,13" -> 0.136363636, "rC:dC,12" -> 0.444444444, "rC:dA,20" -> 0.3, "rC:dC,19" -> 0.125,
    "rC:dC,18" -> 0.133333333, "rA:dA,1" -> 1.0, "rA:dA,3" -> 0.705882353, "rA:dA,2" -> 0.727272727, "rA:dA,5" -> 0.363636364, "rA:dA,4" -> 0.636363636,
    "rA:dA,7" -> 0.4375, "rA:dA,6" -> 0.714285714, "rA:dA,9" -> 0.6, "rA:dA,8" -> 0.428571429, "rU:dG,20" -> 0.090909091, "rU:dT,12" -> 0.8, "rU:dT,13" -> 0.692307692,
    "rU:dT,10" -> 0.857142857, "rU:dT,11" -> 0.75, "rU:dT,16" -> 0.909090909, "rU:dT,17" -> 0.533333333, "rU:dT,14" -> 0.619047619, "rU:dT,15" -> 0.578947368,
    "rC:dC,1" -> 0.913043478, "rU:dT,18" -> 0.666666667, "rC:dC,2" -> 0.695652174, "rC:dC,5" -> 0.6, "rC:dC,4" -> 0.5, "rC:dC,7" -> 0.470588235, "rC:dC,6" -> 0.5,
    "rA:dC,2" -> 0.8, "rU:dT,8" -> 0.8, "rU:dT,9" -> 0.928571429, "rA:dC,11" -> 0.65, "rA:dC,19" -> 0.375, "rA:dC,18" -> 0.4, "rA:dC,17" -> 0.176470588,
    "rA:dC,16" -> 0.192307692, "rA:dC,15" -> 0.65, "rU:dT,3" -> 0.714285714, "rU:dT,4" -> 0.476190476, "rC:dA,18" -> 0.642857143, "rU:dT,6" -> 0.866666667,
    "rA:dC,10" -> 0.555555556, "rC:dT,10" -> 0.866666667, "rU:dT,5" -> 0.5, "rC:dT,8" -> 0.65, "rC:dT,9" -> 0.857142857, "rC:dT,6" -> 0.928571429, "rC:dT,7" -> 0.75,
    "rC:dT,4" -> 0.842105263, "rC:dT,5" -> 0.571428571, "rC:dT,2" -> 0.727272727, "rA:dC,9" -> 0.666666667, "rC:dT,1" -> 1.0, "rA:dC,8" -> 0.733333333,
    "rU:dT,1" -> 1.0, "rC:dT,14" -> 0.35, "rU:dT,2" -> 0.846153846, "rU:dG,19" -> 0.275862069, "rG:dT,14" -> 0.75, "rG:dT,15" -> 0.941176471, "rG:dT,16" -> 1.0,
    "rA:dC,14" -> 0.466666667, "rG:dG,20" -> 0.428571429, "rG:dT,11" -> 1.0, "rG:dT,12" -> 0.933333333, "rG:dT,13" -> 0.923076923, "rA:dG,7" -> 0.4375,
    "rA:dC,13" -> 0.652173913, "rA:dG,5" -> 0.5, "rA:dG,4" -> 0.352941176, "rG:dT,18" -> 0.692307692, "rG:dT,19" -> 0.714285714, "rA:dG,1" -> 0.857142857,
    "rA:dC,12" -> 0.722222222, "rG:dG,1" -> 0.714285714, "rG:dT,3" -> 0.75, "rG:dG,3" -> 0.384615385, "rG:dG,2" -> 0.692307692, "rG:dG,5" -> 0.785714286,
    "rG:dG,4" -> 0.529411765, "rG:dG,7" -> 0.6875, "rG:dT,5" -> 0.866666667, "rG:dG,9" -> 0.538461538, "rG:dG,8" -> 0.615384615, "rG:dT,8" -> 1.0,
    "rG:dT,9" -> 0.642857143, "rU:dG,18" -> 0.428571429, "rU:dT,7" -> 0.875, "rA:dG,6" -> 0.454545455, "rG:dT,6" -> 1.0, "rA:dA,20" -> 0.6, "rA:dC,5" -> 0.72,
    "rA:dG,17" -> 0.176470588, "rU:dC,8" -> 0.733333333, "rA:dG,16" -> 0.0, "rG:dT,2" -> 0.846153846, "rA:dG,19" -> 0.206896552, "rU:dG,3" -> 0.428571429,
    "rU:dG,2" -> 0.857142857, "rU:dG,1" -> 0.857142857, "rA:dG,18" -> 0.19047619, "rU:dG,7" -> 0.6875, "rU:dG,6" -> 0.909090909, "rU:dG,5" -> 1.0,
    "rU:dG,4" -> 0.647058824, "rU:dG,9" -> 0.923076923, "rU:dG,8" -> 1.0, "rC:dT,11" -> 0.75, "rC:dT,3" -> 0.866666667, "rU:dC,19" -> 0.25, "rU:dC,18" -> 0.333333333,
    "rU:dC,13" -> 0.260869565, "rU:dC,12" -> 0.5, "rU:dC,11" -> 0.4, "rU:dC,10" -> 0.5, "rU:dC,17" -> 0.117647059, "rU:dC,16" -> 0.346153846, "rU:dC,15" -> 0.05,
    "rU:dC,14" -> 0.0, "rU:dC,7" -> 0.588235294, "rU:dC,6" -> 0.571428571, "rU:dC,5" -> 0.64, "rU:dC,4" -> 0.625, "rU:dC,3" -> 0.5, "rU:dC,2" -> 0.84,
    "rU:dC,1" -> 0.956521739, "rC:dT,17" -> 0.466666667, "rA:dC,3" -> 0.611111111, "rC:dT,19" -> 0.428571429, "rA:dC,1" -> 1.0, "rA:dC,7" -> 0.705882353,
    "rA:dC,6" -> 0.714285714, "rU:dC,9" -> 0.619047619, "rG:dA,9" -> 0.533333333, "rU:dT,19" -> 0.285714286, "rC:dT,16" -> 1.0, "rG:dT,1" -> 0.9)

  val pamLookup = Map("AA" -> 0.0, "AC" -> 0.0, "GT" -> 0.016129032, "AG" -> 0.259259259,
    "CC" -> 0.0, "CA" -> 0.0, "CG" -> 0.107142857, "TC" -> 0.0, "GG" -> 1.0, "GC" -> 0.022222222,
    "AT" -> 0.0, "GA" -> 0.069444444, "TG" -> 0.038961039, "CT" -> 0.0, "TT" -> 0.0, "TA" -> 0.0)
}