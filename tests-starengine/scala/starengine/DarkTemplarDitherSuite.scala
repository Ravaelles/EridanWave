package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Protoss.DarkTemplar.DarkTemplarEvade
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * DT detector dither: a Templar hunting a hydra parked under an Overlord
 * approaches, enters the sight band, evades 128px out, and re-approaches
 * - the evade destination (away) directly opposes the hunt destination
 * (toward the enemy, past the detector). Live games show Hunt/Evade
 * alternating ~150 times until the Templar dies without connecting.
 * The fixed brain latches the evade (release two tiles past the band)
 * and skips covered hunt targets, so it breaks clean and holds the
 * standoff instead of feeding the loop. Speeds, reaches, damage and the
 * Overlord's sight are the real UnitType numbers; both pilots call the
 * shipped kernels, the engine owns the loop only.
 */
class DarkTemplarDitherSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Detector.name) }

  private def away(me: Body, x: Double, y: Double): (Double, Double) = {
    val dx = me.x - x
    val dy = me.y - y
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist > 0) (dx / dist, dy / dist) else (0.0, 1.0)
  }

  private def pixelOf(b: Body): Pixel = new Pixel(b.x.toInt, b.y.toInt)

  /** Production order, stripped to the duel: guns, detector, hunt. */
  private class DitherPilot(id: Int, latched: Boolean, filtered: Boolean) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val hydra = world.foes.find(f => f.alive && f.kind == Zerg.Hydralisk)
      val over = world.foes.find(f => f.alive && f.kind == Zerg.Overlord)
      val sight = over.map(_.kind.sightPixels.toDouble).getOrElse(0.0)

      // No gun rule in this duel: the attacker-flee is its own concept
      // with its own suite, and the hydra below holds its ground. This
      // isolates the detector loop (evade destination away vs hunt
      // destination toward), which is what dithers in the field.
      over.foreach { o =>
        val evade =
          if (latched) DarkTemplarEvade.evadeLatched(id, me.dist(o), sight)
          else DarkTemplarEvade.shouldEvade(me.dist(o), sight)
        if (evade) {
          val (dx, dy) = away(me, o.x, o.y)

          return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
        }
      }

      val prey = hydra.filter(h =>
        ! filtered || ! DarkTemplarEvade.underDetection(
          pixelOf(h),
          over.map(o => Seq((pixelOf(o), sight))).getOrElse(Seq.empty)))
      prey match {
        case Some(h) => Chase(h)
        case None     => Hold
      }
    }
  }

  private case class Outcome(crossings: Int, dtHp: Double, hydraHp: Double, frames: Int)

  private def runDuel(latched: Boolean, filtered: Boolean): Outcome = {
    if (latched) DarkTemplarEvade.reset()
    val dt = new Body(Protoss.DarkTemplar, 400.0, 820.0, Protoss.DarkTemplar.maxTotalHealth)
    val over = new Body(Zerg.Overlord, 400.0, 280.0, Zerg.Overlord.maxTotalHealth)
    val hydra = new Body(Zerg.Hydralisk, 400.0, 320.0, Zerg.Hydralisk.maxTotalHealth)
    val sight = over.kind.sightPixels
    val pilots: Map[Body, Pilot] = Map(
      dt -> new DitherPilot(1, latched, filtered),
      over -> new Pilots.Hold,
      hydra -> new Pilots.Hold)
    val world = new World(Seq(dt), Seq(over, hydra), pilots, Seq.empty)

    var crossings = 0
    var wasInside = dt.dist(over) < DarkTemplarEvade.evadeRadiusPixels(sight)
    while (world.frame < 2400 && dt.alive) {
      world.step()
      val inside = over.alive && dt.dist(over) < DarkTemplarEvade.evadeRadiusPixels(sight)
      if (inside != wasInside) crossings += 1
      wasInside = inside
    }
    Outcome(crossings, dt.hp, hydra.hp, world.frame)
  }

  test("a sharp release dithers on the band and never connects") {
    val dt = new Body(Protoss.DarkTemplar, 0.0, 0.0, Protoss.DarkTemplar.maxTotalHealth)
    val over = new Body(Zerg.Overlord, 0.0, 0.0, Zerg.Overlord.maxTotalHealth)
    println(s"DIAG dt speed=${dt.speed} hydra range=${new Body(Zerg.Hydralisk, 0, 0, 1).rangeAgainst(dt)} overlord sight=${over.kind.sightPixels}")
    val sharp = runDuel(latched = false, filtered = false)
    println(s"DIAG sharp crossings=${sharp.crossings} dtHp=${sharp.dtHp.toInt} hydraHp=${sharp.hydraHp.toInt} frames=${sharp.frames}")
    assert(sharp.crossings >= 6, s"no dither to fix: ${sharp.crossings} band crossings")
    assert(sharp.hydraHp >= 80.0, "sharp DT connected despite the loop")
  }

  test("a latched evade with a covered-target filter holds the standoff") {
    val fixed = runDuel(latched = true, filtered = true)
    println(s"DIAG fixed crossings=${fixed.crossings} dtHp=${fixed.dtHp.toInt} hydraHp=${fixed.hydraHp.toInt} frames=${fixed.frames}")
    assert(fixed.crossings <= 2, s"still dithering: ${fixed.crossings} band crossings")
    assert(fixed.dtHp > 0.0, "latched DT died holding the standoff")
  }
}
