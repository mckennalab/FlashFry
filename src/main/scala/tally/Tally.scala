package tally

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import main.scala.Config
import main.scala.scopt.OptionParser
import reference.{ReferenceBinnedWriter, ReferenceDictReader, ReferenceEncoder}

/**
  * Created by aaronmck on 2/9/17.
  */
object Tally extends App {

  // parse the command line arguments
  val parser = new OptionParser[TallyConfig]("Tally") {
    head("TallyScores", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("referenceDict") required() valueName ("<string>") action { (x, c) => c.copy(referenceDict = x) } text ("the reference file")
    opt[String]("sortedHits") required() valueName ("<string>") action { (x, c) => c.copy(sortedHits = x) } text ("the sorted hits file")
    opt[String]("output") required() valueName ("<string>") action { (x, c) => c.copy(output = x) } text ("the output file")
    opt[String]("pam") valueName ("<string>") action { (x, c) => c.copy(pam = x) } text ("the pam, with no degenerate bases")
    opt[Int]("seqLength")  valueName ("<int>") action { (x, c) => c.copy(len = x) } text ("the length of the sequence")
    opt[Unit]("fivePrime") valueName ("<boolean>") action { (x, c) => c.copy(fivePrime = true) } text ("is the PAM on the 5 prime")
  }

  // now use the parsed configuration options to run the tool
  parser.parse(args, TallyConfig()) map {
    config => {
      //ReferenceEncoder.encodeFile(new File(config.reference.get), new File(config.output.get), config.len, config.pam, config.fivePrime)


      /* write the data out to a binned file
      def writeToBinnedFile(inputSortedBed: String,
                            output: String,
                            bitEncoder: BitEncoding,
                            positionEncoder: BitPosition,
                            binSize: Int = 9,
                            pam: String,
                            fivePrimePam: Boolean = false): Unit = { */
      val ref = ReferenceDictReader(config.referenceDict)

      val bEncode = new BitEncoding(config.len + config.pam.size)
      val pEncode = ref.generateBitPosition()

      ReferenceBinnedWriter.writeToBinnedFile(config.sortedHits,
        config.output,
        bEncode,pEncode,9,config.pam,config.fivePrime)
    }}

}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class TallyConfig(referenceDict: String = "", output: String = "", sortedHits: String = "", fivePrime: Boolean = false, len: Int = 21, pam: String = "GG")
