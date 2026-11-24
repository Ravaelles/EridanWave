package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Formation.OverwhelminglyStronger
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Formation approach (owner order: fight positions, never charges):
 * a warrior the duel sends to fight with nothing hittable and no
 * contact dresses the line instead of attack-moving - slots when
 * scattered, hold when slotted, creep when dressed with a target past
 * the gun. Melee closes anyway, retreats march home, active chases
 * stay with the duel, overwhelming lines charge as today. Returns true
 * when it moved or held the unit.
 */
object ApproachBuildingFormation {

  /**
   * Dress step, in tiles: walk to a slot only this near - anything
   * farther means the formation moved on without this hull, and
   * chasing it strings the line out instead of dressing it. The rally
   * owns stragglers past the leash; inside it the hull holds.
   */
  val dressStepTiles: Double = 2.0

  /**
   * Formation distance, in tiles (owner order): dress the line once a
   * seen enemy stands inside it - fifteen against Terran siege tanks
   * (their shells outrange the line), twelve against everyone else.
   * Farther out the march owns the walk.
   */
  def formationRangeTiles(terranSieged: Boolean): Double =
    if (terranSieged) 15.0 else 12.0

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! eligible(unit, combat)) return false

    unit.agent.act("Dress")
    // The dressed advance is suspended (owner order): the line forms
    // and freezes instead of creeping - advancing before contact is
    // understood needs stillness as the instrument first. Nearby slots
    // are walked to, never the far goal: chasing a formation across
    // the map strings the line out instead of dressing it. A slot past
    // the leash means the pack moved on without this hull: hold, the
    // rally owns stragglers. Slotless hulls hold where they stand.
    unit.formationEngage.flatMap(_(unit)) match {
      case Some(slot) if unit.pixelDistanceCenter(slot) * Maff.inv32 <= dressStepTiles =>
        unit.agent.decision.set(slot)
        combat.maneuvers.move()

        true
      case _ =>
        // Explicit hold, not the stationary skip: a standing hull keeps
        // whatever order it already holds (map attack-moves, stale
        // marches), so the freeze must own the order outright.
        if (unit.order != ProxyBwapi.Orders.HoldPosition) unit.bwapiUnit.holdPosition()
        Commander.hold(unit)

        true
    }
  }

  /**
   * Seen enemy warrior inside formation range: the line dresses for it
   * (fifteen against Terran siege tanks, twelve against everyone
   * else), never blind and never across the map. Shared with the Close
   * outer gate, so the duel never charges what the line should dress.
   */
  def seenNearFoe(unit: FriendlyUnitInfo): Boolean = {
    val terranSieged = With.enemies.exists(_.isTerran) &&
      With.units.enemy.exists(e => e.alive && e.likelyStillThere && Terran.SiegeTankSieged(e))
    Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32).exists(_ <= formationRangeTiles(terranSieged))
  }
  /**
   * Formation gate (shared with the Hunt stage): the dressed line owns
   * outranged frames, so a ghost hunt never walks it out of formation.
   */
  def eligible(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.unitClass.isWarrior || ! unit.canMove || unit.unitClass.melee) return false
    if (unit.agent.isScout) return false
    // Hittable frames belong to the duel, contact belongs to the kite.
    if (unit.matchups.targetsInRange.nonEmpty) return false
    if (unit.matchups.threatsInRange.nonEmpty) return false
    // Lone warriors duel, never dress: with no pack there are no slots
    // to hold, and holding hands the chaser a free walk-in to our wall.
    if (isLone(unit)) return false
    // Retreats march home; visible runners stay with the duel, ghosts
    // and approachers dress instead of attack-moving at stale pixels.
    // Let them walk into the dressed line instead of meeting them
    // halfway - only a visibly fleeing target keeps its chase.
    if (combat.shouldRetreat) return false
    if (combat.shouldChase && combat.target.exists(t => t.visible && t.likelyStillThere && t.speedApproaching(unit) < 0)) return false

    // Stragglers rally, they never dress: past the leash the march owns
    // the frame (rally, catch-up), inside it the line owns it. Same six
    // tiles the kite leash reads - one distance for pack cohesion.
    // Squadless hulls march: without a pack there is nothing to dress.
    if (unit.squad.isEmpty) return false
    if (farFromPack(unit)) return false
    // Dress on a seen enemy inside formation range, never blind and
    // never across the map. Farther out the march owns the walk.
    // Overwhelming lines charge as today.
    if ( ! seenNearFoe(unit)) return false
    if (OverwhelminglyStronger.skipFormation(unit)) return false

    true
  }

  /** Past the pack leash: the straggler rejoins instead of dressing. */
  private def farFromPack(unit: FriendlyUnitInfo): Boolean =
    unit.squad.exists(squad => {
      val pack = if (unit.flying) squad.centroidAir else squad.centroidGround
      unit.pixelDistanceCenter(pack) * Maff.inv32 > KiteAway.leashTiles
    })

  /** No pack, no line: warriors without another living warrior beside them. */
  private def isLone(unit: FriendlyUnitInfo): Boolean =
    ! unit.squad.exists(_.units.exists(o => o.id != unit.id && o.alive && o.unitClass.isWarrior))
}
