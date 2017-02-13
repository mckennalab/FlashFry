package crispr

import bitcoding.BitEncoding
import reference.CRISPRSite

import scala.collection.mutable.ArrayBuffer

/**
  * store off-target hits associated with a specific CRISPR target
  */
class CRISPRSiteOT(tgt: CRISPRSite, encoding: Long) extends Ordered[CRISPRSiteOT] {
  val target = tgt
  val offTargets = new ArrayBuffer[CRISPRHit]
  val longEncoding = encoding

  def addOT(offTarget: CRISPRHit) = offTargets += offTarget
  def addOTs(offTargetList: Array[CRISPRHit]) = offTargetList.foreach{t => {offTargets += t}}

  def compare(that: CRISPRSiteOT): Int = (target.bases) compare (that.target.bases)
}


case class CRISPRHit(sequence: Long, coordinates: Array[Long])
