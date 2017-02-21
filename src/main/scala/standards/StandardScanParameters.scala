package standards

/**
  * store standard run types for specific CRISPR protein types
  */
object StandardScanParameters {

  val cas9ParameterPack = ParameterPack(
    "GG",
    23,
    0x3FFFFFFFFFC0l, // be super careful with this value!! the cas9 mask only considers the lower 46 bites (23 of 24 bases are used)
                      // and doesn't care about the NGG of NGG (3 prime) -- it's assumed all OT have the NGG
    false
  )

  val cpf1ParameterPack = ParameterPack(
    "TTT",
    24,
    0x00FFFFFFFFFFl, // the Cpf1 check only considers the lower 20 bases, and not the PAM of TTTN
    true
  )

  def nameToParameterPack(name: String): ParameterPack = name.toUpperCase match {
    case "CPF1" => cpf1ParameterPack
    case "CAS9" => cas9ParameterPack
    case _ => throw new IllegalStateException("Unable to find the correct parameter pack for enzyme: " + name)
  }
}

case class ParameterPack(pam: String, totalScanLength: Int, comparisonBitEncoding: Long, fivePrimePam: Boolean)