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

package targetio

import java.io.File

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT
import reference.CRISPRSite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * this object contains methods that take bed files containing off-targets
  * and produce arrays of targets
  */
object TargetInput {

  def inputBedToTargetArray(inputFile: File, bitEnc: BitEncoding, overflowVal: Int, maxOTMismatch: Int): Tuple2[BitPosition, Array[CRISPRSiteOT]] = {

    val posEncoder = new BitPosition()

    val guides = new ArrayBuffer[CRISPRSiteOT]()

    val annotations = new mutable.HashMap[String,Int]()

    Source.fromFile(inputFile.getAbsolutePath).getLines().foreach{ln => {
      if (ln.startsWith("# contig:"))
        posEncoder.addReference(ln.split(":")(1))
      else if (ln.startsWith("# annotation:"))
        annotations(ln.split(":")(1)) = ln.split(":")(3).toInt
      else {
        val guide = GuideEncodingTools.bedLineToCRISPRSiteOT(ln, bitEnc, posEncoder, overflowVal, annotations.toArray.sortBy(ki => ki._2).map{i => i._1})

        guides += guide
      }
    }}

    (posEncoder,guides.toArray)
  }

}


