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
