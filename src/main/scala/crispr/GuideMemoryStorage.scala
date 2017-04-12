package crispr

import java.io.File

import reference.CRISPRSite

import scala.collection.mutable

/**
  * a simple in-memory storage for guides
  */
class GuideMemoryStorage extends GuideContainer {

  val guideHits = new mutable.ArrayBuffer[CRISPRSite]()
  override def addHit(cRISPRSite: CRISPRSite): Unit = guideHits += cRISPRSite

  override def close(): mutable.HashMap[String,File] = {new mutable.HashMap[String,File]()}
}
