package crispr

import com.typesafe.scalalogging.LazyLogging
import utils.BaseCombinationGenerator

/**
 * our CRISPR hit
 */
case class CRISPRGuide(contig: String, start: Int, stop: Int, name: String, maxMismatch: Int = 4, binSize: Int = 7) extends LazyLogging {
  var offContigs = List[String]()
  var offStarts = List[Int]()
  var offStops = List[Int]()
  var offBases = List[String]()
  val binBases = name.slice(name.length - binSize, name.length)
  val maxEntries = 1000

  def validBin(bin:String): Boolean = CRISPRGuide.editDistance(bin, binBases, maxMismatch) <= maxMismatch

  // add an off-target hit, private just to avoid confusion
  private def addOffTarget(contig: String, start: Int, stop: Int, bases: String) {
    offContigs :+= contig
    offStarts :+= start
    offStops :+= stop
    offBases :+= bases
    if (offContigs.size == maxEntries) {
      logger.warn("target " + name + " is now dead to us since it has too many off-targets")
    }
  }

  // is this CRISPR target still taking off-target entries? Some targets are just really prevalent in the genome
  // so drop them once they've gone over a certain hit count
  def isAlive(): Boolean = offContigs.size < maxEntries

  def addTarget(oContig: String, oStart: Int, oStop: Int, oString: String) =
    if (CRISPRGuide.editDistance(name,oString,maxMismatch) <= maxMismatch) addOffTarget(oContig,oStart,oStop,oString)

  def toBed(): String = {
    contig + "\t" + start + "\t" + stop + "\t" + name
  }

  def reconstituteOffTargets() : Array[String] = {
    var ret = Array[String]()
    offContigs.zipWithIndex.foreach{case(contig,index) => {
      ret :+= contig + ":" + offStarts(index) + "-" + offStops(index) + "#" + offBases(index)
    }}
    ret
  }
}

object CRISPRGuide {
  // Case sensitive for speed.  The stop parameter saves us a large number of computations
  // over a traditional zip approach since our stop is usually << str1.length
  def editDistance(str1: String, str2: String, stop: Int): Int = {
    if (str1.length != str2.length)
      throw new IllegalArgumentException("Unequal string lengths: str1 = " + str1 + " str2 = " + str2)
    var dist = 0
    for (ind <- 0 until str1.length) {
      if (str1(ind) != str2(ind)) dist +=1
      if (dist > stop) return dist
    }
    dist

    //str1.zip(str2).map { case (b1, b2) => if (b1 == b2) 0 else 1 }.sum
  }
}

