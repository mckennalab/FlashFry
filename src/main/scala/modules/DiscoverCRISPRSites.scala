package modules

import java.io.{PrintWriter, File}

import crispr.PAMBuffer
import main.scala.util.BaseCombinations
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Source

/**
 * Created by aaronmck on 6/16/15.
 */
class DiscoverCRISPRSites(args: Array[String]) {
  // parse the command line arguments
  val parser = new scopt.OptionParser[DiscoverConfig]("DeepFry") {
    head("DeepFry", "1.0")

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score")


    // *********************************** Inputs *******************************************************
    opt[File]("inputFasta") required() valueName ("<file>") action {
      (x, c) => c.copy(inputFasta = Some(x))
    } text ("the fasta to scan for CRISPR hits")

    opt[File]("hitsFile") required() valueName ("<file>") action {
      (x, c) => c.copy(hitsFile = Some(x))
    } text ("the output file of each hit seen in the genome")

    opt[File]("binedoutput") required() valueName ("<file>") action {
      (x, c) => c.copy(binedoutput = Some(x))
    } text ("the binned output BED file")

    opt[Boolean]("requirePAM") action {
      (x, c) => c.copy(requirePAM = !(x))
    } text ("Do not compute off-target hits (a large cost)")

    opt[Boolean]("keepFullChrName") action {
      (x, c) => c.copy(dropPostSpace = !(x))
    } text ("do we want to keep the full line of the chromosome in the fasta as the name; the default is to split on spaces and use the first word")

    opt[Int]("seqSize") action {
      (x, c) => c.copy(seqSize = x)
    } text ("the size of the CRISPR sequence to look for (including the PAM)")

    opt[Int]("basesToBinBy") action {
      (x, c) => c.copy(binSize = x)
    } text ("the size of bases to bin by, potential targets are binned by the X bases closest to the PAM, which allows for quicker lookup.")

    opt[Int]("filePasses") action {
      (x, c) => c.copy(filePasses = x)
    } text ("how many passes we should take over the file.  Lowering this ups the number bins we use per pass (raising memory requirements)")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome\n")
    help("help") text ("prints the usage information you see here")
  }

  val logger = LoggerFactory.getLogger("Main")

  parser.parse(args, DiscoverConfig()) map {
    config => {
      var cs = new PAMBuffer(config.seqSize)
      var currentContig = ""

      val output = new PrintWriter(config.hitsFile.get.getAbsolutePath)

      var basesAdded = 0L
      Source.fromFile(config.inputFasta.get.getAbsolutePath).getLines().foreach { line => {

        // if we're processing a chromosome line
        if (line.startsWith(">")) {
          logger.info("processing chromosome " + line)
          currentContig = line.stripPrefix(">").stripPrefix(" ")
          if (config.dropPostSpace)
            currentContig = currentContig.split(" ")(0)
          cs = new PAMBuffer(config.seqSize)
        }

        // else process a line of sequence bases
        else {
          basesAdded += line.length
          cs.addLine(line).foreach { case (hit, position, watson) =>
            if (watson)
              output.write(currentContig + "\t" + (position - config.seqSize) + "\t" + (position - 3) + "\t" + hit + "\n")
            else
              output.write(currentContig + "\t" + (position - (config.seqSize - 3)) + "\t" + (position) + "\t" + hit + "\n")
          }
        }
        if (basesAdded % 10000000 < 50)
          println(basesAdded)
      }
      }

      output.close()

      /** now bin each CRISPR we find into an Nmer ending.
          We'll do a 20th of the genome at a time, which should balance the memory usage with the
          number of passes over the target file
        **/
      println("bin and sort")
      val binCount = math.pow(4,config.binSize)
      val binsAtOnce = math.ceil(binCount / config.filePasses).toInt

      println(binCount)
      println(binsAtOnce)

      val binnedOutput = new PrintWriter(config.binedoutput.get)

      //val tp = writer.compound().getInferredAnonType(classOf[HDF5CompoundDataList])

      (new BaseCombinations(config.binSize)).grouped(binsAtOnce).foreach{bins => {
        val holdingPens = new mutable.HashMap[String,mutable.HashMap[String,Array[BedEntry]]]()
        bins.foreach{bin => holdingPens(bin) = new mutable.HashMap[String,Array[BedEntry]]()}
        println("reading file " + bins.length)

        Source.fromFile(config.hitsFile.get.getAbsolutePath).getLines().foreach{line => {
          val sp = line.split("\t")
          val lastX = sp(3).slice(sp(3).length - 3 - config.binSize, sp(3).length - 3)
          val nonPAM = sp(3).slice(0,sp(3).length - 3)

          // wow, this approach is 100X slower, it's crazy:
          // if (bins contains lastX) {
          //  holdingPens(lastX) += line
          //}
          val pen = holdingPens.get(lastX)
          if (pen.isDefined) {
            val old = pen.get.get(nonPAM)
            val entry = BedEntry(sp(0),sp(1).toInt,sp(2).toInt,nonPAM)
            if (old.isDefined)
              (pen.get)(nonPAM) :+= entry
            else
              (pen.get)(nonPAM) = Array[BedEntry](entry)

          }

        }}

        bins.foreach{bin => {
          val mydata = holdingPens(bin).values.toList
          // now for each entry, output a strandard bed, then the bin, then the count, then the other positions
          mydata.foreach{entry => {
            if (entry.size == 1)
              binnedOutput.write(entry(0).toFullString + "\t" + bin + "\t1\t" + ".\n")
            else
              binnedOutput.write(entry(0).toFullString + "\t" + bin + "\t" + entry.size + "\t" + entry.slice(1,entry.size).map{t => t.toShortString}.mkString(";") + "\n")
          }}
        }}
      }}


    }
  }
}

case class BedEntry(contig: String, start: Int, stop: Int, name: String) extends Serializable {
  def toFullString():String  = contig + "\t" + start + "\t" + stop + "\t" + name
  def toShortString():String = contig + ":" + start + "-" + stop
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class DiscoverConfig(inputFasta: Option[File] = None,
                          hitsFile: Option[File] = None,
                          binedoutput: Option[File] = None,
                          requirePAM: Boolean = true,
                          seqSize: Int = 23,
                          analysisType: Option[String] = None,
                          dropPostSpace: Boolean = true,
                          binSize: Int = 8,
                          filePasses: Int = 20)