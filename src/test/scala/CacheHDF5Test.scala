package test.scala

import java.io.File

import main.scala.CRISPROnTarget
import main.scala.hdf5.CacherHDF5
import main.scala.trie.CRISPRPrefixMap
import ncsa.hdf.`object`.FileFormat
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.matchers.ShouldMatchers

import scala.collection.mutable
import scala.io.Source

/**
 * created by aaronmck on 1/7/15
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
/*
class CacheHDF5Test extends FlatSpec with ShouldMatchers {
  val fl = new File("testFile.hd5")

  "CacheHDF5Test" should " create a group successfully" in {
    try {
      val hdf5 = CacherHDF5.createHDF5File(fl)
      val group = CacherHDF5.createGroup(hdf5, "testGroup", None)
      hdf5.close()
    } finally {
      if (fl.exists()) fl.delete()
    }
  }

  "CacheHDF5Test" should " return a valid group from a recently created file" in {
    try {
      var hdf5 = CacherHDF5.createHDF5File(fl)
      CacherHDF5.createGroup(hdf5, "testGroup", None)
      hdf5.close()

      hdf5 = CacherHDF5.openHDF5File(fl)
      val group = CacherHDF5.getGroup(hdf5, "testGroup")

      assert(group != null)
      hdf5.close()


    } finally {
      if (fl.exists()) fl.delete()
    }
  }

  "CacheHDF5Test" should " output a map file correctly" in {
    try {
      val dataToEntry = Map("ACGTACGT" -> 1,"ACGTACTT" -> 1)
      val mp = Source.fromFile("top3M_23mer.txt").getLines().map{case(ln) => {val sp = ln.split("\t"); (sp(0), sp(1).toInt)}}.toMap
      println("Writing")
      CacherHDF5.writeCRISPRMap2("gogo.hd5", "whatever", mp)


    } finally {
      //if (fl.exists()) fl.delete()
    }
  }

  "CacheHDF5Test" should " read faster as a HDF5 opposed to text" in {
    try {
      val dataToEntry = Map("ACGTACGT" -> 1,"ACGTACTT" -> 1)
      println(
        time{
          val mp = Source.fromFile("top3M_23mer.txt").getLines().map{case(ln) => {val sp = ln.split("\t"); (sp(0), sp(1).toInt)}}.toMap
        })
      println(
        time{
          CacherHDF5.readCRISPRMap2("gogo.hd5", "whatever")
        })

      //CacherHDF5.writeCRISPRMap2("gogo.hd5", "whatever", mp, 8, "asdfasd")


    } finally {
      //if (fl.exists()) fl.delete()
    }
  }


  def time(f: => Unit) = {
    val s = System.currentTimeMillis
    f
    System.currentTimeMillis - s
  }

}
*/
