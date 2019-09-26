/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package standards

import scala.util.matching.Regex

/**
  * everything we need to know about an enzyme -- this is the most dangerous file to edit in the
  * codebase, as many of these constants are specific to their use-case in FlashFry. Be careful!
  */
sealed trait ParameterPack {
  def enzyme: Enzyme

  def pam: Array[String]

  def pamLength: Int

  def fwdRegex: Regex

  def revRegex: Regex

  def totalScanLength: Int

  def comparisonBitEncoding: Long

  def fivePrimePam: Boolean

  def guideRange: Tuple2[Int,Int]

  def paddedPam: Array[String]
}

object ParameterPack {
  def nameToParameterPack(name: String): ParameterPack = name.toUpperCase match {
    case "CPF1"   => Cpf1ParameterPack
    case "SPCAS9" => Cas9ParameterPack
    case "SPCAS9NGG" => Cas9NGGParameterPack
    case "SPCAS9NAG" => Cas9NAGParameterPack
    case "SPCAS919" => Cas9ParameterPack19bp
    case "SPCAS9NGG19" => Cas9NGG19ParameterPack
    case _        => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + name)
  }

  def indexToParameterPack(index: Int): ParameterPack = index match {
    case 1 => Cpf1ParameterPack
    case 2 => Cas9ParameterPack
    case 3 => Cas9NGGParameterPack
    case 4 => Cas9NAGParameterPack
    case 5 => Cas9ParameterPack19bp
    case 6 => Cas9NGG19ParameterPack
    case _ => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + index)
  }

  def parameterPackToIndex(pack: ParameterPack): Int = pack match {
    case Cpf1ParameterPack => 1
    case Cas9ParameterPack => 2
    case Cas9NGGParameterPack => 3
    case Cas9NAGParameterPack => 4
    case Cas9ParameterPack19bp => 5
    case Cas9NGG19ParameterPack => 6
    case _ => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + pack.toString)

  }

  val cas9ScanLength20mer = 23
  val cas9ScanLength19mer = 22
  val cas9PAMLength = 3
  val cpf1ScanLength = 24
  val cpf1PAMLength = 4
}


case object Cas9ParameterPack extends ParameterPack {
  def enzyme: Enzyme = SpCAS9
  def pam: Array[String] = Array[String]("GG","AG")
  def paddedPam: Array[String] = Array[String]("NGG","NAG")
  def pamLength: Int = ParameterPack.cas9PAMLength

  def totalScanLength: Int = ParameterPack.cas9ScanLength20mer

  // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  def comparisonBitEncoding: Long = 0x3FFFFFFFFFC0L

  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG or NAG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{20}[AG]G))""".r

  override def revRegex: Regex = """([C])(?=([CT][ACGTacgt]{21}))""".r

  override def guideRange: (Int,Int) = (0,ParameterPack.cas9ScanLength20mer - ParameterPack.cas9PAMLength)
}


case object Cas9ParameterPack19bp extends ParameterPack {
  def enzyme: Enzyme = SpCAS9
  def pam: Array[String] = Array[String]("GG","AG")
  def paddedPam: Array[String] = Array[String]("NGG","NAG")
  def pamLength: Int = ParameterPack.cas9PAMLength

  def totalScanLength: Int = ParameterPack.cas9ScanLength19mer

  // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  def comparisonBitEncoding: Long = 0x0FFFFFFFFFC0L

  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG or NAG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{19}[AG]G))""".r

  override def revRegex: Regex = """([C])(?=([CT][ACGTacgt]{20}))""".r

  override def guideRange: (Int,Int) = (0,ParameterPack.cas9ScanLength19mer - ParameterPack.cas9PAMLength)
}


case object Cas9NGGParameterPack extends ParameterPack {
  def enzyme: Enzyme= SpCAS9
  def pam: Array[String] = Array[String]("GG")
  def paddedPam: Array[String] = Array[String]("NGG")
  def pamLength: Int = ParameterPack.cas9PAMLength

  def totalScanLength: Int = ParameterPack.cas9ScanLength20mer

  // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  def comparisonBitEncoding: Long = 0x3FFFFFFFFFC0L

  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{20}GG))""".r

  override def revRegex: Regex = """([C])(?=(C[ACGTacgt]{21}))""".r

  override def guideRange: (Int,Int) = (0,ParameterPack.cas9ScanLength20mer - ParameterPack.cas9PAMLength)
}


case object Cas9NGG19ParameterPack extends ParameterPack {
  def enzyme: Enzyme= SpCAS9
  def pam: Array[String] = Array[String]("GG")
  def paddedPam: Array[String] = Array[String]("NGG")
  def pamLength: Int = ParameterPack.cas9PAMLength

  def totalScanLength: Int = ParameterPack.cas9ScanLength19mer

  // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  def comparisonBitEncoding: Long = 0x0FFFFFFFFFC0L

  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NGG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{19}GG))""".r

  override def revRegex: Regex = """([C])(?=(C[ACGTacgt]{20}))""".r

  override def guideRange: (Int,Int) = (0,ParameterPack.cas9ScanLength19mer - ParameterPack.cas9PAMLength)
}


case object Cas9NAGParameterPack extends ParameterPack {
  def enzyme: Enzyme = SpCAS9
  def pam: Array[String] = Array[String]("AG")
  def paddedPam: Array[String] = Array[String]("NAG")
  def pamLength: Int = ParameterPack.cas9PAMLength

  def totalScanLength: Int = ParameterPack.cas9ScanLength20mer

  // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
  def comparisonBitEncoding: Long = 0x3FFFFFFFFFC0L

  // and doesn't care about the NGG or NAG (3 prime) -- it's assumed all OT have the NAG
  def fivePrimePam: Boolean = false

  override def fwdRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{20}AG))""".r

  override def revRegex: Regex = """([C])(?=(T[ACGTacgt]{21}))""".r

  override def guideRange: (Int,Int) = (0,ParameterPack.cas9ScanLength20mer - ParameterPack.cas9PAMLength)
}

case object Cpf1ParameterPack extends ParameterPack {
  def enzyme: Enzyme = CPF1
  def pam: Array[String] = Array[String]("TTT")
  def paddedPam: Array[String] = Array[String]("TTTN")
  def pamLength: Int = ParameterPack.cpf1PAMLength
  def totalScanLength: Int = ParameterPack.cpf1ScanLength
  def comparisonBitEncoding: Long = 0x00FFFFFFFFFFL // be super careful with this value!!
  def fivePrimePam: Boolean = true


  override def fwdRegex: Regex = """(T)(?=(TT[ACGTacgt]{21}))""".r

  override def revRegex: Regex = """([ACGTacgt])(?=([ACGTacgt]{20}AAA))""".r

  override def guideRange: (Int,Int) = (ParameterPack.cpf1PAMLength,ParameterPack.cpf1ScanLength)

}

