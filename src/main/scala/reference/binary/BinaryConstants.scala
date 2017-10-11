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

package reference.binary

/**
  * store binary constants associated with reading and writing our custom file format
  */
object BinaryConstants {
  val magicNumber = 0x1234ABCDE123890l // looks magic enough -- just there to make sure the file what we expect
  val version = 1l
  val bytesPerTarget = 8
  val bytesPerGenomeLocation = 8

  val headerSize = 24 // a long for the magic number, the version, and the number of bins
}
