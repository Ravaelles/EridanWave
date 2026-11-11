package Tactic.Squads.defense

import Information.Geography.Chokes
import Lifecycle.With
import Mathematics.Points.Pixel
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.UnitInfo
import Tactic.Squads.Squad
import Tactic.Squads.SquadAutomation
import Utilities.UnitFilters.IsAny

/**
 * The standing guard's post: enemies at any home base first, the natural
 * choke otherwise. One rule shared by Tactician (staffing) and the squad
 * (running), so the two can never disagree about where home is.
 */
object BaseGuardPost {

  def apply(): Pixel = threatened.getOrElse(choke)

  private def threatened: Option[Pixel] = With.geography.ourBases.filterNot(_.isMain)
    .filter(h => h.metro.units.exists(u => u.isEnemy && u.unitClass.isWarrior))
    .headOption.map(_.heart.center)

  private def choke: Pixel = Chokes.naturalChoke.map(_.pixelCenter)
    .getOrElse(With.geography.ourMain.heart.center)
}

/**
 * Standing home guard: posted from twenty warriors, scaled with the army,
 * answering enemies at any of our bases. Sized by Tactician through the
 * lock counter, so it shrinks through standard ablation when the army
 * bleeds. Dynamic defense squads are never staffed without a live division
 * and dissolve on their own once it ends; this is the only standing defense.
 */
class SquadBaseGuards extends Squad {

  // Standing guard, never a specialist: Dragoons and Zealots only.
  lock.setMatcher(IsAny(Protoss.Dragoon, Protoss.Zealot))

  override def launch(): Unit = {} // Staffed externally by Tactician

  override def run(): Unit = {
    vicinity = BaseGuardPost()
    setTargets(guardTargets)
    SquadAutomation.send(this)
  }

  private def guardTargets: Vector[UnitInfo] = With.geography.ourBases
    .flatMap(_.metro.units)
    .filter(u => u.isEnemy && u.unitClass.isWarrior).toVector
}
