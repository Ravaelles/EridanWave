package starengine

import Micro.Actions.Protoss.Dragoon.DragoonPvpRetreat
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Early PvP mirror, step by step: our Dragoons meet enemy Dragoons with
 * no upgrades anywhere. The shipped DragoonPvpRetreat kernel owns the
 * call on plain counts - an outmatched handful walks home to the
 * middle instead of trading into a superior mirror, while a
 * two-to-one pack closes and kills. Speeds, reaches and damage are
 * the real UnitType numbers; the retreat call is the shipped kernel.
 */
class DragoonMirrorSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Pressure.name + StarEngineTags.Retreat.name + StarEngineTags.PvP.name) }

  private val home = (-400.0, 500.0)

  /** Pilot mirroring the production path: kernel decides, legs execute. */
  private class MirrorPilot extends Pilot {
    var retreats = 0
    var engages = 0

    override def direct(me: Body, world: World): Directive = {
      val ours = world.ours.count(o => o.alive && o.kind == Protoss.Dragoon)
      val foes = world.foes.filter(_.alive)
      val retreat = DragoonPvpRetreat.shouldRetreat(
        isDragoon = me.kind == Protoss.Dragoon,
        enemyIsProtoss = true,
        oursDragoons = ours,
        enemyHasDragoons = foes.exists(_.kind == Protoss.Dragoon),
        usUnits = ours.toDouble,
        foeUnits = foes.size.toDouble)
      if (retreat) {
        retreats += 1
        val dx = me.x - home._1
        val dy = me.y - home._2
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
        else Hold
      } else {
        engages += 1
        foes.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
      }
    }
  }

  private def runMirror(ours: Int, foes: Int, frames: Int = 3000): (Seq[Body], Seq[Body], MirrorPilot, Int) = {
    val oursBodies = (0 until ours).map(i =>
      new Body(Protoss.Dragoon, 0.0 - i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foeBodies = (0 until foes).map(i =>
      new Body(Protoss.Dragoon, 400.0 + i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val pilot = new MirrorPilot
    val world = new World(oursBodies, foeBodies,
      (oursBodies.map(_ -> pilot) ++ foeBodies.map(_ -> new Pilots.Hold)).toMap, Seq.empty)

    var f = 0
    while (f < frames && oursBodies.exists(_.alive) && foeBodies.exists(_.alive)) {
      world.step()
      f = world.frame
    }

    (oursBodies, foeBodies, pilot, f)
  }

  test("an outmatched handful walks home instead of trading") {
    val (ours, foes, pilot, frames) = runMirror(3, 5)
    println(s"DIAG retreat frames=$frames retreats=${pilot.retreats} engages=${pilot.engages} ours=${ours.count(_.alive)}/3 foes=${foes.count(_.alive)}/5")
    assert(pilot.engages == 0, "outmatched pack engaged instead of leaving")
    assert(ours.forall(_.alive), "outmatched pack fed the mirror")
  }

  test("a two-to-one pack closes and clears the mirror") {
    val (ours, foes, pilot, frames) = runMirror(4, 2, 4000)
    println(s"DIAG fight frames=$frames retreats=${pilot.retreats} engages=${pilot.engages} ours=${ours.count(_.alive)}/4 foes=${foes.count(_.alive)}/2")
    assert(pilot.retreats == 0, "overmatching pack walked home")
    assert(foes.forall(! _.alive), "overmatching pack never cleared the mirror")
  }
}
