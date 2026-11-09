package Tactic.Tactics.Scouting.Monitor

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Tactics.Scouting.TacticMonitor

/** Army escort duty: one eye overhead once burrowed threats are known. */
object MonitorEscort {

  def applies(armyOut: Boolean, lurkersKnown: Boolean): Boolean =
    armyOut && lurkersKnown

  def anchor(): Pixel = {
    val army = With.tactics.attackSquad

    if (army.units.nonEmpty) army.centroidKey else With.geography.home.center
  }

  def pick(eyes: Seq[FriendlyUnitInfo], anchor: Pixel): Option[FriendlyUnitInfo] =
    Maff.minBy(eyes)(_.pixelDistanceSquared(anchor))

  def act(monitor: TacticMonitor, eye: FriendlyUnitInfo, anchor: Pixel): Unit = {
    Debugging.CherryVis.CherryVis.decision(eye.id, "Escorting attack squad")
    eye.intend(monitor)
      .setTerminus(anchor)
      .setRedoubt(With.geography.home.center)
  }
}
