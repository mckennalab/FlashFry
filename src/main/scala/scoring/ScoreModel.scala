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

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import standards.ParameterPack
import standards.EnzymeType
import com.typesafe.scalalogging.LazyLogging

/** Extension point for guide-level, off-target, and annotation scoring models. */
trait ScoreModel extends LazyLogging {

  /** Biological and layout constraints checked before this model is initialized. */
  def applicability: ScoreApplicability = ScoreApplicability.Any

  /** Stable metric name used in registration and output annotations. */
  def scoreName(): String

  /** Human-readable description written to output metadata. */
  def scoreDescription(): String

  /** Output columns produced by this model, in the same order as values returned during scoring. */
  def headerColumns(): Array[String]

  /**
    * Score or annotate a batch of guides.
    *
    * Batch access supports models that compare guides with each other or traverse an annotation source once.
    * Implementations place results in each guide's `namedAnnotations` map.
    *
    * @param guide guides and their discovered off-targets
    * @param bitEnc sequence encoder matching `pack`
    * @param posEnc genomic position encoder for the active database
    * @param pack active nuclease profile
    */
  def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack)

  /** Return whether this model is calibrated for the supplied profile. */
  def validOverEnzyme(enzyme: ParameterPack): Boolean = applicability.accepts(enzyme)

  /**
    * Return whether one guide has the sequence/context required by this model.
    *
    * This is evaluated after profile-level applicability. For example, an on-target model may additionally require
    * flanking sequence context.
    */
  def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean

  /** Load resources or finalize configuration after the encoder and profile have been accepted. */
  def setup()

  /** Supply the sequence encoder associated with the active database. */
  def bitEncoder(bitEncoding: BitEncoding): Unit

}

/**
  * Declarative constraints for a scoring model.
  *
  * Empty sets are unconstrained. Non-empty sets are combined with logical AND, so a profile must satisfy every
  * configured dimension. Use exact profile IDs for empirically calibrated models unless the evidence supports a whole
  * nuclease family.
  */
final case class ScoreApplicability(profileIds: Set[String] = Set.empty,
                                    nucleaseIds: Set[String] = Set.empty,
                                    families: Set[EnzymeType] = Set.empty,
                                    spacerLengths: Set[Int] = Set.empty) {
  def accepts(pack: ParameterPack): Boolean = {
    val profile = pack.profile
    (profileIds.isEmpty || profileIds.contains(profile.id)) &&
      (nucleaseIds.isEmpty || nucleaseIds.contains(profile.nuclease.stringEncoding.toLowerCase)) &&
      (families.isEmpty || families.contains(profile.nuclease.enzymeParent)) &&
      (spacerLengths.isEmpty || spacerLengths.contains(profile.spacerLength))
  }
}

object ScoreApplicability {
  val Any: ScoreApplicability = ScoreApplicability()

  val SpCas9TwentyBase: ScoreApplicability = ScoreApplicability(profileIds = Set(
    "spcas9-ngg-nag-20", "spcas9-ngg-20", "spcas9-nag-20"))

  val SpCas9NineteenOrTwentyBase: ScoreApplicability = ScoreApplicability(profileIds = Set(
    "spcas9-ngg-nag-20", "spcas9-ngg-20", "spcas9-nag-20",
    "spcas9-ngg-nag-19", "spcas9-ngg-19"))
}

/** Adapter for models that independently score one guide at a time. */
abstract class SingleGuideScoreModel extends ScoreModel with LazyLogging {
  /**
    * Score one guide.
    *
    * @return one array per `headerColumns` entry; an inner array may contain multiple annotations for that column
    */
  def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]]

  /** Apply `scoreGuide` to valid guides and write `NA` for invalid ones. */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    guides.zipWithIndex.foreach { case(hit,index) => {
      if ((index + 1) % 10000 == 0) {
        logger.info("For scoring metric " + this.scoreName() + " we're scoring our " + index + " guide")
      }

      if (this.validOverTargetSequence(pack, hit)) {
        val scores = scoreGuide(hit)
        assert(scores.size == this.headerColumns().size)
        this.headerColumns().zip(scores).foreach{ case(col,scores) =>
          hit.namedAnnotations(col) = scores
        }
      } else {
        this.headerColumns().foreach{ case(col) =>
          hit.namedAnnotations(col) = Array[String](SingleGuideScoreModel.missingAnnotation)
        }
      }
    }
    }
  }
}

object SingleGuideScoreModel {
  val missingAnnotation = "NA"

  /**
    * Return the expected start of a guide inside its symmetrically captured context, or `-1` when no context exists.
    *
    * Discovery stores equal requested flanks on both sides, so the centered offset is deterministic even when the guide
    * sequence itself occurs more than once in the context.
    */
  def findGuideSequenceWithinContext(guide: CRISPRSiteOT): Int = {
    if (!guide.target.sequenceContext.isDefined) return -1

    val guideLen = guide.target.bases.size
    (guide.target.sequenceContext.get.size - guideLen) / 2
  }
}
