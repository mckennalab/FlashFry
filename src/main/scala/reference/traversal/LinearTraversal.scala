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

package reference.traversal

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSiteOT, GuideIndex, ResultsAggregator}
import utils.BaseCombinationGenerator

import scala.collection.mutable

/**
  * linear traversal over the collection of bins in the file
  */
class LinearTraversal(binGenerator: BaseCombinationGenerator,
                      maxMismatch: Int,
                      binaryEncoder: BitEncoding,
                      upperBinProportionToJustSearchAll: Double,
                      guides: ResultsAggregator,
                      filtering: Boolean = true) extends BinTraversal with LazyLogging {

  // an array of guides that are no longer collecting off-target hits
  var guidesToUse = guides.indexedGuides.map{case(guide) => guide}.toArray

  var overFlowedGuides = Array[GuideIndex]()

  // the internal iterator over bins
  val binIterator = binGenerator.iterator

  /**
    * @return how many traversal calls we'll need to traverse the whole off-target space; just a loosely bounded value here
    */
  override def traversalSize: Int = math.pow(4, binGenerator.width).toInt

  /**
    * have we saturated: when we'd traverse the total number of bins, and any enhanced search strategy
    * would be useless
    *
    * @return
    */
  override def saturated: Boolean = false

  /**
    *
    * @param guide a guide that no longer should be considered for off-target sequences
    */
  override def overflowGuide(guide: GuideIndex) {
    logger.warn("Overflowing guide " + guide)
    val guidesToUseBuilder = new mutable.ArrayBuffer[GuideIndex]()
    var guidesIndex = 0
    while (guidesIndex < guidesToUse.size) {
      if (guidesToUse(guidesIndex) == guide)
        overFlowedGuides :+= guidesToUse(guidesIndex)
      else
        guidesToUseBuilder += guidesToUse(guidesIndex)
      guidesIndex += 1
    }
    guidesToUse = guidesToUseBuilder.toArray
  }


  override def hasNext: Boolean = binIterator.hasNext


  override def next(): BinToGuidesLookup = {
    val bin = binaryEncoder.binToLongComparitor(binIterator.next())

    val guidesForRun = new mutable.ArrayBuffer[GuideIndex]()
    guidesForRun.sizeHint(guidesToUse.size)

    var guidesIndex = 0
    while (guidesIndex < guidesToUse.size) {
      if (binaryEncoder.mismatchBin(bin,guidesToUse(guidesIndex).guide) <= maxMismatch) {
        guidesForRun += guidesToUse(guidesIndex)
      }
      guidesIndex += 1
    }

    BinToGuidesLookup(bin.bin,guidesForRun.toArray)
  }

}
