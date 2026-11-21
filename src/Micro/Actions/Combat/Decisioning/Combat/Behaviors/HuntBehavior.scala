package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Minutes

/**
 * Target-less hunt: a warrior with fight consensus but no useful target
 * picks the nearest known hittable enemy instead of posing
 * in formation slots until the timeout. Without a battle cluster there are
 * no matchups, so Target.choose stays empty and the line dresses forever -
 * the UMS duels froze a thousand pixels from passive hulls this way. A
 * far assigned target pins the same way: the squad's pylon call never
 * dies, so micro never re-picks, while the line dresses against a ghost
 * it never shoots. UMS duels only: no economy and static vicinities, so
 * hunting is always right; macro columns ride the march and must not chase
 * remembered contacts. Assigns the target and yields - the pipeline below
 * owns the move through the break conditions, so the advance stays
 * slot-bound and confidence-gated. Reavers keep
 * their pair doctrine (BeReaver owns their movement).
 */
object HuntBehavior {

  /** Sight margin past which an invisible target is a ghost, not a contact. */
  val sightMarginPixels: Double = 32.0

  /**
   * Unit context gate (no game beyond the unit): hunting is warriors'
   * work - non-warriors, Reavers (pair doctrine) and immobile or
   * loaded hulls never reach here, whatever the map or the targets.
   * Map, targets and the verdict stay in the gate below; this answers
   * only which hulls may hunt at all. The run keeps its own checks
   * for direct callers, so both paths agree by construction.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isWarrior && ! Protoss.Reaver(unit) && unit.canMove && ! unit.loaded

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
      targetBeyondSight   = target.exists(t => ! t.visible && unit.pixelsToGetInRange(t) > unit.sightPixels + sightMarginPixels),
      verdictDuel         = Tactic.Squads.Decisions.MotionVerdict.held(unit.id, With.frame) == Tactic.Squads.Decisions.MotionVerdict.Duel)) return false

    val foe = Maff.minBy(huntables(unit))(unit.pixelDistanceCenter(_))
    if (foe.isEmpty) return false

    unit.agent.act("Hunt")
    unit.agent.toAttack = Some(foe.get)
    // Visible prey belongs to the duel: it advances through the break
    // conditions, slot-bound and confidence-gated. An invisible memory
    // gets no such owner - the line would dress against the ghost
    // instead of walking it down (Goon_v_DT posed and died standing),
    // so the hunt keeps the old attack-move and the feet keep moving.
    if (foe.get.visible) return false

    Commander.attackMove(unit, foe.get.pixel)

    true
  }

  def huntables(unit: FriendlyUnitInfo): Seq[UnitInfo] =
    With.units.enemy.view.filter(e =>
      e.alive
      && unit.canAttack(e)
      && (e.likelyStillThere
        || remembersMobile(With.framesSince(e.lastSeen))
        || (e.unitClass.isBuilding && e.lastSeen > 0))).toSeq

  /**
   * Memory horizon (pure frames for the suite): mobiles go stale three
   * minutes after contact (squad HuntContact parity) - they walk away.
   * Buildings never move, so a seen-once hall stays huntable until it
   * dies instead of stranding the line on a forgotten pin.
   */
  def remembersMobile(framesSinceSeen: Int): Boolean =
    framesSinceSeen < Minutes(3)()

  /**
   * Hunt gate (pure booleans for the suite): dueling walkers with no
   * target hunt, and so do walkers whose target is useless - past the
   * pursuit horizon the line would pose instead of fighting, and an
   * invisible target past sight plus a margin is a ghost, not a contact
   * (sight-edge flicker inside the margin keeps the line steady instead
   * of flapping). Reavers keep the pair doctrine; immobile, loaded and
   * non-warrior hulls never reach here. A latched duel pins the hull: the
   * march already holds a live duel under its feet, so chasing a memory
   * beside it is the flap, not mop-up.
   */
  def shouldHunt(
    umsMode             : Boolean,
    isWarrior           : Boolean,
    isReaver            : Boolean,
    canMove             : Boolean,
    hasTarget           : Boolean,
    targetVisible       : Boolean,
    targetBeyondPursuit : Boolean,
    targetBeyondSight   : Boolean,
    verdictDuel         : Boolean): Boolean =
    umsMode && isWarrior && ! isReaver && canMove && ! verdictDuel && (
      ! hasTarget || targetBeyondPursuit || ( ! targetVisible && targetBeyondSight))
}
