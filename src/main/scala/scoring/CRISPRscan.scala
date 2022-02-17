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
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import picocli.CommandLine.Command
import standards.{Cas9ParameterPack, Cas9Type, ParameterPack, SpCAS9}

/**
  * Implementation of Moreno-Mateos et. al. 2015 Nature Methods. Much (or all) credit goes to the team behind CRISPOR who's
  * reference implementation I worked from.
  */
class CRISPRscan extends SingleGuideScoreModel with LazyLogging with RankedScore {

  /**
    * @return true, a high score is good
    */
  override def highScoreIsGood: Boolean = true

  /**
    * define a score over a single guide. The is the core of the method
    * @param guide the guide to score
    * @return a score as a double
    */
  def calcScore(guide: CRISPRSiteOT): Double = {
    1.0 * (CRISPRscan.modelIntercept + CRISPRscan.paramsCRISPRscan.map{case(modelSeq,position,weight) => {
      val guideSubSeq = guide.target.sequenceContext.get.slice((position - 1),(position - 1) + modelSeq.length)
      assert(guideSubSeq.length == modelSeq.length,
        "Our comparison should have the same length: " + guideSubSeq + " and " + modelSeq + " for position " + position)

      if (guideSubSeq.toUpperCase() == modelSeq.toUpperCase()) weight else 0.0
    }}.sum)
  }

  private var bitEncoder: Option[BitEncoding] = None

  override def scoreName(): String = "Moreno-Mateos2015OnTarget"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "On target scoring metric described by Moreno-Mateos et. al. 2015 in Nature Methods"

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    require(validOverTargetSequence(Cas9ParameterPack, guide), "We're not a valid score over this guide")
    Array[Array[String]](Array[String]((calcScore(guide)).toString))
  }

  /**
    * this method is valid for Cas9
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = {
    enzyme.enzyme.enzymeParent == Cas9Type && enzyme.totalScanLength == ParameterPack.cas9ScanLength20mer
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def setup() {}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding the bit encoder to use
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {
    bitEncoder = Some(bitEncoding)
  }

  /**
    * we require 6 bases of sequence context on each side of the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = {
    if (enzyme.enzyme != SpCAS9) {
      false
    }
    else if (!guide.target.sequenceContext.isDefined) {
      false
    } else {

      // find the guide within the context, and determine if we have enough flanking sequence for the scoring metric
      val guidePos = SingleGuideScoreModel.findGuideSequenceWithinContext(guide)

      // do we have enough sequence?
      val enoughContextOnTheLeft = guidePos >= 6
      val enoughContextOnTheRight = guide.target.sequenceContext.get.size - (guidePos + guide.target.bases.size) >= 6

      enoughContextOnTheLeft & enoughContextOnTheRight
    }
  }

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String](scoreName)
}


object CRISPRscan {
  val modelIntercept = 0.183930943629

  val paramsCRISPRscan = Array[Tuple3[String, Int, Double]](("AA",18,-0.097377097),
  ("TT",18,-0.094424075),("TT",13,-0.08618771),("CT",26,-0.084264893),("GC",25,-0.073453609),
  ("T",21,-0.068730497),("TG",23,-0.066388075),("AG",23,-0.054338456),("G",30,-0.046315914),
  ("A",4,-0.042153521),("AG",34,-0.041935908),("GA",34,-0.037797707),("A",18,-0.033820432),
  ("C",25,-0.031648353),("C",31,-0.030715556),("G",1,-0.029693709),("C",16,-0.021638609),
  ("A",14,-0.018487229),("A",11,-0.018287292),("T",34,-0.017647692),("AA",10,-0.016905415),
  ("A",19,-0.015576499),("G",34,-0.014167123),("C",30,-0.013182733),("GA",31,-0.01227989),
  ("T",24,-0.011996172),("A",15,-0.010595296),("G",4,-0.005448869),("GG",9,-0.00157799),
  ("T",23,-0.001422243),("C",15,-0.000477727),("C",26,-0.000368973),("T",27,-0.000280845),
  ("A",31,0.00158975),("GT",18,0.002391744),("C",9,0.002449224),("GA",20,0.009740799),
  ("A",25,0.010506405),("A",12,0.011633235),("A",32,0.012435231),("T",22,0.013224035),
  ("C",20,0.015089514),("G",17,0.01549378),("G",18,0.016457816),("T",30,0.017263162),
  ("A",13,0.017628924),("G",19,0.017916844),("A",27,0.019126815),("G",11,0.020929039),
  ("TG",3,0.022949996),("GC",3,0.024681785),("G",14,0.025116714),("GG",10,0.026802158),
  ("G",12,0.027591138),("G",32,0.03071249),("A",22,0.031930909),("G",20,0.033957008),
  ("C",21,0.034262921),("TT",17,0.03492881),("T",13,0.035445171),("G",26,0.036146649),
  ("A",24,0.037466478),("C",22,0.03763162),("G",16,0.037970942),("GG",12,0.041883009),
  ("TG",18,0.045908991),("TG",31,0.048136812),("A",35,0.048596259),("G",15,0.051129717),
  ("C",24,0.052972314),("TG",15,0.053372822),("GT",11,0.053678436),("GC",9,0.054171402),
  ("CA",30,0.057759851),("GT",24,0.060952114),("G",13,0.061360905),("CA",24,0.06221937),
  ("AG",10,0.063717093),("G",10,0.067739182),("C",13,0.069495944),("GT",31,0.07342535),
  ("GG",13,0.074355848),("C",27,0.079933922),("G",27,0.085151052),("CC",21,0.088919601),
  ("CC",23,0.095072286),("G",22,0.10114438),("G",24,0.105488325),("GT",23,0.106718563),
  ("GG",25,0.111559441),("G",9,0.114600681))
}