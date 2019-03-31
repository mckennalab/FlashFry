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
  * store off-target hits associated with a specific CRISPR target
  */
class CRISPRSiteOT(tgt: CRISPRSite, encoding: Long, overflow: Int, inputOverflow: Boolean = false) extends Ordered[CRISPRSiteOT] with LazyLogging {
  var target = tgt
  var offTargets = new ArrayBuffer[CRISPRHit]
  val longEncoding = encoding
  var currentTotal = 0
  val namedAnnotations = new mutable.HashMap[String,Array[String]]
  val inheritedOverflow = inputOverflow

  def full: Boolean = currentTotal >= overflow

  def addOT(offTarget: CRISPRHit): Unit = {
    assert(currentTotal < overflow || overflow == 0,
      "We should not add off-targets to an overflowed guide: " + encoding + " overflow value: " + overflow + " current size " + currentTotal)
    offTargets += offTarget
    currentTotal += offTarget.getOffTargetCount
  }

  def addOTs(offTargetList: Array[CRISPRHit]): Unit = {
    // so the double ifs are a little weird -- but we'd prefer to both:
    // 1) not even cycle on the buffer they provide if we're already overfull
    // 2) if we do fill up while we're adding from their buffer, again we should stop
    //
    if (currentTotal < overflow || overflow == 0) {
      offTargetList.foreach { t => {
        if (currentTotal < overflow || overflow == 0) {
          offTargets += t
          currentTotal += 1
        }
      }
      }
    }
  }

  def compare(that: CRISPRSiteOT): Int = (target.start) compare (that.target.start)

  /**
    * filter down the off-target list by a maximal off-target distance
     * @param maxMismatch the maximum number of mismatches allowed, otherwise we drop the off-target hit
    * @param bitEnc the bit encoder to use
    */
  def filterOffTargets(maxMismatch: Int, bitEnc: BitEncoding): Unit = {
    var toKeep = new ArrayBuffer[CRISPRHit]()
    offTargets.toArray.foreach{ot => if (bitEnc.mismatches(ot.sequence,encoding) <= maxMismatch) toKeep += ot}
    offTargets = toKeep
  }
}