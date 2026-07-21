/*
 * Copyright (C) 2017 Aaron McKenna
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package scoring

import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
import standards.ParameterPack
import utils.Utils

import scala.io.Source

/**
  * Base class for the 23-position Cas12a CFD matrices bundled with FlashFry.
  *
  * Exact spacer matches are excluded from off-target scoring. For every mismatch, `scorePair` looks up the guide-RNA
  * base (`rX`), complementary off-target DNA base (`dY`), and one-based PAM-proximal position. Multiple mismatch
  * activities are multiplied. Guide specificity is `1 / (1 + sum(activity * occurrenceCount))`.
  *
  * @param resourceName classpath CSV with columns `RDA,Pos,MM,avg_percent_active`
  * @param compatibleProfileId exact profile on which the matrix was calibrated
  * @param metricName stable output metric name
  */
abstract class Cas12aOffTargetScore(resourceName: String,
                                    compatibleProfileId: String,
                                    metricName: String)
  extends SingleGuideScoreModel with RankedScore {

  private var bitEncoding: Option[BitEncoding] = None
  private var scoreMapping: Map[Cas12aMismatch, Double] = Map.empty

  override val applicability: ScoreApplicability = ScoreApplicability(profileIds = Set(compatibleProfileId))

  override def scoreName(): String = metricName

  override def scoreDescription(): String =
    "Position- and nucleotide-specific Cas12a off-target activity score from " + resourceName

  override def headerColumns(): Array[String] =
    Array(metricName + "_maxOT", metricName + "_specificity")

  override def highScoreIsGood: Boolean = false

  override def validOverTargetSequence(pack: ParameterPack, guide: CRISPRSiteOT): Boolean =
    applicability.accepts(pack) && guide.target.bases.length == pack.totalScanLength

  override def bitEncoder(encoder: BitEncoding): Unit = bitEncoding = Some(encoder)

  override def setup(): Unit = {
    val stream = Option(getClass.getResourceAsStream("/" + resourceName)).getOrElse(
      throw new IllegalStateException("Unable to load Cas12a scoring resource " + resourceName))
    val source = Source.fromInputStream(stream)
    try {
      val rows = source.getLines().drop(1).filter(_.trim.nonEmpty).map { line =>
        val fields = line.split(",")
        require(fields.length == 4, "Invalid Cas12a scoring row: " + line)
        Cas12aMismatch(fields(1).toInt, fields(2)) -> fields(3).toDouble
      }.toMap

      require(rows.size == 23 * 12,
        "Cas12a score matrix must define 12 mismatches at each of 23 positions; saw " + rows.size + " rows")
      scoreMapping = rows
    } finally {
      source.close()
    }
  }

  override def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    val encoder = bitEncoding.getOrElse(throw new IllegalStateException("Cas12a score has no bit encoder"))
    require(scoreMapping.nonEmpty, "Cas12a score has not been set up")

    val layout = encoder.mParameterPack.profile.layout
    val targetSpacer = layout.spacer(guide.target.bases)

    val scores = guide.offTargets.flatMap { offTarget =>
      val offTargetSpacer = layout.spacer(encoder.bitDecodeString(offTarget.sequence).str)
      if (targetSpacer == offTargetSpacer) {
        None
      } else {
        val activity = scorePair(targetSpacer, offTargetSpacer)
        offTarget.addScore(metricName, activity.toString)
        Some((activity, offTarget.getOffTargetCount))
      }
    }

    val maximum = if (scores.isEmpty) 0.0 else scores.map(_._1).max
    val specificity = 1.0 / (1.0 + scores.map { case (score, count) => score * count }.sum)
    Array(Array(maximum.toString), Array(specificity.toString))
  }

  /** Return the multiplicative CFD activity for two 23-base, PAM-proximal-first spacers. */
  def scorePair(targetSpacer: String, offTargetSpacer: String): Double = {
    require(targetSpacer.length == 23 && offTargetSpacer.length == 23,
      "Cas12a mismatch matrices require two 23-base spacers")

    targetSpacer.zip(offTargetSpacer).zipWithIndex.foldLeft(1.0) {
      case (score, ((targetBase, offTargetBase), index)) if targetBase != offTargetBase =>
        val rnaBase = targetBase match {
          case 'T' => 'U'
          case base => base
        }
        val dnaBase = Utils.compBase(offTargetBase)
        val mismatch = Cas12aMismatch(index + 1, "r" + rnaBase + ":d" + dnaBase)
        score * scoreMapping.getOrElse(mismatch,
          throw new IllegalStateException("Cas12a score matrix is missing " + mismatch))
      case (score, _) => score
    }
  }
}

/** One matrix key: one-based PAM-proximal position and `rX:dY` mismatch transition. */
case class Cas12aMismatch(position: Int, transition: String)

class EnCas12aOffTargetScore
  extends Cas12aOffTargetScore("off_targ_enCas12a.csv", "encas12a-tttn-23", "EnCas12aOffTarget")

class Cas12aTwoNLSOffTargetScore
  extends Cas12aOffTargetScore("off_targ_2xNLS_Cas12a.csv", "cas12a-2xnls-tttn-23", "Cas12a2xNLSOffTarget")
