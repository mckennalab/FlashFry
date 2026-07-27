/*
 * Copyright (C) 2017 Aaron McKenna
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package standards

import java.io.{File, FileInputStream}
import java.util.Properties

import scala.collection.JavaConverters._

/**
  * Load a user-defined nuclease profile from a Java properties file.
  *
  * Required keys are `id`, `nuclease`, `family`, `spacerLength`, `pamSide`, and `pams`. The optional `aliases` value
  * is a comma-separated list. PAM alternatives are also comma-separated and must have one fixed width.
  */
object NucleaseProfileFile {
  private val requiredKeys = Set("id", "nuclease", "family", "spacerLength", "pamSide", "pams")
  private val optionalKeys = Set("aliases")
  private val allowedKeys = requiredKeys ++ optionalKeys

  /** Parse and fully validate one profile file. */
  def load(file: File): NucleaseProfile = {
    require(file != null, "Profile file was not specified")
    require(file.isFile, "Profile file does not exist or is not a regular file: " + file.getAbsolutePath)
    require(file.canRead, "Profile file is not readable: " + file.getAbsolutePath)

    val properties = new Properties()
    val input = new FileInputStream(file)
    try {
      properties.load(input)
    } finally {
      input.close()
    }

    try {
      buildProfile(properties)
    } catch {
      case error: IllegalArgumentException =>
        val detail = Option(error.getMessage).getOrElse(error.getClass.getSimpleName)
          .stripPrefix("requirement failed: ")
        throw new IllegalArgumentException("Invalid profile file " + file.getAbsolutePath + ": " + detail, error)
    }
  }

  private def buildProfile(properties: Properties): NucleaseProfile = {
    val keys = properties.stringPropertyNames().asScala.toSet
    val unknown = keys -- allowedKeys
    require(unknown.isEmpty, "Unknown properties: " + unknown.toVector.sorted.mkString(", "))

    val missing = requiredKeys.filter(key => value(properties, key).isEmpty)
    require(missing.isEmpty, "Missing required properties: " + missing.toVector.sorted.mkString(", "))

    val id = required(properties, "id")
    val aliases = commaSeparated(value(properties, "aliases").getOrElse(""), "aliases")
    val names = aliases.toSet + id
    val collisions = names.filter(name => NucleaseRegistry.fromNameOption(name).isDefined)
    require(collisions.isEmpty,
      "Custom profile names conflict with built-in profiles: " + collisions.toVector.sorted.mkString(", ") +
        "; use --enzyme for built-in profiles")

    val nucleaseName = required(properties, "nuclease")
    val familyName = required(properties, "family")
    val family = EnzymeType.fromId(familyName)
    val nuclease = Enzyme.fromEncoding(nucleaseName, family)

    val spacerLengthText = required(properties, "spacerLength")
    val spacerLength = try {
      spacerLengthText.toInt
    } catch {
      case _: NumberFormatException =>
        throw new IllegalArgumentException("spacerLength must be an integer, saw: " + spacerLengthText)
    }

    val pamSide = PamSide.fromId(required(properties, "pamSide"))
    val pamPatterns = commaSeparated(required(properties, "pams"), "pams")
    require(pamPatterns.nonEmpty, "pams must contain at least one IUPAC pattern")
    val normalizedPams = pamPatterns.map(Iupac.normalize)
    require(normalizedPams.distinct.size == normalizedPams.size, "pams contains duplicate patterns")

    NucleaseProfile(
      id = id,
      aliases = aliases.toSet,
      nuclease = nuclease,
      spacerLength = spacerLength,
      pamSpec = PamSpec(pamSide, normalizedPams),
      legacyHeaderId = None)
  }

  private def value(properties: Properties, key: String): Option[String] =
    Option(properties.getProperty(key)).map(_.trim).filter(_.nonEmpty)

  private def required(properties: Properties, key: String): String =
    value(properties, key).getOrElse(throw new IllegalArgumentException("Missing required property: " + key))

  private def commaSeparated(raw: String, key: String): Vector[String] = {
    if (raw.trim.isEmpty) {
      Vector.empty
    } else {
      val values = raw.split(",", -1).map(_.trim).toVector
      require(values.forall(_.nonEmpty), key + " contains an empty comma-separated value")
      values
    }
  }
}
