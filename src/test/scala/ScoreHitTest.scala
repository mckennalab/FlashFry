package test.scala

import main.scala.CRISPROnTarget
import main.scala.trie.CRISPRPrefixMap
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.matchers.ShouldMatchers

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
class ScoreHitTest extends FlatSpec with ShouldMatchers {
  "ScoreHit" should " calculate on-target rates correctly" in {
    CRISPROnTarget.calcDoenchScore("TATAGCTGCGATCTGAGGTAGGGAGGGACC") shouldEqual 0.713089368437 +- 0.001
    CRISPROnTarget.calcDoenchScore("TCCGCACCTGTCACGGTCGGGGCTTGGCGC") shouldEqual 0.0189838463593 +- 0.001
  }

  //val offtargetCoeff = Array[Double](0.0,   0.0,   0.014, 0.0,   0.0,   0.395, 0.317, 0.0,   0.389, 0.079,
  // 0.445, 0.508, 0.613, 0.815, 0.732, 0.828, 0.615, 0.804, 0.685, 0.583) // ,
  "ScoreHit" should " zip with weight vector correctly with smaller CRISPR target" in {
    val result = CRISPRPrefixMap.zipAndExpand("ATG")
    assert(result(0)._2 == 0.804)
    assert(result(1)._2 == 0.685)
    assert(result(2)._2 == 0.583)
  }

  "ScoreHit" should " zip with weight vector correctly with larger CRISPR target" in {
    val result = CRISPRPrefixMap.zipAndExpand("ATGTGATGTGATGTGATGTG")
    assert(result(0)._2 == 0.0)
    assert(result(1)._2 == 0.0)
    assert(result(2)._2 == 0.0)
    assert(result(3)._2 == 0.014)
    assert(result(19)._2 == 0.583)
  }

  "ScoreHit" should " zip with weight vector correctly with much larger CRISPR target" in {
    val result = CRISPRPrefixMap.zipAndExpand("ATGTGATGTGATGTGATGTGATGTGATGTGATGTGATGTG")
    assert(result(0)._2 == 0.0)
    assert(result(19)._2 == 0.0)
    assert(result(23)._2 == 0.014)
    assert(result(39)._2 == 0.583)
  }
}
