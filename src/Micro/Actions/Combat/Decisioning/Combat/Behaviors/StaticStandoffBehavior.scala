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
 * ground instead of milling into the guns. Wounded hulls stand further
 * out (up to 2.7 tiles at full wound, same math as the artillery
 * keep-off): a hull that cannot afford the trade holds a longer line.
 * Shoots whatever it can hit from the hold; holds position inside the
 * stand radius; eases a few pixels back only while standing inside the
 * static's own reach. The nudge is a raw projection, never pathing - units die shuffling for a
 * perfect slot the physics blocks, so the correction stays smaller
 * than a step. Steals only the approach, never the retreat: a fleeing
 * hull still runs for safety.
 */
object StaticStandoff {

  /** Hold the line this close to an enemy static, in tiles. */
  val standTiles: Double = 9.1

  /** Wounded hulls stand this much further out, in tiles at full wound. */
  val woundBonusTiles: Double = 2.7

  /** Nudge step while inside the guns, in tiles: a few pixels, not a walk. */
  val nudgeStepTiles: Double = 0.05

  /** Inside the stand radius plus the wound backoff the unit holds instead of closing. */
  def shouldHold(distTiles: Double, backoffTiles: Double): Boolean =
    distTiles <= standTiles + backoffTiles

  /** Inside the static's own reach plus the wound backoff the unit eases out instead of standing. */
  def shouldNudge(distTiles: Double, staticRangeTiles: Double, backoffTiles: Double): Boolean =
    distTiles <= staticRangeTiles + backoffTiles

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
    // Wounded hulls stand further out: the backoff rides both the nudge
    // and the hold, like the artillery keep-off (same wound math).
    val backoffTiles = unit.woundBonus(0.0, StaticStandoff.woundBonusTiles)
    if (unit.matchups.targetsInRange.nonEmpty) {
      unit.agent.act("StaticStandoff")
      NearestShot.take(unit, "StaticStandoff", unit.matchups.targetsInRange)
    } else if (StaticStandoff.shouldNudge(distTiles, gun.pixelRangeAgainst(unit) * Maff.inv32, backoffTiles)) {
      unit.agent.act("StaticNudge")
      unit.agent.decision.set(StaticStandoff.backoff(unit.pixel, gun.pixel).walkablePixel)
      Commander.move(unit)
      true
    } else if (StaticStandoff.shouldHold(distTiles, backoffTiles)) {
      unit.agent.act("StaticStandoff")
      Commander.hold(unit)

      true
    } else false
  }
}
