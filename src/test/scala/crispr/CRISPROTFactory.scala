package crispr

object CRISPROTFactory {
  /**
    * create a fake OT site for testing
    * @return the fake crispr OT object
    */
  def createFakeCRISPROT(name: String): CRISPRSiteOT = {
    // CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, sequenceContext: Option[String])
    val newCrisprSite = CRISPRSite(name,"ACGTACGTACGTACGTACGTCGG",true,0,Some("ACGTACGTTTACGTACGTACGTACGTACGTCGGACGTACGTTT"))

    // (tgt: CRISPRSite, encoding: Long, overflow: Int)
    new CRISPRSiteOT(newCrisprSite,0L,1000)
  }

}
