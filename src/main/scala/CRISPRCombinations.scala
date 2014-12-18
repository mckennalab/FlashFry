package main.scala

/**
 * created by aaronmck on 12/9/14
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
case class CRISPRCombinations(sz: Int) extends Iterator[String] {
  // space to iterate over
  var currentPattern = new Array[Int](sz)
  println("my size is supposed to be " + sz)

  def hasNext(): Boolean = {
    currentPattern.sum != (sz * 3)
  }

  def next(): String = {
    val ret = currentPattern.map{numberToBase(_)}.mkString("")
    addToCurrentPattern()
    ret
  }

  def addToCurrentPattern(): Boolean = {
    for (s <- (sz-1).until(-1,-1)) {currentPattern(s) += 1; if (currentPattern(s) > 4) {currentPattern(s) = 0} else {return true}}
    return true
  }

  def numberToBase(nt: Int): Char = if (nt == 0) 'A' else if (nt == 1) 'C' else if (nt == 2) 'G' else 'T'
  def baseToNumber(nt: Char): Int = if (nt == 'A') 0 else if (nt == 'C') 1 else if (nt == 'G') 2 else 3

}
