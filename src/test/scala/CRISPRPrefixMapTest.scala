package test.scala

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import tree.CRISPRPrefixMap

import scala.collection.Map
import scala.io.Source

/**
 * created by aaronmck on 12/20/14
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
class CRISPRPrefixMapTest extends FlatSpec with ShouldMatchers {

  "CRISPRPrefixMapTest" should " load two hits correcty" in {
    val tree = new CRISPRPrefixMap[Array[String]]()
    val key = "AAACTTGAGGGTGCCTGCAA"

    val ray: Array[String] = tree.getOrElse(key,Array[String]()) :+ key
    tree.put(key, ray )
    val ray2: Array[String] = tree.getOrElse(key,Array[String]()) :+ key
    tree.put(key, ray2 )

    assert(tree.get(key).get.size == 2)
  }

  def fromTestData(fl: String): CRISPRPrefixMap[Int] = {
    val tree = new CRISPRPrefixMap[Int]()
    Source.fromFile(fl).getLines().foreach(ln => {
      //println("adding "+ ln)
      val nm = ln.slice(0,20)
      val ray: Int = tree.getOrElse(nm,0) + 1
      tree.put(nm, ray )
    })
    return tree
  }

  def fromTestDataAlt(fl: String): CRISPRPrefixMap[Int] = {
    val tree = new CRISPRPrefixMap[Int]()
    Source.fromFile(fl).getLines().foreach(ln => {
      val sp = ln.split("-")
      val nm = sp(0)
      println("adding " + nm)
      val sc = sp(1).split("_")(0).toDouble
      tree.update(nm,1)
    })
    return tree
  }
}
