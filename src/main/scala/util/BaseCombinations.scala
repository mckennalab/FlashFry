package main.scala.util

import main.scala.util.Base.Base

import scala.collection.mutable.ListBuffer

/**
 * created by aaronmck on 1/3/15
 *
 * Copyright (c) 2015, aaronmck
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
// make an iterator of Bases from AAAA to TTTT for example
class BaseCombinations(count: Int) extends Iterator[String] {
  var lst = new Array[Base](count)
  var isLast = false
  (0 until count).foreach{case(e) => lst(e) = Base.A}

  val terminalStr = (0 until count).map{_ => Base.T}.mkString

  override def hasNext: Boolean =
    if (terminalStr == lst.mkString) {
      isLast = true
      return true
    }
    else return !isLast

  def incr(pos: Int): Unit = {
    if (lst(pos) == Base.T) {
      lst(pos) = Base.A
      if (pos - 1 >= 0)
        incr(pos-1)
    }
    else
      lst(pos) = Base(Base.baseToInt(lst(pos)) + 1)
  }

  override def next(): String = {
    val ret = lst.mkString
    incr(count - 1)
    return ret

  }
}