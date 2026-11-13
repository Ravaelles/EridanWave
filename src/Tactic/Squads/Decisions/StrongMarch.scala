package Tactic.Squads.Decisions

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway

/**
 * Pure strong-march gate (no game access): an overwhelming force never
 * poses - a banked mineral stock or a maxed strong army walks straight at
 * the squad goal instead of rallying to the pack centroid or marching to
 * a formation slot. Same bankroll policy as the kite skip (one knob, one
 * tuning dimension); the strength half needs both muscle and a full
 * roster, so a strong but half-built army still forms up. The caller
 * measures minerals, strength and supply; this only combines them.
 */
object StrongMarch {

  /** A maxed roster backing real muscle, in standard supply. */
  val maxedSupply: Int = 198

  /** Muscle that walks through instead of forming up, in skim strength. */
  val strongArmy: Double = 200.0

  def shouldMarchDirectly(minerals: Int, armyStrength: Double, supplyUsed: Int): Boolean =
    minerals >= KiteAway.richBankMinerals ||
      (armyStrength >= strongArmy && supplyUsed >= maxedSupply)
}
