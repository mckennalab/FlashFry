package reference.binary

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import main.scala.util.BaseCombinationGenerator
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import reference.CRISPRSite
import standards.{ParameterPack, StandardScanParameters}

/**
  * Created by aaronmck on 2/11/17.
  */
class BinaryFullCircleTest extends FlatSpec with ShouldMatchers {
  val bitEncoder = new BitEncoding(StandardScanParameters.cpf1ParameterPack)
  val posEncoder = new BitPosition()
  posEncoder.addReference("chr22")
  val generator = BaseCombinationGenerator(9)

  "BinaryWriteReadTest" should "write and read " in {
    val inputFile = "test_data/6_target_with_various_counts.txt"
    val outputFile = "test_data/6_target_with_various_counts.binary"

    BinarySiteWriter.writeToBinnedFile(inputFile, outputFile, bitEncoder, posEncoder, generator, StandardScanParameters.cpf1ParameterPack)

    ScanAgainstBinary.scanAgainst(new File(outputFile), Array[CRISPRSite](), 5, StandardScanParameters.cpf1ParameterPack, bitEncoder, posEncoder)
  }
}
