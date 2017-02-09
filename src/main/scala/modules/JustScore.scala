package modules

import java.io.{PrintWriter, File}

import crispr.models.{OffTarget, OnTarget, ScoreModel}
import crispr.{BedReader, BinManager, CRISPRGuide}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/29/15.
 */
class JustScore(args: Array[String]) {
  // parse the command line arguments
  val parser = new scopt.OptionParser[ScoreCfg]("DeepFry") {
    head("DeepFry", "1.0")

    // *********************************** Requied for the lower level management class *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")

    // *********************************** Inputs *******************************************************
    opt[File]("inputBed") required() valueName ("<file>") action {
      (x, c) => c.copy(inputBed = Some(x))
    } text ("the file of guides to score")

    opt[File]("outputBed") required() valueName ("<file>") action {
      (x, c) => c.copy(outputBed = Some(x))
    } text ("the output file")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome\n")
    help("help") text ("prints the usage information you see here")
  }
  val logger = LoggerFactory.getLogger("ScoreSites")

  parser.parse(args, ScoreCfg()) map {
    config => {

      var oldTime = System.nanoTime()
      // setup the scoring systems
      val scoreSystem = Array[ScoreModel](new OnTarget(), new OffTarget())

      val targetManager = new BedReader(config.inputBed.get)

      println("scoring sites")
      // now score each site and output it to the right file
      oldTime = System.nanoTime()
      val output = new PrintWriter(config.outputBed.get)

      targetManager.hits.foreach { crispr => {

        val scores = mutable.HashMap[String, String]()
        scoreSystem.foreach { system => {
          scores(system.scoreName()) = system.scoreGuide(crispr)
        }
        }
        val offTargetString = crispr.reconstituteOffTargets().mkString("$")
        val curTime = System.nanoTime()
        println("scored " + crispr.name + " in " + ((curTime - oldTime) / 1000000000) + " seconds")
        oldTime = curTime
        output.write(crispr.toBed + "\t" + crispr.isAlive() + "\t" + scores.map { case (key, value) => key + "=" + value }.mkString(";") + ";offtarget-count=" + (crispr.reconstituteOffTargets().length) + ";" + offTargetString + "\n")
      }
      }
      output.close()
    }
  }
}

case class ScoreCfg(analysisType: Option[String] = None,
                       inputBed: Option[File] = None,
                       outputBed: Option[File] = None)