package test.scala

import crispr.PAMBuffer
import main.scala.CRISPROnTarget
import main.scala.trie.CRISPRPrefixMap
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.matchers.ShouldMatchers
import util.CircBuffer

/**
 * created by aaronmck on 12/18/14
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
class CircBufferTest extends FlatSpec with ShouldMatchers {
  "PAMBuffer" should " find PAMs correctly" in {
    val buffer = new PAMBuffer(23)
    buffer.addLine("AAAAATTTTTAAAAATTTTTNGG")
    assert(buffer.isPAM()._1)
    assert(!buffer.isPAM()._2)
    buffer.addLine("AAAAATTTTTAAAAATTTTTNGG")
    assert(buffer.isPAM()._1)
    assert(!buffer.isPAM()._2)
    buffer.addLine("CCNAAAAATTTTTAAAAATTTTT")
    assert(buffer.isPAM()._2)
    assert(!buffer.isPAM()._1)
  }

  "PAMBuffer" should " find CRISPR targets correctly" in {
    val buffer = new PAMBuffer(23)
    var ret = buffer.addLine("AAAAATTTTTAAAAATTTTTNGG")
    assert(ret.size == 1)
    assert(ret(0)._3)
    ret = buffer.addLine("AAAAATTTTTAAAAATTTTTNGG")
    assert(ret.size == 1)
    assert(ret(0)._3)
    ret = buffer.addLine("CCNAAAAATTTTTAAAAATTTTT")
    assert(ret.size == 1)
    assert(!ret(0)._3)
  }
}
