package crispr.filter

import reference.CRISPRSite

import scala.collection.mutable

/**
  * do we want to keep a target we found? this trait is used to implement
  * a series of pre-filters, that tell us if a sequence is good or not.
  */
trait SequencePreFilter {

  /**
    * indicate if we want to keep this guide (true) or not (false)
    * @param cRISPRSite the site to filter
    * @return true to keep, false to drop
    */
  def filter(cRISPRSite: CRISPRSite): Boolean

  /**
    *
    * @return describe the filtering metric
    */
  def toStringSummary(): String
}

object SequencePreFilter {

  def standardFilters(): Array[SequencePreFilter] = {
    var filters = Array[SequencePreFilter]()
    filters :+= EntropyFilter(1.0)
    filters :+= MaxPolyNTrackFilter(6)
    filters
  }
}

