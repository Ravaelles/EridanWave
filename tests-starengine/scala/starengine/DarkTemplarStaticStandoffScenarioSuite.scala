package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Protoss.DarkTemplar.{DarkTemplarEvade, DarkTemplarStaticPush, DarkTemplarStaticStandoff}
import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitClasses.UnitClass
import org.scalatest.funsuite.AnyFunSuite

/**
 * DT static-gun standoff: a Templar raiding past a remembered Cannon must
 * never walk into its reach. The ladder bill is two Templars feeding one
 * guarded Cannon one after another: the detector arm never fires on an
 * unseen eye, the reveal only fires once the tile already burns - inside
 * gun reach, eating the guard volley the whole way out, and the hunt
 * re-grabs the covered prey on the next frame. The standoff owns the
 * approach first: a closing hull stops inside the critical band,
 * everything else inside the close band strides 0.2 tiles straight off
 * the remembered gun, never closer than 7.4 tiles to the Cannon. Speeds,
 * reaches, damage and sights are the real UnitType numbers; both pilots
 * call the shipped kernels, the engine owns the loop only. The engine has
 * no facing, so the pilot passes showingBack=false and the facing gate
 * stays pinned by the base suite.
 */
class DarkTemplarStaticStandoffScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Detector.name) }

  private def sightPx(kind: UnitClass): Double =
    kind.sightPixels.toDouble

  private def away(me: Body, x: Double, y: Double): (Double, Double) = {
    val dx = me.x - x
    val dy = me.y - y
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist > 0) (dx / dist, dy / dist) else (0.0, 1.0)
  }

  private def pixelOf(b: Body): Pixel = new Pixel(b.x.toInt, b.y.toInt)

  /**
   * Production order, stripped to the raid: standoff, latched detector
   * evade, reveal withdraw, hunt. Knowledge is modeled, not omniscient:
   * only foes inside our own sight are known (mirrors per-unit matchups),
   * the remembered gun list stands in for With.units.enemy memory, and
   * the ground-truth reveal comes from the world (mirrors
   * tile.enemyDetected). Approaching reads the closing distance (mirrors
   * isOtherUnitFacingUs); the engine stops instantly, like the game.
   */
  private class RaidPilot(
    id: Int,
    goal: (Double, Double),
    safety: (Double, Double),
    remembered: Seq[Body],
    useStandoff: Boolean,
    usePush: Boolean) extends Pilot {

    var standoffFires: Int = 0
    var stopFires: Int = 0
    var pushMoves: Int = 0
    var pushSwipes: Int = 0
    var minGunDist: Double = Double.PositiveInfinity
    var distAtFirstStandoff: Double = Double.PositiveInfinity
    private var lastDist: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val dtSight = me.kind.sightPixels.toDouble
      val guns = remembered.filter(_.alive)
      // Push, the unseen raid: no burn, no shots landed - walk straight
      // at the remembered gun, swiping the weakest hittable in reach
      // plus the margin, masonry included.
      if (usePush && guns.nonEmpty
        && world.detectorsRevealing(me).isEmpty && ! world.underAttack(me)) {
        val gun = guns.minBy(me.dist)
        val near = world.foes.filter(f =>
          f.alive && me.dist(f) <= dtSight && me.canHit(f)
            && me.dist(f) <= me.rangeAgainst(f) + DarkTemplarStaticPush.attackMarginTiles * 32.0)
        near.sortBy(me.dist).headOption match {
          case Some(prey) =>
            pushSwipes += 1

            return Chase(prey)

          case None =>
            pushMoves += 1

            return MoveTo(gun.x, gun.y)
        }
      }

      if (guns.nonEmpty) {
        val dist = guns.map(me.dist).min
        minGunDist = Math.min(minGunDist, dist)
        val approaching = dist < lastDist
        lastDist = dist
        val gun = guns.minBy(me.dist)
        val danger = DarkTemplarStaticStandoff.dangerTiles(
          gun.kind.pixelRangeMax.toDouble / 32.0,
          sightPx(gun.kind) / 32.0)
        if (useStandoff && DarkTemplarStaticStandoff.standoffLatched(
          id, dist / 32.0, danger, approaching, showingBack = false)) {
          if (standoffFires == 0) distAtFirstStandoff = dist
          standoffFires += 1
          if (DarkTemplarStaticStandoff.shouldStop(dist / 32.0, danger, approaching, showingBack = false)) {
            stopFires += 1
            val open = world.foes.filter(f =>
              f.alive && me.dist(f) <= dtSight && DarkTemplarTargets.isPrey(f.kind) && me.dist(f) <= me.rangeAgainst(f))
            open.sortBy(me.dist).headOption match {
              case Some(prey) =>
                return Chase(prey)

              case None =>
                return Hold

            }
          }

          val (dx, dy) = away(me, gun.x, gun.y)
          val stepX = me.x + dx * DarkTemplarStaticStandoff.stepTiles * 32.0
          val stepY = me.y + dy * DarkTemplarStaticStandoff.stepTiles * 32.0

          return MoveTo(stepX, stepY)

        }
      }

      val knownGuns = world.foes.filter(f => f.alive && me.dist(f) <= dtSight && f.canHit(me))
      val nearGuns = knownGuns.filter(g => DarkTemplarEvade.shouldFleeAttacker(me.dist(g) / 32.0))
      if (nearGuns.nonEmpty) {
        val target = nearGuns.minBy(me.dist)
        val (dx, dy) = away(me, target.x, target.y)

        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }

      val knownEyes = world.foes.filter(f => f.alive && f.kind.isDetector && me.dist(f) <= dtSight)
      val evade = knownEyes.filter(e => DarkTemplarEvade.evadeLatched(id, me.dist(e), e.kind.sightPixels.toDouble))
      if (evade.nonEmpty) {
        val eye = evade.minBy(me.dist)
        val (dx, dy) = away(me, eye.x, eye.y)

        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }

      if (DarkTemplarEvade.shouldWithdrawRevealed(
        world.detectorsRevealing(me).nonEmpty, world.underAttack(me))) {
        knownEyes.sortBy(me.dist).headOption match {
          case Some(eye) =>
            val (dx, dy) = away(me, eye.x, eye.y)

            return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)

          case None =>
            return MoveTo(safety._1, safety._2)

        }
      }

      val open = world.foes.filter(f =>
        f.alive && me.dist(f) <= dtSight && DarkTemplarTargets.isPrey(f.kind)
          && ! knownEyes.exists(e => DarkTemplarEvade.underDetection(
            pixelOf(f), Seq((pixelOf(e), e.kind.sightPixels.toDouble)))))
      open.sortBy(me.dist).headOption match {
        case Some(prey) => Chase(prey)
        case None       => MoveTo(goal._1, goal._2)
      }
    }
  }

  private case class Outcome(
    alive: Boolean,
    hpEnd: Double,
    standoffFires: Int,
    stopFires: Int,
    pushMoves: Int,
    pushSwipes: Int,
    minGunDist: Double,
    distAtFirstStandoff: Double)

  private def runRaid(useStandoff: Boolean, usePush: Boolean, dtStart: (Double, Double), frames: Int): Outcome = {
    val cannon = new Body(Protoss.PhotonCannon, 0.0, 0.0, Protoss.PhotonCannon.maxTotalHealth)
    val guards = Seq(
      new Body(Protoss.Dragoon, -100.0, 120.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 100.0, 120.0, Protoss.Dragoon.maxTotalHealth))
    val dt = new Body(Protoss.DarkTemplar, dtStart._1, dtStart._2, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    val pilot = new RaidPilot(11, (0.0, 600.0), (0.0, -500.0), Seq(cannon), useStandoff, usePush)
    val pilots: Map[Body, Pilot] = Map(dt -> pilot, cannon -> new Pilots.Hold) ++ guards.map(_ -> new Pilots.Hold)
    val world = new World(Seq(dt), cannon +: guards, pilots, Seq.empty)
    var f = 0
    while (f < frames && dt.alive) { world.step(); f += 1 }
    Outcome(dt.alive, dt.hp, pilot.standoffFires, pilot.stopFires, pilot.pushMoves, pilot.pushSwipes, pilot.minGunDist, pilot.distAtFirstStandoff)
  }

  test("a raiding Templar never walks into a remembered Cannon") {
    val cannonRange = Protoss.PhotonCannon.pixelRangeMax.toDouble
    val cannonSight = Protoss.PhotonCannon.sightPixels.toDouble
    println(s"DIAG cannonRange=${cannonRange.toInt}px cannonSight=${cannonSight.toInt}px")

    DarkTemplarEvade.reset()
    DarkTemplarStaticStandoff.reset()
    val fixed = runRaid(useStandoff = true, usePush = true, dtStart = (0.0, -448.0), frames = 1500)
    println(s"DIAG fixed alive=${fixed.alive} hp=${fixed.hpEnd.toInt} push=${fixed.pushMoves}+${fixed.pushSwipes} standoff=${fixed.standoffFires} firstAt=${fixed.distAtFirstStandoff.toInt}px minGun=${fixed.minGunDist.toInt}px")
    assert(fixed.pushMoves > 0, "the push never walked the unseen approach straight at the gun")
    assert(fixed.standoffFires > 0, "the standoff never caught the raid at the burn edge")
    assert(fixed.distAtFirstStandoff <= cannonSight, "the standoff fired past the burn instead of catching it at the edge")
    assert(fixed.minGunDist > DarkTemplarStaticStandoff.neverCloserTiles * 32.0,
      "the Templar closed past the 7.4-tile Cannon floor")
    assert(fixed.minGunDist > cannonRange, "the Templar entered gun reach it must never touch")
    assert(fixed.alive, "the Templar died walking past the gun")
    assert(fixed.hpEnd == Protoss.DarkTemplar.maxTotalHealth, "the Templar took fire it should never have met")

    DarkTemplarEvade.reset()
    DarkTemplarStaticStandoff.reset()
    val old = runRaid(useStandoff = false, usePush = false, dtStart = (0.0, -448.0), frames = 1500)
    println(s"DIAG old alive=${old.alive} hp=${old.hpEnd.toInt} minGun=${old.minGunDist.toInt}px")
    assert(old.minGunDist < cannonSight, "the old brain never even enters the tile-burn - nothing to fix here")
  }

  test("a Templar caught inside the critical band stops, then strides out") {
    val cannonRange = Protoss.PhotonCannon.pixelRangeMax.toDouble
    DarkTemplarEvade.reset()
    DarkTemplarStaticStandoff.reset()
    val fixed = runRaid(useStandoff = true, usePush = true, dtStart = (0.0, -240.0), frames = 800)
    println(s"DIAG inside alive=${fixed.alive} hp=${fixed.hpEnd.toInt} stops=${fixed.stopFires} standoff=${fixed.standoffFires} minGun=${fixed.minGunDist.toInt}px")
    assert(fixed.stopFires > 0, "the stop never owned the closing frame inside the critical band")
    assert(fixed.minGunDist > DarkTemplarStaticStandoff.neverCloserTiles * 32.0,
      "the Templar closed past the 7.4-tile Cannon floor")
    assert(fixed.minGunDist > cannonRange, "the Templar entered gun reach it must never touch")
    assert(fixed.alive, "the Templar died inside the critical band")
    assert(fixed.hpEnd == Protoss.DarkTemplar.maxTotalHealth, "the Templar took fire it should never have met")
  }

  test("an unseen Templar walks straight at the gun and swipes the masonry") {
    val dtSight = Protoss.DarkTemplar.sightPixels.toDouble

    def runPush(usePush: Boolean): (Body, Body, Body, RaidPilot) = {
      val dt = new Body(Protoss.DarkTemplar, 0.0, -448.0, Protoss.DarkTemplar.maxTotalHealth)
      dt.cloaked = true
      val bunker = new Body(Terran.Bunker, 0.0, 0.0, Terran.Bunker.maxTotalHealth)
      val depot = new Body(Terran.SupplyDepot, 0.0, -100.0, Terran.SupplyDepot.maxTotalHealth)
      DarkTemplarEvade.reset()
      DarkTemplarStaticStandoff.reset()
      val pilot = new RaidPilot(13, (0.0, 600.0), (0.0, -900.0), Seq(bunker), useStandoff = true, usePush)
      val pilots: Map[Body, Pilot] = Map(dt -> pilot, bunker -> new Pilots.Hold, depot -> new Pilots.Hold)
      val world = new World(Seq(dt), Seq(bunker, depot), pilots, Seq.empty)
      var f = 0
      while (f < 1500 && dt.alive && (bunker.alive || depot.alive)) { world.step(); f += 1 }
      (dt, bunker, depot, pilot)
    }

    val (dt, bunker, depot, pilot) = runPush(usePush = true)
    println(s"DIAG push alive=${dt.alive} hp=${dt.hp.toInt} moves=${pilot.pushMoves} swipes=${pilot.pushSwipes} bunker=${bunker.hp.toInt} depot=${depot.hp.toInt}")
    assert(pilot.pushSwipes > 0, "the push never swiped on the way in - the widened reach buys nothing")
    assert(depot.hp < Terran.SupplyDepot.maxTotalHealth, "the Templar walked past masonry it was ordered to swipe")
    assert(dt.alive, "the unseen Templar died where nothing could see it")
    assert(dt.hp == Protoss.DarkTemplar.maxTotalHealth, "the unseen Templar took fire it should never have met")

    val (oldDt, oldBunker, oldDepot, oldPilot) = runPush(usePush = false)
    println(s"DIAG nopush alive=${oldDt.alive} bunker=${oldBunker.hp.toInt} depot=${oldDepot.hp.toInt}")
    assert(oldPilot.pushSwipes == 0, "the push swiped with its flag off")
    assert(oldDepot.hp == Terran.SupplyDepot.maxTotalHealth, "the old brain swipes masonry it should walk past - nothing to fix here")
  }
}
