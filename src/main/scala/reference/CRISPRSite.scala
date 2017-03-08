package reference

/**
  * This class describes a specific sequence we've found in the genome that matches our target criteria (has the appropriate
  * PAM sequence, etc).
  * @param contig the chromosome or contig we found it in
  * @param bases the bases that make up the target, in the orientation expected (i.e. reverse complement negative strand options)
  * @param forwardStrand are we on the forward or reverse strand?
  * @param position the position we found this sequence at within the contig
  * @param sequenceContext add a broader context to the candidate target. We expect the target to approximately centered (or at least embedded with) the context
  */
case class CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, sequenceContext: Option[String]) extends Ordered[CRISPRSite] {
  val sep = "\t"
  val strandOutput = if (forwardStrand) CRISPRSite.forwardStrandEncoding else CRISPRSite.reverseStrandEncoding

  def to_output = contig + sep + position + sep + (position + bases.length) + sep + bases + sep + strandOutput

  def compare(that: CRISPRSite): Int = (this.bases) compare (that.bases)

}

object CRISPRSite {
  val forwardStrandEncoding = "F"
  val reverseStrandEncoding = "R"
  def header = "contig\tposition\tcutsite\tbases\tstrand\n"

  def fromLine(line: String): CRISPRSite = {
    val sp = line.split("\t")
    val strandEncoding = sp(4) match {
      case `forwardStrandEncoding` => true
      case `reverseStrandEncoding` => false
      case _ => throw new IllegalStateException("Unable to parse strand encoding: " + sp(4))
    }
    CRISPRSite(sp(0), sp(3), strandEncoding, sp(1).toInt, None)
  }
}