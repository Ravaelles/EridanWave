package Micro.Actions.Protoss.Zealot

import Debugging.Visualizations.Rendering.DrawMap
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Tactics.Recharge
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Minutes
import Utilities.UnitFilters.{IsWarrior, IsWorker}

/**
 * An early Zealot past our natural, killing what it can reach. This file
 * only decides the order to give: the field score lives in
 * ZealotRushScoring, the stand-or-go call in ZealotRushAssessment, target
 * picks in ZealotRushTargets and recharges in ZealotShieldBattery.
 */
object BeRushingZealot extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    Protoss.Zealot(unit)
    && With.frame < Minutes(8)()
    && unit.agent.destinationFinal.pixel.exists(p => ! p.metro.exists(_.isOurs) && p.proximity < 0.5)
    && unit.team.forall(u => u.warriorsCasters.forall(u => u.isAny(Protoss.Probe, Protoss.Zealot, Protoss.ShieldBattery) || u.pixelDistanceCenter(unit) > 320))
  )

  override protected def perform(zealot: FriendlyUnitInfo): Unit = {
    val fieldScore            = ZealotRushScoring.fieldScore(zealot)
    val battery               = ZealotShieldBattery.best(zealot)
    zealot.agent.shouldFight  = ZealotRushAssessment.shouldFight(zealot, fieldScore)
    zealot.agent.destinationNext.set(With.scouting.enemyMain.map(_.heart.center).getOrElse(With.scouting.enemyHome.center))
    DrawMap.label(f"$fieldScore%.2f", zealot.pixel.add(0, 24))

    if (onlyAirIsOnUs(zealot) && battery.isDefined) {
      Commander.rightClick(zealot, battery.get)
    } else if (zealot.agent.shouldFight) {
      fight(zealot)
    } else {
      retreat(zealot)
    }
  }

  /** Everything shootable in range flies and nothing left on the ground is hunting us. */
  private def onlyAirIsOnUs(zealot: FriendlyUnitInfo): Boolean = (
    zealot.matchups.targetsInRange.forall( ! _.unitClass.attacksGround)
    && zealot.matchups.threatsInRange.isEmpty
  )

  private def fight(zealot: FriendlyUnitInfo): Unit = {
    val fleeOutnumbered = ZealotRushTargets.outnumberedByChasers(zealot)
    if (ZealotRushAssessment.canPotshot(zealot) && ! fleeOutnumbered) {
      ZealotRushTargets.potshot(zealot).foreach(attack(zealot, _))
    } else if (ZealotRushTargets.visibleGroundAttackers.isEmpty) {
      Commander.move(zealot)
    } else {
      ZealotRushTargets.best(zealot).foreach(chase(zealot, _, fleeOutnumbered))
    }
    Commander.move(zealot)
  }

  /** Close in on the chosen target, or walk away when it is the fight we cannot win. */
  private def chase(zealot: FriendlyUnitInfo, target: UnitInfo, fleeOutnumbered: Boolean): Unit = {
    if (fleeOutnumbered && target.canMove) {
      retreat(zealot)
    } else if (zealot.readyForAttackOrder(target) || ! target.canMove) {
      attack(zealot, target)
    } else if (target.isAny(Protoss.Zealot, Zerg.Zergling, IsWorker) && target.presumptiveTarget.contains(zealot) && target.inRangeToAttack(zealot)) {
      retreat(zealot)
    } else if (target.speedApproaching(zealot) < 0) {
      zealot.agent.destinationNext.set(target.projectFrames(zealot.cooldownLeft))
      Commander.move(zealot)
    } else {
      attack(zealot, target)
    }
  }

  def attack(zealot: FriendlyUnitInfo, target: UnitInfo): Unit = {
    zealot.agent.toAttack = Some(target)
    Commander.attack(zealot)
  }

  def retreat(zealot: FriendlyUnitInfo): Unit = {
    if (zealot.matchups.pixelsToThreatRange.isEmpty) return

    Recharge(zealot)

    val allies  = zealot.matchups.allies.filter(IsWarrior).filter(_.matchups.pixelsToThreatRange.exists(_ > zealot.matchups.pixelsToThreatRange.get + 64))
    val home    = Maff.minBy(allies)(_.matchups.pixelsToThreatRange)
      .map(_.pixel)
      .getOrElse(With.geography.home.center)

    zealot.agent.destinationNext.set(home)
    Commander.move(zealot)
  }
}
