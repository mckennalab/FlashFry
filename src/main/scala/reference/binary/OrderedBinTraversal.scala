package reference.binary

import java.util

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import main.scala.util.BaseCombinationGenerator
import reference.CRISPRSite

import scala.collection.mutable

/**
  * this class stores information for each of the bins we will use in an off-target search
  */
class OrderedBinTraversal(binGenerator: BaseCombinationGenerator,
                          maxMismatch: Int,
                          binaryEncoder: BitEncoding,
                          upperBinProportionToJustSearchAll: Double,
                          guides: Array[CRISPRSiteOT]) extends Iterable[BinToGuides] with LazyLogging {

  val bGenerator = binGenerator

  val mMismatch = maxMismatch

  val mBinaryEncoder = binaryEncoder

  val justSearchEverything = false

  // provide a mapping from each bin to targets we need to score in that bin
  val binToTargets = new mutable.TreeMap[String, Array[Long]]()

  logger.info("Setting up binary lookup table")

  val guideLongToCRISPRSite = new mutable.HashMap[Long, Array[CRISPRSiteOT]]()

  guides.foreach { guide =>
    guideLongToCRISPRSite(guide.longEncoding) = guide +: guideLongToCRISPRSite.getOrElse(guide.longEncoding, Array[CRISPRSiteOT]())
  }

  // take a bin iterator, and make an array of longs
  val binArray = binGenerator.iterator.map{bin => (binaryEncoder.binToLongComparitor(bin), bin)}.toArray
  val maximumBinSize =
  logger.info("Precomputing bin lookup table for " + guides.size + " guides")

  val guideBinMask = binaryEncoder.compBitmaskForBin(binGenerator.width)

  // while loop for speed here
  var index = 0
  while (index < binArray.size) {
    var guideIndex = 0
    val currentBinBuilder = mutable.ArrayBuilder.make[Long]
    while(guideIndex < guides.size) {
      if (binaryEncoder.mismatches(binArray(index)._1,guides(guideIndex).longEncoding,guideBinMask) <= maxMismatch) {
        currentBinBuilder += guides(guideIndex).longEncoding
      }
      guideIndex += 1
    }
    val guideResult = currentBinBuilder.result
    if (guideResult.size > 0)
      binToTargets(binArray(index)._2) = guideResult

    if (index % 10000 == 0)
      logger.info("Searched against bin " + binArray(index)._2 + " the " + index + "th bin")

    index += 1
  }

  logger.info("With " + guides.size + " guides, and " + mMismatch + " mismatches, we're going to scan " + binToTargets.size + " target bins out of a total of " + math.pow(4, binGenerator.width).toInt)

  /**
    * add the off-target hit
    *
    * @param guide   the guide sequence as a long
    * @param oTarget the off-target sequence
    */
  def addOTHit(guide: Long, oTarget: CRISPRHit) {
    assert(guideLongToCRISPRSite contains guide)
    guideLongToCRISPRSite(guide).foreach { hit => hit.addOT(oTarget) }
  }

  /**
    * provide an iterator over our bin to target assigments
    *
    * @return an iterator over bins
    */
  override def iterator: Iterator[BinToGuides] = {
    PrivateBinIterator(binGenerator.iterator, binToTargets.clone())
  }

  /**
    * our private implementation of an iterator over full bins
    *
    * @param binIterator the full iterator over bin sequences
    * @param binToTarget a mapping of bins with at least one target to those targets
    */
  private case class PrivateBinIterator(binIterator: Iterator[String], binToTarget: mutable.TreeMap[String, Array[Long]]) extends Iterator[BinToGuides] {

    var cachedNextBin: Option[BinToGuides] = None

    // find the first entry
    while (binIterator.hasNext && !cachedNextBin.isDefined) {
      val nextBin = binIterator.next()

      if (binToTargets contains nextBin) {
        cachedNextBin = Some(BinToGuides(nextBin, binToTarget(nextBin)))
      }
    }

    /**
      * @return do we have a next value
      */
    override def hasNext: Boolean = cachedNextBin.isDefined

    /**
      * @return the next bin we need to lookup
      */
    override def next(): BinToGuides = {
      val ret = cachedNextBin.get

      cachedNextBin = None

      // find the first entry
      while (binIterator.hasNext && !cachedNextBin.isDefined) {
        val nextBin = binIterator.next()

        if (binToTargets contains nextBin) {
          cachedNextBin = Some(BinToGuides(nextBin, binToTarget(nextBin)))
        }
      }
      ret
    }
  }

}


case class BinToGuides(bin: String, guides: Array[Long])