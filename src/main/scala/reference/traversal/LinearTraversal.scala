package reference.traversal

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import main.scala.util.BaseCombinationGenerator

/**
  * linear traversal over the collection of bins in the file
  */
class LinearTraversal(binGenerator: BaseCombinationGenerator,
                      maxMismatch: Int,
                      binaryEncoder: BitEncoding,
                      upperBinProportionToJustSearchAll: Double,
                      guides: Array[CRISPRSiteOT],
                      filtering: Boolean = true) extends BinTraversal with LazyLogging {

  // an array of guides that are no longer collecting off-target hits
  var guidesToExclude = Array[Long]()

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
  override def overflowGuide(guide: Long) { guidesToExclude :+= guide }


  override def hasNext: Boolean = binIterator.hasNext


  override def next(): BinToGuidesLookup = {
    BinToGuidesLookup(binIterator.next(),guides.filter(cr => guidesToExclude contains cr.longEncoding).map(cr => cr.longEncoding))
  }

}
