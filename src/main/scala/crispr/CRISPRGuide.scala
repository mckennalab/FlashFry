package crispr

import main.scala.util.BaseCombinations

/**
 * our CRISPR hit
 */
case class CRISPRGuide(contig: String, start: Int, stop: Int, name: String, maxMismatch: Int = 4, binSize: Int = 8) {
  var offContigs = List[String]()
  var offStarts = List[Int]()
  var offStops = List[Int]()
  var offBases = List[String]()
  val binBases = name.slice(name.length - binSize, name.length)
  val maxEntries = 2500

  def validBin(bin:String ): Boolean = CRISPRGuide.editDistance(bin, binBases) <= maxMismatch

  // add an off-target hit, private just to avoid confusion
  private def addOffTarget(contig: String, start: Int, stop: Int, bases: String) {
    offContigs :+= contig
    offStarts :+= start
    offStops :+= stop
    offBases :+= bases
    if (offContigs.size == maxEntries) {
      println("warning: target " + name + " is now dead to us since it has too many off-targets")
    }
  }

  // is this CRISPR target still taking off-target entries? Here to speed everything up a bit
  def isAlive(): Boolean = offContigs.size < maxEntries

  def addTarget(oContig: String, oStart: Int, oStop: Int, oString: String) =
    if (CRISPRGuide.editDistance(name,oString) <= maxMismatch) addOffTarget(oContig,oStart,oStop,oString)

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
  // WARNING CASE SENSITIVE FOR SPEED
  def editDistance(str1: String, str2: String): Int = {
    if (str1.length != str2.length)
      throw new IllegalArgumentException("Unequal string lengths")
    str1.zip(str2).map { case (b1, b2) => if (b1 == b2) 0 else 1 }.sum
  }
}

