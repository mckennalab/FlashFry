package crispr

import bitcoding.{BitEncoding, BitPosition}
import targetio.{TabDelimitedOutput}

/**
  * stores an off-target hit from the genome
  *
  * @param sq the sequence
  * @param coord it's coordinates
  */
class CRISPRHit(sq: Long, coord: Array[Long]) {
  val sequence = sq
  val coordinates = coord

  def toOutput(bitEncoding: BitEncoding, posEnc: BitPosition, guide: Long, outputPositions: Boolean): String = {
    // now output the off target information --
    if (outputPositions) {
        val otDecoded = bitEncoding.bitDecodeString(sequence)
        val positions = coordinates.map { otPos => {
          val decoded = posEnc.decode(otPos)
          decoded.contig + TabDelimitedOutput.contigSeperator + decoded.start + TabDelimitedOutput.strandSeperator +
            (if (decoded.forwardStrand) TabDelimitedOutput.positionForward else TabDelimitedOutput.positionReverse)
        }
        }

        if (positions.size == 0) {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeperator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeperator +
            bitEncoding.mismatches(guide, sequence)
        } else {
          otDecoded.str + TabDelimitedOutput.withinOffTargetSeperator +
            otDecoded.count + TabDelimitedOutput.withinOffTargetSeperator +
            bitEncoding.mismatches(guide, sequence) + TabDelimitedOutput.positionListTerminatorFront +
            positions.mkString(TabDelimitedOutput.positionListSeperator) + TabDelimitedOutput.positionListTerminatorBack
        }
      }
    else {
      {
        val otDecoded = bitEncoding.bitDecodeString(sequence)
        otDecoded.str + TabDelimitedOutput.withinOffTargetSeperator + otDecoded.count +
          TabDelimitedOutput.withinOffTargetSeperator + bitEncoding.mismatches(guide,sequence)
      }
    }
  }
}
