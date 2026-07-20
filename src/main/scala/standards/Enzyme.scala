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

/**
  * Biochemical nuclease identity used by scoring applicability and profile persistence.
  *
  * Target geometry belongs to `NucleaseProfile`; the same enzyme may legitimately have multiple profiles.
  */
trait Enzyme {
  /** Stable display/persistence identifier. */
  def stringEncoding: String

  /** Broad nuclease family used by models that apply to a whole family. */
  def enzymeParent: EnzymeType
}

case object SpCAS9 extends Enzyme{
  override def stringEncoding: String = "spCas9"
  override def enzymeParent: EnzymeType = Cas9Type
}
case object CPF1 extends Enzyme{
  override def stringEncoding: String = "Cpf1"
  override def enzymeParent: EnzymeType = Cpf1Type
}
case object EnCas12a extends Enzyme{
  override def stringEncoding: String = "enCas12a"
  override def enzymeParent: EnzymeType = Cpf1Type
}
case object Cas12a2xNLS extends Enzyme{
  override def stringEncoding: String = "2xNLS-Cas12a"
  override def enzymeParent: EnzymeType = Cpf1Type
}

/** Nuclease identity reconstructed from an unregistered version-2 profile. */
final case class ConfiguredEnzyme(stringEncoding: String, enzymeParent: EnzymeType) extends Enzyme

/** Stable family identity; deliberately open so serialized custom profiles can retain an unknown family. */
trait EnzymeType {
  def id: String
}
case object Cas9Type extends EnzymeType { override val id: String = "cas9" }
case object Cpf1Type extends EnzymeType { override val id: String = "cas12a" }
/** Family reconstructed from an ID not known by this FlashFry build. */
final case class ConfiguredEnzymeType(id: String) extends EnzymeType

object EnzymeType {
  /** Parse a stable family ID, preserving unknown values as configured families. */
  def fromId(id: String): EnzymeType = id.toLowerCase match {
    case Cas9Type.id => Cas9Type
    case Cpf1Type.id => Cpf1Type
    case other => ConfiguredEnzymeType(other)
  }
}

object Enzyme {
  /** Resolve a built-in nuclease when possible, otherwise preserve it as a configured identity. */
  def fromEncoding(encoding: String, family: EnzymeType): Enzyme = encoding.toLowerCase match {
    case "spcas9" if family == Cas9Type => SpCAS9
    case "cpf1" if family == Cpf1Type => CPF1
    case "encas12a" if family == Cpf1Type => EnCas12a
    case "2xnls-cas12a" if family == Cpf1Type => Cas12a2xNLS
    case _ => ConfiguredEnzyme(encoding, family)
  }
}
