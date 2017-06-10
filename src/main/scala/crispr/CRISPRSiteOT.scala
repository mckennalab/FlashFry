/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package crispr

import bitcoding.{BitEncoding, BitPosition}
import reference.CRISPRSite
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * store off-target hits associated with a specific CRISPR target
  */
class CRISPRSiteOT(tgt: CRISPRSite, encoding: Long, overflow: Int) extends Ordered[CRISPRSiteOT] with LazyLogging {
  var target = tgt
  var offTargets = new ArrayBuffer[CRISPRHit]
  val longEncoding = encoding
  var currentTotal = 0

  val namedAnnotations = new mutable.HashMap[String,Array[String]]


  def full = currentTotal >= overflow

  def addOT(offTarget: CRISPRHit) = {
    assert(currentTotal < overflow || overflow == 0,"We should not add off-targets to an overflowed guide: " + encoding + " overflow value: " + overflow + " current size " + currentTotal)
    offTargets += offTarget
    currentTotal += 1
  }

  def addOTs(offTargetList: Array[CRISPRHit]) = {
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


