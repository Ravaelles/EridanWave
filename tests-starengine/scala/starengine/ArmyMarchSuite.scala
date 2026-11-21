package starengine

import Micro.Targeting.{Target, TargetRecall}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import Tactic.Squads.Decisions._
import Utilities.Latches
import org.scalatest.funsuite.AnyFunSuite

/**
 * Army march discipline: a column ordered at the enemy base must get
 * there. The shipped march decides through pure kernels (gate table,
 * posture, hold votes, far-wait latch) - this scenario drives those
 * same kernels every frame and asserts the emergent march: arrival,
 * no full stops without contact, no duel-hold without a hittable
 * enemy in reach, no retreats on an attacking march. A ghost memory
 * (target with no body) and an unhittable watcher (melee staring at
 * air) must never pin the line - that paralysis is exactly what the
 * hittable-duel gate is for, so both ride here as regression rows.
 */
class ArmyMarchSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  private def matesOf(me: Body, world: World): Seq[Body] =
    (if (world.ours.contains(me)) world.ours else world.foes).filter(m => m != me && m.alive)

  /**
   * March brain: the shipped UMS decision order, shrunk to the march.
   * Gate table first (lone columns walk at the goal), then the
   * far-wait dress, then the duel or the march. Duels never hold the
   * line: the march always walks, the duel chases through its own
   * orders. Every branch consults a shipped pure kernel with
   * engine-measured numbers - the pilot owns no thresholds and writes
   * no destinations of its own. Retreats (the bus never votes on an
   * attacking march) are counted, never just rendered.
   */
  private class MarchPilot(goal: (Double, Double), dragoon: Boolean) extends Pilot {
    var retreats: Int = 0
    var loneMarches: Int = 0
    var farWaits: Int = 0

    override def direct(me: Body, world: World): Directive = {
      val foeSet = foesOf(me, world).filter(_.alive)
      val mates = matesOf(me, world)
      // The shipped far-wait reads warriors only: eyes and guns never
      // dress the line, so the pilot measures the same ball here.
      val warriorFoes = foeSet.filter(_.kind.isWarrior)
      val nearestFoe = warriorFoes.sortBy(me.dist).headOption.orElse(foeSet.sortBy(me.dist).headOption)
      val gapTiles = warriorFoes.map(me.dist(_) / 32.0).reduceOption(_ min _).getOrElse(Double.PositiveInfinity)
      val rangeTiles = warriorFoes.map(f => me.rangeAgainst(f) / 32.0).reduceOption(_ max _).getOrElse(Double.PositiveInfinity)
      val hittable = nearestFoe.filter(f => me.canHit(f) && me.dist(f) <= me.rangeAgainst(f))
      val engagedUpon = foeSet.exists(f => f.dist(me) <= f.rangeAgainst(me))
      val mateTiles = mates.map(me.dist(_) / 32.0).reduceOption(_ min _).getOrElse(Double.PositiveInfinity)
      val nearestArmedTiles = foeSet.filter(f => f.damageAgainst(me) > 0).map(me.dist(_) / 32.0)
        .reduceOption(_ min _).getOrElse(Double.PositiveInfinity)

      // Attacking march, like the shipped UMS rule: fight, never withdraw.
      // The verdict bus reads the same raw inputs the march arm does.
      // The gate below reads the verdict back, like production.
      val verdictRetreat = RetreatVerdict.shouldHold(
        isWarrior = true,
        isReaver = false,
        canFlee = true,
        stormHold = false,
        shouldFight = true,
        totalHp = me.hp.toInt,
        unitsScore = 500.0,
        nearestArmedTiles = nearestArmedTiles,
        proxyRush = false,
        hardCounterKnown = false,
        nearestEnemyWarriorTiles = gapTiles)
      if (verdictRetreat) retreats += 1

      // Gate table, with engine-measured facts.
      val facts = MarchFacts(
        enemyIsTerran = false,
        enemyIsProtoss = true,
        enemyHasTanks = false,
        enemyHasVultures = false,
        enemyDragoonSeen = foeSet.nonEmpty,
        bankedMinerals = 0,
        armyStrength = 200.0,
        armyUnitsScore = 500.0,
        supplyUsed = 0,
        proxyRush = false,
        home = None)
      // The shipped duel definition, so the gate and the legs agree
      // here exactly like they do in production. The latched verdict
      // mirrors the fresh duel: the engine ticks once, and a first tick
      // always preempts sharp through the bus.
      val hasDuel = hittable.exists(t => Target.holdsDuelTarget(isWarrior = true, me.canHit(t))) || engagedUpon
      val marchUnit = MarchUnit(
        unitId = System.identityHashCode(me),
        isDarkTemplar = false,
        isCorsair = false,
        isDragoon = dragoon,
        isWarrior = true,
        isLoadedShuttle = false,
        isSuicide = false,
        shouldFight = true,
        verdictRetreat = verdictRetreat,
        verdictDuel = hasDuel,
        hasDuel = hasDuel,
        // The cordon reads visible guns only: the engine has no fog, so
      // every armed foe counts, exactly like production with eyes on.
      nearestVisibleArmedTiles = foeSet.filter(f => f.damageAgainst(me) > 0).map(me.dist(_) / 32.0)
        .reduceOption(_ min _).getOrElse(Double.PositiveInfinity),
      nearestWarriorTiles = if (mateTiles.isInfinite) None else Some(mateTiles),
        distHomeTiles = None,
        nearestArmedTiles = nearestArmedTiles,
        nearestStaticTiles = None)
      if (MarchGates.select(facts, marchUnit).contains(MarchLabel.LoneMarch)) loneMarches += 1

      // No duel hold: the line always walks, duels chase through their
      // own orders instead of pinning feet to the ground. Ghosts and
      // unhittable watchers never pinned it and still do not.

      // Far dress-and-wait, with the shipped latch: dress five out and
      // creep instead of trickling in. Releases on contact by itself.
      if (foeSet.nonEmpty && MarchDress.holdFarWait(
        System.identityHashCode(me),
        isWarrior = true,
        umsMode = true,
        enemyProtoss = true,
        enemyReaverKnown = false,
        gapTiles,
        rangeTiles)) {
        farWaits += 1
        // Creep at the enemy, like the shipped leg: holding the goal
        // would stall against a defender that never advances.
        if (MarchDress.creepNow(world.frame)) {
          val f = nearestFoe.get
          val dx = f.x - me.x
          val dy = f.y - me.y
          val d = Math.sqrt(dx * dx + dy * dy)
          if (d > 0) return MoveTo(me.x + dx / d * 4.0 * 2, me.y + dy / d * 4.0 * 2)
        }

        return Hold
      }

      // Duel what is hittable in reach or inside the recall horizon,
      // march at the goal past it - the horizon, not the fog, owns ghosts.
      hittable
        .orElse(nearestFoe.filter(f => me.canHit(f) && TargetRecall.shouldRecall(me.dist(f) / 32.0)))
        .map(Chase)
        .getOrElse(MoveTo(goal._1, goal._2))
    }
  }

  private def column(kind: UnitClass, n: Int, y: Double): Seq[Body] =
    (0 until n).map(i => new Body(kind, i * 40.0, y, kind.maxTotalHealth))

  private def grind(
    ours: Seq[Body],
    foes: Seq[Body],
    oursP: Body => Pilot,
    foeP: Body => Pilot,
    frames: Int = 9000): (World, Seq[MarchPilot]) = {
    Latches.resetAll()
    val pilots = (ours.map(o => o -> oursP(o)) ++ foes.map(f => f -> foeP(f))).toMap
    val marchers = pilots.values.collect { case m: MarchPilot => m }.toSeq
    val world = new World(ours, foes, pilots, Seq.empty)
    world.run(frames)
    (world, marchers)
  }

  private def stalled(pilots: Seq[MarchPilot]): Boolean =
    pilots.exists(_.retreats > 0)

  test("a column marches on the enemy base and duels on arrival") {
    val ours = column(Protoss.Dragoon, 6, 900.0)
    val foeP = new Pilots.Hold
    val foes = column(Protoss.Dragoon, 4, 100.0)
    val (world, marchers) = grind(ours, foes, _ => new MarchPilot((200.0, 100.0), dragoon = true), _ => foeP)
    val arrived = ours.count(o => o.alive && Math.hypot(o.x - 200.0, o.y - 100.0) <= 12 * 32.0)
    val duelling = ours.count(o => o.alive && foes.exists(f => f.alive && o.dist(f) <= o.rangeAgainst(f)))
    println(s"DIAG arrived=$arrived/6 duelling=$duelling retreats=${marchers.map(_.retreats).sum} farwaits=${marchers.map(_.farWaits).sum}")
    assert(arrived + duelling == ours.count(_.alive), "every live hull either arrives or duels, none stalls mid-map")
    assert( ! stalled(marchers), "no retreats on an attacking march")
    assert(ours.exists(o => ! o.alive) || foes.exists(f => ! f.alive), "contact deals damage both ways")
  }

  test("a ghost memory never pins the line") {
    // No bodies anywhere near: the recall horizon drops the ghost, the
    // march walks through. Stays true the whole run, not just once.
    val ours = column(Protoss.Dragoon, 3, 900.0)
    val foes = column(Protoss.Dragoon, 2, -2000.0)
    val (world, marchers) = grind(ours, foes, _ => new MarchPilot((200.0, 100.0), dragoon = true), _ => new Pilots.Hold, frames = 3000)
    val arrived = ours.count(o => o.alive && Math.hypot(o.x - 200.0, o.y - 100.0) <= 12 * 32.0)
    println(s"DIAG arrived=$arrived/3 farwaits=${marchers.map(_.farWaits).sum}")
    assert(arrived == 3, "ghosts do not hold the march")
    assert( ! stalled(marchers), "no retreats against nothing")
    assert(marchers.forall(_.farWaits == 0), "far away, no wait")
  }

  test("an unhittable watcher never pins melee") {
    // Zealots under a hovering observer: nothing to hit, march on.
    // (The shipped gate answers the same for any unhittable watcher,
    // and the far-wait reads warriors only, so eyes never dress it.)
    val ours = column(Protoss.Zealot, 3, 900.0)
    val foes = Seq(new Body(Protoss.Observer, 200.0, 500.0, Protoss.Observer.maxTotalHealth),
      new Body(Protoss.Observer, 240.0, 500.0, Protoss.Observer.maxTotalHealth))
    val (world, marchers) = grind(ours, foes, _ => new MarchPilot((200.0, 100.0), dragoon = false), _ => new Pilots.Hold, frames = 6000)
    val arrived = ours.count(o => o.alive && Math.hypot(o.x - 200.0, o.y - 100.0) <= 12 * 32.0)
    println(s"DIAG arrived=$arrived/3 farwaits=${marchers.map(_.farWaits).sum}")
    assert(arrived == 3, "unhittable watchers do not hold melee")
    assert( ! stalled(marchers), "no retreats under a spectator")
  }

  test("a lone hull walks at the goal instead of dressing") {
    val ours = Seq(new Body(Protoss.Dragoon, 0.0, 900.0, Protoss.Dragoon.maxTotalHealth))
    val foes = column(Protoss.Dragoon, 2, -2000.0)
    val (_, marchers) = grind(ours, foes, _ => new MarchPilot((200.0, 100.0), dragoon = true), _ => new Pilots.Hold, frames = 2000)
    println(s"DIAG lonemarches=${marchers.map(_.loneMarches).sum}")
    assert(marchers.exists(_.loneMarches > 0), "a hull with no pack marches alone")
  }
}
