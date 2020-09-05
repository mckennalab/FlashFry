package scoring

import bitcoding.BitEncoding
import com.typesafe.scalalogging.LazyLogging
import crispr.{CRISPRHit, CRISPRSiteOT}
import standards.{Cas9ParameterPack, Cas9Type, ParameterPack, SpCAS9}
import utils.Utils

import scala.collection.mutable

class JostAndSantosCRISPRi extends SingleGuideScoreModel with LazyLogging with RankedScore {

  private var bitEncoder: Option[BitEncoding] = None

  override def scoreName(): String = "JostAndSantosCRISPRi"

  override def scoreDescription(): String = "CRISPRi score developed by Jost and Santos (BioRxiv, 2019)"

  private var parameterPack: Option[ParameterPack] = None

  /**
    * score an individual guide's off-targets
    *
    * @param guide the guide with it's off-targets
    * @return an array of scores
    */
  def scoreGuide(guide: CRISPRSiteOT): Array[Array[String]] = {
    require(validOverTargetSequence(Cas9ParameterPack, guide), "We're not a valid score over this guide: " + guide.target.bases)

    val sequence = bitEncoder.get.bitDecodeString(guide.longEncoding)

    val scores = guide.offTargets.map { ot => {
      // compare the targets, only considering their active bases
      val baseDifferences = bitEncoder.get.mismatches(ot.sequence, guide.longEncoding)

      (calc_score(sequence.str, bitEncoder.get.bitDecodeString(ot.sequence).str),
        ot.getOffTargetCount,
        baseDifferences)
    }
    }.filter { case (score, otCount, baseDifferences) => (baseDifferences > 0) }

    val specificity_score = 1.0 / (1.0 + scores.map { case (score, count, areEqual) => score * count }.sum)
    val maxscore = if (scores.size == 0) 0.0 else scores.map { st => st._1 }.max.toString

    Array[Array[String]](Array[String](maxscore.toString), Array[String](specificity_score.toString))
  }

  /**
    * this method is valid for Cas9
    *
    * @return if the model is valid over this data
    */
  override def validOverEnzyme(pack: ParameterPack): Boolean = {
    this.parameterPack = Some(pack)

    pack.enzyme.enzymeParent == Cas9Type &&
      (pack.totalScanLength == ParameterPack.cas9ScanLength20mer || pack.totalScanLength == ParameterPack.cas9ScanLength19mer)
  }

  /**
    * parse out any command line arguments that are optional or required for this scoring metric
    *
    * @param args the command line arguments
    */
  override def setup() {}

  /**
    * set the bit encoder for this scoring metric
    *
    * @param bitEncoding the bit encoder to use
    */
  override def bitEncoder(bitEncoding: BitEncoding): Unit = {
    bitEncoder = Some(bitEncoding)
  }

  /**
    * given a enzyme and guide information, can we score this sequence? For instance the on-target sequence
    * scores sometimes take base-context on each side, and without that cannot score the guide
    *
    * @param enzyme the enzyme of choice, with parameters
    * @param guide  the guide sequence we want to score
    * @return are we valid. Scoring methods should also lazy log a warning that guides will be droppped, and why
    */
  override def validOverTargetSequence(pack: ParameterPack, guide: CRISPRSiteOT): Boolean = {
    if (pack.enzyme == SpCAS9) true else false
  }

  /**
    * the main function: here we calculate the CRISPRi scores for each individual off-target and return
    * the highest offf-target CRISPRi score as well as an aggregate score
    */
  def calc_score(target: String, offTargetString: String): Double = {
    var totalScore = 1.0
    assert(target.size == parameterPack.get.totalScanLength, "target should be length " + parameterPack.get.totalScanLength + " we saw " + target.size)
    assert(offTargetString.size == parameterPack.get.totalScanLength, "off-target should be length " + parameterPack.get.totalScanLength + " we saw " + offTargetString.size)


    parameterPack.get.totalScanLength match {
      case ParameterPack.cas9ScanLength20mer => {
        offTargetString.slice(1, 20).zipWithIndex.foreach { case (base, index) => {
          if (target(index + 1) != base) {
            val originalPair = Utils.compBase(target(index + 1))
            val lookup = ScoreLookup(index + 1, base, originalPair)
            val conversion = JostAndSantosCRISPRi.scoreMapping(lookup)

            totalScore *= conversion.mean
          }
        }
        }
        totalScore
      }
      case ParameterPack.cas9ScanLength19mer => {
        offTargetString.slice(0, 19).zipWithIndex.foreach { case (base, index) => {
          if (target(index) != base) {
            val originalPair = Utils.compBase(target(index))
            val lookup = ScoreLookup(index+1, base, originalPair)
            val conversion = JostAndSantosCRISPRi.scoreMapping(lookup)

            totalScore *= conversion.mean
          }
        }
        }
        totalScore
      }
      case _ => throw new IllegalStateException("Unable to match parameter pack: " + parameterPack)
    }
  }

  /**
    * @return get a listing of the header columns for this score metric
    */
  override def headerColumns(): Array[String]

  = Array[String]("JostCRISPRi_maxOT", "JostCRISPRi_specificityscore")

  /**
    * @return true, a high score is good
    */
  override def highScoreIsGood: Boolean

  = true
}

case class ScoreLookup(position: Int, baseA: Char, baseB: Char)

case class CRISPRiEntry(position: Int, baseTransistion: String, mean: Double, std: Double, count: Int)

// constants they use in the paper
object JostAndSantosCRISPRi {


  val scores: Array[CRISPRiEntry] = Array[CRISPRiEntry](
    CRISPRiEntry(1, "rA:dA", 1.0134565761307748, 0.2549818424674031, 46),
    CRISPRiEntry(1, "rA:dC", 0.8004592799862285, 0.33756158996224805, 186),
    CRISPRiEntry(1, "rA:dG", 0.9244558615632211, 0.3233759978093056, 139),
    CRISPRiEntry(1, "rC:dA", 0.9431474929693217, 0.41046937414810636, 68),
    CRISPRiEntry(1, "rC:dC", 0.6969679011207074, 0.4035556990318134, 181),
    CRISPRiEntry(1, "rC:dT", 0.7892831106455067, 0.4288844258878258, 94),
    CRISPRiEntry(1, "rG:dA", 0.9114679730044981, 0.3614716042065421, 65),
    CRISPRiEntry(1, "rG:dG", 0.8195660943080503, 0.44199043169509855, 136),
    CRISPRiEntry(1, "rG:dT", 0.9444743223687666, 0.3924933375777219, 85),
    CRISPRiEntry(1, "rU:dC", 0.7008038648561387, 0.39525514092076774, 208),
    CRISPRiEntry(1, "rU:dG", 0.8942297529777764, 0.30782105229450046, 141),
    CRISPRiEntry(1, "rU:dT", 0.7952747759038213, 0.33191305577545627, 87),
    CRISPRiEntry(2, "rA:dA", 0.8477000201430482, 0.32990088686579566, 70),
    CRISPRiEntry(2, "rA:dC", 0.7962078279947851, 0.37177438580687566, 157),
    CRISPRiEntry(2, "rA:dG", 0.7531190640965973, 0.3849759498156709, 174),
    CRISPRiEntry(2, "rC:dA", 0.8554298997350135, 0.4103856419942698, 63),
    CRISPRiEntry(2, "rC:dC", 0.7939045485020192, 0.44022137832437297, 157),
    CRISPRiEntry(2, "rC:dT", 0.8115299265648859, 0.36879786524547925, 62),
    CRISPRiEntry(2, "rG:dA", 0.8037856858053112, 0.4050931097399671, 61),
    CRISPRiEntry(2, "rG:dG", 0.798607617464852, 0.41309390165093274, 137),
    CRISPRiEntry(2, "rG:dT", 0.8865603384171252, 0.4275383719488994, 95),
    CRISPRiEntry(2, "rU:dC", 0.7396730198146739, 0.36531874372335793, 181),
    CRISPRiEntry(2, "rU:dG", 0.7702015235332976, 0.4191899592320365, 142),
    CRISPRiEntry(2, "rU:dT", 0.758259177243809, 0.40403783012053024, 90),
    CRISPRiEntry(3, "rA:dA", 0.7490602117349844, 0.41315273165627003, 66),
    CRISPRiEntry(3, "rA:dC", 0.6452139305042132, 0.4359434228501023, 190),
    CRISPRiEntry(3, "rA:dG", 0.6790046689778633, 0.4445085082785141, 150),
    CRISPRiEntry(3, "rC:dA", 0.8028970473507057, 0.36877897999749515, 64),
    CRISPRiEntry(3, "rC:dC", 0.5937295283466446, 0.4618260287309943, 200),
    CRISPRiEntry(3, "rC:dT", 0.7835245678296865, 0.45977700736547034, 66),
    CRISPRiEntry(3, "rG:dA", 0.7823998902283172, 0.39902669826063797, 58),
    CRISPRiEntry(3, "rG:dG", 0.6310602988652162, 0.38170810915987574, 157),
    CRISPRiEntry(3, "rG:dT", 1.0100971463809094, 0.3157252661791143, 74),
    CRISPRiEntry(3, "rU:dC", 0.6001613010629193, 0.44279523429667855, 163),
    CRISPRiEntry(3, "rU:dG", 0.6726458178744623, 0.45348170288557443, 146),
    CRISPRiEntry(3, "rU:dT", 0.7232911209206168, 0.39410107795420285, 88),
    CRISPRiEntry(4, "rA:dA", 0.6871351918736284, 0.3796017275697205, 69),
    CRISPRiEntry(4, "rA:dC", 0.599002024978807, 0.42795718253554454, 163),
    CRISPRiEntry(4, "rA:dG", 0.537494150912959, 0.4455822918300461, 156),
    CRISPRiEntry(4, "rC:dA", 0.7247421828360471, 0.5457540364425728, 57),
    CRISPRiEntry(4, "rC:dC", 0.5111089815721563, 0.45487692571584265, 190),
    CRISPRiEntry(4, "rC:dT", 0.6861171707412276, 0.4132720188483426, 83),
    CRISPRiEntry(4, "rG:dA", 0.6715710938571136, 0.3785220591827132, 67),
    CRISPRiEntry(4, "rG:dG", 0.6238416741654459, 0.49409044456198553, 194),
    CRISPRiEntry(4, "rG:dT", 0.8579851212568478, 0.37683799959282616, 71),
    CRISPRiEntry(4, "rU:dC", 0.4227647709009351, 0.45400799931405805, 185),
    CRISPRiEntry(4, "rU:dG", 0.6349277660937551, 0.44020819652405996, 160),
    CRISPRiEntry(4, "rU:dT", 0.6947382165440157, 0.4003774684689298, 73),
    CRISPRiEntry(5, "rA:dA", 0.6418102173451061, 0.3915558766924514, 71),
    CRISPRiEntry(5, "rA:dC", 0.5094452756722135, 0.44486565563306407, 193),
    CRISPRiEntry(5, "rA:dG", 0.5270270377828941, 0.47095345317289466, 150),
    CRISPRiEntry(5, "rC:dA", 0.798659720898155, 0.4013586429136206, 63),
    CRISPRiEntry(5, "rC:dC", 0.47492761450113524, 0.4577542199704251, 160),
    CRISPRiEntry(5, "rC:dT", 0.7401291023849293, 0.4545801606276781, 74),
    CRISPRiEntry(5, "rG:dA", 0.6658314145149566, 0.4299567842619998, 62),
    CRISPRiEntry(5, "rG:dG", 0.6252964912963489, 0.46051029369454294, 137),
    CRISPRiEntry(5, "rG:dT", 0.948556311270519, 0.3290587068350572, 83),
    CRISPRiEntry(5, "rU:dC", 0.44918007418658085, 0.46043293581934264, 169),
    CRISPRiEntry(5, "rU:dG", 0.6598510645790642, 0.40000934629181195, 134),
    CRISPRiEntry(5, "rU:dT", 0.7547181653109127, 0.4033917769947378, 76),
    CRISPRiEntry(6, "rA:dA", 0.5781448477720884, 0.470469372422436, 74),
    CRISPRiEntry(6, "rA:dC", 0.6047440334778832, 0.4335520300112002, 162),
    CRISPRiEntry(6, "rA:dG", 0.5977020768902372, 0.5141370757603614, 148),
    CRISPRiEntry(6, "rC:dA", 0.5956699048216615, 0.4402606139544111, 79),
    CRISPRiEntry(6, "rC:dC", 0.4771032087149824, 0.4626473921659944, 173),
    CRISPRiEntry(6, "rC:dT", 0.6484208104734098, 0.44338709121664943, 76),
    CRISPRiEntry(6, "rG:dA", 0.5964206979042478, 0.4077239991426681, 80),
    CRISPRiEntry(6, "rG:dG", 0.6130469119564934, 0.44775091187876226, 158),
    CRISPRiEntry(6, "rG:dT", 0.8749839066326826, 0.2993308604260047, 82),
    CRISPRiEntry(6, "rU:dC", 0.4562755961725506, 0.45110773086691464, 165),
    CRISPRiEntry(6, "rU:dG", 0.6832996133944906, 0.433554471007193, 161),
    CRISPRiEntry(6, "rU:dT", 0.6422193075158629, 0.44899132159747907, 99),
    CRISPRiEntry(7, "rA:dA", 0.5811154062982572, 0.43323373953538263, 65),
    CRISPRiEntry(7, "rA:dC", 0.4707252141996512, 0.44739009435682486, 191),
    CRISPRiEntry(7, "rA:dG", 0.5080529160026396, 0.44773562612416845, 158),
    CRISPRiEntry(7, "rC:dA", 0.5379093720725565, 0.4330433921624724, 54),
    CRISPRiEntry(7, "rC:dC", 0.34527377248722346, 0.43300310893120547, 197),
    CRISPRiEntry(7, "rC:dT", 0.5878971446610345, 0.46904343596217457, 84),
    CRISPRiEntry(7, "rG:dA", 0.6892844183358594, 0.44386202102716704, 44),
    CRISPRiEntry(7, "rG:dG", 0.6226736068839678, 0.4504669809182679, 147),
    CRISPRiEntry(7, "rG:dT", 0.8803862477321158, 0.2868076797576593, 91),
    CRISPRiEntry(7, "rU:dC", 0.47697400890245323, 0.4649193412122413, 205),
    CRISPRiEntry(7, "rU:dG", 0.6977783955898867, 0.43732549149896466, 150),
    CRISPRiEntry(7, "rU:dT", 0.6870042661939981, 0.40814414767934926, 90),
    CRISPRiEntry(8, "rA:dA", 0.5419603035073562, 0.45462057962722163, 132),
    CRISPRiEntry(8, "rA:dC", 0.4061151702625563, 0.4279243823426123, 405),
    CRISPRiEntry(8, "rA:dG", 0.39860377936732294, 0.4627983547017242, 360),
    CRISPRiEntry(8, "rC:dA", 0.4386504204721522, 0.40822016829074276, 143),
    CRISPRiEntry(8, "rC:dC", 0.2132116628133749, 0.3637844227018677, 417),
    CRISPRiEntry(8, "rC:dT", 0.46893270977913964, 0.4805381408496059, 194),
    CRISPRiEntry(8, "rG:dA", 0.6733771346347838, 0.4430287677745118, 165),
    CRISPRiEntry(8, "rG:dG", 0.5112128647045744, 0.4719806971444433, 322),
    CRISPRiEntry(8, "rG:dT", 0.9665223926020455, 0.3283445055535537, 193),
    CRISPRiEntry(8, "rU:dC", 0.3144654725011591, 0.3889961054588254, 425),
    CRISPRiEntry(8, "rU:dG", 0.590737718786066, 0.4386415952147987, 381),
    CRISPRiEntry(8, "rU:dT", 0.6514414499163452, 0.4513416899669615, 193),
    CRISPRiEntry(9, "rA:dA", 0.3495443493685494, 0.46342224317376973, 132),
    CRISPRiEntry(9, "rA:dC", 0.19252876292059898, 0.32060574285201554, 468),
    CRISPRiEntry(9, "rA:dG", 0.28607500884501635, 0.3774094958836403, 316),
    CRISPRiEntry(9, "rC:dA", 0.26977679846993047, 0.4284266376468267, 130),
    CRISPRiEntry(9, "rC:dC", 0.07671404725849208, 0.2659338366529979, 500),
    CRISPRiEntry(9, "rC:dT", 0.33166337546176783, 0.41314996587542746, 236),
    CRISPRiEntry(9, "rG:dA", 0.4431801503336499, 0.4351496382491935, 108),
    CRISPRiEntry(9, "rG:dG", 0.47541767182885397, 0.4099393982424592, 306),
    CRISPRiEntry(9, "rG:dT", 0.8061452580334683, 0.3967894808875198, 225),
    CRISPRiEntry(9, "rU:dC", 0.10516475399009027, 0.26551414125243605, 440),
    CRISPRiEntry(9, "rU:dG", 0.4392867095069152, 0.3799648470109858, 303),
    CRISPRiEntry(9, "rU:dT", 0.31016952886752025, 0.3821297010627743, 210),
    CRISPRiEntry(10, "rA:dA", 0.13884051914252832, 0.3228596690489309, 121),
    CRISPRiEntry(10, "rA:dC", 0.14851301495586963, 0.3284239660361278, 440),
    CRISPRiEntry(10, "rA:dG", 0.0808167952397293, 0.21357570407477158, 325),
    CRISPRiEntry(10, "rC:dA", 0.2832722103498345, 0.37855216359374644, 122),
    CRISPRiEntry(10, "rC:dC", 0.065778539337348, 0.2703584277992773, 444),
    CRISPRiEntry(10, "rC:dT", 0.22902583720714836, 0.36836912693606494, 204),
    CRISPRiEntry(10, "rG:dA", 0.26104903893529796, 0.3989726577876779, 147),
    CRISPRiEntry(10, "rG:dG", 0.2928148646005806, 0.36825204735479566, 308),
    CRISPRiEntry(10, "rG:dT", 0.8332527667577183, 0.3601825464043143, 191),
    CRISPRiEntry(10, "rU:dC", 0.048655426874427324, 0.21165348604901435, 450),
    CRISPRiEntry(10, "rU:dG", 0.38398465430520806, 0.3893844694543995, 353),
    CRISPRiEntry(10, "rU:dT", 0.317290570592644, 0.384647947594607, 215),
    CRISPRiEntry(11, "rA:dA", 0.0644872624413191, 0.17888062230538004, 134),
    CRISPRiEntry(11, "rA:dC", 0.0967659106676467, 0.22698699578109777, 436),
    CRISPRiEntry(11, "rA:dG", 0.0898744215319925, 0.25776172035142136, 339),
    CRISPRiEntry(11, "rC:dA", 0.2071497168294914, 0.3424485322611594, 134),
    CRISPRiEntry(11, "rC:dC", 0.04437125981965446, 0.17669428948937818, 406),
    CRISPRiEntry(11, "rC:dT", 0.22761640747448533, 0.415068815359019, 209),
    CRISPRiEntry(11, "rG:dA", 0.17545024780228247, 0.297209450408031, 140),
    CRISPRiEntry(11, "rG:dG", 0.31115505793918086, 0.37056716349551805, 307),
    CRISPRiEntry(11, "rG:dT", 0.7519175292167825, 0.3881418226274006, 194),
    CRISPRiEntry(11, "rU:dC", 0.0572547741039174, 0.22037167623433285, 478),
    CRISPRiEntry(11, "rU:dG", 0.4708816081553425, 0.4023676971726624, 329),
    CRISPRiEntry(11, "rU:dT", 0.14829732864719528, 0.27442135277481916, 205),
    CRISPRiEntry(12, "rA:dA", 0.04022812652099125, 0.1498457651600617, 134),
    CRISPRiEntry(12, "rA:dC", 0.07562469810767297, 0.20600613524839226, 461),
    CRISPRiEntry(12, "rA:dG", 0.023724165171595583, 0.15454185504943724, 338),
    CRISPRiEntry(12, "rC:dA", 0.3977057643893842, 0.39626260496353044, 126),
    CRISPRiEntry(12, "rC:dC", 0.03363380785748919, 0.14420783503675508, 420),
    CRISPRiEntry(12, "rC:dT", 0.0831545923979773, 0.22176117470905865, 196),
    CRISPRiEntry(12, "rG:dA", 0.14758114464040817, 0.29128105883701255, 123),
    CRISPRiEntry(12, "rG:dG", 0.1956324408418165, 0.3328542603804984, 342),
    CRISPRiEntry(12, "rG:dT", 0.46721670741150795, 0.4232610424736648, 205),
    CRISPRiEntry(12, "rU:dC", 0.002089243772540433, 0.08707786510507735, 440),
    CRISPRiEntry(12, "rU:dG", 0.2692635905968961, 0.37439743676881193, 326),
    CRISPRiEntry(12, "rU:dT", 0.1262243121010066, 0.277588148182513, 223),
    CRISPRiEntry(13, "rA:dA", 0.060331023325104, 0.20876156091308157, 124),
    CRISPRiEntry(13, "rA:dC", 0.0880947609789701, 0.24359513868583277, 260),
    CRISPRiEntry(13, "rA:dG", 0.01828951626955609, 0.12546556820367938, 244),
    CRISPRiEntry(13, "rC:dA", 0.13045088400021423, 0.24814232523117863, 123),
    CRISPRiEntry(13, "rC:dC", 0.027245129092892954, 0.16650452524177667, 257),
    CRISPRiEntry(13, "rC:dT", 0.060125141109765454, 0.2225684884274416, 184),
    CRISPRiEntry(13, "rG:dA", 0.042684366789692056, 0.17168560066187702, 112),
    CRISPRiEntry(13, "rG:dG", 0.07259178185795996, 0.24534293920407405, 233),
    CRISPRiEntry(13, "rG:dT", 0.2790411965168029, 0.4084157462470164, 185),
    CRISPRiEntry(13, "rU:dC", 0.01193085771828659, 0.09377858991947262, 265),
    CRISPRiEntry(13, "rU:dG", 0.031673104818542415, 0.14822655482335184, 247),
    CRISPRiEntry(13, "rU:dT", 0.05328223641428971, 0.21418805955114797, 171),
    CRISPRiEntry(14, "rA:dA", 0.04853462613745596, 0.19604249597162848, 115),
    CRISPRiEntry(14, "rA:dC", 0.20587300795529795, 0.3141044284390904, 227),
    CRISPRiEntry(14, "rA:dG", 0.018279407393984367, 0.13211491180574575, 272),
    CRISPRiEntry(14, "rC:dA", 0.026741879986252437, 0.16185159956428852, 136),
    CRISPRiEntry(14, "rC:dC", 0.027811752536746424, 0.15182787968800904, 226),
    CRISPRiEntry(14, "rC:dT", 0.03642175210086362, 0.14157737639654153, 186),
    CRISPRiEntry(14, "rG:dA", 0.09692235043984553, 0.30683042591447374, 124),
    CRISPRiEntry(14, "rG:dG", 0.027189074476068456, 0.1450768016411977, 276),
    CRISPRiEntry(14, "rG:dT", 0.26865890093507167, 0.35059207116799834, 203),
    CRISPRiEntry(14, "rU:dC", 0.011010481099589737, 0.09612338005867288, 195),
    CRISPRiEntry(14, "rU:dG", 0.041350908479233436, 0.22440762833852146, 248),
    CRISPRiEntry(14, "rU:dT", 0.05386622681642693, 0.22596457899418562, 186),
    CRISPRiEntry(15, "rA:dA", 0.06342536001886455, 0.2883390886937433, 126),
    CRISPRiEntry(15, "rA:dC", 0.05047305001640061, 0.24768963384905712, 229),
    CRISPRiEntry(15, "rA:dG", 0.02589551545823215, 0.12903187200187857, 281),
    CRISPRiEntry(15, "rC:dA", 0.09894468877321397, 0.24364436432686296, 140),
    CRISPRiEntry(15, "rC:dC", 0.02440147827095775, 0.11407819644397392, 229),
    CRISPRiEntry(15, "rC:dT", 0.12559401074443188, 0.2562288800121519, 158),
    CRISPRiEntry(15, "rG:dA", 0.07946661338252402, 0.1889343556021876, 131),
    CRISPRiEntry(15, "rG:dG", 0.04872320678376308, 0.21697930042098024, 255),
    CRISPRiEntry(15, "rG:dT", 0.25395731097845453, 0.31199627048673095, 183),
    CRISPRiEntry(15, "rU:dC", 0.03777956128373936, 0.1523569053238319, 208),
    CRISPRiEntry(15, "rU:dG", 0.043755740559140874, 0.19447252258961428, 249),
    CRISPRiEntry(15, "rU:dT", 0.08391754030842333, 0.20662122015607387, 190),
    CRISPRiEntry(16, "rA:dA", 0.16708486130378444, 0.29096466293851697, 118),
    CRISPRiEntry(16, "rA:dC", 0.05753996809954845, 0.1962563291199795, 315),
    CRISPRiEntry(16, "rA:dG", 0.05735012844182217, 0.2180228887301193, 192),
    CRISPRiEntry(16, "rC:dA", 0.07581520510454455, 0.20167887444247515, 128),
    CRISPRiEntry(16, "rC:dC", 0.01568301788849709, 0.14595848963612898, 263),
    CRISPRiEntry(16, "rC:dT", 0.044356145950611145, 0.18289414768174053, 186),
    CRISPRiEntry(16, "rG:dA", 0.11398880560889796, 0.26453348692469925, 136),
    CRISPRiEntry(16, "rG:dG", 0.1686697675071983, 0.2918992217465262, 179),
    CRISPRiEntry(16, "rG:dT", 0.35408333599545816, 0.354049031438765, 213),
    CRISPRiEntry(16, "rU:dC", 0.010260430952827368, 0.09152956523558096, 274),
    CRISPRiEntry(16, "rU:dG", 0.09871486472088051, 0.22946346081947436, 185),
    CRISPRiEntry(16, "rU:dT", 0.042363853482777006, 0.14092536601494976, 193),
    CRISPRiEntry(17, "rA:dA", 0.07482961465107561, 0.20910440766706676, 145),
    CRISPRiEntry(17, "rA:dC", 0.04984616136307448, 0.2088736051930897, 211),
    CRISPRiEntry(17, "rA:dG", 0.03144928598705076, 0.15834299186108894, 309),
    CRISPRiEntry(17, "rC:dA", 0.0679608154383905, 0.19980096766896055, 123),
    CRISPRiEntry(17, "rC:dC", 0.01572924786331831, 0.1451583410503704, 245),
    CRISPRiEntry(17, "rC:dT", 0.052047162307013964, 0.22388286641603505, 148),
    CRISPRiEntry(17, "rG:dA", 0.15689529405266184, 0.2445654087701977, 120),
    CRISPRiEntry(17, "rG:dG", 0.04957697414811067, 0.18798196141355233, 316),
    CRISPRiEntry(17, "rG:dT", 0.523275826113342, 0.34026175511882967, 131),
    CRISPRiEntry(17, "rU:dC", 0.015430081767846378, 0.15150823833941335, 210),
    CRISPRiEntry(17, "rU:dG", 0.04939278616385872, 0.12951765726665002, 315),
    CRISPRiEntry(17, "rU:dT", 0.12121805448054888, 0.21432197724764096, 140),
    CRISPRiEntry(18, "rA:dA", 0.09210128388684093, 0.2594575284904484, 140),
    CRISPRiEntry(18, "rA:dC", 0.04059599587835912, 0.20451488431991915, 285),
    CRISPRiEntry(18, "rA:dG", 0.0219451237025736, 0.13369818536241468, 198),
    CRISPRiEntry(18, "rC:dA", 0.05125652606581089, 0.2232168992099068, 128),
    CRISPRiEntry(18, "rC:dC", 0.022793051516872886, 0.1875271184383542, 262),
    CRISPRiEntry(18, "rC:dT", 0.028513271950227306, 0.13935021736679026, 190),
    CRISPRiEntry(18, "rG:dA", 0.11311183121547688, 0.2542663328215147, 137),
    CRISPRiEntry(18, "rG:dG", 0.031175351592834848, 0.13916934473719406, 185),
    CRISPRiEntry(18, "rG:dT", 0.2267568160772559, 0.2961032086352618, 202),
    CRISPRiEntry(18, "rU:dC", 0.007162225340963109, 0.10821018076580916, 292),
    CRISPRiEntry(18, "rU:dG", 0.05119900646026415, 0.16128891942755555, 155),
    CRISPRiEntry(18, "rU:dT", 0.03407120892451836, 0.17070062469034772, 191),
    CRISPRiEntry(19, "rA:dA", 0.02755079190618407, 0.11325864308956547, 137),
    CRISPRiEntry(19, "rA:dC", 0.04888924136121993, 0.16884064429768647, 354),
    CRISPRiEntry(19, "rA:dG", 0.007738282278340234, 0.07570601231550775, 122),
    CRISPRiEntry(19, "rC:dA", 0.02275346384839644, 0.2640199846336422, 111),
    CRISPRiEntry(19, "rC:dC", 0.04582338187996445, 0.26307432049155444, 378),
    CRISPRiEntry(19, "rC:dT", 0.05154689979811011, 0.22132372403137496, 182),
    CRISPRiEntry(19, "rG:dA", 0.142049356109543, 0.29395723680217595, 122),
    CRISPRiEntry(19, "rG:dG", 0.04636531312981942, 0.17059849218537682, 119),
    CRISPRiEntry(19, "rG:dT", 0.23017132234502502, 0.2715224651991805, 177),
    CRISPRiEntry(19, "rU:dC", 0.010883881316916873, 0.10277385009069898, 365),
    CRISPRiEntry(19, "rU:dG", 0.007529032609234753, 0.10296990926566235, 111),
    CRISPRiEntry(19, "rU:dT", 0.03182081449682617, 0.15415194350616354, 200))

  val scoreMapping = new mutable.LinkedHashMap[ScoreLookup, CRISPRiEntry]()

  // provide a mapping from a scoring looking to the entries above, converting to DNA
  scores.foreach { score => {
    val baseFrom = if (score.baseTransistion(1) == 'U') 'T' else score.baseTransistion(1)
    val baseTo = if (score.baseTransistion(4) == 'U') 'T' else score.baseTransistion(4)
    scoreMapping(ScoreLookup(score.position, baseFrom, baseTo)) = score
  }
  }
}