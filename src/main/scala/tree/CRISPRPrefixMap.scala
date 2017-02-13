package tree

/**
 * created by aaronmck on 12/10/14
 *
 * Copyright (c) 2014, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */

import scala.collection._
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{Builder, MapBuilder}
import scala.io.Source
import scala.pickling._
import scala.pickling.binary._

class CRISPRPrefixMap[T] extends mutable.Map[String, T ] with mutable.MapLike[String, T, CRISPRPrefixMap[T]] {
  var CRISPRLength: Option[Int] = None
  var suffixes: immutable.Map[Char, CRISPRPrefixMap[T]] = Map.empty
  var value: Option[T] = None

  def get(s: String): Option[T] =
    if (s.isEmpty) value
    else suffixes get (s(0)) flatMap (_.get(s substring 1))

  def withPrefix(s: String): CRISPRPrefixMap[T] =
    if (s.isEmpty) this
    else {
      val leading = s(0)
      suffixes get leading match {
        case None =>
          suffixes = suffixes + (leading -> empty)
        case _ =>
      }
      suffixes(leading) withPrefix (s substring 1)
    }

  override def update(s: String, elem: T) = {
    withPrefix(s).value = Some(elem)
  }

  override def remove(s: String): Option[T] =
    if (s.isEmpty) {
      val prev = value;
      value = None;
      prev
    }
    else suffixes get (s(0)) flatMap (_.remove(s substring 1))

  /**
   *
   * @param scoreParams a list of bases in the original string, and their respective scores
   * @param currentState the current string we've been building as we walk through the prefix trie
   * @param currentScore the current score of we've been building up
   * @param mismatchThreshold the number of mismatches allowed, minus the mismatches we've already seen. When 0, stop
   * @return a mapping of string hits to their scores, mismatch counts, and location information (generic T)
   */
  def recursiveScore(scoreParams: List[Tuple2[Char, Double]],
                     currentState: String = "",
                     currentScore: Double = 1.0,
                     mismatchThreshold: Int = CRISPRPrefixMap.maxMismatch,
                     debug: Boolean = false): Map[String, Tuple3[Double, Int, T]] = {

    // return conditions
    if (mismatchThreshold == 0) {
      if (debug)
        println("returning empty hit")
      return new immutable.HashMap[String, Tuple3[Double, Int, T]]()
    } else if (suffixes.isEmpty) {
      if (debug)
        println("returning score " + currentScore + " mismatch " + mismatchThreshold + " value " + value.get)
      return immutable.HashMap[String, Tuple3[Double, Int, T]]((currentState, Tuple3[Double, Int, T](currentScore, mismatchThreshold, value.get)))
    }

    return suffixes.flatMap { case (ch, prefixMap) => {
      if (ch != scoreParams(0)._1) {
        if (debug)
          println("mismatch against = " + ch + " toBeScored = " + scoreParams(0)._1 + " " + (scoreParams.length - 20) + " -> " + (currentScore * (1.0 - scoreParams(0)._2)) + " === " + scoreParams(0)._2)
        prefixMap.recursiveScore(scoreParams.tail, currentState + ch, currentScore * (1.0 - scoreParams(0)._2), mismatchThreshold - 1, debug)
      }
      else {
        if (debug)
          println("match against = " + ch + " toBeScored = " + scoreParams(0)._1 + " " + (scoreParams.length - 20) + " -> 1")
        prefixMap.recursiveScore(scoreParams.tail, currentState + ch, currentScore, mismatchThreshold, debug)
      }
    }
    }
  }

  def iterator: Iterator[(String, T)] =
    (for (v <- value.iterator) yield ("", v)) ++
      (for ((chr, m) <- suffixes.iterator;
            (s, v) <- m.iterator) yield (chr +: s, v))

  def +=(kv: (String, T)): this.type = {
    update(kv._1, kv._2);
    this
  }

  def -=(s: String): this.type = {
    remove(s);
    this
  }

  def weightedSearch(weightVector: Array[Double]) {}

  override def empty = new CRISPRPrefixMap[T]
}


// static stuff
object CRISPRPrefixMap extends {

  // stolen from crispr.mit.edu
  val offtargetCoeff = Array[Double](0.0,   0.0,   0.014, 0.0,   0.0,   0.395, 0.317, 0.0,   0.389, 0.079,
    0.445, 0.508, 0.613, 0.851, 0.732, 0.828, 0.615, 0.804, 0.685, 0.583) // ,0.0,   0.0,   0.0) // PAM isn't accounted for

  def zipAndExpand(str: String, trimPam: Boolean = true): List[Tuple2[Char,Double]] = {
    val trimmedVersion = if (trimPam) str.substring(0,str.length) else str

    var tEffects =
      if (offtargetCoeff.length < trimmedVersion.length)
        new Array[Double](trimmedVersion.length - offtargetCoeff.length) ++ offtargetCoeff
    else
        offtargetCoeff.slice(offtargetCoeff.length - trimmedVersion.length, offtargetCoeff.length)
    trimmedVersion.zip(tEffects).toList
  }

  def empty[T] = new CRISPRPrefixMap[T]

  def apply[T](kvs: (String, T)*): CRISPRPrefixMap[T] = {
    val m: CRISPRPrefixMap[T] = empty
    for (kv <- kvs) m += kv
    m
  }

  def newBuilder[T]: Builder[(String, T), CRISPRPrefixMap[T]] =
    new MapBuilder[String, T, CRISPRPrefixMap[T]](empty)

  implicit def canBuildFrom[T] : CanBuildFrom[CRISPRPrefixMap[_], (String, T), CRISPRPrefixMap[T]] =
    new CanBuildFrom[CRISPRPrefixMap[_], (String, T), CRISPRPrefixMap[T]] {
      def apply(from: CRISPRPrefixMap[_]) = newBuilder[T]

      def apply() = newBuilder[T]
    }

  /**
   * generate a tree from an input file where the CRISPR is the first tab seperated entry
   * @param fl the input file
   * @return a trie with the CRISPR targets
   */
  //def fromPath(fl: String, pamDrop: Boolean = true, prefixDrop: Int = 0): Iterator[CRISPRPrefixMap[Int]] =
  //
  //})
  //tree // we've run off the end of the iterator, return what we have

  /**
   * we assume that the name (the 4th column) is the CRISPR entry
   * @param fl the input file
   * @return a trie with the CRISPR targets
   */
  //def fromBed(fl: String, pamDrop: Boolean, prefixDrop: Int = 0): Iterator[CRISPRPrefixMap[Int]] =
  //  new CRISPRPrefixMapIterator(fl,pamDrop,prefixDrop,100000,3,23,sep="\t")
  def fromBed(fl: String, pamDrop: Boolean, prefixDrop: Int = 0): CRISPRPrefixMap[Int] = {
    val tree = new CRISPRPrefixMap[Int]()
    Source.fromFile(fl).getLines().foreach(ln => {
      val sp = ln.split("\t")
      val ray: Int = tree.getOrElse(sp(3),0) + 1
      if (sp.length >= 4)
        tree.put(sp(3), ray )
    })
    return tree
  }

  def CRISPRdistance(guide1: String, guide2: String): Int = {
    guide1.zip(guide2).map { case (c1, c2) => if (c1 == c2) 0 else 1}.sum
  }

  /**
   * score for mismatches against the prefix tree
   * @param hits the string of sequence to search for zipped with the scores
   * @param maxMismatches the maximum number of mismatches, greatly limits the search space
   * @return a mapping from the string of the off-target hit to it's score and entry (of type T, in this case a string)
   */
  def score(hits: Map[String, Tuple3[Double, Int, Int]], maxMismatches: Int = CRISPRPrefixMap.maxMismatch, guideSize: Int = 19): Array[Tuple2[String, Double]] = {
    val distances = hits.keySet.toList.combinations(2).map{
      case (lstOfHits) => { CRISPRPrefixMap.CRISPRdistance(lstOfHits(0), lstOfHits(1)) }
    }.toList

    val meanDistances = (distances.sum.toDouble * 1.0) / math.max(distances.length.toDouble,hits.size)

    hits.map{case (hit, scores) => {
      // see crispr.mit.edu's help page about the equation.  each line is a term in their equation
      val score = new Tuple2[String,Double](hit,(scores._1.toDouble *
        (1.0 / (((guideSize - meanDistances.toDouble) / guideSize) * 4.0 + 1.0)) *
        (1.0 / ((maxMismatches.toDouble-scores._2) * (maxMismatches.toDouble-scores._2)))))

      var ret = new Array[Tuple2[String,Double]](scores._3)
      for (i <- 0 until scores._3 ) ret(i) = score
      ret
    }}.flatten.toArray

  }

  def totalScore(scores: Iterable[Tuple2[String, Double]]): Double = (100.0 / (100.0 + (scores.map{case(s) => s._2}.sum * 100)) * 100.0)

  def toPickle[T: SPickler: FastTypeTag](t: T) = t.pickle
  def fromPickle[T: Unpickler: FastTypeTag](bytes: Array[Byte]) = bytes.unpickle[T]

  val maxMismatch = 5
}
