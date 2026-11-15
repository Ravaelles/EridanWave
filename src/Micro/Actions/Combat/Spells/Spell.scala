package Micro.Actions.Combat.Spells

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Castable spell (shared gate for Cast): which units cast it and whether
 * one can cast right now (caster, tech, energy - never situational gates
 * like detection or safety, those stay in allowed()). TargetedSpell
 * already answers both; point actions (cloak, heal) answer with their own
 * caster and energy rules. Cast lets an energy-user fight only while one
 * of these answers yes.
 */
trait Spell {

  def isCaster(unit: UnitInfo): Boolean

  def canCast(unit: UnitInfo): Boolean
}
