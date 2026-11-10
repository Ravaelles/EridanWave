package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points, Tile}
import Micro.Coordination.Pushing._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

/** See Commander: role-grouped delegates behind the facade. */
object CommanderCore {

  private[Micro] val tryingToMoveThreshold = 32


  private[Micro] def defaultEscalation(unit: FriendlyUnitInfo): Unit = {

    lazy val buildDistance = unit.pixelDistanceCenter(unit.intent.toBuildActive.map(b => b.unitClass.tileArea.add(b.tile).center).get)

    if (unit.flying) return



    if (unit.intent.toScoutTiles.nonEmpty)                      unit.agent.escalatePriority(TrafficPriorities.Pardon)

    if (unit.intent.toFinish.isDefined)                         unit.agent.escalatePriority(TrafficPriorities.Bump)

    if (unit.intent.toHeal.isDefined)                           unit.agent.escalatePriority(TrafficPriorities.Bump)

    if (unit.intent.toBuild.nonEmpty)                           unit.agent.escalatePriority(?(buildDistance < 128, TrafficPriorities.Shove, TrafficPriorities.Bump))

    if (unit.agent.toAttack.exists( ! unit.inRangeToAttack(_))) unit.agent.escalatePriority(TrafficPriorities.Nudge)

    if (unit.battle.isDefined && ! unit.agent.shouldFight) {    unit.agent.escalatePriority(TrafficPriorities.Pardon)

      if (unit.matchups.pixelsEntangled > -80)                  unit.agent.escalatePriority(TrafficPriorities.Nudge)

      if (unit.matchups.pixelsEntangled > -48)                  unit.agent.escalatePriority(TrafficPriorities.Bump)

      if (unit.matchups.pixelsEntangled > -16)                  unit.agent.escalatePriority(TrafficPriorities.Shove)

    }

  }




  private[Micro] def push(unit: FriendlyUnitInfo,  minimumPriority: TrafficPriority, makePush: => Push): Unit = {

    if (unit.flying) return



    unit.agent.escalatePriority(minimumPriority)

    defaultEscalation(unit)

    if (unit.agent.priority > TrafficPriorities.Freedom) {

      With.coordinator.pushes.put(makePush)

    }

  }



  private[Micro] def pushAway(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, radius: Double = 48): Unit = {

    push(unit, minimumPriority, new CircularPush(unit.agent.priority, to, radius, unit))

  }



  private[Micro] def pushThrough(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, maxDistance: Double = 96): Unit = {

    push(unit, minimumPriority, new UnitLinearGroundPush(unit.agent.priority, unit, ?(unit.pixelDistanceCenter(to) > maxDistance, unit.pixel.project(to, maxDistance), to)))

  }




  private[Micro] def autoUnburrow(unit: FriendlyUnitInfo): Boolean = {

    if (unit.unready) return false

    if ( ! unit.burrowed) return false



    CommanderAbilities.unburrow(unit)

    true

  }



  private[Micro] def autoUnload(unit: FriendlyUnitInfo): Boolean = {

    if (unit.unready) return false

    if (unit.transport.isEmpty) return false



    CommanderTransport.requestUnload(unit)

    true

  }



  private[Micro] def autoUnburrowUnlessLurkerInRangeOf(unit: FriendlyUnitInfo, to: Pixel): Boolean = {

    ( ! Zerg.Lurker(unit) || unit.pixelDistanceCenter(to) > unit.pixelRangeGround) && autoUnburrow(unit)

  }

  

  private[Micro] def sleepAttack(unit: FriendlyUnitInfo): Unit = {

    sleep(unit, AttackDelay.framesToWaitAfterIssuingAttackOrder(unit))

  }

  

  private[Micro] def sleepBuild(unit: FriendlyUnitInfo): Unit = {

    // Adapted from https://github.com/tscmoo/tsc-bwai/blame/master/src/unit_controls.h#L1497

    // In practice (1 + 2 * With.latency.latencyFrames) was too low

    sleep(unit, 3 + 2 * With.latency.latencyFrames)

  }

  

  private[Micro] def sleepReturnCargo(unit:FriendlyUnitInfo): Unit = {

    // ReturnMinerals/ReturnGas are processed by Brood War's slow-order loop,

    // which runs once every nine frames. A retry after eight frames can reset

    // the order after it selects a depot but before it processes the deposit.

    // Wait one full processing interval plus one frame while retaining the

    // pathing and recovery benefits of periodically reissuing the order.

    sleep(unit, 10)

  }

  

  private[Micro] def sleep(unit: FriendlyUnitInfo, requiredDelay: Int = 2): Unit = {

    unit.sleepUntil = Maff.vmax(

      With.frame + requiredDelay,

      With.frame + With.latency.turnSize,

      unit.sleepUntil)



    if (With.configuration.trackUnit && (unit.selected || unit.transport.exists(_.selected))) {

      With.configuration.trackUnit = false // This is where you want the trackUnit breakpoint

    }

  }




  private[Micro] def leadFollower(unit: FriendlyUnitInfo, todo: (FriendlyUnitInfo) => Unit): Unit = {

    if (unit.ready) {

      unit.agent.leadFollower = todo

    }

  }
}
