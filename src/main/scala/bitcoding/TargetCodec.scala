/*
 * Copyright (C) 2017 Aaron McKenna
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package bitcoding

import standards.ParameterPack

/**
  * Decoded view of one target record in a database block.
  *
  * @param target two-bit sequence encoding; count bits may also be present for the legacy codec
  * @param count number of genomic occurrences represented by this record
  * @param positions encoded genomic positions, with exactly `count` entries
  * @param nextOffset first array index after this record
  */
case class EncodedTargetRecord(target: Long, count: Int, positions: Array[Long], nextOffset: Int)

/**
  * Storage strategy for target records inside database blocks.
  *
  * Sequence comparison always uses one `Long` with two bits per base. Codecs differ in how many of those bits are
  * available to sequence and where the occurrence count is stored. `encodeRecord` and `decodeRecord` are the canonical
  * APIs for persisted blocks; `addCount` and `countFromSequence` exist for compatibility with the original packed API.
  */
sealed trait TargetCodec {
  /** Stable identifier written to version-2 headers. */
  def id: String

  /** Maximum number of two-bit bases available in the sequence `Long`. */
  def maximumBases: Int

  /** True only when the occurrence count is embedded in the sequence `Long`. */
  def countStoredInSequence: Boolean

  /** Attach a compatibility count to a sequence value. Wide encodings validate the count but store it in records. */
  def addCount(sequence: Long, count: Int): Long

  /** Read an embedded compatibility count. Wide sequence values have no embedded count and therefore return one. */
  def countFromSequence(sequence: Long): Int

  /** Serialize one sequence and all of its genomic positions. */
  def encodeRecord(sequence: Long, positions: Array[Long]): Array[Long]

  /** Decode one record beginning at `offset`. */
  def decodeRecord(block: Array[Long], offset: Int): EncodedTargetRecord
}

object TargetCodec {
  val legacyId = "packed24-v1"
  val wideId = "wide32-v2"

  def forParameterPack(pack: ParameterPack): TargetCodec = {
    if (pack.totalScanLength <= LegacyPackedTargetCodec.maximumBases) LegacyPackedTargetCodec
    else WideTargetCodec
  }

  def fromId(id: String): TargetCodec = id match {
    case `legacyId` => LegacyPackedTargetCodec
    case `wideId` => WideTargetCodec
    case _ => throw new IllegalArgumentException("Unknown target codec: " + id)
  }

  private[bitcoding] def checkedCount(count: Int): Int = {
    require(count > 0, "Target occurrence count must be positive, saw " + count)
    count
  }

  private[bitcoding] def checkedPositions(block: Array[Long], offset: Int, count: Int): Array[Long] = {
    require(offset >= 0 && offset < block.length, "Target record offset is outside the block: " + offset)
    require(count > 0, "Target record occurrence count must be positive, saw " + count)
    require(offset + count <= block.length,
      "Target record declares " + count + " positions at offset " + offset + " in block of size " + block.length)
    block.slice(offset, offset + count)
  }
}

/**
  * Original FlashFry record representation, retained for targets up to 24 bases.
  *
  * Record layout: `[sequence bits 0..47 + count bits 48..63][position 1]...[position count]`.
  * Counts are limited to `Short.MaxValue` to preserve the historical signed-short behavior.
  */
case object LegacyPackedTargetCodec extends TargetCodec {
  override val id: String = TargetCodec.legacyId
  override val maximumBases: Int = 24
  override val countStoredInSequence: Boolean = true

  override def addCount(sequence: Long, count: Int): Long = {
    TargetCodec.checkedCount(count)
    require(count <= Short.MaxValue, "Legacy target count exceeds " + Short.MaxValue + ": " + count)
    (sequence & BitEncoding.legacyStringMask) | (count.toLong << 48)
  }

  override def countFromSequence(sequence: Long): Int = (sequence >> 48).toShort.toInt

  override def encodeRecord(sequence: Long, positions: Array[Long]): Array[Long] = {
    Array(addCount(sequence, positions.length)) ++ positions
  }

  override def decodeRecord(block: Array[Long], offset: Int): EncodedTargetRecord = {
    require(offset >= 0 && offset < block.length, "Target record offset is outside the block: " + offset)
    val target = block(offset)
    val count = countFromSequence(target)
    val positions = TargetCodec.checkedPositions(block, offset + 1, count)
    EncodedTargetRecord(target, count, positions, offset + 1 + count)
  }
}

/**
  * Wide record representation for targets of 25 through 32 bases.
  *
  * Record layout: `[64-bit sequence][count][position 1]...[position count]`. Keeping the count outside the sequence
  * leaves all 64 bits available for 32 two-bit bases.
  */
case object WideTargetCodec extends TargetCodec {
  override val id: String = TargetCodec.wideId
  override val maximumBases: Int = 32
  override val countStoredInSequence: Boolean = false

  override def addCount(sequence: Long, count: Int): Long = {
    TargetCodec.checkedCount(count)
    sequence
  }

  override def countFromSequence(sequence: Long): Int = 1

  override def encodeRecord(sequence: Long, positions: Array[Long]): Array[Long] = {
    TargetCodec.checkedCount(positions.length)
    Array(sequence, positions.length.toLong) ++ positions
  }

  override def decodeRecord(block: Array[Long], offset: Int): EncodedTargetRecord = {
    require(offset >= 0 && offset + 1 < block.length, "Wide target record header is outside the block: " + offset)
    val target = block(offset)
    val rawCount = block(offset + 1)
    require(rawCount <= Int.MaxValue, "Wide target count exceeds Int.MaxValue: " + rawCount)
    val count = TargetCodec.checkedCount(rawCount.toInt)
    val positions = TargetCodec.checkedPositions(block, offset + 2, count)
    EncodedTargetRecord(target, count, positions, offset + 2 + count)
  }
}
