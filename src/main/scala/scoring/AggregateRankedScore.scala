package scoring

/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */


import java.io.File

import bitcoding.{BitEncoding, BitPosition, PositionInformation}
import crispr.{CRISPRSite, CRISPRSiteOT}
import picocli.CommandLine.Command
import scoring.AggregateRankedScore.{scoreHighIsGood, scoreLowIsGood}
import scoring._
import standards.ParameterPack
import utils.{BEDFile, Utils}

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class AggregateRankedScore(trancheCount: Int = 4) extends ScoreModel with AggregateScore {

  // our stored list of score names
  var scoreNames = List[RankedScore]()

  /**
    * @return the name of this score model, used to look up the models when initializing scoring
    */
  override def scoreName(): String = "AggregateRankedScore"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Combines the rank order of scoring metrics using Schulze approach, a voting theory approach, into a single rank order for guides"

  /**
    * load up the BED file and annotate each guide with information from any intersecting annotations
    *
    * @param guides the guides with off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {

    // the range of our rank order
    val range = guides.size

    // our ranked guide container
    val rankedGuides = guides.map { gd => RankedCRISPRSiteOT(gd) }

    // add rank information to the guides
    AggregateRankedScore.rankGuides(rankedGuides,scoreNames)

    // now reverse the sort -- we want the best targets first
    val medianRankedGuides = Array(rankedGuides.map { gd => {
      (gd.medianRank, gd)
    }
    }.toSeq.sortWith(_._1 < _._1): _*)

    //
    val trancheUpperDividers = Range(1, (trancheCount + 1)).map{v => v.toDouble/trancheCount.toDouble}.toArray

    // attach the median-of-median rank to the score
    AggregateRankedScore.assignRank(medianRankedGuides, scoreLowIsGood, this.scoreName + "_medianRank")

    // assign the tranche
    medianRankedGuides.zipWithIndex.foreach { case ((median, guide), index) => {
      val normalizedIndex = (index.toDouble) / (rankedGuides.size - 1).toDouble

      val tranchesGreaterThan = trancheUpperDividers.filter(belowThis =>
        (guide.site.namedAnnotations(this.scoreName + "_medianRank")(0).toDouble / medianRankedGuides.size.toDouble <= belowThis))

      val tranche = trancheUpperDividers.indexOf(tranchesGreaterThan(0)) + 1

      guide.site.namedAnnotations(scoreName + "_tranche") = Array[String](tranche.toString)


    }
    }

    // now rank either the top 1000 guides or the first quantile, using the Schulze method
    val topXGuides = Math.min(Math.ceil(rankedGuides.size / trancheCount.toDouble).toInt, 1000)

    val topGuides = medianRankedGuides.slice(0, topXGuides).map { case (med, gd) => (gd.ranks.values.toArray, gd) }.toArray
    val schRank = new utils.SchulzeRank[RankedCRISPRSiteOT](topGuides)

    topGuides.zipWithIndex.foreach { case ((scores, gd), index) => {
      gd.site.namedAnnotations(scoreName + "_topX") = Array[String]((schRank.indexToRNS(index).rank + 1 /*make it one-based instead of zero-based*/).toString)
    }
    }
  }


  /**
    * are we valid over the enzyme of interest? always true
    *
    * @param enzyme the enzyme
    * @return true
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * Our parameters:
    *
    * inputBedFile         the bed file to annotate with
    * useInGenomeLocations sometimes we don't encode an input fasta with the appropriate contig info (so it doesn't have)
    * the real genome positions - if this parameter is set lookup any 0 mismatch hits and put their
    * annotation info onto the guide. A way to recover genomic info
    *
    * @param args the command line arguments
    */
  override def setup() {}

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String](scoreName + "_medianRank", scoreName + "_tranche", scoreName + "_topX")

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}

  override def initializeScoreNames(scoreNames: List[RankedScore]): Unit = {
    this.scoreNames = scoreNames
  }
}

object AggregateRankedScore {
  /**
    * add rank information to a set of targets, worst to best
    *
    * @param rankedGuides the array of RankedCRISPRSiteOTs
    */
  def rankGuides(rankedGuides: Array[RankedCRISPRSiteOT], scoreNames: List[RankedScore]): Unit = {

    // for each scoring scheme we're considering, create and record a rank order
    scoreNames.foreach { rnkScore => {

      // which way is the metric sorted?
      val (rankingApproach, failScore) = if (rnkScore.highScoreIsGood) {
        (scoreHighIsGood, Int.MinValue)
      } else {
        (scoreLowIsGood, Int.MaxValue)
      }

      val preRanked = rankedGuides.map { gd => {
        (convertToScore(gd.site.namedAnnotations.getOrElse(rnkScore.scoreName(),Array[String](failScore.toString)),failScore), gd)
      }}

      val ranked = preRanked.sortWith(rankingApproach)

      assignRank(ranked,rankingApproach,"RANKED_" + rnkScore.scoreName())
    }
    }
  }

  def assignRank(ranked: Array[(Double, RankedCRISPRSiteOT)],
                         rankingApproach: (Tuple2[Double, RankedCRISPRSiteOT], Tuple2[Double, RankedCRISPRSiteOT]) => Boolean,
                         rankName: String) = {
    var currentRank = 1
    var rankBuffer = new ArrayBuffer[Tuple2[Double, RankedCRISPRSiteOT]]()

    ranked.zipWithIndex.foreach { case ((score, guide), index) => {
      // if we have a better-scoring guide, assign ranks to all those guides in the buffer
      if (rankBuffer.size > 0 && rankingApproach(rankBuffer(rankBuffer.size - 1), (score, guide))) {
        val assignedRank = currentRank + Math.floor(rankBuffer.size / 2).toInt
        rankBuffer.toArray.foreach { rk => {
          rk._2.ranks(rankName) = assignedRank
          rk._2.site.namedAnnotations(rankName) = Array[String](assignedRank.toString)
        }
        }
        currentRank += rankBuffer.size
        rankBuffer.clear()
      }
      rankBuffer += Tuple2[Double, RankedCRISPRSiteOT](score, guide)
    }
    }
    val assignedRank = currentRank + Math.floor(rankBuffer.size / 2).toInt
    rankBuffer.toArray.foreach { rk => {
      rk._2.ranks(rankName) = assignedRank
      rk._2.site.namedAnnotations(rankName) = Array[String](assignedRank.toString)
    }
    }
  }

  val scoreHighIsGood = (s1: Tuple2[Double, RankedCRISPRSiteOT], s2: Tuple2[Double, RankedCRISPRSiteOT]) => s1._1 > s2._1

  val scoreLowIsGood = (s1: Tuple2[Double, RankedCRISPRSiteOT], s2: Tuple2[Double, RankedCRISPRSiteOT]) => s1._1 < s2._1


  def convertToScore(scores: Array[String], failoverScore: Double): Double = {
    try {
      scores.mkString("-fail-").toDouble
    } catch {
      case _ : Throwable => failoverScore
    }
  }
}