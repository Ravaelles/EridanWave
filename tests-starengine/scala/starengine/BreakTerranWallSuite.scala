package starengine

import Micro.Targeting.BreakTerranWallIn
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Terran wall-in, step by step: a Dragoon marches at a goal behind a
 * Supply Depot sitting in the choke, one Marine holding behind it. The
 * engine has no blocking terrain, so the suite brings its own WallBlock
 * constraint: while any wall building lives, ground bodies never cross
 * the wall line (both directions - the guard cannot sally either).
 *
 * First the repro: a pure march pilot mills at the wall forever, never
 * shooting the depot (the field report: NoTankMarch vs Reposition every
 * other frame, nothing attacks). Then the fix: a pilot calling the
 * shipped BreakTerranWallIn gate commits to the depot, chews through,
 * kills the guard and reaches the goal. Speeds, reaches and damage are
 * the real UnitType numbers; the decision is the shipped kernel.
 */
class BreakTerranWallSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Wall.name + StarEngineTags.Dragoon.name + StarEngineTags.PvT.name) }

  private val wallX = 800.0
  private val choke = (800.0, 500.0)
  private val goal = (1600.0, 500.0)

  /**
   * Wall line: ground bodies never cross it while a wall building lives.
   * The crossing test pins the side first seen - landing exactly on the
   * line (stride-sized steps do) still counts as the old side, otherwise
   * the next stride reads as no crossing and the wall leaks.
   */
  private class WallBlock(isWall: Body => Boolean) extends Constraint {
    private val prevX = new scala.collection.mutable.HashMap[Body, Double]()
    private val side = new scala.collection.mutable.HashMap[Body, Double]()

    override def apply(world: World): Unit = {
      val wallUp = (world.ours ++ world.foes).exists(b => b.alive && isWall(b))
      (world.ours ++ world.foes).foreach(blocked => {
        if (blocked.alive && ! blocked.kind.isFlyer && blocked.speed > 0) {
          val prev = prevX.getOrElse(blocked, blocked.x)
          val oldSide = side.getOrElse(blocked, {
            val s = Math.signum(prev - wallX)
            side(blocked) = s

            s
          })
          val crossed = wallUp && oldSide != 0 && oldSide * (blocked.x - wallX) < 0
          if (crossed) blocked.x = prev
          prevX(blocked) = blocked.x
          if (blocked.x != wallX) side(blocked) = Math.signum(blocked.x - wallX)
        }
      })
    }
  }

  private def distTiles(ax: Double, ay: Double, bx: Double, by: Double): Double = {
    val dx = ax - bx
    val dy = ay - by
    Math.sqrt(dx * dx + dy * dy) / 32.0
  }

  /**
   * Flap pilot: the field log verbatim (NoTankMarch f8387, Reposition
   * f8389, NoTankMarch f8393, ... - a new claimant every other frame).
   * The column surges at the goal for two frames, then regroups for two;
   * net progress is zero and the wall is never named, so the engine's
   * contact fire never even starts.
   */
  private class FlapPilot(start: (Double, Double)) extends Pilot {

    override def direct(me: Body, world: World): Directive =
      if ((world.frame / 2) % 2 == 0) MoveTo(goal._1, goal._2) else MoveTo(start._1, start._2)
  }

  /**
   * Breach pilot, production order: the quiet fresh column commits to the
   * nearest choke wall (the shipped gate), holds the commitment like
   * toAttack, then chews the nearest foe and walks the goal. The quiet
   * clock restarts on shots landing in range; the pooled bar reads fresh
   * shields while the hull is untouched.
   */
  private class BreakPilot extends Pilot {
    var committed: Option[Body] = None
    var lastAttackFrame: Int = -100000

    override def direct(me: Body, world: World): Directive = {
      committed = committed.filter(_.alive)
      committed match {
        case Some(wall) =>
          if (me.dist(wall) <= me.rangeAgainst(wall)) lastAttackFrame = world.frame

          Chase(wall)
        case None =>
          val walls = world.foes.filter(f =>
            f.alive && f.kind.isBuilding && f.kind.isTerran
              && distTiles(f.x, f.y, choke._1, choke._2) <= BreakTerranWallIn.chokeRadiusTiles)
          val sieged = world.foes.exists(f => f.alive && f.kind == Terran.SiegeTankSieged)
          val shieldWound = Math.min(80, (me.maxHp - me.hp).toInt)
          val wall = walls.sortBy(me.dist).headOption
          val breach = wall.filter(w => BreakTerranWallIn.shouldBreak(
            shieldWound,
            seesTerranBuilding = true,
            world.frame - lastAttackFrame,
            sieged,
            distTiles(w.x, w.y, choke._1, choke._2)))
          breach match {
            case Some(w) =>
              committed = Some(w)

              Chase(w)
            case None =>
              world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
                case Some(foe) => Chase(foe)
                case None      => MoveTo(goal._1, goal._2)
              }
          }
      }
    }
  }

  private def freshWorld(sieged: Boolean): (Body, Body, Body, Option[Body], World, Constraint) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val depot = new Body(Terran.SupplyDepot, 800.0, 500.0, Terran.SupplyDepot.maxTotalHealth)
    val marine = new Body(Terran.Marine, 1400.0, 500.0, Terran.Marine.maxTotalHealth)
    val tank = if (sieged)
      Some(new Body(Terran.SiegeTankSieged, 1500.0, 600.0, Terran.SiegeTankSieged.maxTotalHealth))
    else None
    val foes = Seq(Some(depot), Some(marine), tank).flatten
    val block = new WallBlock(_.kind.isBuilding)
    val pilots: Map[Body, Pilot] = Map(
      dragoon -> new BreakPilot,
      depot -> new Pilots.Hold,
      marine -> new Pilots.Hold) ++ tank.map(_ -> new Pilots.Hold).toMap
    val world = new World(Seq(dragoon), foes, pilots, Seq(block))

    (dragoon, depot, marine, tank, world, block)
  }

  test("a flapping column mills far out and never shoots") {
    val start = (0.0, 500.0)
    val dragoon = new Body(Protoss.Dragoon, start._1, start._2, Protoss.Dragoon.maxTotalHealth)
    val depot = new Body(Terran.SupplyDepot, 800.0, 500.0, Terran.SupplyDepot.maxTotalHealth)
    val marine = new Body(Terran.Marine, 1400.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      dragoon -> new FlapPilot(start),
      depot -> new Pilots.Hold,
      marine -> new Pilots.Hold)
    val world = new World(Seq(dragoon), Seq(depot, marine), pilots, Seq(new WallBlock(_.kind.isBuilding)))

    while (world.frame < 3000 && dragoon.alive) {
      world.step()
    }

    println(s"DIAG flap dragoon x=${dragoon.x.toInt} depotHp=${depot.hp.toInt} marineHp=${marine.hp.toInt}")
    assert(dragoon.alive, "dragoon died milling")
    assert(dragoon.x < wallX, "flap crossed the standing wall")
    assert(depot.hp == depot.maxHp, "flap shot the wall it never named")
    assert(marine.hp == marine.maxHp, "flap reached the guard behind the wall")
  }

  test("the breach pilot chews the wall, kills the guard, reaches the goal") {
    val (dragoon, depot, marine, _, world, _) = freshWorld(sieged = false)

    while (world.frame < 8000 && dragoon.alive && (depot.alive || marine.alive || dragoon.x < goal._1 - 32)) {
      world.step()
    }

    println(s"DIAG breach frames=${world.frame} dragoon x=${dragoon.x.toInt} hp=${dragoon.hp.toInt} depot=${depot.hp.toInt} marine=${marine.hp.toInt}")
    assert( ! depot.alive, "breach pilot never chewed the depot")
    assert( ! marine.alive, "breach pilot stalled after the wall")
    assert(dragoon.alive, "dragoon died breaking one depot and one marine")
    assert(dragoon.x >= goal._1 - 32, "dragoon never walked the goal after the breach")
  }

  test("sieged tanks hold the veto: the column mills on") {
    val start = (0.0, 500.0)
    val dragoon = new Body(Protoss.Dragoon, start._1, start._2, Protoss.Dragoon.maxTotalHealth)
    val depot = new Body(Terran.SupplyDepot, 800.0, 500.0, Terran.SupplyDepot.maxTotalHealth)
    val marine = new Body(Terran.Marine, 1400.0, 500.0, Terran.Marine.maxTotalHealth)
    val tank = new Body(Terran.SiegeTankSieged, 1500.0, 600.0, Terran.SiegeTankSieged.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      dragoon -> new FlapPilot(start),
      depot -> new Pilots.Hold,
      marine -> new Pilots.Hold,
      tank -> new Pilots.Hold)
    val world = new World(Seq(dragoon), Seq(depot, marine, tank), pilots, Seq(new WallBlock(_.kind.isBuilding)))

    while (world.frame < 3000 && dragoon.alive) {
      world.step()
    }

    println(s"DIAG siege dragoon x=${dragoon.x.toInt} hp=${dragoon.hp.toInt} depotHp=${depot.hp.toInt} tankAlive=${tank.alive}")
    assert(depot.hp == depot.maxHp, "column knocked under siege")
    assert(dragoon.x < wallX, "column walked into sieged tanks")
    assert(dragoon.alive, "siege shelled the milling column")
  }
}
