package standards

import scala.util.matching.Regex

/**
  * everything we need to know about an enzyme
  */
sealed trait ParameterPack {
  def enzyme: Enzyme

  def pam: Array[String]

  def fwdRegex: Regex

  def revRegex: Regex

  def totalScanLength: Int

  def comparisonBitEncoding: Long

  def fivePrimePam: Boolean
}

object ParameterPack {
  def nameToParameterPack(name: String): ParameterPack = name.toUpperCase match {
    case "CPF1"   => Cpf1ParameterPack
    case "SPCAS9" => Cas9ParameterPack
    case "SPCAS9NGG" => Cas9NGGParameterPack
    case _        => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + name)
  }
}


case object Cas9ParameterPack extends ParameterPack {
  def enzyme = SpCAS9
  def pam = Array[String]("GG","AG")

  def totalScanLength: Int = 23
  def comparisonBitEncoding: Long = 0x3FFFFFFFFFC0l // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG or NAG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """(\w{21}[GA]G)""".r

  override def revRegex: Regex = """(C[CT]\w{21})""".r
}


case object Cas9NGGParameterPack extends ParameterPack {
  def enzyme = SpCAS9
  def pam = Array[String]("GG")

  def totalScanLength: Int = 23
  def comparisonBitEncoding: Long = 0x3FFFFFFFFFC0l // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG or NAG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """(\w{21}GG)""".r

  override def revRegex: Regex = """(CC\w{21})""".r
}

case object Cpf1ParameterPack extends ParameterPack {
  def enzyme = CPF1
  def pam = Array[String]("TTT")
  def totalScanLength: Int = 24
  def comparisonBitEncoding: Long = 0x00FFFFFFFFFFl // be super careful with this value!!
  def fivePrimePam: Boolean = true

  override def fwdRegex: Regex = """(TTT\w{21})""".r

  override def revRegex: Regex = """(\w{21}AAA)""".r
}

