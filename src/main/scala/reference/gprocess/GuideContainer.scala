package reference.gprocess

import java.io.File

import reference.CRISPRSite

/**
  * our interface for guide storage / writers
  */
trait GuideContainer {
  def addHit(cRISPRSite: CRISPRSite)
  def close(outputFile: File)
}
