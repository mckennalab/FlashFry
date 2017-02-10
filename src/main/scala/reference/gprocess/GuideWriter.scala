package reference.gprocess

import java.io.File

import reference.CRISPRSite

/**
  * Created by aaronmck on 2/9/17.
  */
trait GuideWriter {
  def addHit(cRISPRSite: CRISPRSite)
  def close(outputFile: File)
}
