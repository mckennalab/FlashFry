
package scoring

import java.io.File

import bitcoding.BitEncoding
import crispr.CRISPRSiteOT
import rnafold4j.RNAFoldAPI
import standards.ParameterPack

import scala.collection.mutable


/**
  * 'score' each target with information from a bed file
  *
  **/
class RNAFold4j extends SingleGuideScoreModel {
  var mBitENcoder : Option[BitEncoding] = None
  var mParameterPack: Option[ParameterPack] = None
  var smallestGuide: Option[Int] = None
  var lengthPairPositions = new mutable.LinkedHashMap[Int,Tuple2[Int,Int]]()


  /**
    * setup a map of guide length to positions within the target
    */
  def setupGuideLengths(): Unit = {
    assert(smallestGuide.isDefined)

    val guideLength = math.abs(mParameterPack.get.guideRange._1 - mParameterPack.get.guideRange._2)
    for (i <- smallestGuide.get to guideLength) {
      if (mParameterPack.get.fivePrimePam) {
        lengthPairPositions(i) = (mParameterPack.get.guideRange._1, mParameterPack.get.guideRange._1 + i)
      } else {
        lengthPairPositions(i) = (mParameterPack.get.guideRange._2 - i, mParameterPack.get.guideRange._2)
      }
    }
  }

  /**
    * score an individual guide
    *
    * @param guide the guide with it's off-targets
    * @return an array of arrays (ugh). For each column the scoring method can return multiple pieces of information
    *         which are then merged at the output stage
    */
  override def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    var energies = Array[Array[String]]()
    lengthPairPositions.map{case(len,(start,stop)) => {
      val seq = new String(guide.target.bases.slice(start,stop))
      energies :+= Array[String](scoreSequence(seq).toString())
    }}
    energies
  }

  /**
    * @param sequence calculate the free energy for this sequence
    * @return
    */
  def scoreSequence(sequence: String): Double = {
    assert(mParameterPack.isDefined,"Undefined enzyme parameter pack")

    // The sequence to be predicted
    val seq = new String(sequence).getBytes() // .slice(mParameterPack.get.guideRange._1,mParameterPack.get.guideRange._2)

    // Create a new instance of RNAFold4J
    val rfa = new RNAFoldAPI();

    // Predict the MFE and corresponding structure
    val mfe = rfa.getMFE(seq).mfe

    mfe
  }

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "RNAFold4j"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "RNAFold4j free energy calculation"

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = lengthPairPositions.keys.map{k => "FreeEnergy" + k}.toArray

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(enzyme: ParameterPack): Boolean = {
    mParameterPack = Some(enzyme)
    true
  }

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = {
    true
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def setup(): Unit = {
    setupGuideLengths()
  }

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {mBitENcoder = Some(bitEncoding)}
}
