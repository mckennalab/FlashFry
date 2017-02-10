package bitcoding

import java.io.{IOException, PrintWriter}

import scala.collection.mutable
import scala.io.Source

/**
  * the bit position mapping
  */
class BitPosition {

  val contigMap = new mutable.HashMap[String,Int]()
  val indexToContig = new mutable.HashMap[Int,String]()
  var nextSeqId = 1
  var mask = 0xFFFFFFFF

  def addReference(refName: String): Unit = {
    contigMap(refName) = nextSeqId
    indexToContig(nextSeqId) = refName
    nextSeqId += 1
  }

  def encode(refName: String, position: Int): Long = {
    if (!(contigMap contains refName))
      throw new IllegalStateException("Reference name is not in map: " + refName)
    val contigNumber: Int = contigMap(refName)
    (contigNumber.toLong << 32) | position.toLong
  }

  def decode(encoding: Long): Tuple2[String,Int] = {
    val contig = indexToContig((encoding >> 32).toInt)
    (contig,(encoding & mask).toInt)
  }

}

object BitPosition {

  def toFile(bitPos: BitPosition, outputFile: String): Unit = {
    val pr = new PrintWriter(outputFile)
    (1 until bitPos.nextSeqId).foreach{index => {
      pr.write(index + "\t" + bitPos.indexToContig(index) + "\n")
    }}

    pr.close()
  }

  def fromFile(inputFile: String): BitPosition = {
    try {
      val bitPos = new BitPosition()
      Source.fromFile(inputFile).getLines().foreach { line => {
        val sp = line.split("\t")
        bitPos.addReference(sp(1))
      }
      }
      bitPos
    } catch {
      case e: IOException => throw new IllegalStateException("Unable to load position mapping file: " + inputFile)
    }

  }

  def positionExtension = ".bitPositionFile"
}
