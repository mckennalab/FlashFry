package modules

import java.io.{File, PrintWriter}
import java.nio.file.Files

import main.scala.Main
import org.scalatest.{FlatSpec, Matchers}
import standards.Cas9NGGParameterPack

class ProfileCommandTest extends FlatSpec with Matchers {

  "BuildOffTargetDatabase" should "select a custom profile file or the historical default" in {
    val profileFile = writeProfile()

    BuildOffTargetDatabase.resolveProfile("", profileFile).profile.id should be ("custom-ngn-20")
    BuildOffTargetDatabase.resolveProfile("", null) should be (Cas9NGGParameterPack)
  }

  it should "reject simultaneous built-in and custom profile selection" in {
    val error = intercept[IllegalArgumentException] {
      BuildOffTargetDatabase.resolveProfile("spg", writeProfile())
    }
    error.getMessage should include ("--enzyme and --profileFile are mutually exclusive")
  }

  "NucleaseProfileValidation" should "describe the normalized layout, codec, and fingerprint" in {
    val profile = BuildOffTargetDatabase.resolveProfile("", writeProfile()).profile
    val report = NucleaseProfileValidation.describe(profile)

    report should include ("status=valid")
    report should include ("id=custom-ngn-20")
    report should include ("pams=NGN")
    report should include ("targetLength=23")
    report should include ("layout=[spacer][PAM]")
    report should include ("targetCodec=packed24-v1")
    report should include ("fingerprint=")
  }

  "Main" should "register the nested profile validation command" in {
    val commandLine = Main.buildCommandLine()

    commandLine.getSubcommands should contain key ("profile")
    commandLine.getSubcommands.get("profile").getSubcommands should contain key ("validate")
  }

  private def writeProfile(): File = {
    val file = Files.createTempFile("flashfry-profile-command-", ".properties").toFile
    file.deleteOnExit()
    val writer = new PrintWriter(file)
    try {
      writer.print(
        """id=custom-ngn-20
          |nuclease=CustomCas
          |family=cas9
          |spacerLength=20
          |pamSide=3prime
          |pams=NGN
          |""".stripMargin)
    } finally {
      writer.close()
    }
    file
  }
}
