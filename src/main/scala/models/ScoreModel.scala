package models

import crispr.{CRISPRGuide}

/**
 * Created by aaronmck on 6/18/15.
 */
trait ScoreModel {
  def scoreName(): String
  def scoreGuide(cRISPRHit: CRISPRGuide): String
}
