package crispr

import java.io.File

import reference.CRISPRSite

import scala.collection.mutable

/**
  * Created by aaronmck on 2/9/17.
  */
class GuideStorage extends GuideContainer {

  val guideHits = new mutable.ArrayBuffer[CRISPRSite]()
  override def addHit(cRISPRSite: CRISPRSite): Unit = guideHits += cRISPRSite

  override def close(outputFile: File): Unit = {/* haha do nothing */}
}
