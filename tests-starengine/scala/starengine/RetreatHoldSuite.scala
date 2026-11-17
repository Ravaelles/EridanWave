package starengine

import Micro.Actions.Combat.Decisioning.ReengageLatch
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Retreat hold, step by step: a Dragoon faces two pressing Marines under
 * verdicts that flap fight-or-flee. The latched pilot calls the shipped
 * ReengageLatch: once fleeing it holds until clear of the deepest gun
 * with margin and the verdict says fight - or the duel is confident
 * enough to turn inside contact. With a settling verdict the holder
 * withdraws, returns and clears the pair; under a forever flap it
 * withdraws past the guns and lives out of reach (an unlatched hull
 * mills in contact instead - ShellShockSuite shows what that costs
 * under real shells). Speeds, reaches and damage are the real UnitType
 * numbers; the hold decision is the shipped kernel.
 */
class RetreatHoldSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Retreat.name + StarEngineTags.Dragoon.name) }

  /** Settling verdict: flaps for 200 frames, then the sim holds fight. */
  private def settling(frame: Int): Boolean =
    if (frame < 200) (frame / 30) % 2 == 0 else true

  /** Forever flap, the field cadence: fight two frames, flee two. */
  private def flapping(frame: Int): Boolean = (frame / 2) % 2 == 0

  /**
   * Latched pilot, production order: the verdict starts a retreat, the
   * shipped latch holds it past the guns, the return needs the verdict
   * plus the line (or the overmatch hatch). Counts holds against a live
   * fight verdict: the latch working, not luck.
   */
  private class LatchedPilot(verdict: Int => Boolean, confidence: Double) extends Pilot {
    var fleeing = false
    var heldAgainstVerdict = 0

    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f))
      val threat = world.foes.filter(f => f.alive && f.canHit(me)).sortBy(me.dist).headOption
      val entangled = threat.map(t => (t.rangeAgainst(me) - me.dist(t)) / 32.0).getOrElse(Double.NegativeInfinity)
      val fight = verdict(world.frame)
      if ( ! fight) fleeing = true
      else if ( ! ReengageLatch.shouldHoldRetreat(fleeing, entangled, verdictFight = true, confidence)) fleeing = false
      if (fleeing && fight) heldAgainstVerdict += 1
      if (fleeing) {
        threat match {
          case Some(t) =>
            val dx = me.x - t.x
            val dy = me.y - t.y
            val dist = Math.sqrt(dx * dx + dy * dy)
            if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
            else Hold
          case None => Hold
        }
      } else {
        foe match {
          case Some(f) => Chase(f)
          case None    => Hold
        }
      }
    }
  }

  private def runDuel(pilot: Pilot): (Body, Seq[Body], Int, Double, Double) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val marines = Seq(
      new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth),
      new Body(Terran.Marine, 360.0, 500.0, Terran.Marine.maxTotalHealth))
    val pilots: Map[Body, Pilot] =
      Map(dragoon -> pilot) ++ marines.map(_ -> new Pilots.ZombieAttack(_.ours, (0.0, 500.0))).toMap
    val world = new World(Seq(dragoon), marines, pilots, Seq.empty)

    var minDist = Double.PositiveInfinity
    var maxDist = 0.0
    while (world.frame < 3000 && dragoon.alive && marines.exists(_.alive)) {
      world.step()
      marines.filter(_.alive).foreach(m => {
        minDist = Math.min(minDist, dragoon.dist(m))
        maxDist = Math.max(maxDist, dragoon.dist(m))
      })
    }

    (dragoon, marines, world.frame, minDist, maxDist)
  }

  test("a latched hull withdraws, returns on the settled verdict, and clears the pair") {
    val pilot = new LatchedPilot(settling, confidence = 0.0)
    val (dragoon, marines, frames, _, _) = runDuel(pilot)
    println(s"DIAG hold frames=$frames held=${pilot.heldAgainstVerdict} dragoon=${dragoon.hp.toInt} marines=${marines.count(_.alive)}/2")
    assert(pilot.heldAgainstVerdict > 0, "latch never held against a live verdict")
    assert(marines.forall(! _.alive), "holding hull never returned to clear the pair")
    assert(dragoon.alive, "holding hull died withdrawing")
  }

  test("under a forever flap the holder lives out of gun reach") {
    val pilot = new LatchedPilot(flapping, confidence = 0.0)
    val (dragoon, marines, frames, minDist, _) = runDuel(pilot)
    println(s"DIAG siege frames=$frames held=${pilot.heldAgainstVerdict} dragoon=${dragoon.hp.toInt} minDist=${minDist.toInt}")
    assert(pilot.heldAgainstVerdict > 0, "latch never held against a live verdict")
    assert(dragoon.alive, "holding hull died under the flap")
    assert(minDist > 200.0, f"holder never got out of gun reach: ${minDist.toInt}px")
  }

  test("overmatch turns inside contact") {
    val pilot = new LatchedPilot(settling, confidence = 1.0)
    val (dragoon, marines, frames, _, _) = runDuel(pilot)
    println(s"DIAG overmatch frames=$frames held=${pilot.heldAgainstVerdict} dragoon=${dragoon.hp.toInt} marines=${marines.count(_.alive)}/2")
    assert(marines.forall(! _.alive), "confident hull never turned to clear the pair")
    assert(dragoon.alive, "confident hull died turning inside contact")
  }
}
