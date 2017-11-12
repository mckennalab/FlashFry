package reference.binary

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSite
import utils.BaseCombinationGenerator
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cpf1ParameterPack, ParameterPack}

/**
  * Created by aaronmck on 2/11/17.
  */
class BinaryFullCircleTest extends FlatSpec with Matchers {
  val bitEncoder = new BitEncoding(Cpf1ParameterPack)
  val posEncoder = new BitPosition()
  posEncoder.addReference("chr22")
  val generator = BaseCombinationGenerator(9)

  "BinaryFullCircleTest" should "write and read " in {
    val inputFile = "test_data/6_target_with_various_counts.txt"
    val outputFile = "test_data/6_target_with_various_counts.binary"

    //BinaryTargetStorage.writeToBinnedFile(inputFile, outputFile, bitEncoder, posEncoder, generator, StandardScanParameters.cpf1ParameterPack)

    //BinaryGuideDatabase.scanAgainst(new File(outputFile), Array[CRISPRSiteOT](),StandardScanParameters.cpf1ParameterPack, bitEncoder, 5, posEncoder)
  }
}
