package scoring

/**
  * indicate that the implementing
  * class produces scores that can
  * be ranked, and indicate which way
  * the ranking goes
  */
trait RankedScore extends ScoreModel {
  def highScoreIsGood: Boolean
}
