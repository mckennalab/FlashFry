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
import modules.{DiscoverConfig, OffTargetBaseOptions}
import scoring._
import standards.ParameterPack
import utils.{BEDFile, Utils}

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class AggregateRankedScore extends ScoreModel with AggregateScore {

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

    println("guide size = " + guides.size)
    // the range of our rank order
    val range = guides.size

    // our ranked guide container
    val rankedGuides = guides.map{gd => RankedCRISPRSiteOT(gd)}

    // add rank information to the guides
    rankGuides(rankedGuides)

    // now reverse the sort -- we want the best targets first
    val medianRankedGuides = Array(rankedGuides.map{gd => {
      (gd.medianRank,gd)
    }}.toSeq.sortWith(_._1 > _._1):_*)

    val trancheDividers = Range(0,rankedGuides.size).grouped(Math.ceil(rankedGuides.size / 4.0).toInt).toArray

    medianRankedGuides.zipWithIndex.foreach{case((median,guide),index) => {

      // assign the tranche name
      val tranche = trancheDividers.zipWithIndex.filter{case(t,ind) => t.contains(index)}.map{case(t,ind) => ind + 1}
      assert(tranche.size == 1)
      guide.site.namedAnnotations(scoreName + "_medianRank") = Array[String]((rankedGuides.size - median).toInt.toString)
      guide.site.namedAnnotations(scoreName + "_medianTranche") = Array[String](tranche.mkString(","))
    }}

    // now rank either the top 1000 guides or the first quantile, using the Schulze method
    val topXGuides = Math.min(Math.ceil(rankedGuides.size / 4.0).toInt,1000)

    val topGuides = medianRankedGuides.slice(0,topXGuides).map{case(med,gd) => (gd.ranks.values.toArray,gd)}.toArray
    val schRank = new utils.SchulzeRank[RankedCRISPRSiteOT](topGuides)

    topGuides.zipWithIndex.foreach{case((scores,gd),index) => {
      gd.site.namedAnnotations(scoreName + "_topX") = Array[String]((schRank.indexToRNS(index).rank + 1 /*make it one-based instead of zero-based*/).toString)
    }}
  }

  /**
    * add rank information to a set of targets
    * @param rankedGuides the array of RankedCRISPRSiteOTs
    */
  private def rankGuides(rankedGuides: Array[RankedCRISPRSiteOT]) = {
    // for each scoring scheme we're considering, create and record a rank order
    scoreNames.foreach { scheme => {
      val preRanked = rankedGuides.map { gd => (convertToScore(gd.site.namedAnnotations.getOrElse(scheme.scoreName, Array[String]("FAIL"))).getOrElse(-1.0), gd) }

      // which way is the metric sorted?
      val ranked = if (scheme.highScoreIsGood) {
        ListMap(preRanked.toSeq.sortWith(_._1 > _._1): _*)
      } else {
        ListMap(preRanked.toSeq.sortWith(_._1 < _._1): _*)
      }

      // now store the rank
      ranked.zipWithIndex.foreach { case ((score, guide), index) => guide.ranks(scheme.scoreName) = index }
    }
    }
  }

  def convertToScore(scores: Array[String]): Option[Double] = {
    try { Some(scores.mkString("").toDouble) } catch { case _ => None }
  }

  /**
    * are we valid over the enzyme of interest? always true
    *
    * @param enzyme the enzyme
    * @return true
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

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
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    Seq[String]()
  }

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String](scoreName + "_medianRank",scoreName + "_medianQuantile",scoreName + "_topX")

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