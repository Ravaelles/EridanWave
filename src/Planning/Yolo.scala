package Planning

import Lifecycle.With
import Performance.Tasks.TimedTask
import Utilities.UnitFilters.IsWorker
import ProxyBwapi.Races.Protoss
import Utilities.Time.Minutes
import Macro.Actions.MacroActions

class Yolo extends TimedTask with MacroActions {
  var lastUpdate: Int = 0
  var maxoutFramesCharged: Int = 0
  var desperationTriggered: Boolean = false
  var maxoutTriggered: Boolean = false
  var frenzyTriggered: Boolean = false

  def maxed: Boolean = With.units.ours.filter(_.complete).filterNot(u => Protoss.Carrier(u) && u.interceptorCount < 7).map(_.unitClass.supplyRequired400).sum / 2 >= 192
  def active: Boolean = desperationTriggered || maxoutTriggered || frenzyTriggered || With.blackboard.yoloing()

  override protected def onRun(budgetMs: Long): Unit = {
    // Trigger desparation
    val weCanNeverMineAgain = ! With.units.existsOurs(IsWorker) || With.geography.ourBases.forall(_.mineralsLeft == 0)
    val enemyIsMining = With.units.existsEnemy(IsWorker) && With.geography.enemyBases.exists(_.mineralsLeft >= 400)
    desperationTriggered = weCanNeverMineAgain && enemyIsMining

    // KillFrenzy latch: a maxed banked army throws everything, and stays
    // thrown until losses drop it below the release supply - no retreat
    // while the bank can still buy the fight back.
    val supply = With.self.supplyUsed200
    if (Yolo.frenzyTrips(supply, With.self.minerals, With.self.gas)) {
      frenzyTriggered = true
    } else if (Yolo.frenzyBreaks(supply)) {
      frenzyTriggered = false
    }

    // Trigger mineout
    val frameDelta: Int = With.framesSince(lastUpdate)
    lastUpdate = With.frame
    if (maxed && With.frame > Minutes(15)()) {
      maxoutFramesCharged += frameDelta
    } else {
      maxoutFramesCharged -= 2 * frameDelta
    }

    val maxoutFrameTarget = Minutes(1)() + Minutes(1)() * With.enemies.size * (if (With.self.isTerran) 5 else if (With.enemies.exists(_.isTerran)) 1 else 3)
    if (maxoutFramesCharged <= 0) {
      maxoutFramesCharged = 0
      maxoutTriggered = false
    } else if (maxoutFramesCharged > maxoutFrameTarget){
      maxoutTriggered = true
    }
  }

  def updateBlackboard(): Unit = {
    if (active) {
      With.blackboard.wantToAttack.set(true)
      With.blackboard.wantToHarass.set(true)
    }
    if (maxoutTriggered) {
      status("YOLO: Maxout")
    }
    if (desperationTriggered) {
      status("YOLO: Desperation")
    }
    if (frenzyTriggered) {
      status("YOLO: KillFrenzy")
    }
  }
}

object Yolo {

  /** Supply at which a banked maxed army throws everything. */
  val frenzySupplyOn: Int = 195

  /** Supply below which the frenzy releases and retreats resume. */
  val frenzySupplyOff: Int = 170

  /** Banked minerals that arm the frenzy alongside gas. */
  val frenzyMineralsBank: Int = 1000

  /** Banked gas that arms the frenzy alongside minerals. */
  val frenzyGasBank: Int = 250

  /** Banked minerals that arm the frenzy alone. */
  val frenzyMineralsSolo: Int = 1500

  /**
   * KillFrenzy trip edge (pure numbers for the suite): maxed and banked
   * means the army attacks instead of sitting on the bank - minerals plus
   * gas, or a mineral mountain alone.
   */
  def frenzyTrips(supplyUsed: Int, minerals: Int, gas: Int): Boolean =
    supplyUsed >= frenzySupplyOn && (
      (minerals >= frenzyMineralsBank && gas >= frenzyGasBank)
      || minerals >= frenzyMineralsSolo)

  /** KillFrenzy release edge: losses below this resume retreating. */
  def frenzyBreaks(supplyUsed: Int): Boolean =
    supplyUsed < frenzySupplyOff
}
