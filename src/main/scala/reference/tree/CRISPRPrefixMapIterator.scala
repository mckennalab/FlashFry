package reference.tree


import java.io.{BufferedInputStream, File, FileInputStream}
import java.util.zip.GZIPInputStream

import scala.collection.Iterator
import scala.io.Source

/**
  * created by aaronmck on 12/21/14
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


/**
  * given an input file, parse out a tree to score CRISPRs with
  * @param inputFile the input file
  * @param pamRemove should we drop the pam off the end of our CRISPR tags
  * @param prefixDrop the number of prefix characters to drop off the front of our tags
  * @param linesPerIterator how many lines we want to split into each CRISPRPrefixMap
  * @param columnCRISPR which column of the input contains the CRISPR text
  * @param lengthCRISPR the length of each CRISPR
  */
class CRISPRPrefixMapIterator(inputFile: String,
                              pamRemove: Boolean,
                              prefixDrop: Int,
                              linesPerIterator: Int,
                              columnCRISPR: Int = 3,
                              lengthCRISPR: Int = 23,
                              sep: String = "\t") extends Iterator[CRISPRPrefixMap[Int]] {

  val lines = if (inputFile.endsWith(".gz")) Source.fromInputStream(gis(inputFile)).getLines() else Source.fromFile(inputFile).getLines()
  var linesAdded = 0
  val pamDrop = pamRemove
  val knownLength = if (pamDrop) lengthCRISPR - 3 else lengthCRISPR
  val pfDrop = prefixDrop
  val cCRISPR = columnCRISPR
  val lCRISPR = lengthCRISPR
  val separator = sep

  // if we have lines, we can make a new PrefixMap
  override def hasNext: Boolean = !lines.isEmpty

  // get the next is PrefixMap
  println("loading free tree")
  override def next(): CRISPRPrefixMap[Int] = {
    val tree = new CRISPRPrefixMap[Int]()
    lines.foreach(ln => {
      val sp = ln.split(separator)
      if (lengthCRISPR != sp(columnCRISPR).length)
        throw new IllegalArgumentException("A CRISPR entry \"" + sp(columnCRISPR) + "\" isn't the same length " + sp(columnCRISPR).length + " as the previously set length of " + knownLength)
      //println("adding " + sp(columnCRISPR).slice(prefixDrop, knownLength))
      val strFrag = sp(columnCRISPR).slice(prefixDrop, knownLength)
      tree.put(strFrag, tree.getOrElse(strFrag,1))
      linesAdded += 1
      if (linesAdded == linesPerIterator) {
        println("returning new tree with " + linesPerIterator)
        linesAdded = 0
        return tree
      }
    })
    tree // we've run off the end of the iterator, return what we have
  }

  // get a input stream from a compressed file
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))
}

object CRISPRPrefixMapIterator {
  def loadFromCacheLocation(location: File): CRISPRPrefixMapIterator = {
    // check to see if the specified directory in-fact has a set of CPM files
    if (location.exists) return new CRISPRPrefixMapIterator(location.getAbsolutePath,true,0,1000000)
    else throw new IllegalArgumentException("Unable to find cache file: " + location.getAbsolutePath)
  }
}