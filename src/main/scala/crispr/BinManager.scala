package crispr

import scala.collection.mutable.ListBuffer

/**
 * Created by aaronmck on 6/18/15.
 */
class BinManager(crisprList: List[CRISPRGuide]) {
  var currentBin = "UNKNOWN"
  val allCRISPRs = crisprList

  var currentScores = List[CRISPRGuide]()

  def scoreHit(bin: String, contig: String, start: Int, stop: Int, target: String) {
    if (bin != currentBin)
      reorganizeBin(bin)
    currentScores.foreach(hit => hit.addTarget(contig,start,stop,target))
  }

  def reorganizeBin(newBin: String): Unit = {
    currentBin = newBin
    //print("old bin size = " + currentScores.size)
    val listbuild = new ListBuffer[CRISPRGuide]()

    crisprList.foreach(hit => {
      if (hit.isAlive() && hit.validBin(newBin))
        listbuild += hit
    })
    currentScores = listbuild.result()
    //println("; new bin size = " + currentScores.size)
  }
}
