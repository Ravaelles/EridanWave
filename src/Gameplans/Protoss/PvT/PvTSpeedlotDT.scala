package Gameplans.Protoss.PvT

import Gameplans.All.GameplanImperative
import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Strategery.Strategies.Protoss.PvTSpeedlotDT
import Utilities.Time.{GameTime, Minutes, Seconds}

class PvTSpeedlotDT extends GameplanImperative {

  override def activated: Boolean = PvTSpeedlotDT()
  override def completed: Boolean = With.frame > Minutes(9)()

  override def executeBuild(): Unit = {
    if (With.fingerprints.workerRush()) {
      pump(Protoss.Zealot)
      pump(Protoss.Probe)
      gasWorkerCeiling(0)
      cancel(Protoss.Assimilator, Protoss.CyberneticsCore, Protoss.CitadelOfAdun, Protoss.TemplarArchives, Protoss.DarkTemplar, Protoss.DragoonRange, Protoss.ZealotSpeed)
    }
    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(9, Protoss.Probe)
    once(Protoss.Gateway)
    once(11, Protoss.Probe)
    once(Protoss.Assimilator)
    once(12, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(14, Protoss.Probe)
    once(Protoss.CitadelOfAdun)
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
  }
  override def executeMain(): Unit = {
    if ( ! foundEnemyBase) {
      scoutAt(14)
    }
    if (With.geography.ourMetro.units.exists(u => Protoss.DarkTemplar(u) && u.complete)) {
      attack() // Just make sure our DTs can get out
    }
    get(2, Protoss.Gateway)
    get(Protoss.TemplarArchives)
    expandWhileContaining()
    once(2, Protoss.DarkTemplar)
    get(5, Protoss.Gateway)
    get(Protoss.DragoonRange)
    get(Protoss.ZealotSpeed)
    if (frame > GameTime(7, 0)()) {
      pump(Protoss.DarkTemplar)
      pump(Protoss.Zealot)
      attack()
      if ((GameTime(7, 18) - Seconds(18)).minutes % 2 == 1) {
        aggression(1e10)
      }
    }
    pump(Protoss.Zealot, units(Protoss.Dragoon))
    pump(Protoss.Dragoon)
    pump(Protoss.Zealot)
  }

  /**
   * Expand while containing: a Terran locked behind his wall cannot
   * punish a Nexus, so the natural goes down instead of waiting for a
   * timing that never needs it. Same shape as the PvP fearContain
   * clause: contain plus a safe path, never blind.
   */
  private def expandWhileContaining(): Unit = {
    if (Tactic.Squads.ContainStatus.now() && With.geography.safeExpansions.nonEmpty) {
      status("ContainExpand")
      expandOnce()
    }
  }
}
