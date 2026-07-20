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
  * Iterates over the database bins that may contain off-targets for a set of
  * guides.
  *
  * Implementations may use progressively broader search strategies. Callers
  * can remove guides with [[overflowGuide]] once they have accumulated enough
  * hits, allowing later traversal steps to skip unnecessary comparisons.
  */
trait BinTraversal extends Iterator[BinToGuidesLookup] {
  /**
    * An approximate upper bound on the number of iterator steps required to
    * cover the configured off-target search space.
    */
  def traversalSize: Int

  /**
    * Whether the strategy already visits every database bin, so broadening the
    * traversal cannot add candidates.
    */
  def saturated: Boolean

  /**
    * Removes a guide that no longer needs additional off-target candidates.
    *
    * @param guide index of the guide to exclude from subsequent iterator steps
    */
  def overflowGuide(guide: GuideIndex)

}
