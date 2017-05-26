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

  val contigPos = 0
  val startPos = 1
  val stopPos = 2
  val targetPos = 3
  val contextPos = 4
  val overflowPos = 5
  val orientationPos = 6

  val setColumnCount = 8

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
    targetString.append(guide.target.sequenceContext.getOrElse("NONE") + GuideEncodingTools.sep)
    targetString.append((if (guide.full) GuideEncodingTools.overflow else GuideEncodingTools.targetOK) + GuideEncodingTools.sep)
    targetString.append((if (guide.target.forwardStrand) GuideEncodingTools.forward else GuideEncodingTools.reverse) + GuideEncodingTools.sep)

    if (guide.namedAnnotations.size > 0)
      targetString.append(activeAnnotations.map { annotation => {
        guide.namedAnnotations.getOrElse(annotation, Array[String]("NA")).mkString(",")
      }
      }.mkString("\t") + GuideEncodingTools.sep)

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
    * column positions:
    * - 1 - chromosome
    * - 2 - start
    * - 3 - stop
    * - 4 - guide
    * - 5 - sequence context
    * - 6 - did we overflow
    * - 7 - direction of target
    * - * - start of any annotations (from 0 to * annotations)
    * - 8 + |*| - number of off-targets, after any annotationed scores
    * - 9 + |*| - off-target sequences, and optionally their positions, after any annotated scores
    *
    * @param line        the text line
    * @param bitEnc      the bit encoder
    * @param bitPosition the position encoder
    * @return the CRISPRSiteOT represented by this line
    */
  def bedLineToCRISPRSiteOT(line: String, bitEnc: BitEncoding, bitPosition: BitPosition, overflowValue: Int, scoringAnnotations: Array[String]): CRISPRSiteOT = {
    val sp = line.split("\t")
    assert(sp.size >= setColumnCount + scoringAnnotations.size, "CRISPRSiteOT bed files must have " + setColumnCount + " columns plus the number of annotated scores, not " + sp.size)
    assert(sp(orientationPos) == forward || sp(orientationPos) == reverse, "CRISPRSiteOT bed files must have FWD or REV in the 5th column, not " + sp(orientationPos))
    assert(sp(orientationPos + 1 + scoringAnnotations.size).toInt >= 0, "CRISPRSiteOT bed files must have a positive or zero number of off-targets, not " + sp(orientationPos + 1 + scoringAnnotations.size).toInt)

    val site = CRISPRSite(sp(contigPos), sp(targetPos), sp(orientationPos) == forward, sp(startPos).toInt, if (sp(contextPos) == "NONE") None else Some(sp(contextPos)))
    val ot = new CRISPRSiteOT(site, bitEnc.bitEncodeString(sp(targetPos)),
      if (sp(overflowPos) == "OK")
        (sp((setColumnCount - 1) + scoringAnnotations.size).toInt + 1)
      else
        sp((setColumnCount - 1) + scoringAnnotations.size).toInt - 1)

    (0 until scoringAnnotations.size).foreach { anIndex => ot.namedAnnotations(scoringAnnotations(anIndex)) = Array[String](sp(7 + anIndex)) }

    sp(setColumnCount + scoringAnnotations.size).split(offTargetSeperator).foreach { token => {

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
    ot
  }
}
