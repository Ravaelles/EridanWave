package Micro.Agency
import Lifecycle.With
import Mathematics.Points.{Pixel, Points, Tile}
import Micro.Coordination.Pushing._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
/** See Commander: role-grouped delegates behind the facade. */
object CommanderEngage {
  private[Micro] def acceptableAttackFrom(unit: FriendlyUnitInfo, target: UnitInfo, attackFrom: Pixel): Boolean = {
    if ( ! target.visible && target.pixelDistanceCenter(attackFrom) > 16)                         false
    else if (unit.inRangeToAttackFrom(target, attackFrom))                                        true
    else if (unit.pixelsToGetInRange(target) <= unit.pixelsToGetInRangeFrom(target, attackFrom))  false
    else                                                                                          true
  }
  private[Micro] def attack(unit: FriendlyUnitInfo): Unit = unit.agent.toAttack.foreach(attack(unit, _))
  private[Micro] def attack(unit: FriendlyUnitInfo, target: UnitInfo): Unit = {
    unit.agent.toAttack = Some(target)
    lazy val attackFrom = unit.agent.perch.pixel.filter(acceptableAttackFrom(unit, target, _)).getOrElse(unit.agent.choosePerch()())
    if (Protoss.Reaver(unit)) {
      if (unit.loaded)  CommanderCore.pushAway    (unit, attackFrom, TrafficPriorities.Bump)
      else              CommanderCore.pushThrough (unit, attackFrom, TrafficPriorities.Bump)
    } else if ( ! target.visible || ! unit.inRangeToAttack(target)) {
      CommanderCore.pushThrough(unit, attackFrom, TrafficPriorities.Nudge)
    }
    unit.agent.setRideGoal(attackFrom)
    CommanderCore.leadFollower(unit, attack(_, target))
    unit.agent.tryingToMove = unit.pixelsToGetInRange(target) > CommanderCore.tryingToMoveThreshold
    if (unit.unready) return

    if (airliftUnload(unit, target, attackFrom)) return
    if ( ! Zerg.Lurker(unit) && CommanderCore.autoUnburrow(unit)) return
    if (interceptorTarget(unit, target, attackFrom)) return
    if (carrierChase(unit, target, attackFrom)) return
    if (lurkerSiege(unit, target, attackFrom)) return
    if (closeIn(unit, target)) return
    if (issueAttack(unit, target)) return

    CommanderMove.move(unit, attackFrom)
  }

  // Airlifted cargo landing on its target: board out instead of fighting
  // from the hall, unless a Reaver has no scarab path to the target.
  private def airliftUnload(unit: FriendlyUnitInfo, target: UnitInfo, attackFrom: => Pixel): Boolean = {
    if ( ! unit.airlifted) return false
    if ( ! (unit.pixelDistanceCenter(attackFrom) < ?(unit.agent.isPrimaryPassenger, 32, 64) || unit.pixelsToGetInRange(target) <= ?(unit.agent.isPrimaryPassenger, 16, 48))) return false

    if ( ! Protoss.Reaver(unit) || unit.hasScarabPathTo(target, unit.pixel)) CommanderTransport.requestUnload(unit)
    CommanderCore.sleep(unit) //  Although we're not doing anything, it's important to sleep here so we don't keep executing and change our ride goal

    true
  }

  // Interceptors are not a target: attack-move at the firing position so
  // the shots catch whatever the Carrier sends out.
  private def interceptorTarget(unit: FriendlyUnitInfo, target: UnitInfo, attackFrom: => Pixel): Boolean = {
    if ( ! Protoss.Interceptor(target)) return false

    CommanderMove.attackMove(unit, attackFrom)

    true
  }

  // Grounded guns on a Carrier attack-move off cooldown: the Interceptors
  // die on the way in even when no route to the hull exists.
  private def carrierChase(unit: FriendlyUnitInfo, target: UnitInfo, attackFrom: => Pixel): Boolean = {
    if ( ! (Protoss.Carrier(target) && unit.readyForAttackOrder(target) && ! unit.inRangeToAttack(target) && ! unit.flying)) return false

    CommanderMove.attackMove(unit, attackFrom)

    true
  }

  // Burrowed Lurkers hold their spine: burrow in range, walk up otherwise.
  private def lurkerSiege(unit: FriendlyUnitInfo, target: UnitInfo, attackFrom: => Pixel): Boolean = {
    if ( ! (Zerg.Lurker(unit) && ! unit.burrowed)) return false

    if (unit.inRangeToAttack(target)) {
      CommanderAbilities.burrow(unit)
    } else {
      CommanderMove.move(unit, attackFrom)
    }

    true
  }

  // Closing in: attack-move at a firing position by the target instead
  // of issuing a raw attack order. A raw order re-issued while walking
  // resets acceleration to a crawl (the sticking the safe-arrival math
  // guards against), so a shooter closing twenty tiles never arrives;
  // attack-move keeps walking and opens fire on arrival. Kiting (in
  // range but cooling) keeps the move path below untouched.
  private def closeIn(unit: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    if ( ! (unit.canMove && ! unit.inRangeToAttack(target))) return false

    CommanderMove.attackMove(unit, unit.pixelToFireAtExhaustive(target))

    true
  }

  // Attack command only when deliberate: a new target, an overdue shot
  // while already engaging, or Carrier warmup spam. Anything else keeps
  // the move path instead of resetting acceleration every frame.
  private def issueAttack(unit: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    // Allow deliberate repositioning between shots
    var useAttackCommand    = unit.readyForAttackOrder(target) || unit.inRangeToAttack(target)
    useAttackCommand      ||= unit.unitClass.melee
    useAttackCommand      ||= ! unit.canMove
    useAttackCommand      ||= unit.flying && unit.readyForAttackOrder(target) // Flying units can attack directly but if repositioning between shots need a move command
    useAttackCommand      ||= unit.framesToGetInRange(target) <= With.latency.latencyFrames + Math.max(unit.cooldownLeft, With.reaction.agencyAverage)
    useAttackCommand      &&= target.visible
    if ( ! useAttackCommand) return false

    lazy val moving           = unit.moving
    lazy val alreadyInRange   = unit.inRangeToAttack(target)
    lazy val overdueToAttack  = unit.cooldownLeft == 0 && With.framesSince(unit.lastFrameStartingAttack) > 2.0 * unit.cooldownMaxAirGround
    lazy val thisIsANewTarget = ! unit.orderTarget.contains(target)
    val shouldOrder = (
      thisIsANewTarget
        || (overdueToAttack && (moving || alreadyInRange))
        || (target.isFriendly && Protoss.Carrier(unit))) // Carrier warmup; spam attack to avoid actually doing damage
    if (shouldOrder) {
      unit.bwapiUnit.attack(target.bwapiUnit)
      target.addFutureAttack(unit)
      CommanderCore.sleepAttack(unit)
    } else {
      CommanderCore.sleep(unit)
    }

    true
  }
  // Return the waypoint or a nearby point with collision clearance, or None if none fits.
  // BW treats touching outer collision boxes as a unit destination. Keep a pixel of clearance.
  private[Micro] def useTech(unit: FriendlyUnitInfo, tech: Tech): Unit = {
    if (tech == Terran.Stim) {
      if (With.framesSince(unit.agent.lastStim) < 24) return

      unit.agent.lastStim = With.frame
    }

    CommanderCore.ready(unit) {
      unit.bwapiUnit.useTech(tech.bwapiTech)
      CommanderCore.sleep(unit)
    }
  }
  private[Micro] def useTechOnUnit(unit: FriendlyUnitInfo, tech: Tech, target: UnitInfo): Unit = {
    unit.agent.setRideGoal(target.pixel)
    CommanderCore.ready(unit) {
      unit.bwapiUnit.useTech(tech.bwapiTech, target.bwapiUnit)
      if (tech == Protoss.ArchonMeld || tech == Protoss.DarkArchonMeld) CommanderCore.sleep(unit, 48)
      else CommanderCore.sleep(unit)
    }
  }
  private[Micro] def useTechOnPixel(unit: FriendlyUnitInfo, tech: Tech, target: Pixel): Unit = {
    unit.agent.setRideGoal(target)
    CommanderCore.ready(unit) {
      unit.bwapiUnit.useTech(tech.bwapiTech, target.bwapi)
      if (tech == Terran.SpiderMinePlant) CommanderCore.sleep(unit, 12)
      else CommanderCore.sleepAttack(unit)
    }
  }
}


