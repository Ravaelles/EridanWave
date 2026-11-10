package Gameplans.Protoss.PvP

import Gameplans.All.GameplanImperative
import Gameplans.Protoss.PvP.opening._
import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Strategery.Strategies.Protoss._

class PvPOpening extends GameplanImperative
  with PvPOpeningState
  with PvPOpeningSequencing
  with PvPOpeningReactions
  with PvPOpeningTimings
  with PvPOpeningStatus
  with PvPOpeningBuilds
  with PvPOpeningTech {

  // Some build references:
  // CoreZ  Robo    Range   Speed Shuttle Double Reaver (2p Ramped):    https://youtu.be/5kvNatT3PuU?t=3340
  // NZCore Range   Gate    Robo  Shuttle Reaver        (4p Inverted):  https://youtu.be/5kvNatT3PuU?t=4400
  // NZCore Range   Gate    Robo  Shuttle Reaver        (4p Inverted):  https://youtu.be/dCn9WWIk4so?t=4658 (Same build as above) Shuttle > Reaver > Observatory > Reaver > Expand > Obs
  // NZCore Range   Citadel                             (4p Inverted):  https://youtu.be/5kvNatT3PuU?t=4423
  // CoreZ  Range   Robo    Gate  Obs     Gate          (4p Inverted):  https://youtu.be/dCn9WWIk4so?t=4658 (After seeing Robo) Shuttle > Reaver > Reaver > All-In
  // ZCoreZ Range   Citadel Gate  Archives              (2p Ramped):    https://youtu.be/dCn9WWIk4so?t=6884 Gate Robo
  // CoreZ  Range   2Goon   Nexus Robo    GateGate      (2p Ramped):    https://youtu.be/dCn9WWIk4so?t=6884 Obs

  override def activated: Boolean = true
  override def completed: Boolean = {
    complete ||= bases > 1
    complete &&= ! PvPDT() || cannonExpand || With.units.everOurs.exists(u => u.isOurs && u.complete && Protoss.DarkTemplar(u))
    complete
  }

  override def executeBuild(): Unit = {
    initSequence()
    PvPGateCore() // Make sure we activate it if selected
    updateStrategySwaps()
    decideTechOptions()
    updateTimings()
    reportStatus()
    orderScouting()
    orderZealotRush()
    orderSharedOpener()
    orderDetection()
    orderProxyDefense()
    orderMainBuilds(orderForgeExpandResponse())
  }

  def executeMain(): Unit = {
    manageGas()
    guardTechTransition()
    runTech()
    finishTech()
  }
}
