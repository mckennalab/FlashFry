package modules

import java.io.{File, PrintWriter}

import crispr.models.{ScoreModel, OnTarget, OffTarget}
import crispr.{CRISPRGuide, BinManager, CRISPRGuide$}
import main.scala.bin.BinIterator
import main.scala.trie.CRISPRPrefixMap
import main.scala.{CRISPROnTarget, BEDFile, Config}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/16/15.
 */
class ScoreSites(args: Array[String]) {
  // parse the command line arguments
  val parser = new scopt.OptionParser[ScoreConfig]("DeepFry") {
    head("DeepFry", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")

    // *********************************** Inputs *******************************************************
    opt[File]("genomeBed") required() valueName ("<file>") action {
      (x, c) => c.copy(genomeBed = Some(x))
    } text ("the file containing all of the known CRISPR hits")

    opt[File]("targetBed") required() valueName ("<file>") action {
      (x, c) => c.copy(targetBed = Some(x))
    } text ("the file of guides to score")

    opt[File]("output") required() valueName ("<file>") action {
      (x, c) => c.copy(output = Some(x))
    } text ("the output file")

    opt[Boolean]("noOffTarget") action {
      (x, c) => c.copy(scoreOffTarget = !(x))
    } text ("Do not compute off-target hits (a large cost)")

    opt[Boolean]("noOnTarget") action {
      (x, c) => c.copy(scoreOffTarget = !(x))
    } text ("Do not compute on-target hits (a small savings at best)")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome\n")
    help("help") text ("prints the usage information you see here")
  }
  val logger = LoggerFactory.getLogger("ScoreSites")

  parser.parse(args, ScoreConfig()) map {
    config => {
      // read in each of the target CRISPRs
      println("Loading targets and precomputing hit bins for each (this takes some time)")
      var lineCount = 0
      val crisprs = Source.fromFile(config.targetBed.get.getAbsolutePath).getLines().map{ln => {
        val sp = ln.split("\t")
        if (lineCount % 100 == 0) println("targets read in so far = " + lineCount)
        lineCount += 1
        CRISPRGuide(sp(0),sp(1).toInt,sp(2).toInt,sp(3))
      }}.toList

      println("creating manager")
      // Create a manager for our targets
      val targetManager = new BinManager(crisprs)

      println("walking through genome sites")
      lineCount = 0
      // now process the input file line by line
      var oldTime = System.nanoTime()
      val scoreLines = Source.fromFile(config.genomeBed.get).getLines().foreach{ln => {

        if (lineCount % 100000 == 0) {
          val curTime = System.nanoTime()
          println("Processed " + lineCount/100000 + " million genomic target sites in " + ((curTime - oldTime) / 1000000000) + " seconds")
          oldTime = curTime
        }
        lineCount += 1
        val sp = ln.split("\t")
        targetManager.scoreHit(sp(4),sp(0),sp(1).toInt,sp(2).toInt,sp(3))
      }}

      // setup the scoring systems
      val scoreSystem = Array[ScoreModel](new OnTarget(), new OffTarget())

      println("scoring sites")
      // now score each site and output it to the right file
      val output = new PrintWriter(config.output.get)
      targetManager.allCRISPRs.foreach{crispr => {
        println("scoring " + crispr.name)
        val scores = mutable.HashMap[String,String]()
        scoreSystem.foreach{system => {
          scores(system.scoreName()) = system.scoreGuide(crispr)
        }}
        val offTargetString = crispr.reconstituteOffTargets().mkString("$")
        output.write(crispr.toBed + "\t" + scores.map{case(key,value) => key + "=" + value}.mkString(";") + ";" + offTargetString + "\n")
      }}
      output.close()
    }
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class ScoreConfig(analysisType: Option[String] = None,
                       genomeBed: Option[File] = None,
                       targetBed: Option[File] = None,
                       reference: Option[File] = None,
                       output: Option[File] = None,
                       scoreOffTarget: Boolean = true,
                       scoreOnTarget: Boolean = true)