package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Target-less hunt: a warrior with fight consensus but no useful target
 * attack-moves toward the nearest known hittable enemy instead of posing
 * in formation slots until the timeout. Without a battle cluster there are
 * no matchups, so Target.choose stays empty and the line dresses forever -
 * the UMS duels froze a thousand pixels from passive hulls this way. A
 * far assigned target pins the same way: the squad's pylon call never
 * dies, so micro never re-picks, while the line dresses against a ghost
 * it never shoots. UMS duels only: no economy and static vicinities, so
 * hunting is always right; macro columns ride the march and must not chase
 * remembered contacts. Returns true when it moved the unit. Reavers keep
 * their pair doctrine (BeReaver owns their movement).
 */
object HuntBehavior {

  /** Sight margin past which an invisible target is a ghost, not a contact. */
  val sightMarginPixels: Double = 32.0

  def run(unit: FriendlyUnitInfo): Boolean = {
    val target = unit.agent.toAttack
    if ( ! shouldHunt(
      umsMode             = With.umsMode,
      isWarrior           = unit.unitClass.isWarrior,
      isReaver            = Protoss.Reaver(unit),
      canMove             = unit.canMove && ! unit.loaded,
      hasTarget           = target.isDefined,
      targetVisible       = target.forall(_.visible),
      targetBeyondPursuit = target.exists(t => unit.pixelsToGetInRange(t) > FormationHold.holdPursuitTiles * 32.0),
      targetBeyondSight   = target.exists(t => ! t.visible && unit.pixelsToGetInRange(t) > unit.sightPixels + sightMarginPixels))) return false

    val foe = Maff.minBy(huntables(unit))(unit.pixelDistanceCenter(_))
    if (foe.isEmpty) return false

    unit.agent.act("Hunt")
    Commander.attackMove(unit, foe.get.pixel)

    true
  }

  def huntables(unit: FriendlyUnitInfo): Seq[UnitInfo] =
    With.units.enemy.view.filter(e =>
      e.alive
      && e.likelyStillThere
      && unit.canAttack(e)).toSeq

  /**
   * Hunt gate (pure booleans for the suite): dueling walkers with no
   * target hunt, and so do walkers whose target is useless - past the
   * pursuit horizon the line would pose instead of fighting, and an
   * invisible target past sight plus a margin is a ghost, not a contact
   * (sight-edge flicker inside the margin keeps the line steady instead
   * of flapping). Reavers keep the pair doctrine; immobile, loaded and
   * non-warrior hulls never reach here.
   */
  def shouldHunt(
    umsMode             : Boolean,
    isWarrior           : Boolean,
    isReaver            : Boolean,
    canMove             : Boolean,
    hasTarget           : Boolean,
    targetVisible       : Boolean,
    targetBeyondPursuit : Boolean,
    targetBeyondSight   : Boolean): Boolean =
    umsMode && isWarrior && ! isReaver && canMove && (
      ! hasTarget || targetBeyondPursuit || ( ! targetVisible && targetBeyondSight))
}
