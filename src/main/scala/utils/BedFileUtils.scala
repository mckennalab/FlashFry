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

package utils

import java.io.File

import scala.collection.mutable
import scala.io.Source

class BEDFile(inputBed: File) extends Iterator[Option[BedEntry]] {
  val inputLines = Source.fromFile(inputBed).getLines()
  var currentLine: Option[String] = None
  val tokensPerBedLine = 4

  if (inputLines.hasNext) currentLine = Some(inputLines.next())

  override def hasNext: Boolean = currentLine.isDefined

  override def next(): Option[BedEntry] = {
    if (!currentLine.isDefined) {
      throw new IllegalStateException("Next called when there is no next")
    }
    while (currentLine.get.startsWith("#")) {
      if (!inputLines.hasNext) {
        return None
      }
      currentLine = Some(inputLines.next())
    }
    val break = currentLine.get.split("\t")

    val mp = new mutable.HashMap[String,String]()


    if (break.length > tokensPerBedLine) {
      break(tokensPerBedLine).split(BedEntry.entriesSeperator).map { case (tl) => {
        val tk = tl.split(BedEntry.kvSeperator)
        if (tk.length == 2) {
          mp(tk(0)) = tk(1)
        }
      }
      }
    }

    val ret = BedEntry(break(0),
      break(1).toInt,
      break(2).toInt,
      break(3),
      mp)

    if (!inputLines.hasNext) {
      currentLine = None
    }
    else {
      currentLine = Some(inputLines.next())
    }

    Some(ret)
  }
}


/**
  * create a BED entry from what constitutes a line from a BED file
  * @param contig the contig this entry is on
  * @param start start position (0 based)
  * @param stop the stop position (closed)
  * @param name the name, provided in the 4th column
  * @param rest tags to add after the name
  */
case class BedEntry(contig: String, start: Int, stop: Int, name: String, rest: mutable.HashMap[String,String]) {
  var allOptions = rest
  var onTarget = 0.0
  var offTargets = new mutable.HashMap[String, Tuple3[Double, Int, Int]]
  var offTargetScore = -1.0
  var strand = "+"


  def addOption(option: String, value: String, allowDup: Boolean = true): Unit = {
    if (allOptions contains option) {
      if (allowDup) {
        allOptions(option) = allOptions(option) + BedEntry.valSeperator + value
      }
    }
    else {
      allOptions(option) = value
    }
  }

  def outputString: String = "BED:" + contig + ":" + start + "-" + stop + "_" + name
}

object BedEntry {
  val kvSeperator = "="
  val valSeperator = ","
  val entriesSeperator = ";"
}
