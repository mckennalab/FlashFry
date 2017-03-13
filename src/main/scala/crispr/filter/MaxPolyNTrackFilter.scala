package crispr.filter

import reference.CRISPRSite

/**
  * keep a crispr hit if it has less than X identical bases in a row
  *
  * @param threshold the maximum number of bases we allow in a row --
  */
case class MaxPolyNTrackFilter(threshold: Int = 6) extends SequencePreFilter {

  def filter(cRISPRSite: CRISPRSite): Boolean = {
    var lastBase = 'N'
    var inARow = 0
    var bestHit = 0
    cRISPRSite.bases.foreach{base => {
      if (base == lastBase) {
        inARow += 1
        if (inARow > bestHit)
          bestHit = inARow
      } else {
        lastBase = base
        inARow = 1
      }
    }}
    bestHit <= threshold
  }

  def toStringSummary() = "maxPolyNTrackFilter:" + threshold
}
