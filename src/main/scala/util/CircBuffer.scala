package util

/**
 * Created by aaronmck on 6/16/15.
 */
class CircBuffer(bSize: Int) {
  val bufferSize = bSize
  val circBuffer = new Array[Char](bufferSize)
  var added = 0
  var currentPos = 0

  // add a base to the circbuffer
  def addBase(base: Char) {
    circBuffer(currentPos) = base
    currentPos += 1
    if (currentPos >= bufferSize)
      currentPos = 0
    added += 1
  }

  // check a buffer
  def isBase(pos: Int, base: Char): Boolean = {
    if (pos > bufferSize)
      throw new IllegalArgumentException("Position " + pos + " is greater than the buffer size " + bufferSize)
    val offset = currentPos - (bufferSize - pos)
    if (offset >= 0)
      return circBuffer(offset) == base
    else
      return circBuffer(bufferSize + offset) == base
  }

  // return a tuple with two booleans: is there a pam in the forward, and is there a
  // pam in the reverse dir
  //
  def isPAM(): Tuple2[Boolean,Boolean] = {
    if (added < bufferSize)
      return (false,false)
    Tuple2[Boolean,Boolean]((isBase(bufferSize-1,'G') && isBase(bufferSize-2,'G')),(isBase(0,'C') && isBase(1,'C')))
  }

  def reverseComp(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
  def reverseCompString(str: String): String = str.map{reverseComp(_)}.reverse.mkString
  def toTarget(): String = circBuffer.slice(currentPos,bufferSize).mkString + circBuffer.slice(0,currentPos).mkString
}
