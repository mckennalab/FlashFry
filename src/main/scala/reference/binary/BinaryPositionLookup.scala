/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
