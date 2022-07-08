/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package scoring

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.{CRISPRSite, CRISPRSiteOT}
import standards.ParameterPack
import utils.BEDFile

import scala.collection.mutable
import scala.io.Source


/**
  * 'score' each target with information from a bed file
  *
  **/
class BedAnnotation() extends ScoreModel {

  var inputBed = ""

  var genomeTransform = ""

  var inputBedFiles = Array[File]()
  var inputBedNames = Array[String]()

  var isRemapping = false
  val oldContigTag = "oldContig"

  var mappingIntervals: scala.Option[mutable.HashMap[String, Tuple4[String, Int, Int, String]]] = None

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "BedAnnotator"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Annotated with overlaps to bed file " + inputBedFiles.map { bd => bd.getAbsolutePath }.mkString(",")

  /**
    * load up the BED file and annotate each guide with information from any intersecting annotations
    *
    * @param guides the guides with off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guides: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack) {
    // remap the intervals given the genome offset
    if (mappingIntervals.isDefined) {
      mappingIntervals.foreach { intervals =>
        intervals.foreach { interval => {

          guides.foreach { guide => {
            // can we find a mapping of the guide position to the original position in the reference
            val ref = guide.target.contig

            if (mappingIntervals.isDefined && mappingIntervals.get.contains(ref)) {
              val newPos = mappingIntervals.get(ref)

              val oldTarget = guide.target
              val newTarget = CRISPRSite(newPos._1, oldTarget.bases, oldTarget.forwardStrand, oldTarget.position + newPos._2, oldTarget.sequenceContext)
              guide.target = newTarget
              guide.namedAnnotations(oldContigTag) = guide.namedAnnotations.getOrElse(oldContigTag, Array[String]()) :+ ref
            }
          }
          }
        }
        }
      }
    }

    // are there annotations that overlap it? -- this is a bit ugly, but I like the isDefined approach over the zip then map here
    assert(inputBedFiles.size == inputBedNames.size)
    inputBedFiles.zip(inputBedNames).foreach { case (bedObj, bedName) => {
      (new BEDFile(bedObj)).foreach(bedEntry => {
        bedEntry.foreach { entry => {
          guides.foreach { guide => {
            if (guide.target.overlap(entry.contig, entry.start, entry.stop)) {
              guide.namedAnnotations(bedName) = guide.namedAnnotations.getOrElse(bedName, Array[String]()) :+ entry.name
            }
          }
          }
        }
        }
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
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  override def setup(): Unit = {
    if (inputBed != "") {
      inputBed.split(",").foreach { bedFile => {
        assert(bedFile contains ":", "Bedfile command line argument " + bedFile + " doesn't contain both a name and a file")
        val nameAndFile = bedFile.split(":")
        assert(nameAndFile.size == 2, "Bedfile command line argument " + bedFile + " doesn't contain both a name and a file")

        require((new File(nameAndFile(1))).exists(), "The input bed file doesn't exist for a name/file pair: " + inputBed)

        inputBedFiles = inputBedFiles :+ new File(nameAndFile(1))
        inputBedNames = inputBedNames :+ nameAndFile(0)
      }
      }
    } else {
      throw new IllegalStateException("If you request bed annotation you must provide a valid BED file")
    }

    if (genomeTransform != "") {
      parseOutInterval(genomeTransform)
      isRemapping = true
    }
  }


  /**
    * store the specified adjustments to the fasta positions
    *
    * @param interval
    */
  def parseOutInterval(interval: String) {
    val intervalMapping = new mutable.HashMap[String, Tuple4[String, Int, Int, String]]()

    Source.fromFile(interval).getLines().foreach {
      line => {
        val matches = line.split("\t")
        assert(matches.size == 4, "The interval " + interval + " didn't parse into a four part interval, instead " + matches.size)
        intervalMapping(matches(3)) =
          (matches(0),
            matches(1).toInt,
            matches(2).toInt,
            matches(3))
      }
    }

    mappingIntervals = Some(intervalMapping)
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {
    // we don't need one
  }
  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = if (isRemapping) {
    inputBedNames :+ oldContigTag
  } else {
    inputBedNames
  }
}

