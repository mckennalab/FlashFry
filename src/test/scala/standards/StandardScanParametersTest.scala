package standards

import crispr.GuideMemoryStorage
import org.scalatest.{FlatSpec, Matchers}
import reference.SimpleSiteFinder

/**
  * Created by aaronmck on 3/9/17.
  */
class StandardScanParametersTest extends FlatSpec with Matchers {

  "StandardScanParameters" should "use the parameter pack forward regex match for cas9 correctly" in {
    val string = "ATTTA AAAAA CCCCC GGGGG GGG".filter{c => c != ' '}.mkString("")


    (Cas9ParameterPack.fwdRegex.findAllMatchIn(string).size) should be (1)
  }


  "StandardScanParameters" should "use the parameter pack reverse regex match for cas9 correctly" in {
    val string = "CCTAA AAAAA CCCCC GGGGG GGT".filter{c => c != ' '}.mkString("")


    (Cas9ParameterPack.revRegex.findAllMatchIn(string).size) should be (1)
  }


  "StandardScanParameters" should "use the parameter pack forward regex match for cpf1 correctly" in {
    val string = "TTTA AAAAA CCCCC GGGGG ATAAA".filter{c => c != ' '}.mkString("")


    (Cpf1ParameterPack.fwdRegex.findAllMatchIn(string).size) should be (1)
  }


  "StandardScanParameters" should "use the parameter pack reverse regex match for cpf1 correctly" in {
    val string = "AATTA AAAAA CCCCC GGGGG AAAA".filter{c => c != ' '}.mkString("")


    (Cpf1ParameterPack.revRegex.findAllMatchIn(string).size) should be (1)
  }
}
