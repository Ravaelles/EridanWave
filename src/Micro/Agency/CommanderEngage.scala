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



    if (unit.airlifted && (unit.pixelDistanceCenter(attackFrom) < ?(unit.agent.isPrimaryPassenger, 32, 64) || unit.pixelsToGetInRange(target) <= ?(unit.agent.isPrimaryPassenger, 16, 48))) {

      if ( ! Protoss.Reaver(unit) || unit.hasScarabPathTo(target, unit.pixel)) CommanderTransport.requestUnload(unit)

      CommanderCore.sleep(unit) //  Although we're not doing anything, it's important to sleep here so we don't keep executing and change our ride goal



      return

    }



    if ( ! Zerg.Lurker(unit) && CommanderCore.autoUnburrow(unit)) return



    // We shouldn't target interceptors anymore, but in case we do:



    if (Protoss.Interceptor(target)) {

      CommanderMove.attackMove(unit, attackFrom); return

    }



    // When a ground unit attacks a Carrier, have it attack-move when off cooldown to ensure we burn off Interceptors even if we can't find a route to the Carrier

    if (Protoss.Carrier(target) && unit.readyForAttackOrder(target) && ! unit.inRangeToAttack(target) && ! unit.flying) {

      CommanderMove.attackMove(unit, attackFrom); return

    }



    if (Zerg.Lurker(unit) && ! unit.burrowed) {

      if (unit.inRangeToAttack(target)) {

        CommanderAbilities.burrow(unit)

      } else {

        CommanderMove.move(unit, attackFrom)

      }

      return

    }



    // If we should be attack-commanding

    var useAttackCommand    = unit.readyForAttackOrder(target) || unit.inRangeToAttack(target) // Allow deliberate repositioning between shots

    useAttackCommand      ||= unit.unitClass.melee

    useAttackCommand      ||= ! unit.canMove

    useAttackCommand      ||= unit.flying && unit.readyForAttackOrder(target) // Flying units can attack directly but if repositioning between shots need a move command

    useAttackCommand      ||= unit.framesToGetInRange(target) <= With.latency.latencyFrames + Math.max(unit.cooldownLeft, With.reaction.agencyAverage)

    useAttackCommand      &&= target.visible

    if (useAttackCommand) {

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

      return

    }



    CommanderMove.move(unit, attackFrom)

  }



  // Return the waypoint or a nearby point with collision clearance, or None if none fits.

  // BW treats touching outer collision boxes as a unit destination. Keep a pixel of clearance.


  private[Micro] def useTech(unit: FriendlyUnitInfo, tech: Tech): Unit = {

    if (tech == Terran.Stim) {

      if (With.framesSince(unit.agent.lastStim) < 24) return



      unit.agent.lastStim = With.frame

    }



    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    unit.bwapiUnit.useTech(tech.bwapiTech)

    CommanderCore.sleep(unit)

  }



  private[Micro] def useTechOnUnit(unit: FriendlyUnitInfo, tech: Tech, target: UnitInfo): Unit = {

    unit.agent.setRideGoal(target.pixel)

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    unit.bwapiUnit.useTech(tech.bwapiTech, target.bwapiUnit)

    if (tech == Protoss.ArchonMeld || tech == Protoss.DarkArchonMeld) return CommanderCore.sleep(unit, 48)


    CommanderCore.sleep(unit)

  }



  private[Micro] def useTechOnPixel(unit: FriendlyUnitInfo, tech: Tech, target: Pixel): Unit = {

    unit.agent.setRideGoal(target)

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    unit.bwapiUnit.useTech(tech.bwapiTech, target.bwapi)

    if (tech == Terran.SpiderMinePlant) return CommanderCore.sleep(unit, 12)


    CommanderCore.sleepAttack(unit)

  }



}
