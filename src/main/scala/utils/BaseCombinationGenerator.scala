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

package utils


// makes iterators of Bases; from AAAA to TTTT for example
case class BaseCombinationGenerator(width: Int) extends Iterable[String] {

  override def iterator: Iterator[String] = new BaseCombinationIterator(width)

  val numberOfLetters = 4

  def totalSearchSpace: Long = math.pow(numberOfLetters,width).toLong
}

class BaseCombinationIterator(count: Int) extends Iterator[String] {

  import utils.Base._

  var lst = new Array[Base](count)
  var isLast = false
  (0 until count).foreach { case (e) => lst(e) = Base.A }

  val terminalStr = (0 until count).map { _ => Base.T }.mkString

  override def hasNext: Boolean =
    if (terminalStr == lst.mkString) {
      isLast = true
      true
    }
    else {
      !isLast
    }

  def incr(pos: Int): Unit = {
    if (lst(pos) == Base.T) {
      lst(pos) = Base.A
      if (pos - 1 >= 0) {
        incr(pos - 1)
      }
    }
    else {
      lst(pos) = Base(Base.baseToInt(lst(pos)) + 1)
    }
  }

  override def next(): String = {
    val ret = lst.mkString
    incr(count - 1)
    ret
  }
}
