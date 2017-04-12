package crispr

import java.io.File

import reference.CRISPRSite

import scala.collection.mutable

/**
  * our interface for guide storage objects
  */
trait GuideContainer {
  def addHit(cRISPRSite: CRISPRSite)
  def close(): mutable.HashMap[String,File]
}
