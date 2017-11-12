package scoring


/**
  * indicate that this score aggregates other scores, and should be run last
  */

trait AggregateScore {
  def initializeScoreNames(scoreNames: List[RankedScore])
}
