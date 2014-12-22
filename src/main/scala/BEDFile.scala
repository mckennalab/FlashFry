package main.scala

import java.io.File

import scala.collection.mutable
import scala.io.Source

/**
 * created by aaronmck on 12/19/14
 *
 * Copyright (c) 2014, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */
class BEDFile(inputBed: File) extends Iterator[Option[BedEntry]] {
  val inputLines = Source.fromFile(inputBed).getLines()
  var currentLine: Option[String] = None

  if (inputLines.hasNext) currentLine = Some(inputLines.next())

  override def hasNext: Boolean = currentLine.isDefined

  override def next(): Option[BedEntry] = {
    if (!currentLine.isDefined)
      throw new IllegalStateException("Next called when there is no next")
    while (currentLine.get.startsWith("#")) {
      if (!inputLines.hasNext) return None
      currentLine = Some(inputLines.next())
    }
    val break = currentLine.get.split("\t")

    val mp = new mutable.HashMap[String,String]()

    if (break.length > 4)
      break(4).split(BedEntry.entriesSeperator).map{case(tl) => {
        val tk = tl.split(BedEntry.kvSeperator)
        if (tk.length == 2)
          mp(tk(0)) = tk(1)
      }}
    val ret = BedEntry(break(0),
      break(1).toInt,
      break(2).toInt,
      break(3),
      mp)

    if (!inputLines.hasNext) currentLine = None
    else currentLine = Some(inputLines.next())

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

  override def toString(): String = {
    var tail = if (!rest.isEmpty) "\t" + name + "\t" + rest.map{case(k,v) => k + BedEntry.kvSeperator + v}.mkString(BedEntry.entriesSeperator)
    else "\t" + name

    contig + "\t" + start + "\t" + stop + tail
  }

  def addOption(option: String, value: String, noDup: Boolean = false): Unit = {
    println(option + "\t" + value)
    if (allOptions contains option)
      if (!noDup) allOptions(option) = allOptions(option) + BedEntry.valSeperator + value
    else
      allOptions(option) = value
  }
}

object BedEntry {
  val kvSeperator = "="
  val valSeperator = ","
  val entriesSeperator = ";"
}