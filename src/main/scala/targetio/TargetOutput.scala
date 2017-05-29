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

import java.io.PrintWriter

import bitcoding.{BitEncoding, BitPosition}
import crispr.{CRISPRSiteOT, ResultsAggregator}
import utils.Utils

/**
  * handle outputing the target list, in a style the user requests
  */
object TargetOutput {

  def output(outputFile: String,
             guides: ResultsAggregator,
             includePositionInformation: Boolean,
             indicateIfTargetHasPefectMatch: Boolean,
             bitEncoder: BitEncoding,
             bitPosition: BitPosition,
             activeAnnotations: Array[String]
            ) {

    // create a output file -- gzip if the file ends with gz
    val output = if (outputFile endsWith ".gz") new PrintWriter(Utils.gos(outputFile)) else new PrintWriter(outputFile)

    // output the positional contig information
    (1 until bitPosition.nextSeqId).foreach { pos => {
      output.write("# contig:" + bitPosition.indexToContig(pos) + "\n")
    }
    }

    activeAnnotations.zipWithIndex.foreach{case(annotation,index) => {
      output.write("# annotation:" + annotation + ":index:" + index + "\n")
    }}

    // output the targets
    guides.wrappedGuides.foreach { guide => {
      val targetString = GuideEncodingTools.guideToBedString(guide.otSite,
        bitEncoder,
        bitPosition,
        indicateIfTargetHasPefectMatch,
        includePositionInformation,
      activeAnnotations)
      output.write(targetString + "\n")
    }
    }

    output.close()
  }
}