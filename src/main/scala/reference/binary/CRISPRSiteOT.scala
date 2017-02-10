package reference.binary

import reference.CRISPRSite

import scala.collection.mutable.ArrayBuffer

/**
  * Created by aaronmck on 2/9/17.
  */
class CRISPRSiteOT(tgt: CRISPRSite) {
  val target = tgt
  val offTargets = new ArrayBuffer[CRISPRHit]

  def addOT(offTarget: CRISPRHit) = offTargets += offTarget
  def addOTs(offTargetList: Array[CRISPRHit]) = offTargetList.foreach{t => {offTargets += t}}
}


case class CRISPRHit(sequence: Long, coordinates: Array[Long])
