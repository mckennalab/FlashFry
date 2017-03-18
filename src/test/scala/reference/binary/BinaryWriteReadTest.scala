package reference.binary

import java.io.{File, FileInputStream}
import java.nio.ByteOrder
import java.nio.channels.FileChannel

import bitcoding.{BitEncoding, BitPosition}
import utils.BaseCombinationGenerator
import org.scalatest.{FlatSpec, Matchers}
import standards.Cpf1ParameterPack

/**
  * test that we can get the expected fields from a binary representation of target sequences
  */
class BinaryWriteReadTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cpf1ParameterPack)
  val posEncoder = new BitPosition()
  posEncoder.addReference("chr22")
  val generator = BaseCombinationGenerator(9)
    /*
  "BinaryWriteReadTest" should "write and read " in {
    val inputFile = "test_data/6_target_with_various_counts.txt"
    val outputFile = "test_data/6_target_with_various_counts.binary"

    DatabaseWriter.writeToBinnedFileSet(new File(inputFile),outputFile,bitEncoder,posEncoder,generator,Cpf1ParameterPack)

    val stream = new FileInputStream(outputFile)
    val inChannel = stream.getChannel()

    // read the header of our file into a buffer
    val buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size())
    buffer.order( ByteOrder.BIG_ENDIAN )
    val longBuffer = buffer.asLongBuffer( )

    val headerResult = new Array[Long](3)
    longBuffer.get(headerResult)

    // check that the first three longs are what we expect
    (headerResult(0)) should be (BinaryConstants.magicNumber)
    (headerResult(1)) should be (BinaryConstants.version)
    (headerResult(2)) should be (math.pow(4,9).toLong)

    {
      // now read in the count table
      val lookuptable = new Array[Long](headerResult(2).toInt * 2)
      longBuffer.get(lookuptable)

      // number of targets
      (lookuptable(0)) should be(56)

      // offset into the file for the first bin
      (lookuptable(1)) should be(4194328)
    }


    // now get our 56 entries, and make sure the guides match up
    val guideTable = new Array[Long](56)
    longBuffer.get(guideTable)

    (guideTable(0)) should be(9847226138361856l)
    (guideTable(35)) should be(1121501860331521l)
  }            */
}