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

package crispr

import bitcoding.{BitEncoding, BitPosition}
import targetio.TabDelimitedOutput
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
  * stores an off-target hit from the genome
  *
  * @param sq the sequence
  * @param coord it's coordinates
  * @param validOffTargetCoordinates do we encode the positional information about off targets? What happens is that
  *                                  we have to pass off-target information through from input files that don't contain
  *                                  positional information. Since we've erased that information we have to mark it here
  *
  *
  */
class CRISPRHit(sq: Long, coord: Array[Long], validOffTargetCoordinates: Boolean = true) {
  val sequence = sq
  private val coordinates = coord
  def getOffTargetCount = coordinates.size
  var scores : Option[mutable.HashMap[String,String]] = None


  /**
    * Generate a string representation of the CRISPR Hit
    * @param bitEncoding our bitencoder
    * @param posEnc the position encodier
    * @param guide our guide as a Long
    * @param outputPositions should we output the position information
    * @return a collapsed string representation
    */
  def toOutput(bitEncoding: BitEncoding, posEnc: BitPosition, guide: Long, outputPositions: Boolean): String = {
    val otDecoded = bitEncoding.bitDecodeString(sequence)

    if (outputPositions) {
        val positions = if (validOffTargetCoordinates) coordinates.map { otPos => {
          val decoded = posEnc.decode(otPos)
          decoded.contig + TabDelimitedOutput.contigSeparator + decoded.start + TabDelimitedOutput.strandSeparator +
            (if (decoded.forwardStrand) TabDelimitedOutput.positionForward else TabDelimitedOutput.positionReverse)
        }
        } else Array[Long]()

        val retString = if (positions.size == 0 || !validOffTargetCoordinates) {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeparator +
            bitEncoding.mismatches(guide, sequence)
        } else {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeparator +
            bitEncoding.mismatches(guide, sequence) + TabDelimitedOutput.positionListTerminatorFront +
            positions.mkString(TabDelimitedOutput.positionListSeparator) + TabDelimitedOutput.positionListTerminatorBack
        }
      if (scores.isDefined) {
        val scoreString = toOutputScores()
        retString + scoreString.get
      } else {
        retString
      }
      }
    else {
      {
        otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator + otDecoded.count +
          TabDelimitedOutput.withinOffTargetSeparator + bitEncoding.mismatches(guide,sequence)
      }
    }
  }

  /**
    *
    * @return a string of
    */
  def toOutputScores(): Option[String] = {
    if (!scores.isDefined) {
      None
    } else {
      Some(TabDelimitedOutput.offTargetScoresTerminatorFront + scores.get.map{case(k,v) => {
        k + TabDelimitedOutput.offTargetScoresPairing + v
      }}.mkString(TabDelimitedOutput.offTargetScoresSeparator) + TabDelimitedOutput.offTargetScoresTerminatorBack)
    }

  }

  def addScore(key: String, value: String) = {
    if (!scores.isDefined)
      scores = Some(new mutable.HashMap[String,String]())

    assert(!scores.get.contains(key),"Scores already contains " + key + "(sequence " + sequence + ")")
    scores.get(key) = value
  }
}
