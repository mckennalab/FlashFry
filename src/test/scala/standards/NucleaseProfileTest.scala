package standards

import org.scalatest.{FlatSpec, Matchers}
import utils.Utils

class NucleaseProfileTest extends FlatSpec with Matchers {

  "NucleaseRegistry" should "resolve legacy aliases and new profile IDs" in {
    ParameterPack.nameToParameterPack("spCas9NGG") should be (Cas9NGGParameterPack)
    ParameterPack.nameToParameterPack("spcas9-ngg-20") should be (Cas9NGGParameterPack)
    ParameterPack.nameToParameterPack("enCas12a").profile should be (NucleaseProfiles.enCas12a)
    ParameterPack.nameToParameterPack("SaCas9").profile should be (NucleaseProfiles.saCas9NNGRRT21)
    ParameterPack.nameToParameterPack("sacas9-nngrrt-22").profile should be (NucleaseProfiles.saCas9NNGRRT22)
    ParameterPack.nameToParameterPack("AsCpf1").profile should be (NucleaseProfiles.asCas12a)
    ParameterPack.nameToParameterPack("lbCas12a").profile should be (NucleaseProfiles.lbCas12a)
  }

  "TargetLayout" should "derive spacer, PAM, and comparison offsets on either side" in {
    Cas9NGGParameterPack.guideRange should be ((0, 20))
    Cas9NGGParameterPack.pamSequence("A" * 20 + "AGG") should be ("AGG")

    NucleaseProfiles.enCas12a.layout.spacerRange should be ((4, 27))
    NucleaseProfiles.enCas12a.layout.spacer("TTTA" + "C" * 23) should be ("C" * 23)
    NucleaseProfiles.enCas12a.layout.pamSequence("TTTA" + "C" * 23) should be ("TTTA")

    NucleaseProfiles.saCas9NNGRRT21.layout.spacerRange should be ((0, 21))
    NucleaseProfiles.saCas9NNGRRT21.layout.pamRange should be ((21, 27))
    NucleaseProfiles.saCas9NNGRRT21.layout.pamSequence("A" * 21 + "AAGGAT") should be ("AAGGAT")

    NucleaseProfiles.asCas12a.layout.spacerRange should be ((4, 27))
    NucleaseProfiles.lbCas12a.layout.spacerRange should be ((4, 27))
  }

  it should "derive the historical comparison masks" in {
    Cas9ParameterPack.comparisonBitEncoding should be (0x3FFFFFFFFFC0L)
    Cas9ParameterPack19bp.comparisonBitEncoding should be (0x0FFFFFFFFFC0L)
    Cpf1ParameterPack.comparisonBitEncoding should be (0x00FFFFFFFFFFL)
  }

  "TargetPatternCompiler" should "find 23-base enCas12a sites on both strands" in {
    val forward = "TTTA" + "ACGTACGTACGTACGTACGTACG"
    val reverseGenomic = Utils.reverseCompString(forward)

    NucleaseProfiles.enCas12a.fwdRegex.findAllMatchIn(forward).size should be (1)
    NucleaseProfiles.enCas12a.revRegex.findAllMatchIn(reverseGenomic).size should be (1)
  }

  it should "apply SaCas9 NNGRRT targeting on both strands" in {
    val forward = "ACGTACGTACGTACGTACGTA" + "AAGGAT"
    val reverseGenomic = Utils.reverseCompString(forward)

    NucleaseProfiles.saCas9NNGRRT21.fwdRegex.findAllMatchIn(forward).size should be (1)
    NucleaseProfiles.saCas9NNGRRT21.revRegex.findAllMatchIn(reverseGenomic).size should be (1)
    NucleaseProfiles.saCas9NNGRRT21.fwdRegex.findAllMatchIn("A" * 21 + "AAGCAT").size should be (0)
  }

  it should "accept TTTV but reject TTTT for AsCas12a and LbCas12a" in {
    val spacer = "ACGTACGTACGTACGTACGTACG"
    val validPams = Vector("TTTA", "TTTC", "TTTG")

    Vector(NucleaseProfiles.asCas12a, NucleaseProfiles.lbCas12a).foreach { profile =>
      validPams.foreach(pam => profile.fwdRegex.findAllMatchIn(pam + spacer).size should be (1))
      profile.fwdRegex.findAllMatchIn("TTTT" + spacer).size should be (0)
    }
  }

  "NucleaseProfileSerialization" should "round-trip registered and configured profiles" in {
    val registered = NucleaseProfileSerialization.deserialize(
      NucleaseProfileSerialization.serialize(NucleaseProfiles.enCas12a))
    registered.profile should be (NucleaseProfiles.enCas12a)

    Vector(
      NucleaseProfiles.saCas9NNGRRT21,
      NucleaseProfiles.saCas9NNGRRT22,
      NucleaseProfiles.asCas12a,
      NucleaseProfiles.lbCas12a
    ).foreach { profile =>
      NucleaseProfileSerialization.deserialize(
        NucleaseProfileSerialization.serialize(profile)).profile should be (profile)
    }

    val custom = NucleaseProfile("custom-cas", Set("custom"),
      ConfiguredEnzyme("CustomCas", ConfiguredEnzymeType("custom-family")), 18,
      PamSpec(ThreePrimePam, Vector("NNGR")))
    val decoded = NucleaseProfileSerialization.deserialize(NucleaseProfileSerialization.serialize(custom))
    NucleaseProfileSerialization.sameTargetingDefinition(decoded.profile, custom) should be (true)
  }
}
