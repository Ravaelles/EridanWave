package Gameplans.Protoss.PvZ

/**
 * Pure gas-bank gate (no game access): late production must not drink the
 * Storm fund dry. From ten Dragoons the bank holds two-fifty before the
 * next one queues; past sixteen it holds three-sixty. Below ten, build.
 */
object GasBank {

  /** Banked gas required per Dragoon past these counts. */
  val bankAtTen: Int = 250
  val bankAtSixteen: Int = 360

  def allowsDragoon(dragoons: Int, gas: Int): Boolean =
    if (dragoons > 16) gas >= bankAtSixteen
    else if (dragoons >= 10) gas >= bankAtTen
    else true
}
