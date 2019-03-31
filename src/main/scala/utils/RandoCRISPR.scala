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

class RandoCRISPR(sz: Int,
                  pams: Array[String],
                  pamFivePrime: Boolean,
                  stringPrefix: String,
                  randomFront: Int,
                  randomRear: Int) extends Iterator[String] {

  def hasNext(): Boolean = true
  val seed = System.currentTimeMillis
  val r = new scala.util.Random(seed)
  val numberOfBases = 4
  /**
    * generate a random sequence according to the rules of the CRISPR enzyme we're interested in
    * @return
    */
  def next(): String = {
    val randomFrt = randomString(randomFront)
    val randomRvs = randomString(randomRear)
    val unpaddedPam = pams(r.nextInt(pams.length)).map { base => {
      if (base == 'N') {
        numberToBase(r.nextInt(numberOfBases))
      } else {
        base
      }
    }}.mkString("")

    if (pamFivePrime) {
      randomFrt + unpaddedPam + stringPrefix + randomString(sz - stringPrefix.size) + randomRvs
    }
    else {
      randomFrt + stringPrefix + randomString(sz - stringPrefix.size) + unpaddedPam + randomRvs
    }
  }

  def numberToBase(nt: Int): Char = if (nt == 0) 'A' else if (nt == 1) 'C' else if (nt == 2) 'G' else 'T'
  def baseToNumber(nt: Char): Int = if (nt == 'A') 0 else if (nt == 'C') 1 else if (nt == 'G') 2 else 3
  def randomString(bases: Int): String = (0.until(bases)).map { ct => numberToBase(r.nextInt(4)) }.mkString("")
}
