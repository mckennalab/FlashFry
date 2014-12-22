package main.scala

/**
 * created by aaronmck on 12/16/14
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
object CRISPROnTarget {
// stolen from http://tefor.net/crispor/doenchScore.py
  val matches = Array(
    (1, "G", -0.2753771), (2, "A", -0.3238875), (2, "C", 0.17212887), (3, "C", -0.1006662),
    (4, "C", -0.2018029), (4, "G", 0.24595663), (5, "A", 0.03644004), (5, "C", 0.09837684),
    (6, "C", -0.7411813), (6, "G", -0.3932644), (11, "A", -0.466099), (14, "A", 0.08537695),
    (14, "C", -0.013814), (15, "A", 0.27262051), (15, "C", -0.1190226), (15, "T", -0.2859442),
    (16, "A", 0.09745459), (16, "G", -0.1755462), (17, "C", -0.3457955), (17, "G", -0.6780964),
    (18, "A", 0.22508903), (18, "C", -0.5077941), (19, "G", -0.4173736), (19, "T", -0.054307),
    (20, "G", 0.37989937), (20, "T", -0.0907126), (21, "C", 0.05782332), (21, "T", -0.5305673),
    (22, "T", -0.8770074), (23, "C", -0.8762358), (23, "G", 0.27891626), (23, "T", -0.4031022),
    (24, "A", -0.0773007), (24, "C", 0.28793562), (24, "T", -0.2216372), (27, "G", -0.6890167),
    (27, "T", 0.11787758), (28, "C", -0.1604453), (29, "G", 0.38634258), (1, "GT", -0.6257787),
    (4, "GC", 0.30004332), (5, "AA", -0.8348362), (5, "TA", 0.76062777), (6, "GG", -0.4908167),
    (11, "GG", -1.5169074), (11, "TA", 0.7092612), (11, "TC", 0.49629861), (11, "TT", -0.5868739),
    (12, "GG", -0.3345637), (13, "GA", 0.76384993), (13, "GC", -0.5370252), (16, "TG", -0.7981461),
    (18, "GG", -0.6668087), (18, "TC", 0.35318325), (19, "CC", 0.74807209), (19, "TG", -0.3672668),
    (20, "AC", 0.56820913), (20, "CG", 0.32907207), (20, "GA", -0.8364568), (20, "GG", -0.7822076),
    (21, "TC", -1.029693), (22, "CG", 0.85619782), (22, "CT", -0.4632077), (23, "AA", -0.5794924),
    (23, "AG", 0.64907554), (24, "AG", -0.0773007), (24, "CG", 0.28793562), (24, "TG", -0.2216372),
    (26, "GT", 0.11787758), (28, "GG", -0.69774))
  // stolen from http://tefor.net/crispor/doenchScore.py
  val intercept = 0.59763615
  val gcHigh = -0.1665878
  val gcLow = -0.2026259

  // stolen and then translated from http://tefor.net/crispor/doenchScore.py
  def calcDoenchScore(seq: String): Double = {
    var score = intercept

    val guideSeq = seq.slice(4, 24).toUpperCase()
    var gcWeight = gcLow
    val gcCount = guideSeq.count(_ == 'G') + guideSeq.count(_ == 'C')
    if (gcCount <= 10)
      gcWeight = gcLow
    else if (gcCount > 10)
      gcWeight = gcHigh
    score += math.abs(10.0 - gcCount) * gcWeight

    matches.foreach { case (pos, modelSeq, weight) =>
      val subSeq = seq.slice(pos, pos + modelSeq.length)
      if (subSeq == modelSeq)
        score += weight
    }
    return 1.0 / (1.0 + math.exp(-score))
  }
}
