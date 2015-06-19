package crispr

import util.CircBuffer

/**
 * Created by aaronmck on 6/16/15.
 */
class PAMBuffer (bufferSize: Int) extends CircBuffer(bufferSize) {

  // return a list of CRISPR target hits for the given line of sequencing.  The return list
  // includes the sequence including the PAM as seen on the strand of interest, the position,
  // and if we found it on the watson (true) or crick (false) strand
  def addLine(line: String): List[Tuple3[String,Long,Boolean]] = {
    var ret = List[Tuple3[String,Long,Boolean]]()
    line.foreach{chr => {
      addBase(chr)
      val is_pam = isPAM()
      if (is_pam._1)
        ret :+= (toTarget(),added.toLong,true)
      if (is_pam._2)
        ret :+= (reverseCompString(toTarget()),added.toLong,false)
    }}
    ret
  }
}