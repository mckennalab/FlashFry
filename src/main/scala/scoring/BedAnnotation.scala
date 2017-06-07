/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package scoring

import java.io.File

import bitcoding.{BitEncoding, BitPosition, PositionInformation}
import crispr.CRISPRSiteOT
import modules.{DiscoverConfig, OffTargetBaseOptions}
import reference.CRISPRSite
import scopt.{OptionParser, PeelParser}
import standards.ParameterPack
import utils.BEDFile

import scala.collection.mutable
import scala.io.Source
import scala.util.matching.Regex

/**
  * 'score' each target with information from a bed file
  *
  **/
class BedAnnotation() extends ScoreModel {
  var inputBed: Option[Array[File]] = None
  val invervalRegex: Regex = """([\w\d]+):(\d+)-(\d+)""".r
  var mappingInterval: Option[Tuple3[String, Int, Int]] = None

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "BedAnnotator"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Annotated with overlaps to bed file " + inputBed.get.map{bd => bd.getAbsolutePath}.mkString(",")

  /**
    * load up the BED file and annotate each guide with information from any intersecting annotations
    *
    * @param guides the guides with off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    // remap the intervals given the genome offset
    if (mappingInterval.isDefined) {
      val interval = mappingInterval.get

      guides.foreach { guide => {
        val oldTarget = guide.target
        val newTarget = CRISPRSite(interval._1, oldTarget.bases, oldTarget.forwardStrand, oldTarget.position + interval._2, oldTarget.sequenceContext)
        guide.target = newTarget
      }
      }
    }

    inputBed.get.foreach{ bedObj => {
      (new BEDFile(bedObj)).foreach(bedEntry => {
        bedEntry.map { entry => {
          guides.foreach { guide => {
            if (guide.target.overlap(entry.contig, entry.start, entry.stop))
              guide.namedAnnotations(scoreName()) = guide.namedAnnotations.getOrElse(scoreName(), Array[String]()) :+ entry.name
          }}
        }}
      })
    }
    }
  }

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme
    * @return
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * Our parameters:
    *
    * inputBedFile         the bed file to annotate with
    * useInGenomeLocations sometimes we don't encode an input fasta with the appropriate contig info (so it doesn't have)
    * the real genome positions - if this parameter is set lookup any 0 mismatch hits and put their
    * annotation info onto the guide. A way to recover genomic info
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    val parser = new BedAnnotationOptions()

    val remaining = parser.parse(args, BedConfig()) map {
      case (config, remainingParameters) => {
        config.inputBed.split(",").foreach { bedFile => {
          require((new File(bedFile)).exists(), "The input bed file doesn't exist: " + config.inputBed)
          if (!inputBed.isDefined)
            inputBed = Some(Array[File](new File(bedFile)))
          else
            inputBed = Some(inputBed.get :+ new File(bedFile))

          if (config.genomeTransform != "NONE")
            parseOutInterval(config.genomeTransform)

        }
        }
        remainingParameters
      }
    }
    remaining.getOrElse(Seq[String]())
  }

  def parseOutInterval(interval: String) {
    val matches = invervalRegex.findAllMatchIn(interval).toArray
    assert(matches.size == 1, "The interval " + interval + " didn't parse into a single interval")
    mappingInterval = Some(matches(0).group(1),
      matches(0).group(2).toInt,
      matches(0).group(3).toInt)
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {} // we don't need one
}


/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class BedConfig(inputBed: String = "",
                     genomeTransform: String = "NONE")

class BedAnnotationOptions extends PeelParser[BedConfig]("") {
  opt[String]("inputAnnotationBed") required() valueName ("<string>") action { (x, c) => c.copy(inputBed = x) } text ("the bed file we'd like to annotate with")
  opt[String]("transformPositions") valueName ("<string>") action { (x, c) => c.copy(genomeTransform = x) } text ("Try to find our genome location by using matching zero-mismatch in-genome targets")
}