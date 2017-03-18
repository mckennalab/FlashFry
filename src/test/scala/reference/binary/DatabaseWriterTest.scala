package reference.binary

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9ParameterPack
import utils.BaseCombinationGenerator

/**
  * This is more of an integration test -- can we read and write blocks of data from disk
  */
class DatabaseWriterTest extends FlatSpec with Matchers {

  /**
    * def writeToBinnedFileSet(inputSortedBed: File,
                           output: String,
                           bitEncoder: BitEncoding,
                           positionEncoder: BitPosition,
                           binGenerator: BaseCombinationGenerator,
                           parameterPack: ParameterPack,
                           maxGenomicLocationsPerTarget: Int = 1000) {

    */
  val inputSortedBed = new File("test_data/test_block_for_GTGTTAACC.txt")
  val output = "test_data/test_block_for_GTGTTAACC_output.txt"
  val encoder = new BitEncoding(Cas9ParameterPack)
  val posEnc = new BitPosition()
  posEnc.addReference("chr22")
  val binGen = BaseCombinationGenerator(9)

  "BinaryWriteReadTest" should "successfully write a block to disk" in {
    DatabaseWriter.writeToBinnedFileSet(inputSortedBed,output,encoder,posEnc,binGen,Cas9ParameterPack)

    val header = BinaryHeader.readHeader(output + BinaryHeader.headerExtension,encoder)
    val blockOffsets = header.blockOffsets("GTGTTAACC")
    (blockOffsets.blockPosition) should be (0)
    (blockOffsets.uncompressedSize) should be (96)
    (blockOffsets.numberOfTargets) should be (6)


  }


}
