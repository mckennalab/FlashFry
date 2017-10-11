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

package reference.traversal

import bitcoding.BitEncoding
import crispr.{CRISPRHit, GuideIndex}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuilder, Map}

/**
  * Created by aaronmck on 3/9/17.
  */
trait BinTraversal extends Iterator[BinToGuidesLookup] {
  /**
    * @return how many traversal calls we'll need to traverse the whole off-target space; just a loosely bounded value here
    */
  def traversalSize: Int

  /**
    * have we saturated: when we'd traverse the total number of bins, and any enhanced search strategy
    * would be useless
    * @return
    */
  def saturated: Boolean

  /**
    *
    * @param guide a guide that no longer should be considered for off-target sequences
    */
  def overflowGuide(guide: GuideIndex)

}
