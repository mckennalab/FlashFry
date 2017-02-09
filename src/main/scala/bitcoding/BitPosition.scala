package bitcoding

import scala.collection.mutable

/**
  * the bit position mapping
  */
class BitPosition {

  val contigMap = new mutable.HashMap[String,Int]()
  val indexToContig = new mutable.HashMap[Int,String]()
  var nextSeqId = 1
  var mask = 0xFFFFFFFF

  def addReference(refName: String): Unit = {
    contigMap(refName) = nextSeqId
    indexToContig(nextSeqId) = refName
    nextSeqId += 1
  }

  def encode(refName: String, position: Int): Long = {
    if (!(contigMap contains refName))
      throw new IllegalStateException("Reference name is not in map: " + refName)
    val contigNumber: Int = contigMap(refName)
    (contigNumber.toLong << 32) | position.toLong
  }

  def decode(encoding: Long): Tuple2[String,Int] = {
    val contig = indexToContig((encoding >> 32).toInt)
    (contig,(encoding & mask).toInt)
  }
}
