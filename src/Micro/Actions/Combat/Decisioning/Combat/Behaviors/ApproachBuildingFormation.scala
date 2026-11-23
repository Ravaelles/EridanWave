package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.Formation.OverwhelminglyStronger
import Micro.Actions.Combat.Decisioning.Kiting.{FacingAway, KiteAway}
import Micro.Actions.Combat.Decisioning.Skirmish.HoldLineAdvanceBehavior
import Micro.Agency.Commander
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

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! eligible(unit, combat)) return false

    unit.agent.act("Dress")
    // Dressed with a target past the gun: the held line advances.
    if (HoldLineAdvanceBehavior.run(unit, combat)) return true
    // Scattered: walk the march-owned destination (pack, goal, slot).
    if ( ! slotted(unit, combat)) {
      combat.maneuvers.move()

      return true
    }
    // Slotted with nothing to shoot: hold the line.
    Commander.hold(unit)

    true
  }

  private def eligible(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.unitClass.isWarrior || ! unit.canMove || unit.unitClass.melee) return false
    if (unit.agent.isScout) return false
    // Hittable frames belong to the duel, contact belongs to the kite.
    if (unit.matchups.targetsInRange.nonEmpty) return false
    if (unit.matchups.threatsInRange.nonEmpty) return false
    // Lone warriors duel, never dress: with no pack there are no slots
    // to hold, and holding hands the chaser a free walk-in to our wall.
    if (isLone(unit)) return false
    // Retreats march home, visible chases stay with the duel - ghosts
    // dress instead of attack-moving at stale pixels. Lone blades kite,
    // never dress: a chased hull with no guns nearby belongs to the duel
    // before contact. Same read as the Close outer gate, so vote and leg
    // agree by construction.
    if (combat.shouldRetreat) return false
    if (combat.shouldChase && combat.target.exists(t => t.visible && t.likelyStillThere)) return false

    val gunsNear = unit.matchups.threats.exists(t =>
      t.likelyStillThere && t.canAttack(unit) && ! t.unitClass.melee &&
      unit.pixelDistanceEdge(t) * Mathematics.Maff.inv32 <= KiteAway.chaseTiles)
    val meleeChase = unit.matchups.threatsInPixels(KiteAway.chaseTiles * 32.0).exists(t =>
      t.likelyStillThere && t.unitClass.melee &&
      ! FacingAway.dismissMelee(isMelee = true, FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(unit.pixel))))
    if (KiteAway.loneBladeDuel(gunsNear, meleeChase, unit.unitClass.melee)) return false
    // Overwhelming lines charge as today.
    if (OverwhelminglyStronger.skipFormation(unit)) return false

    true
  }

  private def slotted(unit: FriendlyUnitInfo, combat: Combat): Boolean =
    unit.formationEngage.isDefined && combat.formationReadiness >= HoldLineAdvance.advanceReadyFraction

  /** No pack, no line: warriors without another living warrior beside them. */
  private def isLone(unit: FriendlyUnitInfo): Boolean =
    ! unit.squad.exists(_.units.exists(o => o.id != unit.id && o.alive && o.unitClass.isWarrior))
}
