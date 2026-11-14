package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * March-goal flap (StarEngine repro of a live log): a lone Zealot
 * marching home alternated LoneMarch / Engage-Approach every few frames
 * - walking, stopping, walking. The trace showed the march destination
 * itself alternating +-300px on nearly every other frame, and every
 * order change costs the frame (re-aiming resets acceleration): a goal
 * that never settles means a march that never moves. The sharp pilot
 * below serves the raw jittered goal; the confirmed pilot only adopts a
 * goal that repeats - the VoteConfirmation shape. Same jitter, same map:
 * the raw goal must stall while the confirmed one walks home.
 */
class MarchFlapSuite extends AnyFunSuite with StarEngineSuite {

  private val goalA = (300.0, 0.0)
  private val goalB = (-300.0, 0.0)
  private val home = (0.0, 600.0)

  /** Jittered march goal, extreme form of the logged +-300px alternation: a new goal every frame. */
  private def jitteredGoal(frame: Int): (Double, Double) =
    if (frame % 2 == 0) goalA else goalB

  /** Sharp arbitration: serve whatever the goal says this frame. Flaps with it. */
  private class SharpMarch extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val g = jitteredGoal(world.frame)
      MoveTo(g._1, g._2)
    }
  }

  /** Confirmed arbitration: adopt a goal only after it repeats; serve the latched one meanwhile. */
  private class ConfirmedMarch(repeatFrames: Int) extends Pilot {
    private var latched: (Double, Double) = goalA
    private var candidate: (Double, Double) = goalA
    private var streak = 0

    override def direct(me: Body, world: World): Directive = {
      val g = jitteredGoal(world.frame)
      if (g == candidate) streak += 1
      else { candidate = g; streak = 1 }
      if (streak >= repeatFrames) latched = candidate
      MoveTo(latched._1, latched._2)
    }
  }

  private def runPilot(pilot: Pilot): (Option[Int], Int) = {
    val zealot = new Body(Protoss.Zealot, home._1, home._2, Protoss.Zealot.maxTotalHealth)
    val world = new World(Seq(zealot), Seq.empty, Map(zealot -> pilot), Seq.empty)
    var arrived: Option[Int] = None
    while (world.frame < 1500 && zealot.alive) {
      world.step()
      val homeDist = Math.hypot(zealot.x - goalA._1, zealot.y - goalA._2)
      if (arrived.isEmpty && homeDist < 32.0) arrived = Some(world.frame)
    }
    (arrived, world.reissues.getOrElse(zealot, 0))
  }

  test("a confirmed goal walks while the raw jitter stalls the march") {
    val (sharpArrival, sharpFlips) = runPilot(new SharpMarch)
    val (calmArrival, calmFlips) = runPilot(new ConfirmedMarch(repeatFrames = 6))
    println(s"DIAG sharp arrival=$sharpArrival flips=$sharpFlips calm arrival=$calmArrival flips=$calmFlips")
    assert(sharpFlips >= 1000, "repro did not flap")
    assert(sharpArrival.isEmpty, "raw jitter should stall the march, not walk it home")
    assert(calmArrival.isDefined, "confirmed goal never arrived")
    assert(calmFlips < 5, "confirmation did not calm the flap")
  }
}
