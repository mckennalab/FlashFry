package standards

/**
  * store standard run types for specific CRISPR protein types
  */
object StandardScanParameters {

  val cas9ParameterPack = ParameterPack(
    "GG",
    23,
    0x3FFFFFFFFFC0l, // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used) and doesn't care about the N of NGG (3 prime)
    false
  )

  val cpf1ParameterPack = ParameterPack(
    "TTT",
    24,
    0xFCFFFFFFFFFFl, // be super careful with this value!! the cpf1 mask doesn't consider the N of the TTTN
    true
  )

  def nameToParameterPack(name: String): ParameterPack = name.toUpperCase match {
    case "CPF1" => cpf1ParameterPack
    case "CAS9" => cas9ParameterPack
    case _ => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + name)
  }
}

case class ParameterPack(pam: String, totalScanLength: Int, comparisonBitEncoding: Long, fivePrimePam: Boolean)