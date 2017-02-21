package reference.filter

import reference.CRISPRSite

import scala.collection.mutable

/**
  * do we want to keep this target hit?
  */
trait HitFilter {
  def filter(cRISPRSite: CRISPRSite): Boolean
  def toStringSummary(): String
}

/**
  * filter a crispr hit by the entropy level -- low is bad
  *
  * @param threshold the minimum entropy to keep this target
  */
case class EntropyFilter(threshold: Double) extends HitFilter {

  def filter(cRISPRSite: CRISPRSite): Boolean = {
    val entropyCounts = new mutable.HashMap[Char,Double]()
    cRISPRSite.bases.foreach{base => {
      entropyCounts(base) = entropyCounts.getOrElse(base,0.0) + 1.0
    }}

    val tot = cRISPRSite.bases.size.toDouble

    val entropy = -1.0 * entropyCounts.map{case(base,count) => count.toDouble/tot * math.log(count.toDouble/tot)}.sum
    entropy > threshold
  }

  def toStringSummary() = "EntropyFilter:" + threshold
}

/**
  * keep a crispr hit if it has less than X identical bases in a row
  *
  * @param threshold the maximum number of bases we allow in a row --
  */
case class maxPolyNTrackFilter(threshold: Int) extends HitFilter {

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