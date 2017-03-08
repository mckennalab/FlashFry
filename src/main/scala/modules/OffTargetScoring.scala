package modules

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import com.typesafe.scalalogging.LazyLogging
import crispr.CRISPRSiteOT
import main.scala.util.BaseCombinationGenerator
import output.TargetOutput
import reference.{CRISPRSite, ReferenceEncoder}
import reference.binary.{BinaryGuideReader, OrderedBinTraversal}
import reference.filter.{EntropyFilter, HitFilter, maxPolyNTrackFilter}
import reference.gprocess.GuideStorage
import standards.{ParameterPack}

import scala.collection.mutable
import scala.io.Source

/**
 * Scan a fasta file for targets and tally their off-targets against the genome
 */
class OffTargetScoring(args: Array[String]) extends LazyLogging {
  // parse the command line arguments
  val parser = new scopt.OptionParser[DiscoverConfig]("DiscoverOTSites") {
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
    opt[Unit]("markExactGenomeHits") valueName ("<string>") action { (x, c) => c.copy(markTargetsWithExactGenomeHits = true) } text ("should we add a column to indicate that a target has a exact genome hit")
    opt[Int]("maxMismatch")  valueName ("<int>") action { (x, c) => c.copy(maxMismatch = x) } text ("the maximum number of mismatches we allow")
    opt[Int]("flankingSequence")  valueName ("<int>") action { (x, c) => c.copy(flankingSequence = x) } text ("number of bases we should save on each side of the target, used in some scoring schemes (default is 10 on each side)")
    opt[String]("enzyme") valueName ("<string>") action { (x, c) => c.copy(enzyme = x) } text ("which enzyme to use (cpf1, cas9)")

    // some general command-line setup stuff
    note("match off-targets for the specified guides to the genome of interest\n")
    help("help") text ("prints the usage information you see here")
  }

  parser.parse(args, DiscoverConfig()) map {
    config => {

      // get our enzyme's (cas9, cpf1) settings
      val params = ParameterPack.nameToParameterPack(config.enzyme)

      // load up their input file, and scan for any potential targets
      val guideHits = new GuideStorage()
      val encoders = ReferenceEncoder.findTargetSites(new File(config.inputFasta), guideHits, params, standardFilters(), config.flankingSequence)

      // get our position encoder and bit encoder setup
      val positionEncoder = BitPosition.fromFile(config.binaryOTFile + BitPosition.positionExtension)
      val bitEcoding = new BitEncoding(params)

      // transform our targets into a list for off-target collection
      val guideOTStorage = guideHits.guideHits.map{guide => new CRISPRSiteOT(guide,bitEcoding.bitEncodeString(StringCount(guide.bases,1)))}.toArray

      // take this target list and tally against the known binary file
      logger.info("scanning against the known targets from the genome with " + guideHits.guideHits.toArray.size + " guides")

      val mapping = BinaryGuideReader.scanAgainstLinear(new File(config.binaryOTFile),guideOTStorage, config.maxMismatch,params,bitEcoding, positionEncoder)

      logger.info("Writing final output for " + guideHits.guideHits.toArray.size + " guides")

      // given the scoring tools they want to run, score each of the guides with all of the information


      // now output the scores per site
      val tgtOutput = TargetOutput(config.outputFile,
        guideOTStorage,
        config.includePositionOutputInformation,
        config.markTargetsWithExactGenomeHits,
        standardFilters(),bitEcoding,positionEncoder)
      }
  }

  def standardFilters(): Array[HitFilter] = {
    var filters = Array[HitFilter]()
    filters :+= EntropyFilter(1.0)
    filters :+= maxPolyNTrackFilter(6)
    filters
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class DiscoverConfig(analysisType: Option[String] = None,
                          inputFasta: String = "",
                          binaryOTFile: String = "",
                          outputFile: String = "",
                          enzyme: String = "cas9",
                          maxMismatch: Int = 5,
                          includePositionOutputInformation: Boolean = false,
                          markTargetsWithExactGenomeHits: Boolean = false,
                          flankingSequence: Int = 10)