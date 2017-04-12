package targetio

import java.io.PrintWriter

import bitcoding.{BitEncoding, BitPosition}
import crispr.{CRISPRSiteOT, ResultsAggregator}

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

    // create a output file
    val output = new PrintWriter(outputFile)

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