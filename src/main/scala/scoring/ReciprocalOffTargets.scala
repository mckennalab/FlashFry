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

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack

/**
  * You sometimes don't want targets within your scan to have reciprocal off-targets: where one guide
  * is a really close off-target for another guide in your results. This may lead to drop-out of a whole
  * region, which can be really confusing when looking at functional effects.
  */
class ReciprocalOffTargets() extends ScoreModel {

  var maxMismatch = 1

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "ReciprocalOffTargets"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Will guides within this design target one-anothers sites? "

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guides the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    guides.foreach{guide1 => {
      guides.foreach{guide2 => {
        if (bitEnc.mismatches(guide1.longEncoding,guide2.longEncoding) != 0 && bitEnc.mismatches(guide1.longEncoding,guide2.longEncoding) <= maxMismatch) {
          guide1.namedAnnotations(scoreName) = guide1.namedAnnotations.getOrElse(scoreName,Array[String]()) :+ guide2.target.bases
        }
      }}
    }}
  }

  /**
    * we're valid over all target types
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = true

  /**
    * always true for ReciprocalOffTargets
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

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
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("ReciprocalOffTargets")
}

