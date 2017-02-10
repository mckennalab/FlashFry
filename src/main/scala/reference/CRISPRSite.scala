package reference

// a hit for a CRISPR site in the genome
case class CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, cutSite: Int) extends Ordered[CRISPRSite] {
  val sep = "\t"
  val strandOutput = if (forwardStrand) CRISPRSite.forwardStrandEncoding else CRISPRSite.reverseStrandEncoding

  def to_output = contig + sep + position + sep + cutSite + sep + bases + sep + strandOutput

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
    CRISPRSite(sp(0), sp(3), strandEncoding, sp(1).toInt, sp(2).toInt)
  }
}