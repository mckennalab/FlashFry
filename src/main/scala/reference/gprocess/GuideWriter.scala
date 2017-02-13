package reference.gprocess

import java.io.File

import reference.CRISPRSite

/**
  * our interface for guide storage / writers
  */
trait GuideWriter {
  def addHit(cRISPRSite: CRISPRSite)
  def close(outputFile: File)
}
