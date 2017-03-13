package standards

/**
  * Created by aaronmck on 3/12/17.
  */
sealed trait Enzyme{
  def stringEncoding: String;
  def enzymeParent: EnzymeType
  def index: Int
}

object Enzyme {
  def indexToParameterPack(index: Long): ParameterPack = {
    index match {
      case 0 => Cas9ParameterPack
      case 1 => Cpf1ParameterPack
      case _ => throw new IllegalStateException("Unknown enzyme type: " + index)
    }
  }
}

case object SpCAS9 extends Enzyme{
  override def stringEncoding: String = "spCas9"
  override def enzymeParent = Cas9Type
  def index: Int = 0
}
case object CPF1 extends Enzyme{
  override def stringEncoding: String = "Cpf1"
  override def enzymeParent = Cpf1Type
  def index: Int = 1
}

sealed trait EnzymeType
case object Cas9Type extends EnzymeType
case object Cpf1Type extends EnzymeType