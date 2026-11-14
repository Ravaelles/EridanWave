package starengine

import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Standoff pursuit: a Dragoon line holds against a far enemy instead of
 * chasing piecemeal - but a hold with no end is a freeze, not discipline.
 * A live kiter inside dragoon sight keeps the line honest; a dummy
 * twenty-five tiles past our reach never will, so the line must walk it
 * down instead of posing at it until the timeout. Speeds and reaches are
 * the real UnitType numbers; the foe holds its ground like the map dummy,
 * the pilot calls the shipped hold kernel, the engine owns the loop only.
 */
class FormationStandoffSuite extends AnyFunSuite {

  private class HoldPilot extends Pilot {
    var pursuing: Boolean = false

    override def direct(me: Body, world: World): Directive = {
      world.foes.find(_.alive) match {
        case None => Hold
        case Some(foe) =>
          // Production order mirrored: the pursuit latch commits once past
          // the horizon and holds until contact, so the cap edge never
          // becomes the new standoff; inside it the shipped hold kernel
          // decides (applyFormationHold).
          val beyond = (me.dist(foe) - me.rangeAgainst(foe)) / 32.0
          if (beyond > FormationHold.holdPursuitTiles) pursuing = true
          else if (beyond <= FormationHold.holdReleaseTiles) pursuing = false
          if (pursuing) Chase(foe)
          else if (FormationHold.shouldHoldLine(
            tilesBeyondRange = beyond,
            melee = false,
            hasFormation = true,
            flanking = false,
            unitEngaged = false,
            enemyFired = false)) Hold
          else Chase(foe)
      }
    }
  }

  private case class Outcome(kills: Int, closestPx: Double, frames: Int)

  private def runStandoff(beyondTiles: Double): Outcome = {
    val ours = new Body(Protoss.Dragoon, 380.0, 700.0, Protoss.Dragoon.maxTotalHealth)
    val wing = new Body(Protoss.Dragoon, 420.0, 700.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Dragoon, 400.0, 700.0 - (128.0 + beyondTiles * 32.0), Protoss.Dragoon.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      ours -> new HoldPilot,
      wing -> new HoldPilot,
      foe -> new Pilots.Hold)
    val world = new World(Seq(ours, wing), Seq(foe), pilots, Seq.empty)

    var closest = ours.dist(foe)
    while (world.frame < 1500 && foe.alive) {
      world.step()
      if (foe.alive) closest = Math.min(closest, ours.dist(foe))
    }
    Outcome(if (foe.alive) 0 else 1, closest, world.frame)
  }

  test("a dummy twenty-five tiles past our reach gets walked down") {
    val calm = runStandoff(beyondTiles = 25.0)
    println(s"DIAG dummy kills=${calm.kills} closest=${calm.closestPx.toInt}px frames=${calm.frames}")
    assert(calm.kills > 0, "line held against a dummy until timeout")
  }
}
