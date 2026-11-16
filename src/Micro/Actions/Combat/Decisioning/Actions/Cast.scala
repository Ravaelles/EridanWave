package Micro.Actions.Combat.Decisioning.Actions

import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Combat.Spells.Protoss._
import Micro.Actions.Combat.Spells.Spell
import Micro.Actions.Combat.Spells.Terran._
import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Cast extends Action {
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.energyMax > 0

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.cooldownSpell < With.latency.remainingFrames) {
      spells.foreach(_(unit))
    }
    // Energy-users fight only while one of our spells can fire: a dry
    // caster retreats instead of trading its cooldown for nothing. The
    // gate reads the spells' own caster and energy rules, so a new spell
    // needs zero edits here. Defiler and Queen have no Spell action yet,
    // so they keep the legacy energy rule instead of fighting dry.
    if (unit.ready && (spells.exists(_.isCaster(unit)) || Zerg.Defiler(unit) || Zerg.Queen(unit))) {
      val spellReady = spells.exists(_.canCast(unit))
      val legacyReady = (Zerg.Defiler(unit) || Zerg.Queen(unit)) &&
        unit.unitClass.spells.exists(s => s() && unit.energy >= s.energyCost - 2)
      unit.agent.decideFight(unit.agent.shouldFight && (spellReady || legacyReady))
    }
  }

  val spells: Array[Action with Spell] = Array(
    Heal,
    Cloak,
    Yamato,
    Irradiate,
    DefensiveMatrix,
    PsionicStorm,
    Stasis,
    DisruptionWeb,
    MindControl,
    Maelstrom,
    Feedback,
    TacticalNuke,
    Lockdown
  )
}
