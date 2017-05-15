package modules

import java.io.File

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.ResultsAggregator
import scoring.ScoringManager
import standards.ParameterPack
import targetio.{TargetInput, TargetOutput}
import scopt._

/**
  * Given a results bed file from off-target discovery, annotate it with scores using established scoring schemes
  */
class ScoreResults extends LazyLogging with Module {

  def runWithOptions(args: Seq[String]) {
    // parse the command line arguments
    val parser = new ScoreBaseOptions()

    parser.parse(args, ScoreConfig()) map {
      case(config,remainingParameters) => {

        // get our settings
        val params = ParameterPack.nameToParameterPack(config.enzyme)

        // make ourselves a bit encoder
        val bitEnc = new BitEncoding(params)

        // load up the scored sites into a container
        logger.info("Loading input BED file into CRISPR objects.. ")
        val posEncoderAndOffTargets = TargetInput.inputBedToTargetArray(new File(config.inputBED), bitEnc, 2000)

        // get a scoring manager
        val scoringManager = new ScoringManager(bitEnc, posEncoderAndOffTargets._1, config.scoringMetrics, args.toArray)

        // score all the sites
        logger.info("Scoring all guides...")
        val newGuides = scoringManager.scoreGuides(posEncoderAndOffTargets._2)

        val results = new ResultsAggregator(newGuides)

        // output a new data file with the scored results
        logger.info("Writing annotated guides to the output file...")
        TargetOutput.output(config.outputBED, results, true, false, bitEnc, posEncoderAndOffTargets._1, scoringManager.scoringAnnotations)

      }
    }
  }
}


/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class ScoreConfig(analysisType: Option[String] = None,
                       inputBED: String = "",
                       outputBED: String = "",
                       enzyme: String = "spCas9",
                       scoringMetrics: Seq[String] = Seq())


class ScoreBaseOptions extends PeelParser[ScoreConfig]("DiscoverOTSites") {
  // *********************************** Inputs *******************************************************
  opt[String]("analysis") required() valueName ("<string>") action {
    (x, c) => c.copy(analysisType = Some(x))
  } text ("The run type: one of: discovery, score")

  // *********************************** Inputs *******************************************************
  opt[String]("inputBED") required() valueName ("<string>") action { (x, c) => c.copy(inputBED = x) } text ("the reference file to scan for putitive targets")
  opt[String]("outputBED") required() valueName ("<string>") action { (x, c) => c.copy(outputBED = x) } text ("the output file (in bed format)")
  opt[Seq[String]]("scoringMetrics") required() valueName("<scoringMethod1>,<scoringMethod1>...") action{ (x,c) => c.copy(scoringMetrics = x) } text ("scoring methods to include")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("match off-targets for the specified guides to the genome of interest\n")
}