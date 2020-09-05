package reference.binary.blocks

import java.io.{DataInputStream, DataOutputStream, File}

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRSite, CRISPRSiteOT, ResultsAggregator}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cas9ParameterPack}
import utils.{BaseCombinationGenerator, RandoCRISPR}

import scala.collection.mutable
import scala.util.Random

/**
  * test out the new block formats -- the linear traversal and the sub-indexed block traversal
  * will get us the same results
  */
class BlockManagerTest extends FlatSpec with Matchers with LazyLogging {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val posEncoder = new BitPosition()
  posEncoder.addReference("chr22")
  val generator = BaseCombinationGenerator(8)

  val prefix = "AACCTTGG"
  val testFile = new File("test_data/test_block" + prefix + ".binary")

  "BlockTest" should "search a linear and indexed array and produce the same results" in {
    val randoGenerator = new RandoCRISPR(20, Array[String]("CGG"), false, prefix, 0, 0,None)

    val guides = (0 until 1000).map { case (ind) => {
      bitEncoder.bitEncodeString(randoGenerator.next().fullTarget)
    }
    }.toArray

    // if we need to generate a new random file, uncomment this line
    //writeRandomBlockToDisk(testFile, prefix, 10000, bitEncoder)

    val linearBlock = generateRandomLinearBlockOfTargets()
    val indexedBlock = generateRandomIndexedBlockOfTargets(prefix, bitEncoder, 4)

    val block = new BlockManager(prefix.size, 4, bitEncoder)
    val initialTime = System.nanoTime()

    val linearAggreagator = new ResultsAggregator(guides.map{g => new CRISPRSiteOT(CRISPRSite("chr22",bitEncoder.bitDecodeString(g).str,true,1,None),g,1000)})
    val indexedAggreagator = new ResultsAggregator(guides.map{g => new CRISPRSiteOT(CRISPRSite("chr22",bitEncoder.bitDecodeString(g).str,true,1,None),g,1000)})

    BitEncoding.allComparisons = 0 // reset the comparison counter
    block.compareBlock(linearBlock._1, linearBlock._2, linearAggreagator.indexedGuides, linearAggreagator, bitEncoder, 1, bitEncoder.binToLongComparitor(prefix))
    logger.info("Total linear  runtime " + "%.2f".format((System.nanoTime() - initialTime) / 1000000000.0) + " seconds; comparisons = " + BitEncoding.allComparisons)

    val initialTime2 = System.nanoTime()
    BitEncoding.allComparisons = 0 // reset the comparison counter
    val indexResults = block.compareBlock(indexedBlock._1, indexedBlock._2, indexedAggreagator.indexedGuides, indexedAggreagator, bitEncoder, 1, bitEncoder.binToLongComparitor(prefix))
    logger.info("Total indexed runtime " + "%.2f".format((System.nanoTime() - initialTime2) / 1000000000.0) + " seconds; comparisons = " + BitEncoding.allComparisons)

    (indexedAggreagator.wrappedGuides.size) should be (linearAggreagator.wrappedGuides.size)

    (indexedAggreagator.wrappedGuides.zip(linearAggreagator.wrappedGuides)).zipWithIndex.foreach{case((lguide, iguide),index) => {
    }}

  }


  /**
    * generate a random linear block of guides -- here we're faking it and just reading from disk and add a ID on the front
    * @return a block encoding matching the linear format
    */
  def generateRandomLinearBlockOfTargets(): Tuple2[Array[Long], Int] = {

    val block = readRandomBlockFromDisk(testFile)

    (Array[Long](1l) ++ block, block.size / 2)
  }

  /**
    *
    * @param prefix
    * @param numberOfTargets
    * @param bitEncoding
    * @return
    */
  def generateRandomBlock(prefix: String, numberOfTargets: Int, bitEncoding: BitEncoding): Array[Long] = {
    val blockOffTargets = mutable.ArrayBuilder.make[Long]()

    val binIterator = new BaseCombinationGenerator(20 - prefix.size)
    val binProp = numberOfTargets.toDouble / binIterator.totalSearchSpace.toDouble
    val rando = new Random()


    binIterator.iterator.foreach { bin => {
      if (rando.nextDouble() <= binProp) {
        val ranoCR = new RandoCRISPR(21, Cas9ParameterPack.pam, false, prefix + bin, 0, 0, None)
        val nxt = ranoCR.next
        assert(nxt.fullTarget.size == 23)
        blockOffTargets += bitEncoding.bitEncodeString(nxt.fullTarget)
        blockOffTargets += posEncoder.encode("chr22", 1, 23, true)
      }
    }
    }
    blockOffTargets.result()
  }

  def writeRandomBlockToDisk(outputFile: File, prefix: String, numberOfTargets: Int, bitEncoding: BitEncoding): Unit = {
    val block = generateRandomBlock(prefix: String, numberOfTargets: Int, bitEncoding: BitEncoding)

    import java.io.FileOutputStream
    val os = new DataOutputStream(new FileOutputStream(outputFile.getAbsolutePath))
    os.writeLong(block.size)
    block.zipWithIndex.foreach{case(blockLong,index) => {
      os.writeLong(blockLong)
    }}

    os.close()
  }

  def readRandomBlockFromDisk(inputFile: File):Array[Long]  = {

    import java.io.FileInputStream
    val os = new DataInputStream(new FileInputStream(inputFile.getAbsolutePath))
    val size = os.readLong().toInt

    val rt = new Array[Long](size)
    (0 until size).foreach{case(pos) => {
      rt(pos) = os.readLong()
    }}

    os.close()
    rt
  }

  /**
    *
    * @param prefix
    * @param bitEncoding
    * @param lookupBinSize
    * @return
    */
  def generateRandomIndexedBlockOfTargets(prefix: String,
                                          bitEncoding: BitEncoding,
                                          lookupBinSize: Int): Tuple2[Array[Long], Int] = {

    val block = readRandomBlockFromDisk(testFile)//.slice(0,1000)

    // now make the lookup part
    val binLookup = new mutable.LinkedHashMap[String, Int]()
    val binLookupSize = new mutable.LinkedHashMap[String, Int]()

    new BaseCombinationGenerator(lookupBinSize).iterator.foreach { case (bin) => {
      binLookup(bin) = -1
      binLookupSize(bin) = 0
    }
    }

    block.zipWithIndex.foreach { case (bitE, index) => {
      // we have a target (long) and it's position (long)
      if (index % 2 == 0) {
        val bn = bitEncoding.bitDecodeString(bitE).str.slice(prefix.size, prefix.size + lookupBinSize)
        if (binLookup(bn) >= index | binLookup(bn) < 0) {
          binLookup(bn) = index
        }
        binLookupSize(bn) = binLookupSize.getOrElse(bn, 0) + 2 // we have two longs per entry here -- the target and it's position
      }
    }
    }

    val arrayOfOffsetsAndSizes = mutable.ArrayBuilder.make[Long]()
    binLookup.foreach{case(key,value) => {
      arrayOfOffsetsAndSizes += (binLookup(key).toLong << 32 | binLookupSize(key).toLong)
    }}

    (Array[Long](2l) ++ arrayOfOffsetsAndSizes.result() ++ block, block.size)
  }
}
