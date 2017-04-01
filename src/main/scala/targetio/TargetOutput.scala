package targetio

import java.io.PrintWriter

import bitcoding.{BitEncoding, BitPosition}
import crispr.CRISPRSiteOT

/**
  * handle outputing the target list, in a style the user requests
  */
object TargetOutput {

  def output(outputFile: String,
             targets: Array[CRISPRSiteOT],
             includePositionInformation: Boolean,
             indicateIfTargetHasPefectMatch: Boolean,
             bitEncoder: BitEncoding,
             bitPosition: BitPosition,
             activeAnnotations: Array[String]
            ) {

    // sort the target array
    scala.util.Sorting.quickSort(targets)

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
    targets.foreach { guide => {
      val targetString = GuideEncodingTools.guideToBedString(guide,
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