/*
 * Copyright (C) 2017 Aaron McKenna
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package standards

import scala.util.matching.Regex

/**
  * Compatibility facade for the original FlashFry parameter-pack API.
  *
  * New code should access `profile` and its `TargetLayout` directly. The remaining accessors preserve source
  * compatibility for existing callers; all values are derived from the profile and must not become independent
  * configuration points.
  */
trait ParameterPack {
  def profile: NucleaseProfile

  final def enzyme: Enzyme = profile.nuclease
  final def pam: Array[String] = profile.pamSpec.legacyUnpaddedPatterns
  final def paddedPam: Array[String] = profile.pamSpec.normalizedPatterns.toArray
  final def pamLength: Int = profile.pamSpec.length
  final def totalScanLength: Int = profile.layout.totalLength
  final def comparisonBitEncoding: Long = profile.layout.comparisonMask
  final def fivePrimePam: Boolean = profile.pamSpec.side == FivePrimePam
  final def guideRange: (Int, Int) = profile.layout.spacerRange
  final def fwdRegex: Regex = profile.fwdTargetRegex
  final def revRegex: Regex = profile.revTargetRegex

  final def spacer(sequence: String): String = profile.layout.spacer(sequence)
  final def pamSequence(sequence: String): String = profile.layout.pamSequence(sequence)
}

object ParameterPack {
  /** Resolve a registered profile ID or legacy CLI alias. */
  def nameToParameterPack(name: String): ParameterPack = NucleaseRegistry.fromName(name)

  /** Resolve a numeric profile ID from a version-1 database header. */
  def indexToParameterPack(index: Int): ParameterPack = NucleaseRegistry.fromLegacyId(index)

  /** Return the version-1 numeric ID, failing for profiles introduced after that format. */
  def parameterPackToIndex(pack: ParameterPack): Int = pack.profile.legacyHeaderId.getOrElse(
    throw new IllegalArgumentException("Profile " + pack.profile.id + " has no version-1 header ID"))

  // Source-compatible constants retained for existing scoring implementations.
  val cas9ScanLength20mer = 23
  val cas9ScanLength19mer = 22
  val cas9PAMLength = 3
  val cpf1ScanLength = 24
  val cpf1PAMLength = 4
}

/** Source-compatible wrapper used by the six historical `ParameterPack` singleton objects. */
sealed abstract class LegacyParameterPack(override val profile: NucleaseProfile) extends ParameterPack

case object Cas9ParameterPack extends LegacyParameterPack(NucleaseProfiles.spCas9)
case object Cas9ParameterPack19bp extends LegacyParameterPack(NucleaseProfiles.spCas919)
case object Cas9NGGParameterPack extends LegacyParameterPack(NucleaseProfiles.spCas9NGG)
case object Cas9NGG19ParameterPack extends LegacyParameterPack(NucleaseProfiles.spCas9NGG19)
case object Cas9NAGParameterPack extends LegacyParameterPack(NucleaseProfiles.spCas9NAG)
case object Cpf1ParameterPack extends LegacyParameterPack(NucleaseProfiles.cpf1)
