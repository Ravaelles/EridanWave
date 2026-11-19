package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Actions.Combat.Decisioning.NearestShot
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Static standoff (pure geometry plus a thin seam): a ranged ground
 * warrior holding the line against an enemy static defense stands its
 * ground instead of milling into the guns. Shoots whatever it can hit
 * from the hold; holds position inside the stand radius; eases a few
 * pixels back only while standing inside the static's own reach. The
 * nudge is a raw projection, never pathing - units die shuffling for a
 * perfect slot the physics blocks, so the correction stays smaller
 * than a step. Steals only the approach, never the retreat: a fleeing
 * hull still runs for safety.
 */
object StaticStandoff {

  /** Hold the line this close to an enemy static, in tiles. */
  val standTiles: Double = 9.1

  /** Nudge step while inside the guns, in tiles: a few pixels, not a walk. */
  val nudgeStepTiles: Double = 0.05

  /** Inside the stand radius the unit holds instead of closing. */
  def shouldHold(distTiles: Double): Boolean =
    distTiles <= standTiles

  /** Inside the static's own reach the unit eases out instead of standing. */
  def shouldNudge(distTiles: Double, staticRangeTiles: Double): Boolean =
    distTiles <= staticRangeTiles

  /** Raw back-off point, straight away from the static. Seam walkables it. */
  def backoff(own: Mathematics.Points.Pixel, threat: Mathematics.Points.Pixel): Mathematics.Points.Pixel =
    own.project(threat, -nudgeStepTiles * 32.0)
}

object StaticStandoffBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.canMove || unit.flying) return false
    if (unit.unitClass.melee || ! unit.unitClass.isWarrior || Protoss.Reaver(unit)) return false
    if (combat.shouldRetreat) return false
    if ( ! unit.squad.exists(_.holdsLine)) return false

    val static = Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.isStaticDefense && e.canAttack(unit)))(
      unit.pixelDistanceEdge(_))
    if (static.isEmpty) return false

    val gun = static.get
    val distTiles = unit.pixelDistanceEdge(gun) * Maff.inv32
    if (unit.matchups.targetsInRange.nonEmpty) {
      unit.agent.act("StaticStandoff")
      NearestShot.take(unit, "StaticStandoff", unit.matchups.targetsInRange)
    } else if (StaticStandoff.shouldNudge(distTiles, gun.pixelRangeAgainst(unit) * Maff.inv32)) {
      unit.agent.act("StaticNudge")
      unit.agent.decision.set(StaticStandoff.backoff(unit.pixel, gun.pixel).walkablePixel)
      Commander.move(unit)
      true
    } else if (StaticStandoff.shouldHold(distTiles)) {
      unit.agent.act("StaticStandoff")
      Commander.hold(unit)

      true
    } else false
  }
}
