package bitcoding

/**
  * Created by aaronmck on 6/6/17.
  */
// everything you might want to know about a target's position in the genome
trait PositionInformation {
  def contig: String
  def start: Int
  def length: Int
  def forwardStrand: Boolean

  def overlap(oContig: String, startPos: Int, endPos: Int): Boolean = {
    if (contig != oContig) return false
    (start < startPos && startPos < (start + length) && (start < (startPos + endPos)) ||
      start >= startPos && start < (startPos + endPos) && (startPos < (start + length)))
  }

  def overlap(pInfo: PositionInformation): Boolean = {
    if (contig != pInfo.contig) return false
    (start < pInfo.start && pInfo.start < (start + length) && (start < (pInfo.start + pInfo.length)) ||
      start >= pInfo.start && start < (pInfo.start + pInfo.length) && (pInfo.start < (start + length)))

  }

}

case class PositionInformationImpl(contig: String, start: Int, length: Int, forwardStrand: Boolean) extends PositionInformation {
}
