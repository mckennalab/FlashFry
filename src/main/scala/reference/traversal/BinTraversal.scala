package reference.traversal

import bitcoding.BitEncoding
import crispr.{CRISPRHit, GuideIndex}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuilder, Map}

/**
  * Created by aaronmck on 3/9/17.
  */
trait BinTraversal extends Iterator[BinToGuidesLookup] {
  /**
    * @return how many traversal calls we'll need to traverse the whole off-target space; just a loosely bounded value here
    */
  def traversalSize: Int

  /**
    * have we saturated: when we'd traverse the total number of bins, and any enhanced search strategy
    * would be useless
    * @return
    */
  def saturated: Boolean

  /**
    *
    * @param guide a guide that no longer should be considered for off-target sequences
    */
  def overflowGuide(guide: GuideIndex)

}
