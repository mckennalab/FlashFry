package crispr.filter

import reference.CRISPRSite

/**
  * filter a crispr hit by the entropy level -- low is bad
  *
  * @param threshold the minimum entropy to keep this target
  */
case class EntropyFilter(threshold: Double = 1.0) extends SequencePreFilter {

  def filter(cRISPRSite: CRISPRSite): Boolean = {
    val entropyCounts = new scala.collection.mutable.HashMap[Char,Double]()
    cRISPRSite.bases.foreach{base => {
      entropyCounts(base) = entropyCounts.getOrElse(base,0.0) + 1.0
    }}

    val tot = cRISPRSite.bases.size.toDouble

    val entropy = -1.0 * entropyCounts.map{case(base,count) => count.toDouble/tot * math.log(count.toDouble/tot)}.sum
    entropy > threshold
  }

  def toStringSummary() = "EntropyFilter:" + threshold
}
