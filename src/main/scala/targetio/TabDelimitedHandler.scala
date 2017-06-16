package targetio

import java.io.{File, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSiteOT}
import reference.CRISPRSite
import scoring.ScoreModel
import utils.Utils

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * handle transforming data to and from our tab-delimited output format
  */
object TabDelimitedOutput {
  val headerComment = "# "
  val sep = "\t"

  val forward = "FWD"
  val reverse = "RVS"
  val overflow = "OVERFLOW"
  val targetOK = "OK"

  val inGenomeTag = "IN_GENOME_"
  val novelTag = "NOVEL"

  val positionForward = "F"
  val positionReverse = "R"

  val contigSeperator = ":"
  val strandSeperator = "\\^"

  val offTargetSeperator = ","
  val withinOffTargetSeperator = "_"
  val positionListTerminatorFront = "<"
  val positionListTerminatorBack = ">"
  val positionListSeperator = "\\|"

  val default_columns = Array[String]("contig", "start", "stop", "target", "context", "overflow", "orientation")
  val final_columns = Array[String]("otCount", "offTargets")


  val contigPos = 0
  val startPos = 1
  val stopPos = 2
  val targetPos = 3
  val contextPos = 4
  val overflowPos = 5
  val orientationPos = 6

  val setColumnCount = 8

}

/**
  * write our results to disk
  *
  * @param outputFile    the file to write to; end it with .gz if you want it compressed
  * @param bitEncoding   our bit encoder for targets
  * @param bitPosition   our position bit encoder
  * @param scoringModels the models we've scored with
  * @param writeOTs      should we write the off-targets?
  * @param writePositons should we write the off-target positions with the off-targets?
  */
class TabDelimitedOutput(outputFile: File,
                         bitEncoding: BitEncoding,
                         bitPosition: BitPosition,
                         scoringModels: Array[ScoreModel],
                         writeOTs: Boolean,
                         writePositons: Boolean) {

  val models = scoringModels

  val output = if (outputFile.getAbsolutePath().endsWith(".gz")) {
    new PrintWriter(Utils.gos(outputFile.getAbsolutePath))
  } else {
    new PrintWriter(outputFile.getAbsolutePath)
  }

  // output the header
  output.write((TabDelimitedOutput.default_columns ++
    scoringModels.flatMap { case (sc) => sc.headerColumns() }).mkString(TabDelimitedOutput.sep))

  if (writeOTs)
    output.write(TabDelimitedOutput.sep + TabDelimitedOutput.final_columns.mkString(TabDelimitedOutput.sep) + "\n")
  else
    output.write(TabDelimitedOutput.sep + TabDelimitedOutput.final_columns.slice(0, 1).mkString(TabDelimitedOutput.sep) + "\n")


  /**
    *
    * @param guide add a guide to the output
    */
  def write(guide: CRISPRSiteOT): Unit = {
    output.write(guide.target.contig + TabDelimitedOutput.sep)
    output.write(guide.target.start + TabDelimitedOutput.sep)
    output.write((guide.target.start + guide.target.length) + TabDelimitedOutput.sep)
    output.write((guide.target.bases) + TabDelimitedOutput.sep)
    output.write((guide.target.sequenceContext.getOrElse("NONE")) + TabDelimitedOutput.sep)
    output.write((if (guide.full) TabDelimitedOutput.overflow else TabDelimitedOutput.targetOK) + TabDelimitedOutput.sep)
    output.write((if (guide.target.forwardStrand) TabDelimitedOutput.forward else TabDelimitedOutput.reverse) + TabDelimitedOutput.sep)

    models.foreach { model =>
      output.write(model.headerColumns().map{col => guide.namedAnnotations.getOrElse(col,Array[String]("NONE"))}.map{t => t.mkString(",")}.mkString(TabDelimitedOutput.sep) + TabDelimitedOutput.sep)
    }

    output.write(guide.offTargets.size + TabDelimitedOutput.sep)

    if (writeOTs)
      output.write(guide.offTargets.map { ot => ot.toOutput(bitEncoding, bitPosition, guide.longEncoding, writePositons) }.mkString(TabDelimitedOutput.offTargetSeperator) + "\n")
    else
      output.write("\n")
  }

  def close(): Unit = {
    output.close()
  }
}


/**
  * turn a file into an array builder of crisprOT objects
  *
  * @param inputFile   the input file
  * @param bitEncoding our bit encoder for targets
  * @param bitPosition our bit encoder for target positions
  */
class TabDelimitedInput(inputFile: File,
                        bitEncoding: BitEncoding,
                        bitPosition: BitPosition,
                       maximumMismatches: Int) {

  val input = if (inputFile.getAbsolutePath().endsWith(".gz")) {
    Source.fromInputStream(Utils.gis(inputFile.getAbsolutePath)).getLines()
  } else {
    Source.fromFile(inputFile.getAbsolutePath).getLines()
  }

  // read in the header and figure out what the file includes
  val headerLine = input.next().split(TabDelimitedOutput.sep)

  assert(headerLine.size > TabDelimitedOutput.default_columns.size + TabDelimitedOutput.final_columns.size - 1, "Header line not long enough for file: " + inputFile)
  assert(TabDelimitedOutput.default_columns.zip(headerLine.slice(0, TabDelimitedOutput.default_columns.size)).map { case (a, b) => if (a == b) 0 else 1 }.sum == 0,
    "Mismatched line doesn't contain the standard header tokens: " + inputFile)

  // now figure out what the remaining columns are
  val remainingColumns = headerLine.slice(TabDelimitedOutput.default_columns.size, headerLine.size)

  val withOTs = (remainingColumns(remainingColumns.size - 2) == TabDelimitedOutput.final_columns(0) &&
    remainingColumns(remainingColumns.size - 1) == TabDelimitedOutput.final_columns(1))

  assert(withOTs || (remainingColumns(remainingColumns.size - 1) == TabDelimitedOutput.final_columns(0)), "Unable to parse out the final columns in the header")

  val annotations = if (withOTs) remainingColumns.slice(0, remainingColumns.size - 2) else remainingColumns.slice(0, remainingColumns.size - 1)

  // now make an array of all the target sites
  val guides = new ArrayBuffer[CRISPRSiteOT]()

  input.foreach { ln => {
    val sp = ln.split(TabDelimitedOutput.sep)

    val site = CRISPRSite(sp(TabDelimitedOutput.contigPos),
      sp(TabDelimitedOutput.targetPos),
      sp(TabDelimitedOutput.orientationPos) == TabDelimitedOutput.forward,
      sp(TabDelimitedOutput.startPos).toInt,
      if (sp(TabDelimitedOutput.contextPos) == "NONE") None else Some(sp(TabDelimitedOutput.contextPos)))

    val ot = new CRISPRSiteOT(site, bitEncoding.bitEncodeString(sp(TabDelimitedOutput.targetPos)),
      if (sp(TabDelimitedOutput.overflowPos) == "OK")
        (sp((TabDelimitedOutput.setColumnCount - 1) + annotations.size).toInt + 1)
      else
        (sp((TabDelimitedOutput.setColumnCount - 1) + annotations.size).toInt)
    )

    (0 until annotations.size).foreach(anIndex => ot.namedAnnotations(annotations(anIndex)) = Array[String](sp(7 + anIndex)))

    if (withOTs) {
      sp(sp.size - 1).split(TabDelimitedOutput.offTargetSeperator).foreach { token => {

        val offTargetSeq = token.split(TabDelimitedOutput.withinOffTargetSeperator)(0)
        val offTargetCount = token.split(TabDelimitedOutput.withinOffTargetSeperator)(1).toInt
        val offTargetMismatches = token.split(TabDelimitedOutput.withinOffTargetSeperator)(2).toInt

        if (offTargetMismatches <= maximumMismatches) {

          // we're encoding positional information
          if (token contains TabDelimitedOutput.positionListTerminatorFront) {
            val targetAndPositions = token.split(TabDelimitedOutput.positionListTerminatorFront)

            val positions = targetAndPositions(1).stripSuffix(TabDelimitedOutput.positionListTerminatorBack).split(TabDelimitedOutput.positionListSeperator).map { positionEncoded => {
              bitPosition.encode(positionEncoded.split(TabDelimitedOutput.contigSeperator)(0),
                positionEncoded.split(TabDelimitedOutput.contigSeperator)(1).split(TabDelimitedOutput.strandSeperator)(0).toInt,
                offTargetSeq.size,
                positionEncoded.split(TabDelimitedOutput.strandSeperator)(1) == "F")
            }
            }

            assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
            val otHit = new CRISPRHit(bitEncoding.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), positions)
            if (!ot.full)
              ot.addOT(otHit)
          } else {
            assert(offTargetCount <= Short.MaxValue, "The count was too large to encode in a Scala Short value")
            val otHit = new CRISPRHit(bitEncoding.bitEncodeString(StringCount(offTargetSeq, offTargetCount.toShort)), Array[Long]())
            if (!ot.full)
              ot.addOT(otHit)
          }
        }
      }
      }
    }
    guides += ot
  }
  }
}
