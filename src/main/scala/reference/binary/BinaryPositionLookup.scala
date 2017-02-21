package reference.binary

import main.scala.util.BaseCombinationGenerator

import scala.collection.mutable

/**
  * handle our mapping from bin to size / count
  * @param binGenerator the bin generation
  */
case class BinaryPositionLookup(binGenerator: BaseCombinationGenerator) {

  val guidesPerBin = new mutable.HashMap[String, Int]()
  val longsPerBin = new mutable.HashMap[String, Int]()

  def append(baseString: String, guides: Int, numLongs: Int) {
    guidesPerBin(baseString) = guides
    longsPerBin(baseString) = guides
  }

  def toBinaryBlock(): Array[Long] = {
    val longs = mutable.ArrayBuilder.make[Long]

    binGenerator.foreach{bin => {
      longs += longsPerBin.getOrElse(bin,0).toLong
    }}

    longs.result()
  }
}
