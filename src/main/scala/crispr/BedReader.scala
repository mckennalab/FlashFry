package crispr

import java.io.File
import org.slf4j.LoggerFactory

import scala.io._

/**
 * Created by aaronmck on 6/29/15.
 */
case class BedReader(input: File, maxOffTargets: Int = 2500) {
  var hits = List[CRISPRGuide]()
  val logger = LoggerFactory.getLogger("Main")

  // open the input file
  Source.fromFile(input.getAbsoluteFile).getLines().foreach { line => {
    val sp = line.split("\t")
    val bedEntry = CRISPRGuide(sp(0), sp(1).toInt, sp(2).toInt, sp(3))
    logger.info("adding " + sp(3))
    if (sp.length > 4 && sp(4) != "") {
      val splits = sp(4).split("\\$")
      if (splits.size < maxOffTargets) {
        splits.foreach { ht => {
          val tokens = ht.split("\\#")
          val contig = tokens(0).split(":")(0)
          val startStop = tokens(0).split(":")(1)
          bedEntry.addTarget(contig, startStop.split("-")(0).toInt, startStop.split("-")(1).toInt, tokens(1))
        }
        }
        hits :+= bedEntry
      } else {
        logger.warn("Dropping target " + sp(3) + " as it has too many off-targets (" + splits.size + ">=" + maxOffTargets + ")")
      }
    }
  }
  }
}
