package Tactic.Squads.attack

import Lifecycle.With
import Mathematics.Maff
import Tactic.Squads.{Squad, SquadAutomation}
import Utilities.UnitFilters.IsWarrior

/**
 * Raze squad: while the main army contains, a fresh enemy expansion must
 * die before it pays for itself. The squad holds 8-10 warriors pulled
 * from the attack squad and marches the expansion heart; when the base
 * falls the members walk back into the main army instead of idling
 * abroad. Staffed explicitly by Tactician (locks, not freelancers), so
 * the split always comes out of the main army and always comes home.
 */
class SquadRazeExpansion extends Squad {

  lock.matcher = IsWarrior

  override def launch(): Unit = {} // Staffed from the attack squad by Tactician

  override def run(): Unit = {
    SquadRazeExpansion.expansionTarget.foreach(base => {
      vicinity = base.heart.center
      setTargets(base.units.filter(u => u.isEnemy && u.likelyStillThere).toVector)
    })
    SquadAutomation.send(this)
  }
}

object SquadRazeExpansion {

  /** Raze detail size, in warriors. */
  val razeWarriors: Int = 10

  /** Warriors the attack squad keeps while donating. */
  val attackReserve: Int = 10

  /**
   * Split rule (pure numbers for the suite): grow the detail toward ten
   * warriors, but never below the attack reserve - a contain that donates
   * its last Zealot is not a contain anymore.
   */
  def donorsNeeded(mine: Int, attackWarriors: Int): Int =
    Math.max(0, Math.min(razeWarriors - mine, attackWarriors - attackReserve))

  /** Weak-army raid detail, in warriors: too weak to contain, still raze. */
  val weakRazeWarriors: Int = 4

  /** Warriors the attack squad keeps while raiding weak. */
  val weakAttackReserve: Int = 6

  /**
   * Weak split rule (pure numbers for the suite): grow the raid toward four
   * warriors, but never below the raid reserve - a retreating army that
   * donates its last Zealot is not an army anymore. Below two warriors the
   * raid stays home: a lone Zealot abroad dies for nothing.
   */
  def donorsNeededWeak(mine: Int, attackWarriors: Int): Int = {
    val need = Math.max(0, Math.min(weakRazeWarriors - mine, attackWarriors - weakAttackReserve))

    if (mine + need >= 2) need else 0
  }

  /** Enemy expansion with a live town hall, nearest the main army. */
  def expansionTarget: Option[Information.Geography.Types.Base] = {
    val main = With.scouting.enemyMain
    val expansions = With.geography.enemyBases.view.filter(b =>
      main.forall(_ != b) && b.townHall.exists(t => t.isEnemy && t.likelyStillThere)).toVector
    if (expansions.isEmpty) None
    else Maff.minBy(expansions)(_.heart.center.pixelDistanceSquared(With.tactics.attackSquad.centroidGround))
  }
}
