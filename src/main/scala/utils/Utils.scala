package utils

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

  def compString(str: String): String = str.map {
    compBase(_)
  }.mkString

  def reverseCompString(str: String): String = compString(str).reverse



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


}
