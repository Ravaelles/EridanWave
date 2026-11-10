package Tactic

/**
 * Pure home-guard rule (no game access): a big army spares exactly one
 * body to sit on the threatened home base, so the main cluster never
 * walks home mid-contain to chase harass. Created on demand only, while
 * harassers stand on home soil; released when the base is clean.
 */
object HomeGuard {

  /** An army this big can spare a guard without noticing. */
  val armySize: Int = 25

  def shouldGuard(armyWarriors: Int, harassers: Int): Boolean =
    armyWarriors >= armySize && harassers >= 1
}
