package reference.traverser.parallel

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout
import bitcoding.{BitEncoding, BitPosition}
import crispr.{CRISPRHit, CRISPRSiteOT, ResultsAggregator}
import htsjdk.samtools.util.BlockCompressedInputStream
import reference.binary.{BinaryHeader, BlockOffset}
import reference.traversal.BinTraversal
import reference.traverser.Traverser
import standards.ParameterPack
import utils.Utils

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  *
  */
object ParallelTraverser {

  import ParallelBinController._

  val system = ActorSystem("ParallelTraverser")

  var numberOfThreads = 0


  def scan(binaryFile: File,
           header: BinaryHeader,
           traversal: BinTraversal,
           aggregator: ResultsAggregator,
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition) {

    // create the master
    if (0 == 1) throw new IllegalStateException("This code isn't up and running")
    val master = system.actorOf(ParallelBinController.props(binaryFile, bitCoder, maxMismatch, traversal, header, numberOfThreads, aggregator.wrappedGuides.map{gd => gd.otSite}), name = "master")

    implicit val timeout = Timeout(5 day)
    val future: Future[GuideResult] = ask(master, StartChecking).mapTo[GuideResult]
    val result = Await.result(future, 1 day)
    system.terminate()

    result.result
  }
}


object ParallelBinController {
  def props(binaryFile: File, bitEncoding: BitEncoding, numberOfMismatches: Int, traversal: BinTraversal, header: BinaryHeader, threads: Int, targets: Array[CRISPRSiteOT]): Props =
    Props(new ParallelBinController(binaryFile, bitEncoding, numberOfMismatches, traversal, header, threads, targets))


  case object GuideStatusCheck
  case class GuideStatus(remainingBins: Int, totalBins: Int)
  case object StartChecking
  case class GuideResult(result: Array[CRISPRSiteOT])

}

/**
  * create the controller object
  * @param binaryFile the binary file to load from
  * @param bitEncoding encode values into longs
  * @param numberOfMismatches the number of mismatches
  * @param traversal the traversal
  * @param header our file header
  * @param numberOfThreads the number of threads to use
  * @param targets the CRISPR targets to look at
  */
class ParallelBinController(binaryFile: File,
                            bitEncoding: BitEncoding,
                            numberOfMismatches: Int,
                            traversal: BinTraversal,
                            header: BinaryHeader,
                            numberOfThreads: Int,
                            targets: Array[CRISPRSiteOT]) extends Actor with ActorLogging {

  import ParallelBinCheck._
  import ParallelBinController._
  var savedSender: Option[ActorRef] = None

  val siteSequenceToSite = new mutable.LinkedHashMap[Long, CRISPRSiteOT]()
  val overflowed = new mutable.LinkedHashMap[Long, Boolean]()
  val collected = new mutable.LinkedHashMap[String, Boolean]()
  var completedBins = 0

  val blockCompressedInput = new BlockCompressedInputStream(new File(binaryFile.getAbsolutePath))

  targets.zipWithIndex.foreach { case (tgt, index) => {
    siteSequenceToSite(tgt.longEncoding) = tgt
    overflowed(tgt.longEncoding) = false
  }
  }

  val workerPool = context.actorOf(
    Props[ParallelBinCheck].withRouter(SmallestMailboxPool(numberOfThreads)), name = "workerRouter")

  (0 until numberOfThreads).foreach { index =>
    if (traversal.hasNext) {

      val guidesToSeekForBin = traversal.next()
      collected(guidesToSeekForBin.bin) = false
      val binPositionInformation = header.blockOffsets(guidesToSeekForBin.bin)

      val longBuffer = fillBlock(binPositionInformation, blockCompressedInput)

      //log.warning("Starting actor for bin " + guidesToSeekForBin.bin)
      workerPool ! ParallelWork(longBuffer,
        guidesToSeekForBin.bin,
        binPositionInformation.numberOfTargets,
        guidesToSeekForBin.guides.map{g => g.guide},
        binaryFile,
        bitEncoding,
        numberOfMismatches,
        header.binMask)
    }
  }


  def receive = {
    case StartChecking => {
      savedSender = Some(sender())
    }
    case GuideStatusCheck => {
      val remainingBins = collected.filter { case (key, value) => !value }.size
      sender() ! GuideStatus(remainingBins,collected.size)
    }
    case result: ParallelResult => {
      completedBins += 1
      result.guides.zipWithIndex.foreach { case (guide, index) => {
        if (result.hits(index).size > 0)
          siteSequenceToSite(guide).addOTs(result.hits(index))
      }
      }

      collected(result.bin) = true

      if (traversal.hasNext) {

        val guidesToSeekForBin = traversal.next()
        collected(guidesToSeekForBin.bin) = false

        log.warning("We have a next bin: " + guidesToSeekForBin)
        val binPositionInformation = header.blockOffsets(guidesToSeekForBin.bin)

        val longBuffer = fillBlock(binPositionInformation, blockCompressedInput)

        workerPool ! ParallelWork(longBuffer,
          guidesToSeekForBin.bin,
          binPositionInformation.numberOfTargets,
          guidesToSeekForBin.guides.map{g => g.guide},
          binaryFile,
          bitEncoding,
          numberOfMismatches,
          header.binMask)

        val remainingBins = collected.filter { case (key, value) => !value }.size

        if (remainingBins == 0)
          savedSender.map{sendr => sendr ! GuideResult(siteSequenceToSite.values.toArray)}

      } else {
        //log.warning("NO MORE " + collected.filter { case (key, value) => !value }.size)
        if (collected.filter { case (key, value) => !value }.size == 0)
          savedSender.map{sendr => sendr ! GuideResult(siteSequenceToSite.values.toArray)}
      }

      if (completedBins % 1 == 0) {
        log.warning("Completed bin count " + completedBins + " comparisons " + BitEncoding.allComparisons)
      }
    }
  }


  /**
    * fill a block of off-targets from the database
    *
    * @param blockInformation information about the block we'd like to fetch
    * @return
    */
  def fillBlock(blockInformation: BlockOffset, blockCompressedInput:BlockCompressedInputStream): (Array[Long]) = {
    assert(blockInformation.uncompressedSize >= 0, "Bin sizes must be positive (or zero)")

    blockCompressedInput.seek(blockInformation.blockPosition)
    val readToBlock = new Array[Byte](blockInformation.uncompressedSize)
    val read = blockCompressedInput.read(readToBlock)

    Utils.byteArrayToLong(readToBlock)
  }
}


object ParallelBinCheck {
  // our messages
  case class ParallelWork(block: Array[Long],
                          bin: String,
                          numberOfTargets: Int,
                          guides: Array[BitEncoding.TargetLong],
                          binaryFile: File,
                          bitEncoding: BitEncoding,
                          numberOfMismatches: Int,
                          binMask: Long)

  case class ParallelResult(bin: String, guides: Array[Long], hits: Array[Array[CRISPRHit]])

}

/**
  * the actor that actually does the work: for a given block check for overlap against a guide set

  */
class ParallelBinCheck extends Actor with ActorLogging {

  import ParallelBinCheck._

  def receive = {
    case pFetch: ParallelWork => {
      val blockCompressedInput = new BlockCompressedInputStream(new File(pFetch.binaryFile.getAbsolutePath))

      val binRes = Traverser.compareBlock(pFetch.block,
        pFetch.numberOfTargets,
        pFetch.guides,
        pFetch.bitEncoding,
        pFetch.numberOfMismatches,
        pFetch.bitEncoding.binToLongComparitor(pFetch.bin))

      log.warning("processing actor for bin " + pFetch.bin + " with " + pFetch.guides.size + ", " + BitEncoding.allComparisons + " comparisons")

      sender ! ParallelResult(pFetch.bin, pFetch.guides, binRes)

    }
  }

}


