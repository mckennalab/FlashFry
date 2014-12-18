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
object Main extends App {
  // load up the list of known hits in the genome
  val inputFile = "top300K_fc.txt"
  println("loading trie")
  val trie = CRISPRPrefixMap.fromPath(inputFile,6)

  println("scoring")
  // for each region, find a list of CRISPR sites in the region
  // GTTAGGGTTAGGGTTAGGGTTAGG
  // GTTACGGTTACGGTTAGGGTTAGG
  // GTTACGGTTACGGTTAGG
  println("GTTACGGTTACGGTTAGGGTTAGG")
  println("CAGTG TAAGA GGACA GTCCG GGG")
  val scores = "GTTACGGTTACGGTTAGGGTTAGG".zip(ScoreHit.offtargetCoeff).toList
  val returnScores = trie.score(scores)

  println("score length = " + returnScores.size)
  returnScores.foreach{case(hit,score) => println("hit = " + hit + " score = " + score)}
  println("total score = " + 100.0 / (100.0 + trie.score(scores).values.sum))
  // score each hit for uniqueness and optimality

  // output each target region -> hit combination for further analysis
}
