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
  def filter_by_GC(oldGC: GuideMemoryStorage, lowGC: Double, highGC: Double) : GuideMemoryStorage = {
    val newGC = new GuideMemoryStorage()
    oldGC.guideHits.foreach({guide => {
      val gc = Utils.gcContent(guide.bases)
      if (gc >= lowGC && gc <= highGC) {
        newGC.addHit(guide)
      }
    }})
    newGC
  }
}