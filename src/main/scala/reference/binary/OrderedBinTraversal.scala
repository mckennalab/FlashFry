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
                          guides: Array[CRISPRSiteOT]) extends Iterable[BinToGuides] with LazyLogging {

  val bGenerator = binGenerator

  val mMismatch = maxMismatch

  val mBinaryEncoder = binaryEncoder

  // provide a mapping from each bin to targets we need to score in that bin
  val binToTargets = new mutable.TreeMap[String, mutable.ArrayBuilder[Long]]()

  logger.warn("Precomputing bin lookup table for " + guides.size + " guides")

  val guideLongToCRISPRSite = new mutable.HashMap[Long, Array[CRISPRSiteOT]]()

  guides.foreach{guide => addGuide(guide)}

  logger.warn("With " + guides.size + " guides, and " + mMismatch + " mismatches, we're going to scan " + binToTargets.size + " target bins out of a total of " + math.pow(4,binGenerator.width).toInt)

  /**
    * add a guide to our off-target
    * @param guide the guide as a CRISPRSiteOT
    */
  private def addGuide(guide: CRISPRSiteOT) {
    guideLongToCRISPRSite(guide.longEncoding) = guide +: guideLongToCRISPRSite.getOrElse(guide.longEncoding, Array[CRISPRSiteOT]())

    binGenerator.iterator.foreach(bin => {
      if (mBinaryEncoder.mismatchBin(bin, guide.longEncoding) <= maxMismatch) {
        if (!(binToTargets contains bin))
          binToTargets(bin) = mutable.ArrayBuilder.make[Long]
        binToTargets(bin) += guide.longEncoding
      }
    })
  }

  /**
    * add the off-target hit
    * @param guide the guide sequence as a long
    * @param oTarget the off-target sequence
    */
  def addOTHit(guide: Long, oTarget: CRISPRHit) {
    assert(guideLongToCRISPRSite contains guide)
    guideLongToCRISPRSite(guide).foreach{hit => hit.addOT(oTarget)}
  }

  /**
    * provide an iterator over our bin to target assigments
    * @return an iterator over bins
    */
  override def iterator: Iterator[BinToGuides] = {
    // render
    val renderedBinMap = binToTargets.map{case(bin,lst) => (bin,lst.result())}.toMap

    PrivateBinIterator(binGenerator.iterator,renderedBinMap)
  }

  /**
    * our private implementation of an iterator over full bins
    *
    * @param binIterator the full iterator over bin sequences
    * @param binToTarget a mapping of bins with at least one target to those targets
    */
  private case class PrivateBinIterator(binIterator: Iterator[String], binToTarget: Map[String, Array[Long]]) extends Iterator[BinToGuides] {

    var cachedNextBin : Option[BinToGuides] = None

    // find the first entry
    while (binIterator.hasNext && !cachedNextBin.isDefined) {
      val nextBin = binIterator.next()

      if (binToTargets contains nextBin) {
        cachedNextBin = Some(BinToGuides(nextBin,binToTarget(nextBin)))
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
          cachedNextBin = Some(BinToGuides(nextBin,binToTarget(nextBin)))
        }
      }
      ret
    }
  }

}


case class BinToGuides(bin: String, guides: Array[Long])