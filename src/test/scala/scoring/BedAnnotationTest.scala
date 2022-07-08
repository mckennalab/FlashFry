package scoring

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import standards.ParameterPack
import standards.Cas9ParameterPack
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

    bedAnnot.inputBedFiles = Array[File](new File("test_data/test_overlap.bed"))
    bedAnnot.inputBedNames = Array[String]("test")

    val posEnc = new BitPosition()
    bedAnnot.scoreGuides(Array[crispr.CRISPRSiteOT](crisprOT), bitEncoder, posEnc, Cas9ParameterPack)

    (crisprOT.namedAnnotations("test")) should be (Array[String]("region1"))
    (crisprOT.target.position) should be (150)
  }

  "BedAnnotation" should "throw an exception when no BED is provided" in {
    val bitEncoder = new BitEncoding(Cas9ParameterPack)
    val target = new CRISPRSite("chr8", "GACTTGCATCCGAAGCCGGTGGG", true, 150, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
    crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))
    val bedAnnot = new BedAnnotation()
    assertThrows[java.lang.IllegalStateException] {
      bedAnnot.setup()
    }

  }

  "BedAnnotation" should "correctly not annotation a guide that doesnt match a contig" in {
    val bitEncoder = new BitEncoding(Cas9ParameterPack)
    val target = new CRISPRSite("chr7", "GACTTGCATCCGAAGCCGGTGGG", true, 150000, None)
    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)),1000)
    crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount("CACTAGCATCCCAGGCCGGTGGG",1)),Array[Long]()))
    val bedAnnot = new BedAnnotation()

    bedAnnot.inputBedFiles = Array[File](new File("test_data/test_overlap.bed"))
    bedAnnot.inputBedNames = Array[String]("test")

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
    bedAnnot.parseOutInterval("test_data/remap_1_to_chr8:10000-15000.bed")

    bedAnnot.inputBedFiles = Array[File](new File("test_data/test_overlap.bed"))
    bedAnnot.inputBedNames = Array[String]("test")
    val posEnc = new BitPosition()
    bedAnnot.scoreGuides(Array[crispr.CRISPRSiteOT](crisprOT), bitEncoder, posEnc, Cas9ParameterPack)

    (crisprOT.target.position) should be (10000 + 50)
    (crisprOT.namedAnnotations("test")) should be (Array[String]("region2"))
  }
}
