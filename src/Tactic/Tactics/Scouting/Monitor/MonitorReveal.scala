package Tactic.Tactics.Scouting.Monitor

import Information.Geography.Types.Base
import Mathematics.Maff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Tactics.Scouting.TacticMonitor

/** Expo reveal duty: the nearest free eye over a blocked planned base. */
object MonitorReveal {

  def pick(
    eyes        : Seq[FriendlyUnitInfo],
    escortTaken : Boolean): Option[(FriendlyUnitInfo, Base)] =
    RevealExpansionBlockedWithHiddenUnit.siteToReveal().flatMap(site => {
      val loneFirstEye = eyes.size == 1 && ! escortTaken
      if (eyes.isEmpty || (loneFirstEye && ! RevealExpansionBlockedWithHiddenUnit.mayUseFirstEye)) {
        None
      } else {
        Maff.minBy(eyes)(_.pixelDistanceSquared(site.heart.center)).map(eye => (eye, site))
      }
    })

  def act(monitor: TacticMonitor, eye: FriendlyUnitInfo, site: Base): Unit = {
    eye.intend(monitor).setTerminus(site.heart.center)
  }
}
