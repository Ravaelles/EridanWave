package Micro.Actions.Combat.Decisioning.Actions

import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Combat.Spells.Protoss._
import Micro.Actions.Combat.Spells.Spell
import Micro.Actions.Combat.Spells.Terran._
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
    // needs zero edits here.
    if (unit.ready && spells.exists(_.isCaster(unit))) {
      unit.agent.decideFight(unit.agent.shouldFight && spells.exists(_.canCast(unit)))
    }
  }

  val spells: Array[Action with Spell] = Array(
    Heal,
    WraithCloak,
    WraithUncloak,
    Yamato,
    Irradiate,
    DefensiveMatrix,
    PsionicStorm,
    Stasis,
    DisruptionWeb,
    MindControl,
    Maelstrom,
    Feedback,
    GhostCloak,
    TacticalNuke,
    GhostUncloak,
    Lockdown
  )
}
