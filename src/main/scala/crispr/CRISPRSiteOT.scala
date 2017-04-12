package crispr

import bitcoding.BitEncoding
import reference.CRISPRSite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * store off-target hits associated with a specific CRISPR target
  */
class CRISPRSiteOT(tgt: CRISPRSite, encoding: Long, overflow: Int) extends Ordered[CRISPRSiteOT] {
  val target = tgt
  val offTargets = new ArrayBuffer[CRISPRHit]
  val longEncoding = encoding
  var currentTotal = 0

  val namedAnnotations = new mutable.HashMap[String,Array[String]]


  def full = currentTotal >= overflow

  def addOT(offTarget: CRISPRHit) = {
    assert(currentTotal < overflow || overflow == 0,"We should not add off-targets to an overflowed guide: " + encoding)
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

  def compare(that: CRISPRSiteOT): Int = (target.bases) compare (that.target.bases)
}

case class CRISPRHit(sequence: Long, coordinates: Array[Long])
