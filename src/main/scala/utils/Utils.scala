/*
 * Copyright (c) 2015 Aaron McKenna
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package utils

import scala.annotation._, elidable._
import java.io._
import java.math.BigInteger
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

/**
 * just basic / general utilities that don't really have any other home
 */
object Utils extends LazyLogging {

  // get the GC content of the a string
  def gcContent(guide: String): Double = guide.toUpperCase.map{b => if (b == 'C' || b == 'G') 1 else 0}.sum.toDouble / guide.length.toDouble

  // time a functional block
  // from http://stackoverflow.com/questions/15436593/how-to-measure-and-display-the-running-time-of-a-single-test
  def time[T](str: String)(thunk: => T): T = {
    print(str + "... ")
    val t1 = System.currentTimeMillis
    val x = thunk
    val t2 = System.currentTimeMillis
    logger.info((t2 - t1) + " msecs")
    x
  }

  // input and output shortcuts
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))
  def gos(s: String) = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(s)))

  def inputSource(s: String) = s match {
    case gz if s endsWith ".gz" => Source.fromInputStream(gis(s))
    case _ => Source.fromFile(s)
  }

  implicit def fileToString(fl: File): String = fl.getAbsolutePath

  def longToBitString(long: Long): String = {
    val str = String.format("%064d", new BigInteger(java.lang.Long.toBinaryString(long)))
    str.grouped(4).mkString(" ")
  }


  // utilities to reverse complement
  def compBase(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
  def compRNABase(c: Char): Char = if (c == 'A') 'U' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'U') 'A' else c

  def compString(str: String): String = str.map {
    compBase(_)
  }.mkString

  def reverseCompString(str: String): String = compString(str).reverse

  def baseToIndex(base: Char): Int = base match {
    case 'A' => 0
    case 'a' => 0
    case 'C' => 1
    case 'c' => 1
    case 'G' => 2
    case 'g' => 2
    case 'T' => 3
    case 't' => 3
    case _ => 4
  }

  /**
    * the entropy of a base sequence (non ACGT bases removed from the calculation)
    * @param sequence the sequence
    * @return the entropy
    */
  def sequenceEntropy(sequence: String): Double = {
    if (sequence.size == 0) return 0.0
    val probs = Array.fill[Double](4)(0.0)
    sequence.map{base => baseToIndex(base)}.filter(b => b < 4).foreach{ind => probs(ind) += 1.0}
    -1.0 * probs.map{prob => if (prob == 0) 0 else prob/probs.sum * (math.log(prob/probs.sum)/math.log(2.0))}.sum
  }

  /**
    * find the longest string of bases within a string (case sensitive)
    * @param sequence the string of bases
    * @return the longest run of bases in the string
    */
  def longestHomopolymerRun(sequence: String): Int = {
    var longestRun = 0
    var currentRun = 1
    var index = 1
    while (index < sequence.length) {
      if (sequence(index -1) == sequence(index))
        currentRun += 1
      else
        currentRun = 1

      if (currentRun > longestRun)
        longestRun = currentRun
      index += 1
    }
    longestRun
  }

  /**
    * convert a long array to a byte array - just to hide the uglyness of block conversion
    * @param larray the array of longs
    * @return an array of bytes
    */
  def longArrayToByteArray(larray: Array[Long]): Array[Byte] = {
    val bbuf = java.nio.ByteBuffer.allocate(8*larray.length)
    bbuf.order(java.nio.ByteOrder.nativeOrder)
    bbuf.asLongBuffer.put(larray)
    bbuf.flip()
    bbuf.array()
  }

  /**
    * convert a byte array to a long array - just to hide the uglyness of block conversion
    * @param larray the array of longs
    * @return an array of bytes
    */
  def byteArrayToLong(larray: Array[Byte]): Array[Long] = {
    assert(larray.size % 8 == 0,"You byte array is not a multiple of 8")

    val bbuf = java.nio.ByteBuffer.allocate(larray.length)
    bbuf.order(java.nio.ByteOrder.nativeOrder)
    bbuf.put(larray)
    bbuf.flip()
    bbuf.asLongBuffer()

    // we should have a long return that's (byte size / 8) longs
    val ret = new Array[Long](larray.size / 8)

    var index = 0
    while (index < ret.size) { // for speed
      ret(index) = bbuf.getLong()
      index += 1
    }

    ret
  }

  /**
    * Custom version of assert, that we can remove later for performance reasons.
    * The strategy is to richly decorate our code in assertion checks, for testing
    * and sample runs, but produce a high-speed production version later
    *
    *
    * Tests an expression, throwing an `AssertionError` if false.
    *  Calls to this method will not be generated if `-Xelide-below`
    *  is at least `ASSERTION`.
    *
    *  @see elidable
    *  @param assertionFunction   the expression to test
    */
  @elidable(WARNING)
  def verificationAssert(assertionFunction: Boolean) {
    if (!assertionFunction)
      throw new java.lang.AssertionError("assertion failed")
  }

}
