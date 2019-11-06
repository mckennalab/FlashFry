package scoring

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPRSite, CRISPRSiteOT}
import standards.ParameterPack
import standards.Cas9ParameterPack
import utils.Utils

import scala.util.Random

/**
  * test out the closest hit module
  */
class ClosestHitTest extends FlatSpec with Matchers {

  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val sequence = "GACTTGCATCCGAAGCCGGTGGG"
  val target = new CRISPRSite("chr8", sequence, true, 150, None)

  "ClosestHit" should "count a single off-target correctly" in {
    val singleOffTarget = ClosestHitTest.generateOffTargetArray(sequence, 0, 20, Array[Int](1), Array[Short](1), bitEncoder)
    (singleOffTarget.offTargets.size should be(1))

    val closestHit = new ClosestHit()
    closestHit.bitEncoder(bitEncoder)
    val scored = closestHit.scoreGuide(singleOffTarget)

    (scored(0)(0) should be("1"))
    (scored(1)(0) should be("1"))
    (scored(2)(0) should be("0,1,0,0,0"))
  }

  "ClosestHit" should "count a higher count off-target correctly" in {
    val singleOffTarget = ClosestHitTest.generateOffTargetArray(sequence, 0, 20, Array[Int](1), Array[Short](40), bitEncoder)

    val closestHit = new ClosestHit()
    closestHit.bitEncoder(bitEncoder)
    val scored = closestHit.scoreGuide(singleOffTarget)

    (scored(0)(0) should be("1"))
    (scored(1)(0) should be("40"))
    (scored(2)(0) should be("0,40,0,0,0"))
  }


  "ClosestHit" should "count many more off-target correctly" in {
    val singleOffTarget = ClosestHitTest.generateOffTargetArray(sequence, 0, 20, Array[Int](1, 1, 2, 4), Array[Short](40, 30, 20, 10), bitEncoder)

    val closestHit = new ClosestHit()
    closestHit.bitEncoder(bitEncoder)
    val scored = closestHit.scoreGuide(singleOffTarget)

    (scored(0)(0) should be("1"))
    (scored(1)(0) should be("70"))
    (scored(2)(0) should be("0,70,20,0,10"))
  }

}


object ClosestHitTest {
  /**
    * generate a fake CRISPR candidate target with a number of off-targets. Of note the target is set to chr8, foward strand
    * at postion 150 (arbitrary choices)
    *
    * @param sequence            the sequence to use
    * @param offTargetMismatches an array of ints, where each int represents the number of mismatches the off-target should have
    * @param offTargetCounts     for each off target, how many occurances should we have
    * @param bitEncoder          our BitEncoder for turning locations into Longs
    * @return the CRISPRSite containing the off-targets generated with the input parameters
    */
  def generateOffTargetArray(sequence: String, start: Int, stop: Int, offTargetMismatches: Array[Int], offTargetCounts: Array[Short], bitEncoder: BitEncoding): CRISPRSiteOT = {
    val target = new CRISPRSite("chr8", sequence, true, 150, None)

    assert(offTargetCounts.size == offTargetMismatches.size)

    val crisprOT = new CRISPRSiteOT(target, bitEncoder.bitEncodeString(StringCount(target.bases, 1)), 1000)

    // save the generated sequences so we can check we've not made duplicates
    var generatedSequences = Array[String]()

    offTargetMismatches.zip(offTargetCounts).zipWithIndex.foreach { case ((mismatches, count), index) =>

      // make a mismatched sequence
      val positions = Random.shuffle(List.range(start,stop)).take(mismatches)
      var newRandoString = randomizeString(sequence, positions)
      while (generatedSequences contains newRandoString) {
        newRandoString = randomizeString(sequence, positions)
      }
      crisprOT.addOT(new CRISPRHit(bitEncoder.bitEncodeString(StringCount(newRandoString, count)), Array[Long]()))
    }
    crisprOT
  }

  def randomizeString(str: String, positions: List[Int]): String = {
    var mystr = str
    positions.foreach { pos =>
      mystr = mystr.slice(0, pos) + ClosestHitTest.randomOtherBase(mystr(pos)) + mystr.slice(pos + 1, mystr.length)
    }
    mystr
  }

  /**
    * randomize a base to any base besides what it currently is
    *
    * @param original the original base
    * @return a new random base
    */
  def randomOtherBase(original: Char): Char = {
    val rd = new Random()
    var nextBase = Utils.indexToBase(rd.nextInt(4))
    while (nextBase == original)
      nextBase = Utils.indexToBase(rd.nextInt(4))
    nextBase
  }
}