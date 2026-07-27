/*
 * Copyright (C) 2017 Aaron McKenna
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package modules

import java.io.File

import bitcoding.TargetCodec
import picocli.CommandLine.{Command, Option}
import standards.{FivePrimePam, NucleaseProfile, NucleaseProfileFile, NucleaseProfileSerialization}

/** Parent command for operations on user-defined nuclease profile files. */
@Command(name = "profile", description = Array("Create and validate user-defined nuclease targeting profiles"))
class ProfileCommand extends Runnable {
  override def run(): Unit =
    System.err.println("Specify a profile subcommand. Available subcommands: validate")
}

/** Validate and summarize a user-defined nuclease profile without building a database. */
@Command(name = "validate", description = Array("Validate and summarize a custom nuclease profile file"))
class ValidateNucleaseProfile extends Runnable {
  @Option(names = Array("-profileFile", "--profileFile"), required = true, paramLabel = "FILE",
    description = Array("the custom nuclease profile properties file"))
  private var profileFile: File = null

  override def run(): Unit = {
    val profile = NucleaseProfileFile.load(profileFile)
    println(NucleaseProfileValidation.describe(profile))
  }
}

object NucleaseProfileValidation {
  /** Produce a stable, human-readable summary after all profile invariants have been checked. */
  def describe(profile: NucleaseProfile): String = {
    val layout = if (profile.pamSpec.side == FivePrimePam) "[PAM][spacer]" else "[spacer][PAM]"
    val aliases = if (profile.aliases.isEmpty) "(none)" else profile.aliases.toVector.sorted.mkString(",")

    Vector(
      "status=valid",
      "id=" + profile.id,
      "aliases=" + aliases,
      "nuclease=" + profile.nuclease.stringEncoding,
      "family=" + profile.nuclease.enzymeParent.id,
      "spacerLength=" + profile.spacerLength,
      "pamSide=" + profile.pamSpec.side.id,
      "pams=" + profile.pamSpec.normalizedPatterns.mkString(","),
      "targetLength=" + profile.layout.totalLength,
      "layout=" + layout,
      "targetCodec=" + TargetCodec.forParameterPack(profile).id,
      "fingerprint=" + NucleaseProfileSerialization.fingerprint(profile)
    ).mkString("\n")
  }
}
