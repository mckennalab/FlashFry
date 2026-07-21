package scoring

import bitcoding.BitEncoding
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cpf1ParameterPack, NucleaseProfiles}

class Cas12aOffTargetScoreTest extends FlatSpec with Matchers {

  "EnCas12aOffTargetScore" should "load and apply the bundled 23-position mismatch matrix" in {
    val model = new EnCas12aOffTargetScore()
    model.bitEncoder(new BitEncoding(NucleaseProfiles.enCas12a))
    model.setup()

    model.scorePair("A" * 23, "C" + "A" * 22) should be (0.14414414414414414 +- 1e-12)
    model.validOverEnzyme(NucleaseProfiles.enCas12a) should be (true)
    model.validOverEnzyme(Cpf1ParameterPack) should be (false)
    model.validOverEnzyme(NucleaseProfiles.asCas12a) should be (false)
    model.validOverEnzyme(NucleaseProfiles.lbCas12a) should be (false)
  }

  "Cas12aTwoNLSOffTargetScore" should "use its distinct bundled matrix" in {
    val model = new Cas12aTwoNLSOffTargetScore()
    model.bitEncoder(new BitEncoding(NucleaseProfiles.cas12a2xNLS))
    model.setup()

    model.scorePair("A" * 23, "C" + "A" * 22) should be (0.13513513513513514 +- 1e-12)
  }

  "EnCas12aOffTargetScore" should "weight off-target activity by independent wide-record occurrence counts" in {
    val pack = NucleaseProfiles.enCas12a
    val encoder = new BitEncoding(pack)
    val model = new EnCas12aOffTargetScore()
    model.bitEncoder(encoder)
    model.setup()

    val target = "TTTA" + "A" * 23
    val offTarget = "TTTA" + "C" + "A" * 22
    val guide = new CRISPRSiteOT(CRISPRSite("chr1", target, true, 0, None), encoder.bitEncodeString(target), 100)
    guide.addOT(new CRISPRHit(encoder.bitEncodeString(offTarget), Array.empty[Long],
      validOffTargetCoordinates = false, occurrenceCount = 2))

    val score = model.scoreGuide(guide)
    score(0)(0).toDouble should be (0.14414414414414414 +- 1e-12)
    score(1)(0).toDouble should be ((1.0 / (1.0 + 2.0 * 0.14414414414414414)) +- 1e-12)
  }
}
