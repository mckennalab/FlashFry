package reference.binary

import java.io.{File, FileOutputStream, PrintWriter}

import bitcoding.{BitEncoding, BitPosition, TargetCodec, WideTargetCodec}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cas9NGGParameterPack, NucleaseProfiles}
import utils.BaseCombinationGenerator

import scala.collection.mutable

class BinaryHeaderVersionTest extends FlatSpec with Matchers {

  "BinaryHeader" should "round-trip a version-2 wide profile definition" in {
    val file = File.createTempFile("flashfry-v2", ".header")
    file.deleteOnExit()
    val positions = new BitPosition()
    positions.addReference("chr1")
    val header = BinaryHeader(BaseCombinationGenerator(1), NucleaseProfiles.enCas12a,
      new BitEncoding(NucleaseProfiles.enCas12a), positions,
      mutable.HashMap("A" -> BlockOffset(0, 0, 0), "C" -> BlockOffset(0, 0, 0),
        "G" -> BlockOffset(0, 0, 0), "T" -> BlockOffset(0, 0, 0)))

    val output = new PrintWriter(new FileOutputStream(file))
    BinaryHeader.writeHeader(header, output)
    output.close()

    val decoded = BinaryHeader.readHeader(file.getAbsolutePath)
    decoded.formatVersion should be (BinaryConstants.version)
    decoded.parameterPack.profile should be (NucleaseProfiles.enCas12a)
    decoded.bitCoder.targetCodec should be (WideTargetCodec)
  }

  it should "continue to read version-1 numeric profile IDs" in {
    val file = File.createTempFile("flashfry-v1", ".header")
    file.deleteOnExit()
    val output = new PrintWriter(new FileOutputStream(file))
    output.println(BinaryConstants.magicNumber)
    output.println(BinaryConstants.legacyVersion)
    output.println(3)
    output.println(4)
    Array("A", "C", "G", "T").foreach(bin => output.println(bin + "=0,0,0"))
    output.println("chr1=1")
    output.close()

    val decoded = BinaryHeader.readHeader(file.getAbsolutePath)
    decoded.parameterPack should be (Cas9NGGParameterPack)
    decoded.bitCoder.targetCodec.id should be (TargetCodec.legacyId)
    decoded.formatVersion should be (BinaryConstants.legacyVersion)
  }
}
