package models

import java.io.File

import crispr.CRISPRSiteOT
import standards.ParameterPack

/**
  * 'score' each target with information from a bed file

class BedAnnotation(inputBedFile: File, useInGenomeLocations: Boolean = false) extends ScoreModel {
  val mUseGLocation = useInGenomeLocations

  require(inputBedFile.exists(), "The input bed file doesn't exist: " + inputBedFile.getAbsolutePath)

  val bedIntervals => {

  }

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "BedAnnotator"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "Annotated with overlaps to bed file " + inputBedFile.getAbsolutePath

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuide(guide: CRISPRSiteOT): String = ???

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme
    * @return
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = ???
}
*/