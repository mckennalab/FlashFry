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

package reference.binary

import utils.BaseCombinationGenerator

import scala.collection.mutable

/**
  * handle our mapping from bin to size / count
  * @param binGenerator the bin generation
  */
case class BinaryPositionLookup(binGenerator: BaseCombinationGenerator) {

  val guidesPerBin = new mutable.HashMap[String, Int]()
  val longsPerBin = new mutable.HashMap[String, Int]()

  def append(baseString: String, guides: Int, numLongs: Int) {
    guidesPerBin(baseString) = guides
    longsPerBin(baseString) = guides
  }

  def toBinaryBlock(): Array[Long] = {
    val longs = mutable.ArrayBuilder.make[Long]

    binGenerator.foreach{bin => {
      longs += longsPerBin.getOrElse(bin,0).toLong
    }}

    longs.result()
  }
}
