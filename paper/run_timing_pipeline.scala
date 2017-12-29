/** *********************************************
  * Requires Java 1.8. On the UW machines this means
  * running at least: "module load java/7u17", or the appropriate
  * module if there's a more recent version, to load java 1.7 into
  * your environment.  The best idea is to place this into your
  * bash/shell profile
  *
  *
  * Copyright (c) 2014, 2015, 2016, aaronmck
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice, this
  * list of conditions and the following disclaimer.
  * 2.  Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * @author Aaron
  * @date June, 2015
  *
  */

package org.broadinstitute.gatk.queue.qscripts

import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.picard._
import org.broadinstitute.gatk.queue.util.QScriptUtils
import org.broadinstitute.gatk.queue.function.ListWriterFunction
import org.broadinstitute.gatk.utils.commandline.Hidden
import org.broadinstitute.gatk.utils.commandline

import collection.JavaConversions._
import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMFileHeader.SortOrder
import scala.io.Source
import scala.collection.immutable._
import java.io.File

/**
  * Given amplicon reads from a CRISPR experiment, align and call events over those cut sites.
  *
  * PLEASE READ:
  *
  * - This pipeline assumes your reference file, say myref.fa, has the following additional files in the same dir:
  * - myref.fa.cutSites <-- contains the CRISPR target seq, position start, the cutsite position
  * - myref.fa.primers <-- two lines, containing the positive strand primers on both ends of your amplicon.
  *
 **/
class DNAQC extends QScript {
  qscript =>

  /** **************************************************************************
    * Data Parameters
    * ************************************************************************** */

  @Argument(doc = "Deep fry java jar file", fullName = "jarFile", shortName = "jarFile", required = false)
  var fryJar: String = "/net/shendure/vol10/projects/DeepFry/nobackup/paper_analysis/bin/FlashFry-assembly-1.2.jar"

  @Argument(doc = "where to put all the output", fullName = "outputDir", shortName = "outputDir", required = false)
  var outputDir: String = "/net/shendure/vol10/projects/DeepFry/nobackup/paper_analysis/May28th_bwa_vs_flashfry_nrg/"

  @Argument(doc = "FlashFry database", fullName = "binaryFile", shortName = "binaryFile", required = false)
  var binaryFile: String = "/net/shendure/vol8/projects/spiny.mouse.genome/nobackup/human_hg19_cas9_bin7_v8_all_index"

  @Argument(doc = "enzyme name", fullName = "enzyme", shortName = "enzyme", required = false)
  var enzyme: String = "spcas9"

  // Gracefully hide Queue's output -- this is very important otherwise there's Queue output everywhere and it's a mess
  val queueLogDir: String = ".qlog/"

  // ** **************************************************************************
  // * Main script entry point
  // * **************************************************************************
  def script() {

    val targetCounts = Array[Int](1,10 ,100,1000,10000,100000)
    val targetIterations = Array[Char]('a' ,'b','c','d','e',   'f','g','h','i','j',   'k','l','m','n','o',   'p','q','r','s','t',    'u','v','w','x','y')
    val mismatches = Array[Int](1 ,2,3,4,5)

    targetCounts.foreach{count => {
      targetIterations.foreach{iteration => {
        val randomSitesOutput = outputDir + count + "_" + iteration + "_target.txt"
        val randomFastqSitesOutput = outputDir + count + "_" + iteration + "_target.fastq"

        add(Randomer(count, randomSitesOutput))
        add(FastaToFastq(randomSitesOutput, randomFastqSitesOutput))

        mismatches.foreach{mismatch => {
          val mismatchTally = outputDir + count + "_" + iteration + "_target.txt.mismatch" + mismatch
          val alnFile = new File(outputDir + "count" + count + "_mismatch" + mismatch + "_iter" + iteration + ".aln")
          val samFile = new File(outputDir + "count" + count + "_mismatch" + mismatch + "_iter" + iteration + ".sam")

          add(Discover(randomSitesOutput, mismatch, mismatchTally, count))

          // we've already collected this data
          add(BWAAlign(randomFastqSitesOutput, alnFile, mismatch, count, iteration.toString))
          add(BWAAMap(samFile, alnFile,randomFastqSitesOutput))
        }}
      }}
    }}
  }

  /** **************************************************************************
    * Helper classes and methods
    * ************************************************************************** */

  // if a directory doesn't exist, create it. Otherwise just return the dir. If anything fails, exception out.
  def dirOrCreateOrFail(dir: File, contentsDescription: String): File = {
    if (!dir.exists()) {
      println("Trying to make dir " + dir)
      if (!dir.mkdirs()) // mkdirs tries to make all the parent directories as well, if it can
        throw new IllegalArgumentException("Unable to find or create " + contentsDescription + " directory: " + dir)
      else
        println("created directory : " + dir.getAbsolutePath)
    } else {
      //println("directory exists! " + dir)
    }
    dir
  }


  /** **************************************************************************
    * traits that get tacked onto runnable objects
    * ************************************************************************** */

  // General arguments to non-GATK tools
  trait ExternalCommonArgs extends CommandLineFunction {
    this.memoryLimit = 6 // set this a bit high, there's a weird java / SGE interactions that require higher mem reservations
    this.residentRequest = 6
    this.residentLimit = 6
    this.isIntermediate = false // by default delete the intermediate files, if you want to keep output from a task set this to false in the case class
  }

   // ********************************************************************************************************
  case class Randomer(numSites: Int, outputFile: File) extends ExternalCommonArgs {
    @Argument(doc = "the input stats file") var numberOfSites = numSites
    @Output(doc = "new per base event style") var outputFl = outputFile

    var command = "java -Xmx8g -jar " + fryJar.getAbsolutePath() + " --analysis random --outputFile " + outputFl.getAbsolutePath + " --enzyme " + enzyme + " --randomCount " + numSites + " --onlyUnidirectional"
    def commandLine = command

    this.analysisName = queueLogDir + outputFile + ".randomer"
    this.jobName = queueLogDir + outputFile + ".randomer"
    this.memoryLimit = 10
    this.residentRequest = 10
    this.residentLimit = 10
  }


   // ********************************************************************************************************
  case class Discover(inputFile: File, mismatch: Int, outputFile: File, guideCount: Int) extends ExternalCommonArgs {
    @Argument(doc = "allowed number of mismatches") var misMt = mismatch
    @Input(doc = "the input fasta file") var inputFl = inputFile
    @Output(doc = "new per base event style") var outputFl = outputFile

    val memory = if (guideCount > 10000) "-Xmx31g" else "-Xmx15g"


    var command = "java " + memory + " -jar " + fryJar.getAbsolutePath() +
    " --fasta " + inputFl.getAbsolutePath + " --database " + binaryFile +
    " --output " + outputFl + " --maxMismatch " + misMt + " --analysis discover"

    def commandLine = command

    this.analysisName = queueLogDir + outputFile + ".tally"
    this.jobName = queueLogDir + outputFile + ".tally"

     if (guideCount > 10000) {
       this.memoryLimit = 32
       this.residentRequest = 32
       this.residentLimit = 32
     } else {
       this.memoryLimit = 16
       this.residentRequest = 16
       this.residentLimit = 16
     }
  }

  // ********************************************************************************************************
  case class FastaToFastq(inputFastaFile: File, outputFastqFile: File) extends ExternalCommonArgs {
    @Input(doc = "the input fasta file") var inputFile = inputFastaFile
    @Output(doc = "new per base event style") var outputFile = outputFastqFile

    val fastaConverter = "/net/shendure/vol10/projects/DeepFry/nobackup/paper_analysis/bin/fasta_to_fastq.scala"

    def commandLine = "scala " + fastaConverter + " " + inputFile + " " + outputFile

    this.analysisName = queueLogDir + outputFile + ".convert"
    this.jobName = queueLogDir + outputFile + ".convert"
    this.memoryLimit = 2
    this.residentRequest = 2
    this.residentLimit = 2
  }

  // ********************************************************************************************************
  case class BWAAlign(inputFastqFile: File, outputFile: File, mismatches: Int, countVal: Int, iter: String) extends ExternalCommonArgs {
    @Argument(doc = "allowed number of mismatches") var mismatch = mismatches
    @Argument(doc = "allowed number of count") var count = countVal
    @Argument(doc = "iteration value") var iteration = iter
    @Input(doc = "the input fasta file") var inputFile = inputFastqFile
    @Output(doc = "new per base event style") var output = outputFile

    val humanRef = "/net/shendure/vol10/nobackup/shared/alignments/bwa-0.6.1/human_g1k_hs37d5/hs37d5.fa"
    

    def commandLine = "time bwa aln -o 0 -m 20000000 -n " + mismatch + " -k " + mismatch + " -N -l 20 " + humanRef + " " + inputFile + " > " + output

    this.analysisName = queueLogDir + output + ".align"
    this.jobName = queueLogDir + output + ".align"
    this.memoryLimit = 6
    this.residentRequest = 6
    this.residentLimit = 6
  }


  // ********************************************************************************************************
  case class BWAAMap(samFile: File, alnFile: File, fastqFile: File) extends ExternalCommonArgs {
    @Output(doc = "the output sam file") var sam = samFile
    @Input(doc = "the input alignment file") var aln = alnFile
    @Input(doc = "the input alignment file") var fastq = fastqFile

    val humanRef = "/net/shendure/vol10/nobackup/shared/alignments/bwa-0.6.1/human_g1k_hs37d5/hs37d5.fa"
    
    def commandLine = "time bwa samse -n 10000000 -f " + sam + " " + humanRef + " " + aln + " " + fastq

    this.analysisName = queueLogDir + samFile + ".map"
    this.jobName = queueLogDir + samFile + ".map"
    this.memoryLimit = 10
    this.residentRequest = 10
    this.residentLimit = 10
  }
}
//87298ab6-cb8c-4489-aecd-eaad490178c4
