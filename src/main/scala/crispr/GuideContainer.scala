package crispr

import java.io.File

import reference.CRISPRSite

/**
  * our interface for guide storage objects
  */
trait GuideContainer {
  def addHit(cRISPRSite: CRISPRSite)
  def close(outputFile: File)
}
