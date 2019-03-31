package utils

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Implements the Schulze method, which generates a ranked list that combines
  * other ranked orders
  *
  * @param votes an array of rankings ('votes') for each candidate
  */
class SchulzeRank[T](votes: Array[Tuple2[Array[Int],T]]) {

  // make a score matrix
  val guideCount = votes.size
  val inputMatrix  = Array.ofDim[Int](guideCount,guideCount)
  val outputMatrix = Array.ofDim[Int](guideCount,guideCount)

  // first make a who prefers who matrix
  val inputPreferences  = Array.ofDim[Int](guideCount,guideCount)

  // now for each pair of candidates how preferred one is over the other
  votes.indices.foreach{ i => {
    votes.indices.foreach{ j => {
      inputPreferences(i)(j) = votes(i)._1.zip(votes(j)._1).map{case(ival,jval) => {
        ival - jval
      }}.sum
    }}
  }}

  // now compute the output matrix
  val output  = Array.ofDim[Int](guideCount,guideCount)

  // set the preferences
  votes.indices.foreach{ i => {
    votes.indices.foreach { j => {
      if (i != j) {
        if (inputPreferences(i)(j) > inputPreferences(j)(i)) {
          output(i)(j) = inputPreferences(i)(j)
        } else {
          output(i)(j) = 0
        }
      }
    }
    }
  }}

  votes.indices.foreach{ i => {
    votes.indices.foreach { j => {
      if (i != j) {
        votes.indices.foreach { k => {
          if (i != k && j != k) {
            output(j)(k) = Math.max(output(j)(k), Math.min(output(j)(i), output(i)(k)))
          }
        }
        }
      }
    }
    }
  }}

  // now get the row ranks, and return them
  private val rowsToSort = votes.indices.map { i => {
    votes.indices.map { j => output(i)(j)}.sum
  }}

  private val finalRanks = Range(0,guideCount).map{case(ind) => RankAndScore(ind, rowsToSort(ind))}.sortWith(_.score > _.score)

  val indexToRNS = finalRanks.zipWithIndex.map{case(rk,index) => {
    rk.rank = index
    (index,rk)
  }}.toMap

}

case class RankAndScore(index: Int, score: Int) {
  var rank = -1
}
