package modules

import java.io.{File, PrintWriter}

import crispr.models.{ScoreModel, OnTarget, OffTarget}
import crispr.{CRISPRGuide, BinManager, CRISPRGuide$}
import main.scala.bin.BinIterator
import main.scala.trie.CRISPRPrefixMap
import main.scala.util.Utils
import main.scala.util.Utils.fileToString

import main.scala.{CRISPROnTarget, Config}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/16/15.
 */
class TallyScoringInformation(args: Array[String]) {
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

    opt[Int]("targetLength") action {
      (x, c) => c.copy(targetLength = x)
    } text ("The length of a target (20 assumed)")

    opt[Double]("maxGCDev") action {
      (x, c) => c.copy(maxGCDev = x)
    } text ("the maximum amount of deviation from 50% a guide can be (default 30%, so <= 20% or >= 80% GC is filtered out)")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome\n")
    help("help") text ("prints the usage information you see here")
  }
  val logger = LoggerFactory.getLogger("ScoreSites")

  parser.parse(args, ScoreConfig()) map {
    config => {
      // read in each of the target CRISPRs
      // ---------------------------------------------------------------------------------------------------------------------------------------
      println("Loading targets and precomputing hit bins for each (this takes some time) from " + config.targetBed.get.getAbsolutePath)
      var lineCount = 0
      var crisprs = Array[CRISPRGuide]()
      Utils.inputSource(config.targetBed.get.getAbsolutePath).getLines().foreach{ ln => {
        val sp = ln.split("\t")
        if (lineCount % 1000 == 0) println("targets read in so far = " + lineCount)
        lineCount += 1

        if (sp.length > 3 &&
          Utils.gcContent(sp(3).slice(0, 20)) < 0.5 + config.maxGCDev &&
          Utils.gcContent(sp(3).slice(0, 20)) > 0.5 - config.maxGCDev) {
          val guide = sp(3).slice(0, 20)
          crisprs :+= CRISPRGuide(sp(0), sp(1).toInt, sp(2).toInt, guide)
        } else {
          println("Dropped line " + ln)
        }
      }}

      println("creating manager for " + crisprs.size + " guides (post GC filter)")
      // Create a manager for our targets
      val targetManager = new BinManager(crisprs)

      println("walking through genomic sites")
      lineCount = 0
      // now process the input file line by line
      // ---------------------------------------------------------------------------------------------------------------------------------------
      var oldTime = System.nanoTime()
      var lineCounter = 1000000 // 1 million
      var currentBin = ""

      Utils.inputSource(config.genomeBed.get).getLines().foreach { ln => {
        if (lineCount % lineCounter == 0 && lineCount != 0) {
          val curTime = System.nanoTime()
          println("Processed " + lineCount / lineCounter + " million genomic target sites (" + ((curTime - oldTime) / 1000000000) + " seconds per million)")
          oldTime = curTime
        }
        lineCount += 1

        // are we a header line or a normal line?
        if (ln startsWith("#")) {
          currentBin = ln.stripPrefix("#").stripPrefix(" ")
        } else {

          val sp = ln.split("\t")
          if (sp.length > 1) {
            sp(2).split(";").foreach { tgt => {
              val contig = tgt.split(":")(0)
              val startStop = tgt.split(":")(1).split("-").map { tk => tk.toInt }
              targetManager.scoreHit(currentBin, contig, startStop(0), startStop(1), sp(0))
            }}
          }
        }
      }}

      val output = new PrintWriter(config.output.get)
      targetManager.allCRISPRs.foreach { crispr => {
        val offTargetString = crispr.reconstituteOffTargets().mkString("$")
        output.write(crispr.toBed + "\t" + offTargetString + "\n")
      }
      }
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
                       output: Option[File] = None,
                       scoreOffTarget: Boolean = true,
                       scoreOnTarget: Boolean = true,
                       targetLength: Int = 20,
                       maxGCDev: Double = 0.30)