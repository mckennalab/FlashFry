package main.scala

import main.java.com.zunama.Dawg
import org.apache.commons.collections4.trie.PatriciaTrie

import scala.io.Source

object FindTargets extends App {
  val inputFile = "top300K.txt"

  val inputFl = Source.fromFile(inputFile).getLines()
  val testTree = new CRISPRPrefixMap[String]()
  //val dawg = new Dawg()

  var dup = 0
  // load up all of the
  inputFl.foreach(ln => {
    val sp = ln.split("\t")
    if (testTree.contains(sp(0))) dup += 1
    testTree.put(sp(0),sp(0))
  })

  // spin through all of the possible combinations of CRISPR targets of the specified length
  val lengthOfCRISPR = 23
  val terminalPAM = "GG"

  ScoreHit.test()

  // make each possible guide of the specified length
  println(testTree.size)
  println(dup)

  val iter = new RandoCRISPR(21)
  var nt = 0
  var dups = 0
  iter.toStream.takeWhile(_ => nt < 10000000).foreach(ct => {
    nt += 1
    if (("G" + ct + "GG").length != 24) throw new IllegalArgumentException("Not 24 ==> " + ("G" + ct + "GG").length)
    if (nt < 10)
      println(ct)
    if (testTree contains ("G" + ct + "GG"))
      dups += 1
  })
  println(nt)
  println(dups)

  // load up the windowed hits


  /*def scoreOffTarget(sourceCRISPR: String, offTarget: String): Double =
    sourceCRISPR.zipWithIndex.map{case(letter,index) => {if (letter != offTarget(index)) return 1.0 - offtargetCoeff(index) else 1}}.foldLeft(0)(_ * _)*/
}