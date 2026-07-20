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

package crispr

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Candidate guide together with its discovered off-target sequences and scoring annotations.
  *
  * `currentTotal` counts genomic occurrences, not merely distinct `CRISPRHit` objects. Normal pipeline callers provide
  * a positive overflow threshold.
  *
  * @param tgt discovered candidate target
  * @param encoding two-bit target encoding
  * @param overflow maximum aggregate off-target count
  * @param inputOverflow whether an upstream input had already marked this guide as overflowed
  */
class CRISPRSiteOT(tgt: CRISPRSite, encoding: Long, overflow: Int, inputOverflow: Boolean = false) extends Ordered[CRISPRSiteOT] with LazyLogging {
  var target = tgt
  var offTargets = new ArrayBuffer[CRISPRHit]
  val longEncoding = encoding
  var currentTotal = 0
  val namedAnnotations = new mutable.HashMap[String,Array[String]]
  val inheritedOverflow = inputOverflow

  /** Whether the aggregate occurrence count has reached the configured limit. */
  def full: Boolean = currentTotal >= overflow

  /** Add one distinct sequence and advance the aggregate total by its occurrence count. */
  def addOT(offTarget: CRISPRHit): Unit = {
    assert(currentTotal < overflow || overflow == 0,
      "We should not add off-targets to an overflowed guide: " + encoding + " overflow value: " + overflow + " current size " + currentTotal)
    offTargets += offTarget
    currentTotal += offTarget.getOffTargetCount
  }

  /** Add hits in order until the configured aggregate occurrence limit is reached. */
  def addOTs(offTargetList: Array[CRISPRHit]): Unit = {
    // Check both before iteration and per item because a hit can cross the aggregate occurrence limit.
    if (currentTotal < overflow || overflow == 0) {
      offTargetList.foreach { t => {
        if (currentTotal < overflow || overflow == 0) {
          offTargets += t
          currentTotal += t.getOffTargetCount
        }
      }
      }
    }
  }

  def compare(that: CRISPRSiteOT): Int = (target.start) compare (that.target.start)

  /**
    * Retain only off-target sequences within the requested spacer mismatch distance.
    *
    * @param maxMismatch maximum allowed spacer mismatches
    * @param bitEnc the bit encoder to use
    */
  def filterOffTargets(maxMismatch: Int, bitEnc: BitEncoding): Unit = {
    var toKeep = new ArrayBuffer[CRISPRHit]()
    offTargets.toArray.foreach{ot => if (bitEnc.mismatches(ot.sequence,encoding) <= maxMismatch) toKeep += ot}
    offTargets = toKeep
  }
}
