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

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

/**
  * we spent a lot of time in various search functions mapping guides stored as Long values
  * back to their intended sequence -- this class aims to make that as simple and fast as possible
  */
class ResultsAggregator(guides: Array[CRISPRSiteOT]) extends LazyLogging {

  // presort the guides
  Sorting.quickSort(guides)

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