package reference

import bitcoding.BitPosition

import scala.io.Source

/**
  * give us an iterator over sequences
  */
case class ReferenceDictReader(ref: String) extends Iterable[ReferenceEntry] {
  var seqList = Array[ReferenceEntry]()

  val sequences = Source.fromFile(ref).getLines().foreach{line => {
    if (line startsWith "@SQ") {
      val sp = line.split("\t")
      seqList :+= ReferenceEntry(sp(1).stripPrefix("SN:"),sp(2).stripPrefix("LN:").toInt)
    }
  }}

  override def iterator: Iterator[ReferenceEntry] = seqList.toIterator

  def generateBitPosition(): BitPosition = {
    val ret = new BitPosition()
    iterator.foreach{refEntry => ret.addReference(refEntry.seqName)}
    ret
  }
}

case class ReferenceEntry(seqName: String, length: Int)