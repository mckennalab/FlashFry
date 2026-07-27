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
  * Location of the PAM in FlashFry's normalized target sequence.
  *
  * The `id` is part of the version-2 database header format and must remain stable.
  */
sealed trait PamSide {
  def id: String
}

case object FivePrimePam extends PamSide {
  override val id: String = "5prime"
}

case object ThreePrimePam extends PamSide {
  override val id: String = "3prime"
}

object PamSide {
  /** Parse the stable value stored in a version-2 database header. */
  def fromId(id: String): PamSide = id.toLowerCase match {
    case FivePrimePam.id => FivePrimePam
    case ThreePrimePam.id => ThreePrimePam
    case _ => throw new IllegalArgumentException("Unknown PAM side: " + id)
  }
}

/**
  * Fixed-width IUPAC PAM patterns accepted by one targeting profile.
  *
  * Every alternative must have the same length because a FlashFry database has one fixed target layout. Patterns are
  * normalized to uppercase when the instance is constructed.
  *
  * @param side location of the PAM in a normalized target
  * @param patterns one or more equal-length IUPAC patterns, such as `NGG` or `TTTN`
  */
final case class PamSpec(side: PamSide, patterns: Vector[String]) {
  require(patterns.nonEmpty, "At least one PAM pattern is required")

  val normalizedPatterns: Vector[String] = patterns.map(Iupac.normalize)
  val length: Int = normalizedPatterns.head.length

  require(length > 0, "PAM patterns cannot be empty")
  require(normalizedPatterns.forall(_.length == length), "All PAM patterns in a profile must have the same length")

  /** Return true when `sequence` has the PAM width and matches at least one configured IUPAC pattern. */
  def matches(sequence: String): Boolean = normalizedPatterns.exists(Iupac.matches(_, sequence))

  /**
    * Reproduce the historical `ParameterPack.pam` values.
    *
    * The legacy API omitted an outer `N` from PAMs such as `NGG` and `TTTN`. New code should use
    * `normalizedPatterns` instead.
    */
  def legacyUnpaddedPatterns: Array[String] = normalizedPatterns.map { pattern =>
    side match {
      case FivePrimePam => if (pattern.endsWith("N")) pattern.dropRight(1) else pattern
      case ThreePrimePam => if (pattern.startsWith("N")) pattern.drop(1) else pattern
    }
  }.toArray
}

/**
  * Coordinates and masks for FlashFry's normalized target representation.
  *
  * A target is always stored in guide orientation, including reverse-strand genomic hits. Its shape is therefore one
  * of the following for every consumer:
  *
  *  - 5-prime PAM: `[PAM][spacer]`
  *  - 3-prime PAM: `[spacer][PAM]`
  *
  * Ranges use Scala's usual half-open `[start, end)` convention.
  *
  * @param spacerLength number of guide-complementary bases
  * @param pam accepted PAM definition
  */
final case class TargetLayout(spacerLength: Int, pam: PamSpec) {
  require(spacerLength > 0, "Spacer length must be positive")

  val totalLength: Int = spacerLength + pam.length
  require(totalLength <= 32, "The current target codecs support at most 32 total bases, saw " + totalLength)

  val spacerRange: (Int, Int) = pam.side match {
    case FivePrimePam => (pam.length, totalLength)
    case ThreePrimePam => (0, spacerLength)
  }

  val pamRange: (Int, Int) = pam.side match {
    case FivePrimePam => (0, pam.length)
    case ThreePrimePam => (spacerLength, totalLength)
  }

  /** Extract the guide spacer, rejecting sequences that do not have this layout's exact total length. */
  def spacer(sequence: String): String = checkedSlice(sequence, spacerRange, "spacer")

  /** Extract the PAM, rejecting sequences that do not have this layout's exact total length. */
  def pamSequence(sequence: String): String = checkedSlice(sequence, pamRange, "PAM")

  /**
    * Extract a bin key from the spacer rather than from the full target.
    *
    * @param sequence normalized full target
    * @param width number of spacer bases in the key
    * @param spacerOffset zero-based offset within the spacer
    */
  def binSequence(sequence: String, width: Int, spacerOffset: Int = 0): String = {
    require(width > 0, "Bin width must be positive")
    require(spacerOffset >= 0, "Bin offset cannot be negative")
    require(spacerOffset + width <= spacerLength,
      "Bin offset " + spacerOffset + " and width " + width + " exceed spacer length " + spacerLength)
    spacer(sequence).slice(spacerOffset, spacerOffset + width)
  }

  /** Two bits set for each spacer base and cleared for each PAM base in the right-aligned target encoding. */
  val comparisonMask: Long = {
    (spacerRange._1 until spacerRange._2).foldLeft(0L) { case (mask, index) =>
      val shift = 2 * (totalLength - index - 1)
      mask | (3L << shift)
    }
  }

  /** Mask covering every base in the normalized target, including the PAM. */
  val fullSequenceMask: Long = {
    val bitCount = totalLength * 2
    if (bitCount == 64) -1L else (1L << bitCount) - 1L
  }

  private def checkedSlice(sequence: String, range: (Int, Int), label: String): String = {
    require(sequence.length == totalLength,
      "Cannot extract " + label + " from sequence of length " + sequence.length + "; expected " + totalLength)
    sequence.slice(range._1, range._2)
  }
}

/**
  * Complete immutable definition of one nuclease targeting mode.
  *
  * A profile—not an enzyme name alone—is FlashFry's unit of compatibility. Two profiles for the same nuclease may
  * differ in spacer length or accepted PAMs and can therefore require different databases or scoring models.
  *
  * @param id stable, machine-readable profile identifier stored in version-2 database headers
  * @param aliases case- and punctuation-insensitive CLI names
  * @param nuclease biochemical nuclease identity and family
  * @param spacerLength number of guide-complementary bases
  * @param pamSpec PAM side and accepted IUPAC patterns
  * @param legacyHeaderId numeric version-1 header ID, reserved for the six historical profiles
  */
final case class NucleaseProfile(id: String,
                                 aliases: Set[String],
                                 nuclease: Enzyme,
                                 spacerLength: Int,
                                 pamSpec: PamSpec,
                                 legacyHeaderId: Option[Int] = None) extends ParameterPack {
  require(id.nonEmpty, "Profile ID cannot be empty")
  require(id.forall(ch => ch.isLetterOrDigit || ch == '-' || ch == '_'),
    "Profile ID contains unsupported characters: " + id)

  override val profile: NucleaseProfile = this
  val layout: TargetLayout = TargetLayout(spacerLength, pamSpec)

  lazy val fwdTargetRegex: Regex = TargetPatternCompiler.forward(this)
  lazy val revTargetRegex: Regex = TargetPatternCompiler.reverse(this)
}

/** Compile profile rules into zero-width look-ahead regexes so adjacent and overlapping targets are retained. */
object TargetPatternCompiler {
  private val spacerBase = "[ACGT]"

  def forward(profile: NucleaseProfile): Regex = compile(profile, reverse = false)

  def reverse(profile: NucleaseProfile): Regex = compile(profile, reverse = true)

  private def compile(profile: NucleaseProfile, reverse: Boolean): Regex = {
    val pamPatterns = if (reverse)
      profile.pamSpec.normalizedPatterns.map(Iupac.reverseComplement)
    else
      profile.pamSpec.normalizedPatterns

    val alternatives = pamPatterns.map { pam =>
      val pamRegex = Iupac.toRegex(pam)
      val spacerRegex = spacerBase + "{" + profile.spacerLength + "}"

      (profile.pamSpec.side, reverse) match {
        case (FivePrimePam, false) => pamRegex + spacerRegex
        case (ThreePrimePam, false) => spacerRegex + pamRegex
        case (FivePrimePam, true) => spacerRegex + pamRegex
        case (ThreePrimePam, true) => pamRegex + spacerRegex
      }
    }

    ("(?i)(?=(?:" + alternatives.mkString("|") + "))").r
  }
}

/** IUPAC DNA utilities shared by profile validation, scanning, random generation, and header round-tripping. */
object Iupac {
  private val bases = Map[Char, String](
    'A' -> "A", 'C' -> "C", 'G' -> "G", 'T' -> "T",
    'R' -> "AG", 'Y' -> "CT", 'K' -> "GT", 'M' -> "AC",
    'S' -> "CG", 'W' -> "AT", 'B' -> "CGT", 'D' -> "AGT",
    'H' -> "ACT", 'V' -> "ACG", 'N' -> "ACGT"
  )

  private val complements = Map[Char, Char](
    'A' -> 'T', 'C' -> 'G', 'G' -> 'C', 'T' -> 'A',
    'R' -> 'Y', 'Y' -> 'R', 'K' -> 'M', 'M' -> 'K',
    'S' -> 'S', 'W' -> 'W', 'B' -> 'V', 'V' -> 'B',
    'D' -> 'H', 'H' -> 'D', 'N' -> 'N'
  )

  def normalize(pattern: String): String = {
    val normalized = pattern.toUpperCase
    require(normalized.nonEmpty, "IUPAC pattern cannot be empty")
    normalized.foreach(ch => require(bases.contains(ch), "Unsupported IUPAC base: " + ch))
    normalized
  }

  def toRegex(pattern: String): String = normalize(pattern).map { ch =>
    val allowed = bases(ch)
    if (allowed.length == 1) allowed else "[" + allowed + "]"
  }.mkString("")

  def reverseComplement(pattern: String): String = normalize(pattern).reverse.map(complements).mkString("")

  def matches(pattern: String, sequence: String): Boolean = {
    val normalized = normalize(pattern)
    val candidate = sequence.toUpperCase
    normalized.length == candidate.length && normalized.zip(candidate).forall { case (iupac, base) =>
      bases(iupac).contains(base)
    }
  }
}

/**
  * Built-in profile definitions.
  *
  * To expose another profile through the CLI, define it here and add it to `NucleaseRegistry.builtIns`. Do not assign
  * a `legacyHeaderId`; those IDs describe the immutable version-1 file format.
  */
object NucleaseProfiles {
  val cpf1: NucleaseProfile = NucleaseProfile(
    "cpf1-tttn-20", Set("cpf1", "cas12a"), CPF1, 20, PamSpec(FivePrimePam, Vector("TTTN")), Some(1))

  val spCas9: NucleaseProfile = NucleaseProfile(
    "spcas9-ngg-nag-20", Set("spcas9"), SpCAS9, 20, PamSpec(ThreePrimePam, Vector("NGG", "NAG")), Some(2))

  val spCas9NGG: NucleaseProfile = NucleaseProfile(
    "spcas9-ngg-20", Set("spcas9ngg"), SpCAS9, 20, PamSpec(ThreePrimePam, Vector("NGG")), Some(3))

  val spCas9NAG: NucleaseProfile = NucleaseProfile(
    "spcas9-nag-20", Set("spcas9nag"), SpCAS9, 20, PamSpec(ThreePrimePam, Vector("NAG")), Some(4))

  val spCas919: NucleaseProfile = NucleaseProfile(
    "spcas9-ngg-nag-19", Set("spcas919"), SpCAS9, 19, PamSpec(ThreePrimePam, Vector("NGG", "NAG")), Some(5))

  val spCas9NGG19: NucleaseProfile = NucleaseProfile(
    "spcas9-ngg-19", Set("spcas9ngg19"), SpCAS9, 19, PamSpec(ThreePrimePam, Vector("NGG")), Some(6))

  val spG: NucleaseProfile = NucleaseProfile(
    "spg-ngn-20", Set("spg"), SpG, 20, PamSpec(ThreePrimePam, Vector("NGN")), None)

  val saCas9NNGRRT21: NucleaseProfile = NucleaseProfile(
    "sacas9-nngrrt-21", Set("sacas9", "sacas921", "sacas9-21"), SaCAS9, 21,
    PamSpec(ThreePrimePam, Vector("NNGRRT")), None)

  val saCas9NNGRRT22: NucleaseProfile = NucleaseProfile(
    "sacas9-nngrrt-22", Set("sacas922", "sacas9-22"), SaCAS9, 22,
    PamSpec(ThreePrimePam, Vector("NNGRRT")), None)

  val asCas12a: NucleaseProfile = NucleaseProfile(
    "ascas12a-tttv-23", Set("ascas12a", "ascpf1"), AsCas12a, 23,
    PamSpec(FivePrimePam, Vector("TTTV")), None)

  val lbCas12a: NucleaseProfile = NucleaseProfile(
    "lbcas12a-tttv-23", Set("lbcas12a", "lbcpf1"), LbCas12a, 23,
    PamSpec(FivePrimePam, Vector("TTTV")), None)

  // The bundled enCas12a mismatch matrix contains positions 1-23, so this profile stores a 23-base spacer.
  val enCas12a: NucleaseProfile = NucleaseProfile(
    "encas12a-tttn-23", Set("encas12a"), EnCas12a, 23, PamSpec(FivePrimePam, Vector("TTTN")), None)

  val cas12a2xNLS: NucleaseProfile = NucleaseProfile(
    "cas12a-2xnls-tttn-23", Set("2xnlscas12a", "cas12a2xnls"), Cas12a2xNLS, 23,
    PamSpec(FivePrimePam, Vector("TTTN")), None)
}

/** Registry used by CLI name resolution and database-header compatibility checks. */
object NucleaseRegistry {
  /** Profiles available by ID or alias at the command line. */
  val builtIns: Vector[ParameterPack] = Vector(
    Cpf1ParameterPack,
    Cas9ParameterPack,
    Cas9NGGParameterPack,
    Cas9NAGParameterPack,
    Cas9ParameterPack19bp,
    Cas9NGG19ParameterPack,
    NucleaseProfiles.spG,
    NucleaseProfiles.saCas9NNGRRT21,
    NucleaseProfiles.saCas9NNGRRT22,
    NucleaseProfiles.asCas12a,
    NucleaseProfiles.lbCas12a,
    NucleaseProfiles.enCas12a,
    NucleaseProfiles.cas12a2xNLS
  )

  private def normalizeName(value: String): String = value.toLowerCase.replaceAll("[^a-z0-9]", "")

  private val byName: Map[String, ParameterPack] = builtIns.flatMap { pack =>
    val profile = pack.profile
    (profile.aliases + profile.id).map(alias => normalizeName(alias) -> pack)
  }.toMap

  private val byLegacyId: Map[Int, ParameterPack] = builtIns.flatMap { pack =>
    pack.profile.legacyHeaderId.map(_ -> pack)
  }.toMap

  private val byProfileId: Map[String, ParameterPack] = builtIns.map(pack => pack.profile.id -> pack).toMap

  /** Look up a CLI alias or profile ID without throwing when it is absent. */
  def fromNameOption(name: String): Option[ParameterPack] = byName.get(normalizeName(name))

  /** Resolve a CLI alias or profile ID, ignoring case and punctuation. */
  def fromName(name: String): ParameterPack = fromNameOption(name).getOrElse(
    throw new IllegalArgumentException("Unknown nuclease profile '" + name + "'. Supported profiles: " + supportedNames))

  /** Resolve one of the six numeric profile IDs used by version-1 database headers. */
  def fromLegacyId(id: Int): ParameterPack = byLegacyId.getOrElse(id,
    throw new IllegalArgumentException("Unknown legacy nuclease profile ID: " + id))

  /** Look up a built-in profile by its exact stable ID. */
  def fromProfileId(id: String): Option[ParameterPack] = byProfileId.get(id)

  /**
    * Prefer the registered object for a serialized built-in profile, while rejecting conflicting definitions.
    * Unregistered profiles remain usable when read from a version-2 header.
    */
  def resolveSerialized(profile: NucleaseProfile): ParameterPack = fromProfileId(profile.id) match {
    case Some(registered) =>
      require(NucleaseProfileSerialization.sameTargetingDefinition(registered.profile, profile),
        "Serialized targeting profile '" + profile.id + "' conflicts with the registered definition")
      registered
    case None => profile
  }

  def supportedNames: String = builtIns.map(_.profile.id).mkString(",")
}

/**
  * Stable one-line profile representation used by version-2 database headers.
  *
  * Fields are pipe-delimited and URL-encoded in this order: profile ID, aliases, nuclease ID, family ID, spacer
  * length, PAM side, PAM patterns, and optional legacy ID. The fingerprint intentionally covers targeting identity
  * only; aliases and the legacy ID may change without invalidating an otherwise compatible database.
  */
object NucleaseProfileSerialization {
  private val separator = "\\|"

  def serialize(pack: ParameterPack): String = {
    val profile = pack.profile
    Vector(
      encode(profile.id),
      profile.aliases.toVector.sorted.map(encode).mkString(","),
      encode(profile.nuclease.stringEncoding),
      encode(profile.nuclease.enzymeParent.id),
      profile.spacerLength.toString,
      profile.pamSpec.side.id,
      profile.pamSpec.normalizedPatterns.map(encode).mkString(","),
      profile.legacyHeaderId.map(_.toString).getOrElse("")
    ).mkString("|")
  }

  def deserialize(value: String): ParameterPack = {
    val fields = value.split(separator, -1)
    require(fields.length == 8, "Invalid serialized nuclease profile: expected 8 fields, saw " + fields.length)

    val family = EnzymeType.fromId(decode(fields(3)))
    val enzyme = Enzyme.fromEncoding(decode(fields(2)), family)
    val aliases = if (fields(1).isEmpty) Set.empty[String] else fields(1).split(",").map(decode).toSet
    val pams = fields(6).split(",").map(decode).toVector
    val legacyId = if (fields(7).isEmpty) None else Some(fields(7).toInt)

    NucleaseRegistry.resolveSerialized(NucleaseProfile(
      decode(fields(0)), aliases, enzyme, fields(4).toInt, PamSpec(PamSide.fromId(fields(5)), pams), legacyId))
  }

  def fingerprint(pack: ParameterPack): String = {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    digest.digest(canonicalTargetingDefinition(pack.profile).getBytes("UTF-8")).map(byte => "%02x".format(byte & 0xff)).mkString
  }

  def sameTargetingDefinition(left: NucleaseProfile, right: NucleaseProfile): Boolean = {
    left.id == right.id &&
      left.nuclease.stringEncoding.equalsIgnoreCase(right.nuclease.stringEncoding) &&
      left.nuclease.enzymeParent.id == right.nuclease.enzymeParent.id &&
      left.spacerLength == right.spacerLength &&
      left.pamSpec.side == right.pamSpec.side &&
      left.pamSpec.normalizedPatterns == right.pamSpec.normalizedPatterns
  }

  private def canonicalTargetingDefinition(profile: NucleaseProfile): String = Vector(
    profile.id,
    profile.nuclease.stringEncoding.toLowerCase,
    profile.nuclease.enzymeParent.id,
    profile.spacerLength.toString,
    profile.pamSpec.side.id,
    profile.pamSpec.normalizedPatterns.mkString(",")
  ).mkString("|")

  private def encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
  private def decode(value: String): String = java.net.URLDecoder.decode(value, "UTF-8")
}
