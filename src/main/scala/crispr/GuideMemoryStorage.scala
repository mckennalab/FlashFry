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

import java.io.File

import utils.Utils
import standards.ParameterPack

import scala.collection.mutable

/**
  * a simple in-memory storage for guides
  */
class GuideMemoryStorage extends GuideContainer {

  val guideHits = new mutable.ArrayBuffer[CRISPRSite]()

  override def addHit(cRISPRSite: CRISPRSite): Unit = guideHits += cRISPRSite

  override def close(): mutable.HashMap[String,File] = {new mutable.HashMap[String,File]()}
}

object GuideMemoryStorage {
  /** Compatibility overload for callers storing arbitrary sequences rather than nuclease targets. */
  def filter_by_GC(oldGC: GuideMemoryStorage, lowGC: Double, highGC: Double) : GuideMemoryStorage = {
    filter_by_GC(oldGC, lowGC, highGC, None)
  }

  /** Filter on the guide spacer, explicitly excluding the PAM. */
  def filter_by_GC(oldGC: GuideMemoryStorage,
                   lowGC: Double,
                   highGC: Double,
                   parameterPack: ParameterPack): GuideMemoryStorage = {
    filter_by_GC(oldGC, lowGC, highGC, Some(parameterPack))
  }

  private def filter_by_GC(oldGC: GuideMemoryStorage,
                           lowGC: Double,
                           highGC: Double,
                           parameterPack: Option[ParameterPack]): GuideMemoryStorage = {
    val newGC = new GuideMemoryStorage()
    oldGC.guideHits.foreach({guide => {
      val sequence = parameterPack.map(_.profile.layout.spacer(guide.bases)).getOrElse(guide.bases)
      val gc = Utils.gcContent(sequence)
      if (gc >= lowGC && gc <= highGC) {
        newGC.addHit(guide)
      }
    }})
    newGC
  }
}
