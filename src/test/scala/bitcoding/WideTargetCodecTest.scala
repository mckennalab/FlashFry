package bitcoding

import org.scalatest.{FlatSpec, Matchers}
import standards.NucleaseProfiles
import crispr.{CRISPRSite, CRISPRSiteOT, ResultsAggregator}
import reference.binary.TargetPos
import reference.binary.blocks.BlockManager

class WideTargetCodecTest extends FlatSpec with Matchers {
  private val pack = NucleaseProfiles.enCas12a
  private val encoder = new BitEncoding(pack)

  "WideTargetCodec" should "round-trip a 27-base target without using sequence bits for the count" in {
    val target = "TTTA" + "ACGTACGTACGTACGTACGTACG"
    val encoded = encoder.bitEncodeString(StringCount(target, 7))

    encoder.targetCodec should be (WideTargetCodec)
    encoder.bitDecodeString(encoded).str should be (target)
    encoder.bitDecodeString(encoded).count should be (1)
  }

  it should "round-trip a target record and its independent occurrence count" in {
    val target = encoder.bitEncodeString("TTTA" + "A" * 23)
    val positions = Array(11L, 12L, 13L)
    val encodedRecord = encoder.encodeTargetRecord(target, positions)
    val decoded = encoder.decodeTargetRecord(encodedRecord, 0)

    decoded.target should be (target)
    decoded.count should be (3)
    decoded.positions should be (positions)
    decoded.nextOffset should be (encodedRecord.length)
  }

  it should "exclude the PAM and compare all 23 spacer bases" in {
    val first = encoder.bitEncodeString("TTTA" + "A" * 23)
    val pamOnlyChange = encoder.bitEncodeString("TTTC" + "A" * 23)
    val spacerChange = encoder.bitEncodeString("TTTA" + "C" + "A" * 22)

    encoder.mismatches(first, pamOnlyChange) should be (0)
    encoder.mismatches(first, spacerChange) should be (1)
  }

  it should "exclude a six-base three-prime SaCas9 PAM and compare its full spacer" in {
    val saEncoder = new BitEncoding(NucleaseProfiles.saCas9NNGRRT22)
    val first = saEncoder.bitEncodeString("A" * 22 + "AAGGAT")
    val pamOnlyChange = saEncoder.bitEncodeString("A" * 22 + "CCGGGT")
    val spacerChange = saEncoder.bitEncodeString("C" + "A" * 21 + "AAGGAT")

    saEncoder.targetCodec should be (WideTargetCodec)
    saEncoder.mismatches(first, pamOnlyChange) should be (0)
    saEncoder.mismatches(first, spacerChange) should be (1)
  }

  it should "produce identical linear and indexed results for a five-prime PAM" in {
    val prefix = "AAC"
    val firstSequence = "TTTA" + prefix + "A" * 20
    val secondSequence = "TTTC" + prefix + "C" + "A" * 19
    val thirdSequence = "TTTG" + prefix + "A" * 20
    val targets = Array(
      TargetPos(encoder.bitEncodeString(firstSequence), Array(11L), encoder),
      TargetPos(encoder.bitEncodeString(secondSequence), Array(12L), encoder),
      TargetPos(encoder.bitEncodeString(thirdSequence), Array(13L), encoder))

    val linear = BlockManager.createLinearBlock(targets, prefix, encoder)
    val indexed = BlockManager.createIndexedBlock(targets, prefix, encoder, 2)
    val guideEncoding = encoder.bitEncodeString(firstSequence)

    def aggregator(): ResultsAggregator = new ResultsAggregator(Array(
      new CRISPRSiteOT(CRISPRSite("chr1", firstSequence, true, 0, None), guideEncoding, 100)))

    val linearResults = aggregator()
    val indexedResults = aggregator()
    val manager = new BlockManager(prefix.length, 2, encoder)
    val parentBin = encoder.binToLongComparitor(prefix)

    manager.compareBlock(linear, targets.length, linearResults.indexedGuides, linearResults, encoder, 0, parentBin)
    manager.compareBlock(indexed, targets.length, indexedResults.indexedGuides, indexedResults, encoder, 0, parentBin)

    linearResults.wrappedGuides.head.otSite.currentTotal should be (2)
    indexedResults.wrappedGuides.head.otSite.currentTotal should be (2)
  }
}
