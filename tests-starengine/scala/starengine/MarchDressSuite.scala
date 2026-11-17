package starengine

import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

/**
 * Dressed march: four Dragoons and four Zealots start scattered over a
 * twenty-tile box while four enemy Dragoons press from thirty tiles out.
 * The line walks a shared destination (the pack marches at the enemy
 * centroid, not strung between personal duels), dresses inside the
 * pursuit horizon on the shipped hold kernel, and engages on contact.
 * Speeds, reaches and cooldowns are the real UnitType numbers; the enemy
 * presses like a real pack so holds release on fire and contact, the
 * pilot calls the shipped kernels, the engine owns the loop only.
 *
 * The pilot logs every mode change per unit - the manager sequence:
 * March never comes back after Engage (no retreat to slotting), and the
 * engine's own reissue counter stays flat (marching is not stop-and-go).
 */
class MarchDressSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.March.name + StarEngineTags.Formation.name + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name) }

  /** March at the pack, dress inside the horizon, fight on contact. */
  private class DressMarchPilot extends Pilot {
    val modes: mutable.ListBuffer[(Int, String)] = mutable.ListBuffer.empty
    private var last: String = ""

    private def set(frame: Int, mode: String): String = {
      if (mode != last) {
        modes.append((frame, mode))
        last = mode
      }
      mode
    }

    override def direct(me: Body, world: World): Directive = {
      val live = world.foes.filter(_.alive)
      if (live.isEmpty) {
        set(world.frame, "Won")

        return Hold
      }

      val foe = live.minBy(me.dist)
      val beyond = (me.dist(foe) - me.rangeAgainst(foe)) / 32.0
      if (beyond > FormationHold.holdPursuitTiles) {
        set(world.frame, "March")
        val cx = live.map(_.x).sum / live.size
        val cy = live.map(_.y).sum / live.size
        MoveTo(cx, cy)
      } else if (FormationHold.shouldHoldLine(
        tilesBeyondRange = beyond,
        melee             = me.kind.melee,
        hasFormation      = true,
        flanking          = false,
        unitEngaged       = live.exists(f => f.dist(me) <= f.rangeAgainst(me)),
        enemyFired        = live.exists(f => f.cooldownLeft > 0))) {
        set(world.frame, "Dress")
        Hold
      } else {
        set(world.frame, "Engage")
        Chase(foe)
      }
    }
  }

  private case class March(
    kills: Int,
    reissues: Int,
    frames: Int,
    minDistPx: Map[Body, Double],
    transitions: Map[Body, Seq[String]],
    initialSpreadPx: Double,
    finalSpreadPx: Double,
    initialAllyGapPx: Double,
    finalAllyGapPx: Double)

  private def spread(units: Seq[Body], foes: Seq[Body]): Double = {
    val live = foes.filter(_.alive)
    if (live.isEmpty) return 0.0

    val ds = units.filter(_.alive).map(u => live.map(u.dist).min)
    if (ds.isEmpty) 0.0 else ds.max - ds.min
  }

  private def allyGap(units: Seq[Body]): Double = {
    val live = units.filter(_.alive)
    if (live.size < 2) 0.0
    else live.map(u => live.filterNot(_ eq u).map(u.dist).min).max
  }

  private def runMarch(): (March, Seq[Body], Seq[Body]) = {
    val dragoons = Seq(
      new Body(Protoss.Dragoon, -384.0, 0.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, -128.0, 160.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 128.0, -160.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 384.0, 64.0, Protoss.Dragoon.maxTotalHealth))
    val zealots = Seq(
      new Body(Protoss.Zealot, -256.0, -320.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 64.0, -352.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 288.0, -288.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, -32.0, 224.0, Protoss.Zealot.maxTotalHealth))
    val ours = dragoons ++ zealots
    val foes = Seq(
      new Body(Protoss.Dragoon, -192.0, -960.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, -64.0, -960.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 64.0, -960.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 192.0, -960.0, Protoss.Dragoon.maxTotalHealth))
    val pilots: Map[Body, Pilot] = (
      ours.map(_ -> new DressMarchPilot) ++
        foes.map(_ -> new Pilots.ZombieAttack(_.ours, (0.0, -960.0)))).toMap
    val world = new World(ours, foes, pilots, Seq.empty)
    val initialSpread = spread(ours, foes)
    val initialAlly = allyGap(ours)

    val minDist = mutable.HashMap[Body, Double]() ++ ours.map(_ -> Double.PositiveInfinity)
    while (world.frame < 1500 && foes.exists(_.alive) && ours.exists(_.alive)) {
      world.step()
      ours.filter(_.alive).foreach(u => {
        val ds = foes.filter(_.alive).map(u.dist)
        if (ds.nonEmpty) minDist(u) = Math.min(minDist(u), ds.min)
      })
    }

    val transitions = pilots.collect {
      case (b, p: DressMarchPilot) if ours.contains(b) => b -> p.modes.map(_._2).toSeq
    }.toMap
    val outcome = March(
      kills = foes.count(! _.alive),
      reissues = ours.map(world.reissues.getOrElse(_, 0)).sum,
      frames = world.frame,
      minDistPx = minDist.toMap,
      transitions = transitions,
      initialSpreadPx = initialSpread,
      finalSpreadPx = spread(ours, foes),
      initialAllyGapPx = initialAlly,
      finalAllyGapPx = allyGap(ours))
    (outcome, ours, foes)
  }

  test("a scattered line marches dressed, never stops, and kills") {
    val (m, ours, foes) = runMarch()
    ours.foreach(u => println(
      f"DIAG ${u.kind} @(${u.x.toInt},${u.y.toInt}) minDist=${m.minDistPx(u).toInt}px " +
        f"modes=${m.transitions.getOrElse(u, Seq.empty).mkString(">")}"))
    println(f"DIAG kills=${m.kills} reissues=${m.reissues} frames=${m.frames} " +
      f"spread=${m.initialSpreadPx.toInt}px->${m.finalSpreadPx.toInt}px " +
      f"allyGap=${m.initialAllyGapPx.toInt}px->${m.finalAllyGapPx.toInt}px")

    assert(m.kills == foes.size, "the dressed line did not finish the pack")
    assert(m.reissues <= 16, f"marching stuttered into stops: ${m.reissues} redirects")
    m.transitions.foreach { case (u, modes) =>
      assert(modes.size <= 4, f"manager flapped: ${modes.mkString(">")}")
      assert( ! modes.sliding(2).exists(w => w == Seq("Engage", "March")), "engaged unit walked back to slotting")
    }
    ours.foreach(u =>
      assert(m.minDistPx(u) < 6 * 32.0, "a unit never closed to contact"))
    assert(m.finalAllyGapPx <= m.initialAllyGapPx, "the pack scattered instead of rejoining")
  }
}
