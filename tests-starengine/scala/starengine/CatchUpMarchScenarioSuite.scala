package starengine

import ProxyBwapi.Races.Protoss
import Tactic.Squads.Decisions._
import Utilities.Latches
import org.scalatest.funsuite.AnyFunSuite

/**
 * Catch-up march: a reinforcement trailing the alpha with clear skies
 * joins at full speed. The BetaStar bill was mates arriving together:
 * LoneMarch needs no company, so it never fired for them, and a far
 * assigned target owned their movement through the duel verdict - the
 * march slotted while the technique danced, every frame the other way.
 * The pilot below drives the shipped kernels with engine-measured
 * numbers: the gate must fire with mates nearby (where LoneMarch
 * cannot), and a forced far duel must read Advance the whole run, so
 * no frame dances. Nobody shoots here (passive line, passive guns):
 * this isolates the joining, not the contact.
 */
class CatchUpMarchScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.PvP.name + StarEngineTags.Targeting.name) }

  private class TrailerPilot(
    id: Int,
    goal: (Double, Double),
    alpha: Seq[Body],
    useCatchUp: Boolean) extends Pilot {

    var catchUps: Int = 0
    var advances: Int = 0
    var duels: Int = 0
    var duelsWhileClear: Int = 0

    override def direct(me: Body, world: World): Directive = {
      val foeSet = world.foes.filter(_.alive)
      val mates = world.ours.filter(m => m != me && m.alive)
      val alphaLive = alpha.filter(_.alive)
      val anchor = alphaLive.flatMap(a =>
        foeSet.filterNot(_.kind.isFlyer).sortBy(a.dist).headOption).sortBy(me.dist).headOption
      val alphaCentroid = (
        alphaLive.map(_.x).sum / alphaLive.size.max(1),
        alphaLive.map(_.y).sum / alphaLive.size.max(1))
      val behindTiles = anchor.map(g =>
        me.dist(g) / 32.0 - Math.hypot(alphaCentroid._1 - g.x, alphaCentroid._2 - g.y) / 32.0)
        .getOrElse(Double.NegativeInfinity)
      val nearestArmedTiles = foeSet.filter(f => f.damageAgainst(me) > 0).map(me.dist(_) / 32.0)
        .reduceOption(_ min _).getOrElse(Double.PositiveInfinity)
      val gapTiles = foeSet.filter(_.kind.isWarrior).map(me.dist(_) / 32.0)
        .reduceOption(_ min _).getOrElse(Double.PositiveInfinity)

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
      // A far assigned target, like the production Target hands the
      // march: hittable nowhere near, but owned by the squad.
      val hasDuel = true
      val contact = foeSet.exists(f => me.canHit(f) && me.dist(f) <= me.rangeAgainst(f)) || world.underAttack(me)
      val clear = ArmedCordon.clearOfArmed(id, nearestArmedTiles)
      val catchUp = useCatchUp && ! verdictRetreat && ! contact && clear &&
        CatchUpMarch.catchUp(id, behindTiles)
      val desired = MotionVerdict.desired(verdictRetreat, hasDuel && ! catchUp, dressing = false)
      MotionVerdict.update(id, desired, world.frame)
      val heldNow = MotionVerdict.held(id, world.frame)
      if (heldNow == MotionVerdict.Advance) advances += 1
      else if (heldNow == MotionVerdict.Duel) {
        duels += 1
        if ( ! contact) duelsWhileClear += 1
      }

      val facts = MarchFacts(
        enemyIsTerran = false,
        enemyIsProtoss = true,
        enemyHasTanks = false,
        enemyHasVultures = false,
        enemyDragoonSeen = true,
        bankedMinerals = 0,
        armyStrength = 200.0,
        armyUnitsScore = 500.0,
        supplyUsed = 0,
        proxyRush = false,
        home = None,
        catchUpAnchor = None,
        alphaAnchorTiles = Double.PositiveInfinity)
      val marchUnit = MarchUnit(
        unitId = id,
        isDarkTemplar = false,
        isCorsair = false,
        isDragoon = true,
        isWarrior = true,
        isLoadedShuttle = false,
        isSuicide = false,
        shouldFight = true,
        verdictRetreat = verdictRetreat,
        verdictDuel = heldNow == MotionVerdict.Duel,
        hasDuel = hasDuel,
        nearestWarriorTiles = mates.map(me.dist(_) / 32.0).reduceOption(_ min _),
        distHomeTiles = None,
        nearestArmedTiles = nearestArmedTiles,
        nearestStaticTiles = None,
        nearestVisibleArmedTiles = nearestArmedTiles,
        behindAlphaTiles = behindTiles,
        hasContact = contact)
      if (MarchGates.select(facts, marchUnit).contains(MarchLabel.CatchUp)) catchUps += 1

      if (heldNow == MotionVerdict.Advance) MoveTo(goal._1, goal._2)
      else foeSet.sortBy(me.dist).headOption.map(Chase).getOrElse(MoveTo(goal._1, goal._2))
    }
  }

  private def runJoin(useCatchUp: Boolean, frames: Int = 1500): (Body, TrailerPilot) = {
    val foes = Seq(
      new Body(Protoss.Dragoon, -60.0, 0.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 60.0, 0.0, Protoss.Dragoon.maxTotalHealth))
    foes.foreach(_.passive = true)
    val alpha = Seq(
      new Body(Protoss.Dragoon, -40.0, -120.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 0.0, -120.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 40.0, -120.0, Protoss.Dragoon.maxTotalHealth))
    alpha.foreach(_.passive = true)
    val trailer = new Body(Protoss.Dragoon, 0.0, -800.0, Protoss.Dragoon.maxTotalHealth)
    val mate = new Body(Protoss.Dragoon, 50.0, -800.0, Protoss.Dragoon.maxTotalHealth)
    Latches.resetAll()
    val trailerPilot = new TrailerPilot(31, (0.0, 100.0), alpha, useCatchUp)
    val matePilot = new TrailerPilot(32, (0.0, 100.0), alpha, useCatchUp)
    val pilots: Map[Body, Pilot] = Map(
      trailer -> trailerPilot,
      mate -> matePilot) ++
      alpha.map(_ -> new Pilots.Hold) ++
      foes.map(_ -> new Pilots.Hold)
    val world = new World(Seq(trailer, mate) ++ alpha, foes, pilots, Seq.empty)
    var f = 0
    while (f < frames && trailer.alive) { world.step(); f += 1 }
    (trailer, trailerPilot)
  }

  test("a reinforcement trailing the alpha joins at full speed, never dueling") {
    val (trailer, pilot) = runJoin(useCatchUp = true)
    val goalDist = Math.hypot(trailer.x - 0.0, trailer.y - 100.0)
    println(s"DIAG join alive=${trailer.alive} catchUps=${pilot.catchUps} advances=${pilot.advances} duels=${pilot.duels} clearDuels=${pilot.duelsWhileClear} goalDist=${goalDist.toInt}px")
    assert(pilot.catchUps > 0, "the gate never fired with mates nearby - LoneMarch territory, nothing proven")
    assert(pilot.duelsWhileClear == 0, "a forced far duel owned frames it must never own")
    assert(pilot.advances > 0, "the verdict never read Advance on a joining march")
    assert(trailer.alive, "the reinforcement died joining with clear skies")
    assert(goalDist <= 12 * 32.0, "the reinforcement never reached the pack it was joining")
  }

  test("without the catch-up a far assigned target owns the march") {
    val (trailer, pilot) = runJoin(useCatchUp = false)
    println(s"DIAG old catchUps=${pilot.catchUps} advances=${pilot.advances} duels=${pilot.duels}")
    assert(pilot.catchUps == 0, "the gate fired with its flag off")
    assert(pilot.duels > 0, "the old brain marches a far duel instead of dancing it - nothing to fix here")
  }
}
