package main.scala.util

import java.io._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}

import scala.io.Source

/**
 * just basic / general utilities that don't really have any other home
 */
object Utils {

  // get the GC content of the a string
  def gcContent(guide: String): Double = guide.toUpperCase.map{b => if (b == 'C' || b == 'G') 1 else 0}.sum.toDouble / guide.length.toDouble

  // time a functional block
  // from http://stackoverflow.com/questions/15436593/how-to-measure-and-display-the-running-time-of-a-single-test
  def time[T](str: String)(thunk: => T): T = {
    print(str + "... ")
    val t1 = System.currentTimeMillis
    val x = thunk
    val t2 = System.currentTimeMillis
    println((t2 - t1) + " msecs")
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
}
