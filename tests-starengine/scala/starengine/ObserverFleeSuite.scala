package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Observer hunter early warning: with an enemy detector inside eleven
 * tiles the eye is seen, so the fast hunters (Scourge, Corsair, Wraith,
 * Mutalisk) trigger the flee from twenty tiles out instead of twelve.
 * Same geometry twice, detector present or not: the compromised eye
 * must break first and keep the larger cushion. Speeds, reaches and
 * damage are the real UnitType numbers; the muta always presses.
 */
class ObserverFleeSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Observer.name + StarEngineTags.Detector.name) }

  /** Production order, stripped to the flee: hunter early warning first, gun flee second, hold otherwise. */
  private class EyePilot(detector: Option[Body]) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val hunters = world.foes.filter(f => f.alive && (f.kind == Zerg.Mutalisk))
      val hunterNear = (if (hunters.isEmpty) Double.PositiveInfinity else hunters.map(me.dist).min)
      val detectorNear = detector.filter(_.alive).map(me.dist).getOrElse(Double.PositiveInfinity)
      if (detectorNear <= 11 * 32.0 && hunterNear <= 20 * 32.0 && hunters.nonEmpty) {
        val focus = hunters.minBy(me.dist)
        val dx = me.x - focus.x
        val dy = me.y - focus.y
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) return MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)

      }

      val guns = world.foes.filter(f => f.alive && f.kind.canAttack(me.kind) && me.dist(f) <= 12 * 32.0)
      if (guns.nonEmpty) {
        val focus = guns.minBy(me.dist)
        val dx = me.x - focus.x
        val dy = me.y - focus.y
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) return MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)

      }

      Hold
    }
  }

  private case class Outcome(firstMoveFrame: Option[Int], minDist: Double, aliveFrames: Int, distAt20: Double)

  private def runEye(withDetector: Boolean): Outcome = {
    val eye = new Body(Protoss.Observer, 0.0, 500.0, Protoss.Observer.maxTotalHealth)
    val spore = new Body(Zerg.SporeColony, 0.0, 150.0, Zerg.SporeColony.maxTotalHealth)
    val muta = new Body(Zerg.Mutalisk, 400.0, 150.0, Zerg.Mutalisk.maxTotalHealth)
    val foes = if (withDetector) Seq(spore, muta) else Seq(muta)
    val pilots: Map[Body, Pilot] = Map(
      eye -> new EyePilot(if (withDetector) Some(spore) else None),
      muta -> new Pilots.ZombieAttack(_.ours, (400.0, 150.0)),
      spore -> new Pilots.Hold)
    val world = new World(Seq(eye), foes, pilots, Seq.empty)

    var firstMove: Option[Int] = None
    var minDist = Double.PositiveInfinity
    var distAt20 = Double.PositiveInfinity
    while (world.frame < 600 && eye.alive) {
      world.step()
      if (firstMove.isEmpty && Math.hypot(eye.x, eye.y - 500.0) > 32.0) firstMove = Some(world.frame)
      if (muta.alive) minDist = Math.min(minDist, eye.dist(muta))
      if (world.frame == 20) distAt20 = if (muta.alive) eye.dist(muta) else Double.PositiveInfinity

    }
    Outcome(firstMove, minDist, world.frame, distAt20)
  }

  test("a compromised eye breaks earlier and keeps a larger cushion") {
    val obs = new Body(Protoss.Observer, 0.0, 0.0, 1.0)
    val mut = new Body(Zerg.Mutalisk, 0.0, 0.0, 1.0)
    println(s"DIAG observer speed=${obs.speed} muta speed=${mut.speed} muta range=${mut.rangeAgainst(obs)}")
    val seen = runEye(withDetector = true)
    val blind = runEye(withDetector = false)
    println(s"DIAG seen firstMove=${seen.firstMoveFrame} d20=${seen.distAt20.toInt}px alive=${seen.aliveFrames}f")
    println(s"DIAG blind firstMove=${blind.firstMoveFrame} d20=${blind.distAt20.toInt}px alive=${blind.aliveFrames}f")
    assert(seen.firstMoveFrame.exists(_ <= 15), "compromised eye did not break immediately")
    assert(blind.firstMoveFrame.exists(_ >= 20), "unseen eye fled before any gun could see it")
    assert(seen.distAt20 > blind.distAt20, "early warning did not buy distance")
  }
}

