package crispr

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * we spent a lot of time in various search functions mapping guides stored as Long values
  * back to their intended sequence -- this class aims to make that as simple and fast as possible
  */
class ResultsAggregator(guides: Array[CRISPRSiteOT]) extends LazyLogging {

  var wrappedGuides = Array[IndexedCRISPROT]()
  var indexedGuides = Array[GuideIndex]()
  val guidesToOverflow = new mutable.HashMap[GuideIndex,Boolean]()
  var callBackForOverFlow: Option[GuideIndex => Unit] = None


  guides.zipWithIndex.foreach{case(guide,index) => {
    val wrapped = IndexedCRISPROT(guide,index)
    val gIndex = GuideIndex(guide.longEncoding,index)
    wrappedGuides :+= wrapped
    indexedGuides :+= gIndex
    guidesToOverflow(gIndex) = false
  }}

  def updateOTs(guideIndex: GuideIndex, hits: Array[CRISPRHit]): Unit = {
    if (!wrappedGuides(guideIndex.index).otSite.full) {
      wrappedGuides(guideIndex.index).otSite.addOTs(hits)
      if (wrappedGuides(guideIndex.index).otSite.full) {
        guidesToOverflow(guideIndex) = true
        callBackForOverFlow.get(guideIndex)
      }
    }
  }

  def updateOT(guideIndex: GuideIndex, hit: CRISPRHit): Unit = {
    if (!wrappedGuides(guideIndex.index).otSite.full) {
      wrappedGuides(guideIndex.index).otSite.addOT(hit)
      if (wrappedGuides(guideIndex.index).otSite.full) {
        guidesToOverflow(guideIndex) = true
        callBackForOverFlow.get(guideIndex)
      }
    }
  }

  def setTraversalOverFlowCallback(func: GuideIndex => Unit): Unit = {
    callBackForOverFlow = Some(func)
  }

}

case class IndexedCRISPROT(otSite: CRISPRSiteOT, index: Int)

case class GuideIndex(guide: Long, index: Int)