package scoring

import crispr.CRISPRSiteOT
import utils.Utils

import scala.collection.mutable

/**
  * This class wraps a CRISPRSiteOT with ranking information
  * @param site the CRISPR target with it's off-target information
  */
case class RankedCRISPRSiteOT(site: CRISPRSiteOT) {

  val ranks = new mutable.HashMap[String,Int]()

  /**
    * add a ranked score to the object
    * @param scoreName the name of the score, use ScoreModel.scoreName()
    * @param rank the rank of this target within that score
    */
  def addRank(scoreName: String, rank: Int): Unit = {
    ranks(scoreName) = rank
  }

  lazy val medianRank:Double = {
    Utils.median(ranks.values.map{t => t.toDouble}.toSeq)
  }
}
