package reference

import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.Files

import bitcoding.{StringCount, TargetCodec}
import crispr.{BinWriter, CRISPRSiteOT, GuideMemoryStorage, ResultsAggregator}
import modules.BuildOffTargetDatabase
import org.scalatest.{FlatSpec, Matchers}
import picocli.CommandLine
import reference.binary.{BinaryHeader, DatabaseWriter}
import reference.traversal.LinearTraversal
import reference.traverser.LinearTraverser
import standards.{NucleaseProfile, NucleaseProfiles}
import utils.BaseCombinationGenerator

class AdditionalNucleaseIntegrationTest extends FlatSpec with Matchers {

  "A custom profile file" should "build a readable database through the index CLI" in {
    val temporaryDirectory = Files.createTempDirectory("flashfry-custom-profile-cli").toFile
    temporaryDirectory.deleteOnExit()
    val fasta = writeFile(new File(temporaryDirectory, "reference.fa"),
      ">chr1\n" + "A" * 20 + "AGA\n")
    val profileFile = writeFile(new File(temporaryDirectory, "custom.properties"),
      """id=custom-ngn-20
        |nuclease=CustomCas
        |family=cas9
        |spacerLength=20
        |pamSide=3prime
        |pams=NGN
        |""".stripMargin)
    val database = new File(temporaryDirectory, "targets.db")
    val bins = new File(temporaryDirectory, "bins")
    bins.mkdir() should be (true)

    new CommandLine(new BuildOffTargetDatabase()).parseWithHandler(
      new CommandLine.RunLast,
      Array(
        "--reference", fasta.getAbsolutePath,
        "--database", database.getAbsolutePath,
        "--tmpLocation", bins.getAbsolutePath,
        "--profileFile", profileFile.getAbsolutePath,
        "--binSize", "3"))

    val header = BinaryHeader.readHeader(database.getAbsolutePath + BinaryHeader.headerExtension)
    header.parameterPack.profile.id should be ("custom-ngn-20")
    header.parameterPack.profile.pamSpec.normalizedPatterns should be (Vector("NGN"))
    header.parameterPack.profile.spacerLength should be (20)
    header.bitCoder.targetCodec should be (TargetCodec.forParameterPack(header.parameterPack))
  }

  "SpG" should "index and rediscover an NGN target" in {
    assertDatabaseRoundTrip(NucleaseProfiles.spG, "A" * 20 + "AGA")
  }

  "SaCas9" should "index and rediscover its 21- and 22-base spacer profiles" in {
    val targets = Vector(
      NucleaseProfiles.saCas9NNGRRT21 -> ("ACGTACGTACGTACGTACGTA" + "AAGGAT"),
      NucleaseProfiles.saCas9NNGRRT22 -> ("ACGTACGTACGTACGTACGTAC" + "AAGGAT")
    )

    targets.foreach { case (profile, target) =>
      withClue(profile.id + ": ") {
        assertDatabaseRoundTrip(profile, target)
      }
    }
  }

  "AsCas12a and LbCas12a" should "index and rediscover TTTV targets as distinct profiles" in {
    val target = "TTTA" + "ACGTACGTACGTACGTACGTACG"

    Vector(NucleaseProfiles.asCas12a, NucleaseProfiles.lbCas12a).foreach { profile =>
      withClue(profile.id + ": ") {
        assertDatabaseRoundTrip(profile, target)
      }
    }
  }

  private def assertDatabaseRoundTrip(profile: NucleaseProfile, target: String): Unit = {
    val temporaryDirectory = Files.createTempDirectory("flashfry-" + profile.id).toFile
    temporaryDirectory.deleteOnExit()
    val fasta = new File(temporaryDirectory, "reference.fa")
    val database = new File(temporaryDirectory, "targets.db")
    fasta.deleteOnExit()
    database.deleteOnExit()
    new File(database.getAbsolutePath + BinaryHeader.headerExtension).deleteOnExit()

    val fastaOutput = new PrintWriter(new FileOutputStream(fasta))
    fastaOutput.println(">chr1")
    fastaOutput.println(target)
    fastaOutput.close()

    val temporaryBins = BaseCombinationGenerator(3)
    val binWriter = BinWriter(temporaryDirectory, temporaryBins, profile)
    val encoders = ReferenceEncoder.findTargetSites(fasta, binWriter, profile, 0)
    val bins = binWriter.close()

    DatabaseWriter.writeToBinnedFileSet(bins, temporaryBins.width, database.getAbsolutePath,
      encoders._1, encoders._2, BaseCombinationGenerator(3), profile, maxTargetsPerLinearBin = 0)

    val header = BinaryHeader.readHeader(database.getAbsolutePath + BinaryHeader.headerExtension)
    header.parameterPack.profile should be (profile)
    header.bitCoder.targetCodec should be (TargetCodec.forParameterPack(profile))

    val guides = new GuideMemoryStorage()
    ReferenceEncoder.findTargetSites(fasta, guides, header.parameterPack, 0)
    guides.guideHits.length should be (1)
    guides.guideHits.head.bases should be (target)

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

  private def writeFile(file: File, contents: String): File = {
    file.deleteOnExit()
    val writer = new PrintWriter(new FileOutputStream(file))
    try {
      writer.print(contents)
    } finally {
      writer.close()
    }
    file
  }
}
