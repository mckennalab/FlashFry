package scoring

import java.io.File

import bitcoding.{BitEncoding, BitPosition, PositionInformation}
import crispr.CRISPRSiteOT
import modules.{DiscoverConfig, OffTargetBaseOptions}
import scopt.{OptionParser, PeelParser}
import standards.ParameterPack
import utils.BEDFile

import scala.collection.mutable
import scala.io.Source

/**
  * 'score' each target with information from a bed file
  *
  **/
class BedAnnotation() extends ScoreModel {
  var mUseGLocation = false
  var inputBed: Option[File] = None

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "BedAnnotator"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Annotated with overlaps to bed file " + inputBed.get.getAbsolutePath

  /**
    * load up the BED file and annotate each guide with information from any intersecting annotations
    *
    * @param guides the guides with off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition) {
    // make a mapping of positions to guides -- if useInGenomeLocations is set, try to find any 0 mismatch candidates
    val positionToGuide = new mutable.HashMap[PositionInformation, CRISPRSiteOT]()

    if (mUseGLocation) {
      guides.map { guide => {
        guide.offTargets.filter(zh => bitEnc.mismatches(zh.sequence, guide.longEncoding) == 0).
          flatMap { case (t) => t.coordinates }.
          foreach { position => positionToGuide(posEnc.decode(position)) = guide }
      }
      }
    } else {
      guides.map { guide => {
        val pos = PositionInformation(guide.target.contig, guide.target.position, guide.target.bases.size, guide.target.forwardStrand)
        positionToGuide(pos) = guide
      }
      }
    }

    (new BEDFile(inputBed.get)).foreach(bedEntry => {
      bedEntry.map { entry => {
        positionToGuide.foreach { case (pos, site) =>
          if (pos.overlap(entry.contig, entry.start, entry.stop))
            site.namedAnnotations(scoreName()) = site.namedAnnotations.getOrElse(scoreName(), Array[String]()) :+ entry.outputString
        }
      }
      }
    })
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
    *                             the real genome positions - if this parameter is set lookup any 0 mismatch hits and put their
    *                             annotation info onto the guide. A way to recover genomic info
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = {
    val parser = new BedAnnotationOptions()

    val remaining = parser.parse(args, BedConfig()) map {
      case(config,remainingParameters) => {
        require((new File(config.inputBed)).exists(), "The input bed file doesn't exist: " + config.inputBed)
        inputBed = Some(new File(config.inputBed))

        if (config.useInGenomeLocations) mUseGLocation = true

        remainingParameters
      }
    }
    remaining.getOrElse(Seq[String]())
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
                          useInGenomeLocations: Boolean = false)

class BedAnnotationOptions extends PeelParser[BedConfig]("") {
  opt[String]("inputAnnotationBed") required() valueName ("<string>") action { (x, c) => c.copy(inputBed = x) } text ("the bed file we'd like to annotate with")
  opt[Unit]("useInGenomeLocations") valueName ("<string>") action { (x, c) => c.copy(useInGenomeLocations = true) } text ("Try to find our genome location by using matching zero-mismatch in-genome targets")
}