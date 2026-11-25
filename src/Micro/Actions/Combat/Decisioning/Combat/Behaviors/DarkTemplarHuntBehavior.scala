package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import Micro.Targeting.DarkTemplarHunt
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Main-base Templar hunt: the army walks down a detected enemy Dark
 * Templar near our main, even out of contact - forty ground tiles out,
 * fifty while below thirty warriors.
 * A Templar at the workers kills for free while the ladder scores
 * shinier fights, so the hunt outranks retreat like the always-attack
 * ladder does. Already dueling one in range keeps its duel - the pick
 * is nearest to the main, never nearest to me, so hunters cannot flap
 * a shared victim between them. Reavers and our own Templars keep
 * their own disciplines and never enter. Baseless maps anchor on our
 * start tile instead of a main that does not exist; the anchor never
 * moves either way. Returns true when it hunts.
 */
object DarkTemplarHuntBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false
    if ( ! unit.unitClass.isWarrior) return false
    if ( ! unit.unitClass.attacksGround) return false
    if (Protoss.Reaver(unit) || Protoss.DarkTemplar(unit)) return false
    // Cooling guns walk, never chew (owner order): a dragoon that cannot
    // answer this cycle yields instead of standing into the blades - the
    // kite leg below owns the escape, the hunt resumes off cooldown.
    if (KiteAway.mustHideFromTemplar(Protoss.Dragoon(unit), unit.cooldownLeft, templarNear = true)) return false
    if (unit.agent.toAttack.exists(t => Protoss.DarkTemplar(t) && unit.inRangeToAttack(t))) return false

    val main =
      if (With.geography.ourBases.nonEmpty) With.geography.ourMain.heart.center
      else With.geography.home.center
    // Below thirty warriors the whole army answers the blade: a small
    // force cannot spare a detachment, so the hunt reaches fifty ground
    // tiles out instead of forty. Large armies hold the old line.
    val warriors = With.units.ours.view.count(u => u.alive && u.unitClass.isWarrior)
    val candidates = unit.matchups.targets.view.filter(t =>
      t.alive && t.visible && t.detected && Protoss.DarkTemplar(t) &&
        DarkTemplarHunt.inHuntRange(t.tile.groundPixels(main) * Maff.inv32, warriors)).toVector
    if (candidates.isEmpty) return false

    val marks = candidates.map(t =>
      DarkTemplarHunt.Mark(t.tile.groundPixels(main) * Maff.inv32))
    DarkTemplarHunt.pick(marks).foreach(i =>
      NearestShot.take(unit, "HuntEnemyTemplar", Seq(candidates(i))))

    true
  }
}
