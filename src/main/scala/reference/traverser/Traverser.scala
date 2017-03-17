package reference.traverser

import java.io._

import bitcoding.{BitEncoding, BitPosition}
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import utils.BaseCombinationGenerator
import reference.binary.{BinaryConstants, BinaryHeader}
import reference.traversal.BinTraversal
import standards.ParameterPack

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ArrayBuilder, LinkedHashMap, Map}

/**
  * an object that traverses over a file
  */
trait Traverser {
  /**
    * scan against the binary database of off-target sites in an implmenetation specific way
    *
    * @param binaryFile    the file we're scanning from
    * @param header        we have to parse the header ahead of time so that we know
    * @param traversal     the traversal over bins we'll use
    * @param targets       the array of candidate guides we have
    * @param maxMismatch   how many mismatches we support
    * @param configuration our enzyme configuration
    * @param bitCoder      our bit encoder
    * @param posCoder      the position encoder
    * @return a guide to OT hit array
    */
  def scan(binaryFile: File,
           header: BinaryHeader,
           traversal: BinTraversal,
           targets: Array[CRISPRSiteOT],
           maxMismatch: Int,
           configuration: ParameterPack,
           bitCoder: BitEncoding,
           posCoder: BitPosition): Array[CRISPRSiteOT]

}

/**
  * common methods that any traverser may need -- unified here to help future parallelism
  */
object Traverser extends LazyLogging {

  // record the total number of comparisons we do
  var allComparisions = 0l

  // compute the mismatch count between two strings
  def mismatches(str1: String, str2: String): Int = str1.zip(str2).map { case (a, b) => if (a == b) 0 else 1 }.sum

  // format a number into a comma-seperated list
  val formatter = java.text.NumberFormat.getInstance()

  /**
    * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
    * each guide it matches
    *
    * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
    * @param guides                     the guides
    * @return an mapping from a potential guide to it's discovered off target sequences
    */
  def compareBlock(blockOfTargetsAndPositions: Array[Long],
                   numberOfTargets: Int,
                   guides: Array[Long],
                   bitEncoding: BitEncoding,
                   maxMismatches: Int,
                   bin: Long,
                   binMask: Long): Array[Array[CRISPRHit]] = {

    val returnArray = Array.fill[ArrayBuffer[CRISPRHit]](guides.size)(new ArrayBuffer[CRISPRHit]())

    // filter down the guides we actually want to look at
    val lookAtGuides = guides.filter(gd => bitEncoding.mismatches(gd, bin, binMask) <= maxMismatches)

    // we aim to be as fast as possible here, so while loops over foreach's, etc
    var offset = 0
    var currentTarget = 0l
    var targetIndex = 0

    // process each target in the block
    while (targetIndex < numberOfTargets) {
      targetIndex += 1


      val currentTarget = blockOfTargetsAndPositions(offset)
      val count = bitEncoding.getCount(currentTarget)

      require(blockOfTargetsAndPositions.size >= offset + count,
        "Failed to correctly parse block, the number of position entries exceeds the buffer size, count = " + count + " offset = " + offset + " block size " + blockOfTargetsAndPositions.size)

      val positions = blockOfTargetsAndPositions.slice(offset + 1, (offset + 1) + count)


      var guideIndex = 0
      while (guideIndex < lookAtGuides.size) {

        allComparisions += 1
        val mismatches = bitEncoding.mismatches(lookAtGuides(guideIndex), currentTarget)
        if (mismatches <= maxMismatches) {
          returnArray(guideIndex) += CRISPRHit(blockOfTargetsAndPositions(offset), blockOfTargetsAndPositions.slice(offset, offset + count + 1))
        }
        guideIndex += 1
      }
      offset += count + 1
    }

    val returnMap = new mutable.HashMap[Long, Array[CRISPRHit]]()
    returnArray.map{case(offTargetsFound) => offTargetsFound.toArray}
  }
}
