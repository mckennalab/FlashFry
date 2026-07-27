package standards

import java.io.{File, PrintWriter}
import java.nio.file.Files

import bitcoding.WideTargetCodec
import org.scalatest.{FlatSpec, Matchers}

class NucleaseProfileFileTest extends FlatSpec with Matchers {

  "NucleaseProfileFile" should "load and normalize a valid custom profile" in {
    val file = writeProfile(
      """id=custom-cas12a-tttv-23
        |aliases=custom12a,custom-cpf1
        |nuclease=CustomCas12a
        |family=cas12a
        |spacerLength=23
        |pamSide=5prime
        |pams=tttv,tctv
        |""".stripMargin)

    val profile = NucleaseProfileFile.load(file)

    profile.id should be ("custom-cas12a-tttv-23")
    profile.aliases should be (Set("custom12a", "custom-cpf1"))
    profile.nuclease.stringEncoding should be ("CustomCas12a")
    profile.nuclease.enzymeParent should be (Cpf1Type)
    profile.spacerLength should be (23)
    profile.pamSpec.side should be (FivePrimePam)
    profile.pamSpec.normalizedPatterns should be (Vector("TTTV", "TCTV"))
    profile.legacyHeaderId should be (None)
    bitcoding.TargetCodec.forParameterPack(profile) should be (WideTargetCodec)

    val roundTripped = NucleaseProfileSerialization.deserialize(
      NucleaseProfileSerialization.serialize(profile)).profile
    NucleaseProfileSerialization.sameTargetingDefinition(roundTripped, profile) should be (true)
  }

  it should "reject missing, unknown, malformed, and conflicting properties" in {
    val missing = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(
        """id=missing-pams
          |nuclease=MissingCas
          |family=custom
          |spacerLength=20
          |pamSide=3prime
          |""".stripMargin))
    }
    missing.getMessage should include ("Missing required properties: pams")

    val unknown = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(validPackedProfile + "pamSdie=3prime\n"))
    }
    unknown.getMessage should include ("Unknown properties: pamSdie")

    val malformed = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(validPackedProfile.replace("pams=NGN", "pams=NGN,N")))
    }
    malformed.getMessage should include ("All PAM patterns in a profile must have the same length")

    val collision = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(validPackedProfile.replace("id=custom-ngn-20", "id=spcas9ngg")))
    }
    collision.getMessage should include ("conflict with built-in profiles")
  }

  it should "reject invalid IUPAC symbols and targets wider than 32 bases" in {
    val invalidIupac = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(validPackedProfile.replace("pams=NGN", "pams=NGZ")))
    }
    invalidIupac.getMessage should include ("Unsupported IUPAC base: Z")

    val tooWide = intercept[IllegalArgumentException] {
      NucleaseProfileFile.load(writeProfile(validPackedProfile.replace("spacerLength=20", "spacerLength=30")))
    }
    tooWide.getMessage should include ("support at most 32 total bases")
  }

  private val validPackedProfile =
    """id=custom-ngn-20
      |nuclease=CustomCas
      |family=cas9
      |spacerLength=20
      |pamSide=3prime
      |pams=NGN
      |""".stripMargin

  private def writeProfile(contents: String): File = {
    val file = Files.createTempFile("flashfry-profile-", ".properties").toFile
    file.deleteOnExit()
    val writer = new PrintWriter(file)
    try {
      writer.print(contents)
    } finally {
      writer.close()
    }
    file
  }
}
