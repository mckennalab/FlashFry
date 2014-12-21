package main.scala

import java.io.File

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

    val ret = Some(BedEntry(break(0),
      break(1).toInt,
      break(2).toInt,
      if (break.length > 3) Some(break(3)) else None,
      if (break.length > 4) Some(break.slice(4,break.length)) else None))
    if (!inputLines.hasNext) currentLine = None
    else currentLine = Some(inputLines.next())

    ret
  }
}

case class BedEntry(contig: String, start: Int, stop: Int, name: Option[String], rest: Option[Array[String]]) {
  override def toString(): String = {
    var tail = if (name.isDefined && rest.isDefined) "\t" + name.get + "\t" + rest.get.mkString(",")
    else if (name.isDefined && !rest.isDefined) "\t" + name.get
    else if (!name.isDefined && rest.isDefined) "\t" + rest.get.mkString(",")
    else ""

    contig + "\t" + start + "\t" + stop + tail
  }
}

object BedEntry {
  def append(bed: BedEntry, tags: Array[String]) = BedEntry(bed.contig,bed.start,bed.stop,bed.name,if (bed.rest.isDefined) Some(bed.rest.get ++ tags) else Some(tags))
}