package main.scala

import scala.collection.mutable.ArrayBuffer

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
object CRISPRFinder {
  // search the target chromosome for CRISPR hits
  def findCRISPRSites(seq: String, contig: String, offset: Int, crisprLength: Int, upstreamDownstreamFlank: Int = 200, allowNInFlankingRegion: Boolean = false): ArrayBuffer[BedEntry] = {
    var retArray = ArrayBuffer[BedEntry]()

    // the pattern is G(N*)GG, so go from 0 to seq.length -CRISPRLength and find any possible CRISPR sites
    for (i <- (0 until (seq.length - (crisprLength + 1)))) {
      val subptr = seq.substring(i, i + crisprLength).toUpperCase

      if ((subptr(0) == 'G' && subptr(crisprLength - 2) == 'G' && subptr(crisprLength - 1) == 'G' && !(subptr contains 'N')) ||
        (subptr(0) == 'C' && subptr(1) == 'C' && !(subptr contains 'N') && subptr(crisprLength - 1) == 'C')) {
        val reverseComp = if (subptr(0) == 'C') true else false

        val flankingRegion = seq.substring(Math.max(0, i - upstreamDownstreamFlank), Math.min(seq.length, i + crisprLength + upstreamDownstreamFlank)).toUpperCase
        if ((!allowNInFlankingRegion && (!(flankingRegion contains "N"))) || allowNInFlankingRegion) {
          val ret = BedEntry(contig,
            i + offset,
            i + crisprLength + offset,
            Some(if (reverseComp) CRISPRLocator.revComp(subptr) else subptr),
            Some(Array[String]("offset="+offset))) // + "," + (if (reverseComp) CRISPRLocator.revComp(flankingRegion) else flankingRegion))))
          retArray += ret

        }
      }
    }
    return retArray
  }
}


object CRISPRLocator {
  def revComp(st: String): String = st.reverse.toUpperCase.map {
    case (base) => revBase(base)
  }.mkString("")

  def revBase(base: Char): Char = {
    if (base == 'A') return 'T'
    if (base == 'C') return 'G'
    if (base == 'G') return 'C'
    if (base == 'T') return 'A'
    return base // we don't know the base
  }
}
