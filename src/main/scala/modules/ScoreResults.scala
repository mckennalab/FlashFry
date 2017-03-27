package modules

import com.typesafe.scalalogging.LazyLogging

/**
  * Given a results bed file from off-target discovery, annotate it with scores using established scoring schemes
  */
class ScoreResults(args: Array[String]) extends LazyLogging {

  // parse the command line arguments
  val parser = new OffTargetBaseOptions()

  parser.parse(args, DiscoverConfig()) map {
    config => {

    }
  }
}


/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class ScoreConfig(analysisType: Option[String] = None,
                          inputFasta: String = "",
                          binaryOTFile: String = "",
                          outputFile: String = "",
                          enzyme: String = "cas9",
                          maxMismatch: Int = 5,
                          includePositionOutputInformation: Boolean = false,
                          markTargetsWithExactGenomeHits: Boolean = false,
                          flankingSequence: Int = 10,
                          maximumOffTargets: Int = 2000,
                          forceLinear: Boolean = false,
                          numberOfThreads: Int = 1)


class ScoreBaseOptions extends scopt.OptionParser[DiscoverConfig]("DiscoverOTSites") {
  head("DiscoverOTSites", "1.0")

  // *********************************** Inputs *******************************************************
  opt[String]("analysis") required() valueName ("<string>") action {
    (x, c) => c.copy(analysisType = Some(x))
  } text ("The run type: one of: discovery, score")

  // *********************************** Inputs *******************************************************
  opt[String]("inputFasta") required() valueName ("<string>") action { (x, c) => c.copy(inputFasta = x) } text ("the reference file to scan for putitive targets")
  opt[String]("binaryOTFile") required() valueName ("<string>") action { (x, c) => c.copy(binaryOTFile = x) } text ("the binary off-target file")
  opt[String]("outputFile") required() valueName ("<string>") action { (x, c) => c.copy(outputFile = x) } text ("the output file (in bed format)")
  opt[Unit]("positionalOTOutput") valueName ("<string>") action { (x, c) => c.copy(includePositionOutputInformation = true) } text ("include the position information of off-target hits")
  opt[Unit]("forceLinear") valueName ("<string>") action { (x, c) => c.copy(forceLinear = true) } text ("force the run to use a linear traversal of the bins; really only good for testing")
  opt[Unit]("markExactGenomeHits") valueName ("<string>") action { (x, c) => c.copy(markTargetsWithExactGenomeHits = true) } text ("should we add a column to indicate that a target has a exact genome hit")
  opt[Int]("maxMismatch") valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches we allow")
  opt[Int]("flankingSequence") valueName ("<int>") action { (x, c) => c.copy(flankingSequence = x) } text ("number of bases we should save on each side of the target, used in some scoring schemes (default is 10 on each side)")
  opt[Int]("maximumOffTargets") valueName ("<int>") action { (x, c) => c.copy(maximumOffTargets = x) } text ("the maximum number of off-targets for a guide, after which we stop adding new off-targets")
  // opt[Int]("numberOfThreads") valueName ("<int>") action { (x, c) => c.copy(numberOfThreads = x) } text ("the number of threads to use")
  opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")

  // some general command-line setup stuff
  note("match off-targets for the specified guides to the genome of interest\n")
  help("help") text ("prints the usage information you see here")
}