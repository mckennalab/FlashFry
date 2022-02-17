package bitcoding

import org.scalatest._

import scala.util.Random

/**
  * test that we can encode and decode string and counts to a binary Long
  */
class BitPositionTest extends FlatSpec with Matchers {

  val encodingSize = 20
  val rando = new Random()

  "A Bit Position" should "warn the user when they've added too many contigs (1M+)" in {
    val bitPos = new BitPosition()
    try {
      (0 until 0x1fffff).foreach { count => bitPos.addReference("chr" + count.toString) }
      fail()
    } catch {
      case _: java.lang.AssertionError => // Expected, so continue
    }
  }

  "A Bit Position" should "correctly encode a forward strand position" in {
    val bitPos = new BitPosition()
    bitPos.addReference("chr1")
    bitPos.addReference("chr2")

    val chrom = "chr2"
    val position = 1000
    val len = 23
    val forStrand = true

    val encoding = bitPos.encode(chrom, position, len, forStrand)
    val decode = bitPos.decode(encoding)

    (decode.contig) should be(chrom)
    (decode.start) should be(position)
    (decode.length) should be(len)
    (decode.forwardStrand) should be(forStrand)
  }

  "A Bit Position" should "correctly encode a reverse strand position" in {
    val bitPos = new BitPosition()
    bitPos.addReference("chr1")
    bitPos.addReference("chr2")

    val chrom = "chr2"
    val position = 102200
    val len = 23
    val forStrand = false

    val encoding = bitPos.encode(chrom, position, len, forStrand)
    val decode = bitPos.decode(encoding)

    (decode.contig) should be(chrom)
    (decode.start) should be(position)
    (decode.length) should be(len)
    (decode.forwardStrand) should be(forStrand)
  }

  "A Bit Position" should "correctly determine if two things overlap" in {
    val bitPos = new BitPosition()
    bitPos.addReference("chr1")
    bitPos.addReference("chr2")

    val chrom = "chr2"
    val position = 100
    val len = 23
    val forStrand = false

    val encoding = bitPos.encode(chrom, position, len, forStrand)
    val decode = bitPos.decode(encoding)

    // different chromosome
    (decode.overlap("chr1", 100, 130)) should be(false)

    // the bit positions is embedded completely within the range
    (decode.overlap("chr2", 10, 130)) should be(true)

    // check overlap by one base on the low end of the bit positions
    (decode.overlap("chr2", 90, 100)) should be(false)
    (decode.overlap("chr2", 90, 101)) should be(true)

    // they overlap by one base on the high end of the bit positions
    (decode.overlap("chr2", 122, 200)) should be(true)

    // the new region is embeded within the bit position
    (decode.overlap("chr2", 110, 115)) should be(true)

    // same chromosome, different position
    (decode.overlap("chr2", 1100, 1150)) should be(false)
  }
}