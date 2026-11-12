package Gameplans.Protoss.PvZ.policy

/**
 * Pure gas-bank gate (no game access): late production must not drink the
 * Storm fund dry. From ten Dragoons the bank holds two-fifty before the
 * next one queues; past sixteen it holds three-sixty. Below ten, build.
 * Once the army counts over twenty warriors every Dragoon needs two-ten,
 * so a big Zealot core cannot silently starve Archons and Storm.
 */
object GasBank {

  /** Banked gas required per Dragoon past these counts. */
  val bankAtTen: Int = 250
  val bankAtSixteen: Int = 360

  /** Past this many warriors every Dragoon needs the army bank. */
  val armySize: Int = 20
  val bankForArmy: Int = 210

  def allowsDragoon(dragoons: Int, gas: Int, armyWarriors: Int): Boolean =
    (armyWarriors <= armySize || gas >= bankForArmy) && allowsDragoon(dragoons, gas)

  def allowsDragoon(dragoons: Int, gas: Int): Boolean =
    if (dragoons > 16) gas >= bankAtSixteen
    else if (dragoons >= 10) gas >= bankAtTen
    else true
}
