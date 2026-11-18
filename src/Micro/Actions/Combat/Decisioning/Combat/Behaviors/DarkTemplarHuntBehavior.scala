package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Targeting.DarkTemplarHunt
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Main-base Templar hunt: the army walks down a detected enemy Dark
 * Templar inside forty ground tiles of our main, even out of contact.
 * A Templar at the workers kills for free while the ladder scores
 * shinier fights, so the hunt outranks retreat like the always-attack
 * ladder does. Already dueling one in range keeps its duel - the pick
 * is nearest to the main, never nearest to me, so hunters cannot flap
 * a shared victim between them. Reavers and our own Templars keep
 * their own disciplines and never enter. Returns true when it hunts.
 */
object DarkTemplarHuntBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false
    if ( ! unit.unitClass.isWarrior) return false
    if ( ! unit.unitClass.attacksGround) return false
    if (Protoss.Reaver(unit) || Protoss.DarkTemplar(unit)) return false
    if (With.geography.ourBases.isEmpty) return false
    if (unit.agent.toAttack.exists(t => Protoss.DarkTemplar(t) && unit.inRangeToAttack(t))) return false

    val main = With.geography.ourMain.heart.center
    val candidates = unit.matchups.targets.view.filter(t =>
      t.alive && t.visible && t.detected && Protoss.DarkTemplar(t) &&
        DarkTemplarHunt.inHuntRange(t.tile.groundPixels(main) * Maff.inv32)).toVector
    if (candidates.isEmpty) return false

    val marks = candidates.map(t =>
      DarkTemplarHunt.Mark(t.tile.groundPixels(main) * Maff.inv32))
    DarkTemplarHunt.pick(marks).foreach(i =>
      NearestShot.take(unit, "HuntEnemyTemplar", Seq(candidates(i))))

    true
  }
}
