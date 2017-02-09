package bitcoding

import java.lang.{Integer => JavaInteger}

import org.scalatest._

import scala.util.Random

/**
  * test that we can encode and decode string and counts to a binary Long
  */
class BitPositionTest extends FlatSpec with Matchers {

  val encodingSize = 20
  val rando = new Random()

  "A Bit Position" should "correctly encode a position" in {
    val bitPos = new BitPosition()
    bitPos.addReference("chr1")
    bitPos.addReference("chr2")

    val chrom = "chr2"
    val position = 1000

    val encoding = bitPos.encode(chrom,position)
    val decode = bitPos.decode(encoding)

    (decode._1) should be(chrom)
    (decode._2) should be(position)
  }
}