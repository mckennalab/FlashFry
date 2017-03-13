package output

import java.io.PrintWriter

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import crispr.filter.SequencePreFilter

/**
  * handle outputing the target list, in a style the user requests
  */
case class TargetOutput(outputFile: String,
                        targets: Array[CRISPRSiteOT],
                        includePositionInformation: Boolean,
                        indicateIfTargetHasPefectMatch: Boolean,
                        filters: Array[SequencePreFilter],
                        bitEncoder: BitEncoding,
                        bitPosition: BitPosition
                       ) {

  // sort the target array
  scala.util.Sorting.quickSort(targets)

  // create a output file
  val output = new PrintWriter(outputFile)

  // add the filter informations to the top
  filters.foreach{filter => {
    output.write(TargetOutput.headerComment + filter.toStringSummary() + "\n")
  }}

  // output the targets
  targets.foreach{target => {
    val targetString = new StringBuilder()
    targetString.append(target.target.contig + TargetOutput.sep)
    targetString.append(target.target.position + TargetOutput.sep)
    targetString.append((target.target.position + target.target.bases.size) + TargetOutput.sep)
    targetString.append(target.target.bases + TargetOutput.sep)
    targetString.append((if (target.full) "OVERFLOW" else "OK") + TargetOutput.sep)
    targetString.append((if (target.target.forwardStrand) "FWD" else "RVS") + TargetOutput.sep)

    val offTargets = target.offTargets.toArray

    targetString.append(offTargets.size + TargetOutput.sep)

    if (indicateIfTargetHasPefectMatch) {
      val perfectMatches = offTargets.map(ot => if (bitEncoder.mismatches(target.longEncoding, ot.sequence) == 0) 1 else 0).sum
      if (perfectMatches > 0) targetString.append("IN_GENOME_" + perfectMatches + TargetOutput.sep) else targetString.append("NOVEL"+ TargetOutput.sep)
    }

    // now output the off target information --
    // if we want to include the positions of each OT in the genome:
    if (includePositionInformation) {
      val offTargetsString = target.offTargets.toArray.map{ot => {
        val otDecoded = bitEncoder.bitDecodeString(ot.sequence)
        val positions = ot.coordinates.map{otPos => {
          bitPosition.decode(otPos)._1 + ":" + bitPosition.decode(otPos)._2
        }}.mkString(",")
        otDecoded.str + "_" + otDecoded.count + bitEncoder.mismatches(target.longEncoding,ot.sequence) + "<" + positions + ">"
      }}.mkString(",")

      targetString.append(offTargetsString)
    }
    // don't output positional information
    else {
      val offTargetsString = target.offTargets.toArray.map{ot => {
        val otDecoded = bitEncoder.bitDecodeString(ot.sequence)
        otDecoded.str + "_" + otDecoded.count + "_" + bitEncoder.mismatches(target.longEncoding,ot.sequence)
      }}.mkString(",")

      targetString.append(offTargetsString)
    }

    output.write(targetString.result() + "\n")
  }}

  // close it up
  output.close()
}

object TargetOutput {
  val headerComment = "# "
  val sep = "\t"
}