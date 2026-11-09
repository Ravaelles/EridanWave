package Tactic.Tactics

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Protoss

class TacticMeldArchons extends Tactic {

  def launch(): Unit = {
    // Archon production temporarily disabled entirely: delete this return
    // to re-enable melding (High Templars also arrive only at 150 supply).
    return

    lock.release() // Ditch units that are already Archons
    val threshold = With.blackboard.maximumArchonEnergy()
    if (threshold > 0) {
      lock.matcher = u => Protoss.HighTemplar(u) && u.energy < threshold
      lock.acquire()
      val partyCentral = Maff.minBy(With.geography.ourBases)(_.groundPixels(With.scouting.ourMuscleOrigin)).map(_.heart).getOrElse(With.geography.home).center
      units.foreach(_.intend(this)
        .setShouldMeld(true)
        .setTerminus(partyCentral))
    }
  }
}
