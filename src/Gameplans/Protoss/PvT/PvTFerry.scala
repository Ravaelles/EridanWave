package Gameplans.Protoss.PvT

import Lifecycle.With
import Macro.Actions.MacroActions
import ProxyBwapi.Races.Protoss

/**
 * Shared ferry planning (Stardust ShuttleHarass doctrine): from Robotics
 * with two Gates, keep two Shuttles coming and raise the ferry flag once a
 * Shuttle and four Dragoons stand ready. The MissionFerry itself stages at
 * the vanguard and cycles the army to the front; this only plans the metal.
 */
object PvTFerry extends MacroActions {

  def ensureFerry(): Unit = {
    if ( ! have(Protoss.RoboticsFacility) || units(Protoss.Gateway) < 2) return

    pump(Protoss.Shuttle, 2)
    val ready = have(Protoss.Shuttle) && units(Protoss.Dragoon) >= 4
    if (ready) {
      harass()
    }
    With.blackboard.ferry.set(ready)
  }
}
