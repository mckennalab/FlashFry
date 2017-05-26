package standards

/**
  * Created by aaronmck on 3/12/17.
  */
sealed trait Enzyme{
  def stringEncoding: String;
  def enzymeParent: EnzymeType
}

case object SpCAS9 extends Enzyme{
  override def stringEncoding: String = "spCas9"
  override def enzymeParent = Cas9Type
}
case object CPF1 extends Enzyme{
  override def stringEncoding: String = "Cpf1"
  override def enzymeParent = Cpf1Type
}

sealed trait EnzymeType
case object Cas9Type extends EnzymeType
case object Cpf1Type extends EnzymeType