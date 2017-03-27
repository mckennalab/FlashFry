package reference.traversal

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import utils.BaseCombinationGenerator

import scala.collection.mutable

/**
  * this class stores information for each of the bins we will use in an off-target search
  */
class OrderedBinTraversalFactory(binGenerator: BaseCombinationGenerator,
                                 maxMismatch: Int,
                                 binaryEncoder: BitEncoding,
                                 upperBinProportionToJustSearchAll: Double,
                                 guides: Array[CRISPRSiteOT],
                                 filtering: Boolean = true) extends LazyLogging {

  val bGenerator = binGenerator
  val mMismatch = maxMismatch
  val mBinaryEncoder = binaryEncoder

  var isSaturated = false
  def saturated = isSaturated

  // provide a mapping from each bin to targets we need to score in that bin
  val binToTargets = new mutable.TreeMap[String, Array[Long]]()
  def traversalSize() = binToTargets.size

  // take a bin iterator, and make an array of longs
  val binArray = binGenerator.iterator.map { bin => (binaryEncoder.binToLongComparitor(bin), bin) }.toArray

  /**
    * provide an iterator over our bin to target assigments
    *
    * @return an iterator over bins
    */
  def iterator: BinTraversal = {
    PrivateBinIterator(binGenerator.iterator, binToTargets.clone(), saturated)
  }


  /**
    * our private implementation of an iterator over target sequence bins
    *
    * @param binIterator the full iterator over bin sequences
    * @param binToTarget a mapping of bins with at least one target to those targets
    */
  private case class PrivateBinIterator(binIterator: Iterator[String], binToTarget: mutable.TreeMap[String, Array[Long]], isSaturated: Boolean) extends BinTraversal {

    var guidesToExclude = Array[Long]()

    var cachedNextBin: Option[BinToGuidesLookup] = None

    // find the first entry
    while (binIterator.hasNext && !cachedNextBin.isDefined) {
      val nextBin = binIterator.next()

      if (binToTargets contains nextBin) {
        cachedNextBin = Some(BinToGuidesLookup(nextBin, binToTarget(nextBin)))
      }
    }

    /**
      * @param guide a guide that no longer should be considered for off-target sequences
      */
    override def overflowGuide(guide: Long): Unit = guidesToExclude :+= guide

    /**
      * @return do we have a next value
      */
    override def hasNext: Boolean = cachedNextBin.isDefined

    /**
      * @return the next bin we need to lookup
      */
    override def next(): BinToGuidesLookup = {
      val ret = cachedNextBin.get

      cachedNextBin = None

      // find the first entry
      while (binIterator.hasNext && !cachedNextBin.isDefined) {
        val nextBin = binIterator.next()

        if (binToTargets contains nextBin) {
          if (filtering) {
            cachedNextBin = Some(BinToGuidesLookup(nextBin, binToTarget(nextBin).filter { case (target) => !(guidesToExclude contains target) }))
            //if (binToTarget(nextBin).size > cachedNextBin.get.guides.size)
            //  logger.info(binToTarget(nextBin).size + " to " + cachedNextBin.get.guides.size + " --" + cachedNextBin.isDefined + " ==== " + binIterator.hasNext)
          } else {
            cachedNextBin = Some(BinToGuidesLookup(nextBin, binToTarget(nextBin)))
          }
        }
      }
      ret
    }

    /**
      * @return how many traversal calls we'll need to traverse the whole off-target space; just a loosely bounded value here
      */
    override def traversalSize: Int = binToTarget.size

    /**
      * have we saturated: when we'd traverse the total number of bins, and any enhanced search strategy
      * would be useless
      *
      * @return
      */
    override def saturated: Boolean = isSaturated
  }


  logger.info("Precomputing bin lookup table for " + guides.size + " guides")

  val guideBinMask = binaryEncoder.compBitmaskForBin(binGenerator.width)
  val totalPossibleBins = math.pow(4, binGenerator.width).toInt

  // ----------------------------------------------------------------------------------------------------
  // while loop for speed here -- for each guide create a traversal of bins -- this code is ugly
  var index = 0
  while (index < binArray.size) {


    var guideIndex = 0
    val currentBinBuilder = mutable.ArrayBuilder.make[Long]

    while (guideIndex < guides.size) {
      if (binaryEncoder.mismatchBin(binArray(index)._1, guides(guideIndex).longEncoding) <= maxMismatch) {
        currentBinBuilder += guides(guideIndex).longEncoding
      }
      guideIndex += 1
    }
    val guideResult = currentBinBuilder.result
    if (guideResult.size > 0)
      binToTargets(binArray(index)._2) = guideResult

    if (index % 50000 == 0) {
      val currentBinSaturation = binToTargets.size.toDouble / totalPossibleBins.toDouble
      val binPropSeen = index.toDouble / totalPossibleBins.toDouble
      logger.info("Comparing guides against bin prefix " + binArray(index)._2 + " the " + index + "th bin prefix we've looked at, total bin saturation = " + currentBinSaturation + ", proportion of bins seen = " + binPropSeen)
      if (currentBinSaturation >= upperBinProportionToJustSearchAll || (currentBinSaturation / binPropSeen >= 0.4 && index > 20000)) {
        logger.info("Stopping bin lookup early, as we've already exceeded the maximum threshold of bins before we move over to a linear traversal ( saturation = " + currentBinSaturation + ", proportion = " + (currentBinSaturation / binPropSeen) + " )")
        index = binArray.size
        isSaturated = true
      }
    }

    index += 1
  }

  val binProp = binToTargets.size.toDouble / totalPossibleBins.toDouble
  if (binProp >= upperBinProportionToJustSearchAll)
    isSaturated = true
  logger.info("With " + guides.size + " guides, and allowing " +
    mMismatch + " mismatch(es), we're going to scan " + binToTargets.size +
    " target bins out of a total of " + totalPossibleBins)
}


