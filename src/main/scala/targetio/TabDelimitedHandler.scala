
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

package targetio

import java.io.{File, PrintWriter}
import java.util.regex.Pattern

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import scoring.{ScoreModel, SingleGuideScoreModel}
import utils.Utils

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * handle transforming data to and from our tab-delimited output format
  */
object TabDelimitedOutput {
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

  val contigSeparator = ":"
  val contigSeparatorInput = Pattern.quote(contigSeparator)
  val strandSeparator = "^"
  val strandSeparatorInput = Pattern.quote(strandSeparator)


  // Off-target symbols
  // *******************************************
  val offTargetSeparator = ","
  val withinOffTargetSeparator = "_"
  val positionListTerminatorFront = "<"
  val positionListTerminatorBack = ">"
  val positionListSeparator = "|"
  val offTargetScoresTerminatorFront = "{"
  val offTargetScoresTerminatorBack = "}"
  val offTargetScoresSeparator = "!"
  val offTargetScoresPairing = "="
  val positionListSeparatorInput = Pattern.quote(positionListSeparator)

  val extractPositionBlock = """.*\<(.+?)\>.*""".r
  //Regex.quote(".*") + Regex.quote(positionListTerminatorFront) + "(.+?)" + Regex.quote(positionListTerminatorFront) + Regex.quote(".*") r
  val extractScoreBlock = """.*\{(.+?)\}.*""".r
  //Regex.quote(".*") + Regex.quote(offTargetScoresTerminatorFront) + "(.+?)" + Regex.quote(offTargetScoresTerminatorBack) + Regex.quote(".*") r

  val default_columns = Array[String]("contig", "start", "stop", "target", "context", "overflow", "orientation")
  val final_columns = Array[String]("otCount", "offTargets")


  val contigPos = 0
  val startPos = 1
  val stopPos = 2
  val targetPos = 3
  val contextPos = 4
  val overflowPos = 5
  val orientationPos = 6

  val setColumnCount = 8

}

/**
  * write our results to disk
  *
  * @param outputFile    the file to write to; end it with .gz if you want it compressed
  * @param bitEncoding   our bit encoder for targets
  * @param bitPosition   our position bit encoder
  * @param scoringModels the models we've scored with
  * @param writeOTs      should we write the off-targets?
  * @param writePositons should we write the off-target positions with the off-targets?
  */
class TabDelimitedOutput(outputFile: File,
                         bitEncoding: BitEncoding,
                         bitPosition: BitPosition,
                         scoringModels: Array[ScoreModel],
                         writeOTs: Boolean,
                         writePositons: Boolean) {

  val models = scoringModels

  val output = if (outputFile.getAbsolutePath().endsWith(".gz")) {
    new PrintWriter(Utils.gos(outputFile.getAbsolutePath))
  } else {
    new PrintWriter(outputFile.getAbsolutePath)
  }

  // output the header
  output.write((TabDelimitedOutput.default_columns ++
    scoringModels.flatMap { case (sc) => sc.headerColumns() }).mkString(TabDelimitedOutput.sep))

  if (writeOTs)
    output.write(TabDelimitedOutput.sep + TabDelimitedOutput.final_columns.mkString(TabDelimitedOutput.sep) + "\n")
  else
    output.write(TabDelimitedOutput.sep + TabDelimitedOutput.final_columns.slice(0, 1).mkString(TabDelimitedOutput.sep) + "\n")


  /**
    *
    * @param guide add a guide to the output
    */
  def write(guide: CRISPRSiteOT): Unit = {
    output.write(guide.target.contig + TabDelimitedOutput.sep)
    output.write(guide.target.start + TabDelimitedOutput.sep)
    output.write((guide.target.start + guide.target.length) + TabDelimitedOutput.sep)
    output.write((guide.target.bases) + TabDelimitedOutput.sep)
    output.write((guide.target.sequenceContext.getOrElse("NONE")) + TabDelimitedOutput.sep)
    output.write((if (guide.full | guide.inheritedOverflow) TabDelimitedOutput.overflow else TabDelimitedOutput.targetOK) + TabDelimitedOutput.sep)
    output.write((if (guide.target.forwardStrand) TabDelimitedOutput.forward else TabDelimitedOutput.reverse) + TabDelimitedOutput.sep)

    models.foreach { model =>
      output.write(model.headerColumns().map { col => guide.namedAnnotations.getOrElse(col, Array[String](SingleGuideScoreModel.missingAnnotation)) }.
        map { t => t.mkString(",") }.mkString(TabDelimitedOutput.sep) + TabDelimitedOutput.sep)
    }

    // output the total number of targets we found -- not just the number of sequences
    output.write(guide.offTargets.map { ot => ot.getOffTargetCount }.sum.toString)

    if (writeOTs)
      output.write(TabDelimitedOutput.sep + guide.offTargets.
        map { ot => ot.toOutput(bitEncoding, bitPosition, guide.longEncoding, writePositons) }.mkString(TabDelimitedOutput.offTargetSeparator) + "\n")
    else
      output.write("\n")
  }

  def close(): Unit = {
    output.close()
  }
}


/**
  * turn a file into an array builder of crisprOT objects
  *
  * @param inputFile   the input file
  * @param bitEncoding our bit encoder for targets
  * @param bitPosition our bit encoder for target positions
  */
class TabDelimitedInput(inputFile: File,
                        bitEncoding: BitEncoding,
                        bitPosition: BitPosition,
                        maximumMismatches: Int,
                        requirePositionInfo: Boolean,
                        filterOutOverflowedGuides: Boolean) extends LazyLogging {

  val input = if (inputFile.getAbsolutePath().endsWith(".gz")) {
    Source.fromInputStream(Utils.gis(inputFile.getAbsolutePath)).getLines()
  } else {
    Source.fromFile(inputFile.getAbsolutePath).getLines()
  }

  // read in the header and figure out what the file includes
  val headerLine = input.next().split(TabDelimitedOutput.sep)

  assert(headerLine.size > TabDelimitedOutput.default_columns.size + TabDelimitedOutput.final_columns.size - 1, "Header line not long enough for file: " + inputFile)
  assert(TabDelimitedOutput.default_columns.zip(headerLine.slice(0, TabDelimitedOutput.default_columns.size)).map { case (a, b) => if (a == b) 0 else 1 }.sum == 0,
    "Mismatched line doesn't contain the standard header tokens: " + inputFile)

  // now figure out what the remaining columns are
  val remainingColumns = headerLine.slice(TabDelimitedOutput.default_columns.size, headerLine.size)

  val withOTs = (remainingColumns(remainingColumns.size - 2) == TabDelimitedOutput.final_columns(0) &&
    remainingColumns(remainingColumns.size - 1) == TabDelimitedOutput.final_columns(1))

  val withOTTokenLength = headerLine.size

  assert(withOTs || (remainingColumns(remainingColumns.size - 1) == TabDelimitedOutput.final_columns(0)), "Unable to parse out the final columns in the header")

  val annotations = if (withOTs) remainingColumns.slice(0, remainingColumns.size - 2) else remainingColumns.slice(0, remainingColumns.size - 1)

  // now make an array of all the target sites
  val guides = new ArrayBuffer[CRISPRSiteOT]()

  input.foreach { ln => {
    TabDelimitedInput.extractCRISPRSiteOT(ln,
      annotations,
      bitEncoding,
      bitPosition,
      withOTs,
      withOTTokenLength,
      filterOutOverflowedGuides,
      maximumMismatches).map { gd => guides += gd }
  }
  }
}

object TabDelimitedInput extends LazyLogging {
  /**
    * extract an individual hit from the input file
    *
    * @param ln the string containing the target information
    * @return Some(CRISPRSiteOT) if we can parse the string fragment, NONE if not
    */
  private def extractCRISPRSiteOT(ln: String,
                                  annotations: Array[String],
                                  bitEncoding: BitEncoding,
                                  bitPosition: BitPosition,
                                  withOTs: Boolean,
                                  withOTTokenLength: Int,
                                  filterOutOverflowedGuides: Boolean,
                                  maximumMismatches: Int
                                 ): Option[CRISPRSiteOT] = {
    try {
      val sp = ln.split(TabDelimitedOutput.sep)

      val site = CRISPRSite(sp(TabDelimitedOutput.contigPos),
        sp(TabDelimitedOutput.targetPos),
        sp(TabDelimitedOutput.orientationPos) == TabDelimitedOutput.forward,
        sp(TabDelimitedOutput.startPos).toInt,
        if (sp(TabDelimitedOutput.contextPos) == "NONE") None else Some(sp(TabDelimitedOutput.contextPos)))

      val isOverflowed = sp(TabDelimitedOutput.overflowPos) != "OK"
      val overflowCount = if (!isOverflowed)
        (sp((TabDelimitedOutput.setColumnCount - 1) + annotations.size).toInt + 1)
      else {
        (sp((TabDelimitedOutput.setColumnCount - 1) + annotations.size).toInt)
      }

      val ot = new CRISPRSiteOT(site, bitEncoding.bitEncodeString(sp(TabDelimitedOutput.targetPos)), overflowCount, isOverflowed)

      (0 until annotations.size).foreach(anIndex => ot.namedAnnotations(annotations(anIndex)) = Array[String](sp(7 + anIndex)))

      if (withOTs && sp.size == withOTTokenLength) {
        sp(sp.size - 1).split(Pattern.quote(TabDelimitedOutput.offTargetSeparator)).foreach { token => {
          TabDelimitedInput.addOffTargets(ot, token, maximumMismatches, bitPosition, bitEncoding)
        }
        }
      }
      if ((!filterOutOverflowedGuides) || (!ot.inheritedOverflow && !ot.full))
        Some(ot)
      else
        None
    } catch {
      case e: Exception => {
        logger.error("Unable to parse line: " + ln.slice(0, 100) + "... with error: ")
        e.printStackTrace()
        throw e
      }
    }
  }

  /**
    * add the off-targets we discover to the CRISPRSiteOT object
    *
    * @param ot    a CRISPRSiteOT to append our results to
    * @param token the string containing off-target representations
    */
  private def addOffTargets(ot: CRISPRSiteOT,
                            token: String,
                            maximumMismatches: Int,
                            bitPosition: BitPosition,
                            bitEncoding: BitEncoding
                           ) = {

    val offTargetSeq = token.split(TabDelimitedOutput.withinOffTargetSeparator)(0)
    val offTargetCount = token.split(TabDelimitedOutput.withinOffTargetSeparator)(1).toInt
    val offTargetMismatches = if (token.split(TabDelimitedOutput.withinOffTargetSeparator)(2) contains TabDelimitedOutput.positionListTerminatorFront) {
      token.split(TabDelimitedOutput.withinOffTargetSeparator)(2).split(TabDelimitedOutput.positionListTerminatorFront)(0).toInt
    } else {
      token.split(TabDelimitedOutput.withinOffTargetSeparator)(2).toInt
    }

    val otHit = if (offTargetMismatches <= maximumMismatches) {

      // are we encoding positional information?
      if (token contains TabDelimitedOutput.positionListTerminatorFront) {
        val s = TabDelimitedOutput.extractPositionBlock.findAllIn(token).group(1)
        val positions = s.split(TabDelimitedOutput.positionListSeparatorInput).map { positionEncoded => {
          bitPosition.encode(positionEncoded.split(TabDelimitedOutput.contigSeparatorInput)(0),
            positionEncoded.split(TabDelimitedOutput.contigSeparatorInput)(1).split(TabDelimitedOutput.strandSeparatorInput)(0).toInt,
            offTargetSeq.size,
            positionEncoded.split(TabDelimitedOutput.strandSeparatorInput)(1) == "F")
        }
        }

        assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
        val otHit = new CRISPRHit(bitEncoding.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), positions)
        if (!ot.full)
          ot.addOT(otHit)
        Some(otHit)
      } else {
        assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
        val otHit = new CRISPRHit(bitEncoding.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), new Array[Long](offTargetCount), false)
        if (!ot.full)
          ot.addOT(otHit)
        Some(otHit)
      }
    } else {
      None
    }

    if (token contains TabDelimitedOutput.offTargetScoresTerminatorFront) {
      assert(otHit.isDefined, "We currently require positional information to define off-target scores ")
      val s = TabDelimitedOutput.extractScoreBlock.findAllIn(token).group(1)
      s.split(TabDelimitedOutput.offTargetScoresSeparator).map { scorePair => {
        val pair = scorePair.split(TabDelimitedOutput.offTargetScoresPairing)
        assert(pair.size == 2, "Score pairs should be key=value")
        otHit.get.addScore(pair(0), pair(1))
      }
      }
    }

    Some(otHit)
  }
}
