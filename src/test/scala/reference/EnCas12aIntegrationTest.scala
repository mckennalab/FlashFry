package reference

import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.Files

import bitcoding.StringCount
import crispr.{BinWriter, CRISPRSiteOT, GuideMemoryStorage, ResultsAggregator}
import org.scalatest.{FlatSpec, Matchers}
import reference.binary.{BinaryHeader, DatabaseWriter}
import reference.traversal.LinearTraversal
import reference.traverser.LinearTraverser
import standards.NucleaseProfiles
import utils.BaseCombinationGenerator

class EnCas12aIntegrationTest extends FlatSpec with Matchers {

  "enCas12a" should "index and rediscover a 27-base target through the wide five-prime-PAM database path" in {
    val temporaryDirectory = Files.createTempDirectory("flashfry-encas12a").toFile
    temporaryDirectory.deleteOnExit()
    val fasta = new File(temporaryDirectory, "reference.fa")
    val database = new File(temporaryDirectory, "targets.db")
    fasta.deleteOnExit()
    database.deleteOnExit()
    new File(database.getAbsolutePath + BinaryHeader.headerExtension).deleteOnExit()

    val target = "TTTA" + "ACGTACGTACGTACGTACGTACG"
    val fastaOutput = new PrintWriter(new FileOutputStream(fasta))
    fastaOutput.println(">chr1")
    fastaOutput.println(target)
    fastaOutput.close()

    val pack = NucleaseProfiles.enCas12a
    val temporaryBins = BaseCombinationGenerator(3)
    val binWriter = BinWriter(temporaryDirectory, temporaryBins, pack)
    val encoders = ReferenceEncoder.findTargetSites(fasta, binWriter, pack, 0)
    val bins = binWriter.close()

    DatabaseWriter.writeToBinnedFileSet(bins, temporaryBins.width, database.getAbsolutePath,
      encoders._1, encoders._2, BaseCombinationGenerator(3), pack, maxTargetsPerLinearBin = 0)

    val header = BinaryHeader.readHeader(database.getAbsolutePath + BinaryHeader.headerExtension)
    val guides = new GuideMemoryStorage()
    ReferenceEncoder.findTargetSites(fasta, guides, header.parameterPack, 0)
    guides.guideHits.length should be (1)

    val guide = guides.guideHits.head
    val guideOT = new CRISPRSiteOT(guide,
      header.bitCoder.bitEncodeString(StringCount(guide.bases, 1)), 100)
    val results = new ResultsAggregator(Array(guideOT))
    val traversal = new LinearTraversal(header.binGenerator, 0, header.bitCoder, 1.0, results)
    results.setTraversalOverFlowCallback(traversal.overflowGuide)

    LinearTraverser.scan(database, header, traversal, results, 0, header.parameterPack,
      header.bitCoder, header.bitPosition)

    guideOT.currentTotal should be (1)
    guideOT.offTargets.length should be (1)
    header.bitCoder.bitDecodeString(guideOT.offTargets.head.sequence).str should be (target)
  }
}
