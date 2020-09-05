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

import scala.collection.mutable
import scala.util.Random

class RandoCRISPR(sz: Int,
                  pams: Array[String],
                  pamFivePrime: Boolean,
                  stringPrefix: String,
                  randomFront: Int,
                  randomRear: Int,
                  pattern: Option[String]) extends Iterator[RandomCRISPR] {

  def hasNext(): Boolean = true

  val seed = System.currentTimeMillis
  val r = new scala.util.Random(seed)
  val numberOfBases = 4
  val patternedTarget = pattern

  /**
    * generate a random sequence according to the rules of the CRISPR enzyme we're interested in
    *
    * @return
    */
  def next(): RandomCRISPR = {
    val randomFrt = randomString(randomFront)
    val randomRvs = randomString(randomRear)

    val unpaddedPam =
        (pams(r.nextInt(pams.length)).map { base => {
          if (base == 'N') {
            numberToBase(r.nextInt(numberOfBases))
          } else {
            base
          }
        }
        }.mkString(""))


    val guideDraw = if (patternedTarget.isDefined)
      PatternedTarget.patternedDraw(patternedTarget.get, sz, ",")
    else
      randomString(sz - stringPrefix.size)

    if (pamFivePrime) {
      RandomCRISPR(guideDraw,randomFrt + unpaddedPam + stringPrefix + guideDraw + randomRvs)
    }
    else {
      RandomCRISPR(guideDraw,randomFrt + stringPrefix + guideDraw + unpaddedPam + randomRvs)
    }
  }

  def numberToBase(nt: Int): Char = if (nt == 0) 'A' else if (nt == 1) 'C' else if (nt == 2) 'G' else 'T'

  def baseToNumber(nt: Char): Int = if (nt == 'A') 0 else if (nt == 'C') 1 else if (nt == 'G') 2 else 3

  def randomString(bases: Int): String = (0.until(bases)).map {
    ct => numberToBase(r.nextInt(numberOfBases))
  }.mkString("")
}

case class RandomCRISPR(guide: String, fullTarget: String)

object PatternedTarget {
  val random = new Random

  def randomDraw(bases: Array[Char]): Char = {
    bases(random.nextInt(bases.length))
  }

  def drawRandom(base: Char): Char = base match {
    case 'A' => 'A'
    case 'C' => 'C'
    case 'G' => 'G'
    case 'T' => 'T'
    case 'R' => randomDraw(Array[Char]('A', 'G'))
    case 'Y' => randomDraw(Array[Char]('C', 'T'))
    case 'K' => randomDraw(Array[Char]('G', 'T'))
    case 'M' => randomDraw(Array[Char]('A', 'C'))
    case 'S' => randomDraw(Array[Char]('C', 'G'))
    case 'W' => randomDraw(Array[Char]('A', 'T'))
    case 'B' => randomDraw(Array[Char]('C', 'G', 'T'))
    case 'D' => randomDraw(Array[Char]('A', 'G', 'T'))
    case 'H' => randomDraw(Array[Char]('A', 'A', 'T'))
    case 'V' => randomDraw(Array[Char]('A', 'C', 'G'))
    case 'N' => randomDraw(Array[Char]('A', 'C', 'G', 'T'))
    case _ => throw new IllegalStateException("Unknown or not allowed FASTA character: " + base)
  }

  def patternedDraw(pattern: String, setLength: Int, splitMark: String = ","): String = {
    val brokenPattern = pattern.split(splitMark)
    assert(brokenPattern.size == setLength, "We're seeing an unexpected pattern length: " + brokenPattern.size + "; expected " + setLength)

    // store a memory of our called bases for reuse -- the tuple contains the specified pattern and the actual drawn base
    val memories = mutable.HashMap[Int, Tuple2[Char, Char]]()

    brokenPattern.zipWithIndex.map { case (request, index) => {
      request match {
        case x if x.size == 1 => drawRandom(request(0))
        case x if x.size > 1 => {

          val memorySlotRC = if (x(x.size - 1) == '-') {
            (x.slice(1, x.length - 1).toInt,true)
          } else {
            (x.slice(1, x.length).toInt,false)
          }
          val basePattern = x.charAt(0)

          if (memories contains memorySlotRC._1) {
            assert(memories(memorySlotRC._1)._1 == basePattern,
              "Mismatched assignments of memory base at position " + index + " with base " + basePattern)
            if (memorySlotRC._2)
              Utils.compBase(memories(memorySlotRC._1)._2)
            else
              memories(memorySlotRC._1)._2
          } else {
            assert(!memorySlotRC._2,"We dont have a memory of x's parent, so we can't complement: " + x)
            val assignedBase = drawRandom(basePattern)
            memories(memorySlotRC._1) = (basePattern, assignedBase)
            assignedBase
          }
        }
      }
    }
    }.mkString("")
  }
}
