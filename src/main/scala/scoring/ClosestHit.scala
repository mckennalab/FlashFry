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
import standards.ParameterPack
import utils.Utils

import scala.collection.mutable

/**
  * what's the closest off-target in mismatch space? a convenience class
  * to make guide selection easier
  */
class ClosestHit extends SingleGuideScoreModel {
  var bitEncoder : Option[BitEncoding] = None

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    var closest = Int.MaxValue
    var count = 0
    val maxMisMatchStoredInArray = 4

    val otCounts = new mutable.LinkedHashMap[Int, Int]()
    (0 until (maxMisMatchStoredInArray + 1)).foreach{cnt => otCounts(cnt) = 0}

    if (guide.offTargets.size > 0) {
      guide.offTargets.foreach { ot => {
        val mismatches = bitEncoder.get.mismatches(ot.sequence, guide.longEncoding)
        val otCount = bitEncoder.get.getCount(ot.sequence)

        if (mismatches <= maxMisMatchStoredInArray) {
          otCounts(mismatches) += otCount
          // println(guide.offTargets.size + " -- MIMASTCHES " + mismatches + " COUNT " + otCount + " --> " + otCounts(mismatches))
        }

        if (mismatches < closest && mismatches > 0) {
          closest = mismatches
          count = otCount
        } else if (mismatches == closest) {
          count += otCount
        }
      }
      }
    }

    if (closest == Int.MaxValue) {
      Array[Array[String]](Array[String]("UNK"), Array[String]("0"), Array[String](otCounts.values.mkString(",")))
    } else {
      Array[Array[String]](Array[String](closest.toString), Array[String](count.toString), Array[String](otCounts.values.mkString(",")))
    }
  }

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "closest"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "recorded the minimum number of mismatches to the set of off-targets"

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
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
  override def setup() {}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {this.bitEncoder = Some(bitEncoding)}

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("basesDiffToClosestHit","closestHitCount","0-1-2-3-4_mismatch")
}
