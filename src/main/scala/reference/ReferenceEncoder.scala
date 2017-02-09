package reference

import java.io._

import bitcoding.{BitEncoding, BitPosition}

import scala.io._

/**
  * encode a reference file to a binary file
  */
object ReferenceEncoder {

  def encodeFile(reference: File, outputFile: File, guideSize: Int, pamSequence: String, guideFivePrime: Boolean): Unit = {

    val bitEncoder = new BitEncoding(guideSize)
    val posEncoder = new BitPosition()
    val cls = CRISPRCircle(outputFile.getAbsolutePath, guideSize, pamSequence, !guideFivePrime)

    Source.fromFile(reference).getLines().foreach{line => {
      if (line.startsWith(">")) {
        println("Switching to chromosome " + line)
        posEncoder.addReference(line.stripPrefix(">").split(" ")(0))
        cls.reset(line.split(" ")(0).slice(1,line.split(" ")(0).length))
      } else {
        cls.addLine(line.toUpperCase)
      }
    }}

    cls.output.close()
  }
}


// a hit for a CRISPR site in the genome
case class CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, cutSite: Int) {
  val sep = "\t"
  val strandOutput = if (forwardStrand) "FWD" else "RVS"
  def to_output = contig + sep + position + sep + cutSite + sep + bases + sep + strandOutput
}

object CRISPRSite {
  def header = "contig\tposition\tcutsite\tbases\tstrand\n"
}

// --------------------------------------------------------------------------------------------
// this class finds potential CRISPR cutsites in the genome -- we use a rolling (circular)
// buffer to speed things up
// --------------------------------------------------------------------------------------------
case class CRISPRCircle(outputFile: String, guideSize: Int, pamSequence: String, pamThreePrime: Boolean = true) {

  val totalBufferSize = guideSize + pamSequence.size
  println("totalBufferSize " + totalBufferSize)
  val revCompPam = reverseCompString(pamSequence)
  val cutSiteFromEnd = 6
  var stack = new Array[Char](totalBufferSize)
  var currentPos = 0
  var contig = "UNKNOWN"
  val output = new PrintWriter(outputFile)
  //output.write(CRISPRSite.header)

  def addLine(line: String) {line.foreach{chr => {addBase(chr)}}}

  /**
    * add a base to our circular buffer
    */
  def addBase(chr: Char) {
    stack(currentPos % totalBufferSize) = chr
    currentPos += 1
    if (currentPos >= totalBufferSize)
      checkCRISPR().foreach{ct => {
        //val encoding = bitEncoder.bitEncodeString(StringCount(ct.bases.slice(0,bufferSize),1))
        //guideToCount(encoding) = guideToCount.getOrElse(encoding,0) + 1
        //positionToGuide(encoding) = posEncoder.encode(contig,currentPos - (bufferSize + pamSequence.size))
        output.write(ct.to_output + "\n")
      }}
  }

  def reset(cntig: String) {
    contig = cntig
    currentPos = 0
  }

  def checkCRISPR(): Array[CRISPRSite] = {
    val str = toTarget()
    if (str.map{base => if (base == 'A' || base == 'C' || base == 'G' || base == 'T') 0 else 1}.sum > 0)
      return Array[CRISPRSite]()

    var ret = Array[CRISPRSite]()

    if (pamThreePrime) {
      if (str.endsWith(pamSequence))
        ret :+= CRISPRSite(contig, str, true, (currentPos - totalBufferSize), (currentPos - cutSiteFromEnd))
      if (str.startsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, (currentPos - totalBufferSize), (currentPos - totalBufferSize) + cutSiteFromEnd)
      ret
    } else {
      if (str.startsWith(pamSequence))
        ret :+= CRISPRSite(contig, str, true, (currentPos - totalBufferSize), (currentPos - cutSiteFromEnd))
      if (str.endsWith(revCompPam))
        ret :+= CRISPRSite(contig, reverseCompString(str), false, (currentPos - totalBufferSize), (currentPos - totalBufferSize) + cutSiteFromEnd)
      ret
    }
  }

  // utilities to reverse complement
  def reverseComp(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
  def reverseCompString(str: String): String = str.map{reverseComp(_)}.reverse.mkString

  // create a target string from the buffer
  def toTarget(): String = stack.slice(currentPos % totalBufferSize,totalBufferSize).mkString + stack.slice(0,currentPos % totalBufferSize).mkString.toUpperCase()

}

