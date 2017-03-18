package reference.traverser

import java.io.File

import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRSiteOT, GuideMemoryStorage}
import org.scalatest.{FlatSpec, Matchers}
import reference.CRISPRSite
import reference.binary.{BinaryHeader, DatabaseWriter}
import reference.traversal.OrderedBinTraversalFactory
import standards.Cas9ParameterPack
import utils.BaseCombinationGenerator

/**
  * check that our test traversal works -- this is really an integration test
  */
class SeekTraverserTest extends FlatSpec with Matchers {

  val inputSortedBed = new File("test_data/test_block_for_GTGTTAACC.txt")
  val output = "test_data/test_block_for_GTGTTAACC_output"
  val encoder = new BitEncoding(Cas9ParameterPack)
  val posEnc = new BitPosition()
  posEnc.addReference("chr22")
  val binGen = BaseCombinationGenerator(9)

  "SeekTraverser" should "successfully write and read a block to and from disk" in {
    DatabaseWriter.writeToBinnedFileSet(inputSortedBed,output,encoder,posEnc,binGen,Cas9ParameterPack)

    val header = BinaryHeader.readHeader(output + BinaryHeader.headerExtension,encoder)
    val blockOffsets = header.blockOffsets("GTGTTAACC")
    (blockOffsets.blockPosition) should be (0)
    (blockOffsets.uncompressedSize) should be (96)
    (blockOffsets.numberOfTargets) should be (6)

    // transform our targets into a list for off-target collection
    val guideHits = new GuideMemoryStorage()
    guideHits.addHit(CRISPRSite("chr22","GTGTTAACCCTAGGAAAATGTGG",true,0,None))
    val guideOTStorage = guideHits.guideHits.map {
      guide => new CRISPRSiteOT(guide, encoder.bitEncodeString(StringCount(guide.bases, 1)), 10000)
    }.toArray

    // get our traversal
    val traversal = new OrderedBinTraversalFactory(header.binGenerator, 1, encoder, 0.90, guideOTStorage)

    SeekTraverser.scan(new File(output),header,traversal.iterator,guideOTStorage,1,Cas9ParameterPack,encoder,posEnc)
    guideOTStorage(0).offTargets.foreach{ot => println(ot.sequence)}
    (guideOTStorage.size) should be (1)
    (guideOTStorage(0).offTargets.size) should be (1)

  }


}

