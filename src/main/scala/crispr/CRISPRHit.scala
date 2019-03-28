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
import targetio.{TabDelimitedOutput}

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

  def toOutput(bitEncoding: BitEncoding, posEnc: BitPosition, guide: Long, outputPositions: Boolean): String = {
    // now output the off-target information --
    if (outputPositions) {
        val otDecoded = bitEncoding.bitDecodeString(sequence)

        val positions = if (validOffTargetCoordinates) coordinates.map { otPos => {
          val decoded = posEnc.decode(otPos)
          decoded.contig + TabDelimitedOutput.contigSeparator + decoded.start + TabDelimitedOutput.strandSeparator +
            (if (decoded.forwardStrand) TabDelimitedOutput.positionForward else TabDelimitedOutput.positionReverse)
        }
        } else Array[Long]()

        if (positions.size == 0 || !validOffTargetCoordinates) {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeparator +
            bitEncoding.mismatches(guide, sequence)
        } else {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeparator +
            bitEncoding.mismatches(guide, sequence) + TabDelimitedOutput.positionListTerminatorFront +
            positions.mkString(TabDelimitedOutput.positionListSeparator) + TabDelimitedOutput.positionListTerminatorBack
        }
      }
    else {
      {
        val otDecoded = bitEncoding.bitDecodeString(sequence)
        otDecoded.str + TabDelimitedOutput.withinOffTargetSeparator + otDecoded.count +
          TabDelimitedOutput.withinOffTargetSeparator + bitEncoding.mismatches(guide,sequence)
      }
    }
  }
}
