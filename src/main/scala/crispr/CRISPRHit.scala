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
  * One distinct off-target sequence and its aggregate genomic occurrence information.
  *
  * `occurrenceCount` is separate from `sq` so wide target encodings can use all 64 bits for sequence. When callers omit
  * it, the class first uses the number of supplied coordinates and then falls back to the count embedded in a legacy
  * packed sequence. New code that lacks coordinates should always pass the count explicitly.
  *
  * @param sq two-bit encoded target sequence
  * @param coord encoded genomic positions; may be empty when an input file omitted positions
  * @param validOffTargetCoordinates whether `coord` represents real positions suitable for output
  * @param occurrenceCount explicit aggregate count, or `-1` to use the compatibility fallback
  */
class CRISPRHit(sq: Long,
                coord: Array[Long],
                validOffTargetCoordinates: Boolean = true,
                occurrenceCount: Int = -1) {
  val sequence = sq
  private val coordinates = coord
  private val legacyEncodedCount = (sq >> 48).toShort.toInt
  private val storedOccurrenceCount = if (occurrenceCount >= 0) occurrenceCount
    else if (coordinates.nonEmpty) coordinates.length
    else math.max(0, legacyEncodedCount)
  require(storedOccurrenceCount >= 0, "Off-target occurrence count cannot be negative")
  /** Number of genomic occurrences represented by this distinct sequence. */
  def getOffTargetCount: Int = storedOccurrenceCount
  var scores : Option[mutable.HashMap[String,String]] = None


  /**
    * Render this hit in FlashFry's tab-delimited off-target field format.
    *
    * @param bitEncoding target decoder and mismatch calculator
    * @param posEnc genomic position decoder
    * @param guide encoded guide used to calculate mismatch distance
    * @param outputPositions include positions when valid coordinates are available
    * @return serialized off-target entry, including per-hit scores when present
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
            getOffTargetCount + TabDelimitedOutput.withinOffTargetSeparator +
            bitEncoding.mismatches(guide, sequence)
        } else {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator +
            getOffTargetCount + TabDelimitedOutput.withinOffTargetSeparator +
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
        otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator + getOffTargetCount +
          TabDelimitedOutput.withinOffTargetSeparator + bitEncoding.mismatches(guide,sequence)
      }
    }
  }

  /** Serialize attached per-hit scores, or return `None` when no score has been attached. */
  def toOutputScores(): Option[String] = {
    if (!scores.isDefined) {
      None
    } else {
      Some(TabDelimitedOutput.offTargetScoresTerminatorFront + scores.get.map{case(k,v) => {
        k + TabDelimitedOutput.offTargetScoresPairing + v
      }}.mkString(TabDelimitedOutput.offTargetScoresSeparator) + TabDelimitedOutput.offTargetScoresTerminatorBack)
    }

  }

  /** Attach a score exactly once under `key`. */
  def addScore(key: String, value: String) = {
    if (!scores.isDefined)
      scores = Some(new mutable.HashMap[String,String]())

    assert(!scores.get.contains(key),"Scores already contains " + key + "(sequence " + sequence + ")")
    scores.get(key) = value
  }
}
