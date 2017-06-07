package scoring

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSiteOT}
import standards.ParameterPack
import standards.Cas9ParameterPack
import reference.CRISPRSite
/**
  * test out the bed annotation tool
  */
class BedAnnotationTest extends FlatSpec with Matchers {


  "BedAnnotation" should "correctly annotation a guide that directly matches a contig" in {
    val bitEncoder = new BitEncoding(Cas9ParameterPack)
    val target = new CRISPRSite("chr8", "GACTTGCATCCGAAGCCGGTGGG", true, 150, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
    crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))
    val bedAnnot = new BedAnnotation()

    bedAnnot.inputBed = Some(Array[File](new File("test_data/test_overlap.bed")))
    val posEnc = new BitPosition()
    bedAnnot.scoreGuides(Array[crispr.CRISPRSiteOT](crisprOT), bitEncoder, posEnc, Cas9ParameterPack)

    (crisprOT.namedAnnotations(bedAnnot.scoreName)) should be (Array[String]("region1"))
    (crisprOT.target.position) should be (150)
  }

  "BedAnnotation" should "correctly not annotation a guide that doesnt match a contig" in {
    val bitEncoder = new BitEncoding(Cas9ParameterPack)
    val target = new CRISPRSite("chr7", "GACTTGCATCCGAAGCCGGTGGG", true, 150000, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
    crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))
    val bedAnnot = new BedAnnotation()

    bedAnnot.inputBed = Some(Array[File](new File("test_data/test_overlap.bed")))
    val posEnc = new BitPosition()
    bedAnnot.scoreGuides(Array[crispr.CRISPRSiteOT](crisprOT), bitEncoder, posEnc, Cas9ParameterPack)

    (crisprOT.namedAnnotations contains bedAnnot.scoreName) should be (false)
    (crisprOT.target.position) should be (150000)
  }

  "BedAnnotation" should "correctly transform a guide to the correct coordinates and then annotate it" in {
    val bitEncoder = new BitEncoding(Cas9ParameterPack)
    val target = new CRISPRSite("1", "GACTTGCATCCGAAGCCGGTGGG", true, 50, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
    crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))
    val bedAnnot = new BedAnnotation()
    bedAnnot.parseOutInterval("chr8:10000-15000")

    bedAnnot.inputBed = Some(Array[File](new File("test_data/test_overlap.bed")))
    val posEnc = new BitPosition()
    bedAnnot.scoreGuides(Array[crispr.CRISPRSiteOT](crisprOT), bitEncoder, posEnc, Cas9ParameterPack)

    (crisprOT.target.position) should be (10000 + 50)
    (crisprOT.namedAnnotations(bedAnnot.scoreName)) should be (Array[String]("region2"))
  }
}
