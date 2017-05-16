package targetio

import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSiteOT}
import reference.CRISPRSite

/**
  * Created by aaronmck on 3/27/17.
  */
object GuideEncodingTools {
  val headerComment = "# "
  val sep = "\t"

  val forward = "FWD"
  val reverse = "RVS"
  val overflow = "OVERFLOW"
  val targetOK = "OK"

  val inGenomeTag = "IN_GENOME_"
  val novelTag = "NOVEL"

  val positionForward = "F"
  val positionReverse = "R"

  val contigSeperator = ":"
  val strandSeperator = "\\^"

  val offTargetSeperator = ","
  val withinOffTargetSeperator = "_"
  val positionListTerminatorFront = "<"
  val positionListTerminatorBack = ">"
  val positionListSeperator = "\\|"


  /**
    * create a bed file representation of a crispr guide and it's off-target sites
    *
    * @param guide                      the guide
    * @param bitEncoder                 the appropriate bit encoder
    * @param bitPosition                the appropriate position encoder
    * @param includePositionInformation should we include positional information
    * @return a string representing this guide
    */
  def guideToBedString(guide: CRISPRSiteOT,
                       bitEncoder: BitEncoding,
                       bitPosition: BitPosition,
                       indicateIfTargetHasPefectMatch: Boolean,
                       includePositionInformation: Boolean,
                       activeAnnotations: Array[String]): String = {


    val targetString = new StringBuilder()
    targetString.append(guide.target.contig + GuideEncodingTools.sep)
    targetString.append(guide.target.position + GuideEncodingTools.sep)
    targetString.append((guide.target.position + guide.target.bases.size) + GuideEncodingTools.sep)
    targetString.append(guide.target.bases + GuideEncodingTools.sep)
    targetString.append((if (guide.full) GuideEncodingTools.overflow else GuideEncodingTools.targetOK) + GuideEncodingTools.sep)
    targetString.append((if (guide.target.forwardStrand) GuideEncodingTools.forward else GuideEncodingTools.reverse) + GuideEncodingTools.sep)

    if (guide.namedAnnotations.size > 0)
      targetString.append(activeAnnotations.map{annotation => {
        guide.namedAnnotations.getOrElse(annotation,Array[String]("NA")).mkString(",")
      }}.mkString("\t") + GuideEncodingTools.sep)

    val offTargets = guide.offTargets.toArray

    targetString.append(offTargets.size + GuideEncodingTools.sep)

    // now output the off target information --
    if (includePositionInformation) {
      val offTargetsString = guide.offTargets.toArray.map { ot => {
        val otDecoded = bitEncoder.bitDecodeString(ot.sequence)
        val positions = ot.coordinates.map { otPos => {
          val decoded = bitPosition.decode(otPos)
          decoded.contig + GuideEncodingTools.contigSeperator + decoded.start + GuideEncodingTools.strandSeperator +
            (if (decoded.forwardStrand) GuideEncodingTools.positionForward else GuideEncodingTools.positionReverse)
        }
        } // .mkString(positionListSeperator)

        if (positions.size == 0) {
          otDecoded.str + withinOffTargetSeperator +
            otDecoded.count + withinOffTargetSeperator +
            bitEncoder.mismatches(guide.longEncoding, ot.sequence)
        } else {
          otDecoded.str + withinOffTargetSeperator +
            otDecoded.count + withinOffTargetSeperator +
            bitEncoder.mismatches(guide.longEncoding, ot.sequence) + positionListTerminatorFront +
            positions.mkString(positionListSeperator) + positionListTerminatorBack
        }
      }
      }.mkString(offTargetSeperator)

      targetString.append(offTargetsString)
    }
    // don't output positional information
    else {
      val offTargetsString = guide.offTargets.toArray.map { ot => {
        val otDecoded = bitEncoder.bitDecodeString(ot.sequence)
        otDecoded.str + "_" + otDecoded.count + "_" + bitEncoder.mismatches(guide.longEncoding, ot.sequence)
      }
      }.mkString(",")

      targetString.append(offTargetsString)
    }
    targetString.result()
  }


  /**
    * validate and convert a line in a BED file to a CRISPRSiteOT
    *
    * @param line        the text line
    * @param bitEnc      the bit encoder
    * @param bitPosition the position encoder
    * @return the CRISPRSiteOT represented by this line
    */
  def bedLineToCRISPRSiteOT(line: String, bitEnc: BitEncoding, bitPosition: BitPosition, overflowValue: Int): CRISPRSiteOT = {
    val sp = line.split("\t")
    assert(sp.size >= 7 || sp.size >= 8, "CRISPRSiteOT bed files must have either 7 or 8 columns, not " + sp.size)
    assert(sp(5) == forward || sp(5) == reverse, "CRISPRSiteOT bed files must have FWD or REV in the 5th column, not " + sp(5))
    assert(sp(6).toInt >= 0, "CRISPRSiteOT bed files must have a positive or zero number of off-targets, not " + sp(6).toInt)

    val hasOffTargets = sp.size == 8

    val site = CRISPRSite(sp(0), sp(3), sp(5) == forward, sp(1).toInt, None) // TODO: FIX THE LAST PARAMETER
    val ot = new CRISPRSiteOT(site, bitEnc.bitEncodeString(sp(3)), overflowValue)

    if (hasOffTargets) {
      sp(7).split(offTargetSeperator).foreach { token => {


        val offTargetSeq = token.split(withinOffTargetSeperator)(0)
        val offTargetCount = token.split(withinOffTargetSeperator)(1).toInt
        val offTargetMismatches = token.split(withinOffTargetSeperator)(2).toInt

        // we're encoding positional information
        if (token contains positionListTerminatorFront) {
          val targetAndPositions = token.split(positionListTerminatorFront)

          val positions = targetAndPositions(1).stripSuffix(positionListTerminatorBack).split(positionListSeperator).map { positionEncoded => {
            bitPosition.encode(positionEncoded.split(contigSeperator)(0),
              positionEncoded.split(contigSeperator)(1).split(strandSeperator)(0).toInt,
              offTargetSeq.size,
              positionEncoded.split(strandSeperator)(1) == "F")
          }
          }

          assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
          val otHit = new CRISPRHit(bitEnc.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), positions)
          ot.addOT(otHit)
        } else {
          assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
          val otHit = new CRISPRHit(bitEnc.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), Array[Long]())
          ot.addOT(otHit)
        }
      }
      }
    }
    ot
  }
}
