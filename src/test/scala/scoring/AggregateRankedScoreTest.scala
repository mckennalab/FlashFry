package scoring
import java.io.File

import bitcoding.{BitEncoding, BitPosition, StringCount}
import crispr.{CRISPRHit, CRISPROTFactory, CRISPRSite, CRISPRSiteOT}
import org.scalatest.{FlatSpec, Matchers}
import standards.{Cas9ParameterPack, ParameterPack}


class AggregateRankedScoreTest extends FlatSpec with Matchers {
  val bitEncoder = new BitEncoding(Cas9ParameterPack)
  val target = new CRISPRSite("chr8", "GACTTGCATCCGAAGCCGGTGGG", true, 150, None)
  val posEnc = new BitPosition()

  "AggregateRankedScore" should "correctly rank scores by a descending score" in {
    val fakeTargets = Range(0,100).map{ind => CRISPROTFactory.createFakeCRISPROT()}.toArray
    val fakeRankedScore1 = new FakeRankedScore1()
    fakeRankedScore1.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)

    val aggScore = new AggregateRankedScore()
    aggScore.initializeScoreNames(List[RankedScore](fakeRankedScore1))

    aggScore.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)

    var targetRank = 1
    fakeTargets.foreach{gd => {
      (gd.namedAnnotations(aggScore.scoreName() + "_medianTranche").mkString("")) should be (Math.ceil(targetRank/25.0).toInt.toString)
      (gd.namedAnnotations(aggScore.scoreName() + "_medianRank").mkString("")) should be (targetRank.toString)
      targetRank += 1
    }}
  }


  "AggregateRankedScore" should "handle a scoring metric that ranks in the reverse order (high scores are bad)" in {
    val fakeTargets = Range(0,100).map{ind => CRISPROTFactory.createFakeCRISPROT()}.toArray
    val fakeRankedScore2 = new FakeRankedScore2()
    fakeRankedScore2.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)

    val aggScore = new AggregateRankedScore()
    aggScore.initializeScoreNames(List[RankedScore](fakeRankedScore2))

    aggScore.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)

    var targetRank = 100
    fakeTargets.foreach{gd => {
      (gd.namedAnnotations(aggScore.scoreName() + "_medianTranche").mkString("")) should be (Math.ceil(targetRank/25.0).toInt.toString)
      (gd.namedAnnotations(aggScore.scoreName() + "_medianRank").mkString("")) should be (targetRank.toString)
      targetRank -= 1
    }}
  }



  "AggregateRankedScore" should "correctly merge two score systems into a unified rank" in {
    val fakeTargets = Range(0,100).map{ind => CRISPROTFactory.createFakeCRISPROT()}.toArray

    val fakeRankedScore1 = new FakeRankedScore1()
    val fakeRankedScore2 = new FakeRankedScore2()

    fakeRankedScore1.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)
    fakeRankedScore2.scoreGuides(fakeTargets.reverse,bitEncoder,posEnc,Cas9ParameterPack)

    val aggScore = new AggregateRankedScore()
    aggScore.initializeScoreNames(List[RankedScore](fakeRankedScore1,fakeRankedScore2))
    aggScore.scoreGuides(fakeTargets,bitEncoder,posEnc,Cas9ParameterPack)

    var targetRank = 1
    //println("SZ = = " + fakeTargets.size)
    fakeTargets.foreach{gd => {
      //println(gd.namedAnnotations.map{case(k,v) => k + " -> " + v.mkString("_")}.mkString(","))
      (gd.namedAnnotations(aggScore.scoreName() + "_medianTranche").mkString("")) should be (Math.ceil(targetRank/25.0).toInt.toString)
      (gd.namedAnnotations(aggScore.scoreName() + "_medianRank").mkString("")) should be (targetRank.toString)
      targetRank += 1
    }}
  }
}

class FakeRankedScore1 extends RankedScore {
  override def highScoreIsGood: Boolean = true

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "fakeRankBestHigh"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "fakeRankBestHigh"

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("fakeRankBestHigh")


  var fakeGuideCounter = 0

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack): Unit = {
    guide.foreach{gd => {
      fakeGuideCounter += 1
      gd.namedAnnotations(scoreName()) = Array[String](fakeGuideCounter.toString)
    }}
  }

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = args

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}
}


class FakeRankedScore2(tieTopX: Int = 0) extends RankedScore {
  override def highScoreIsGood: Boolean = false

  /**
    * @return the name of this score model, used to look up the models when initalizing scoring
    */
  override def scoreName(): String = "fakeRankBestLow"

  /**
    * @return the description of method for the header of the output file
    */
  override def scoreDescription(): String = "fakeRankBestLow"

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String] = Array[String]("fakeRankBestLow")


  var fakeGuideCounter = 0
  var remainingTiesToUse = tieTopX

  /**
    * score an array of guides. We provide all the guides at once because some metrics
    * look at reciprocal off-targets, or are better suited to traverse an input file once
    * while considering all guides (like BED annotation)
    *
    * @param guide the guide with it's off-targets
    * @return a score (as a string)
    */
  override def scoreGuides(guide: Array[CRISPRSiteOT], bitEnc: BitEncoding, posEnc: BitPosition, pack: ParameterPack): Unit = {
    guide.foreach{gd => {
      if (remainingTiesToUse > 0) {
        remainingTiesToUse -= 1
      } else {
        fakeGuideCounter += 1
      }
      gd.namedAnnotations(scoreName()) = Array[String](fakeGuideCounter.toString)
    }}
  }

  /**
    * are we valid over the enzyme of interest?
    *
    * @param enzyme the enzyme (as a parameter pack)
    * @return if the model is valid over this data
    */
  override def validOverScoreModel(enzyme: ParameterPack): Boolean = true

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverGuideSequence(enzyme: ParameterPack, guide: CRISPRSiteOT): Boolean = true

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def parseScoringParameters(args: Seq[String]): Seq[String] = args

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {}
}