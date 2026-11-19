package starengine

import Micro.Actions.Protoss.ReaverShuttle.{ReaverDropSpot, ReaverStaticHold, ReaverStaticStand}
import Micro.Actions.Transportation.DropSafety
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Sunkens2 pair loop, mechanism by mechanism: one Reaver plus its Shuttle
 * against Sunken Colonies (two Zerglings screening in the full loop).
 * The live map never wins: the hull walks at the colony, stands at
 * engine range without ever launching, and dies - the game only
 * launches scarabs inside 7.25 tiles from a hold.
 *
 * The engine models exactly that (World.buggedStaticLaunch): reaver
 * shots vs defensive statics resolve only inside the shipped launch
 * band and only from a Hold order. Every pilot below calls the shipped
 * kernels, so the rows prove the shipped numbers win end to end:
 * the wall without them, the kill with them. Ammo is capped at the
 * live five where economy matters; uncapped rows carry the count.
 */
class SunkensPairScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Reaver.name + StarEngineTags.Shuttle.name + StarEngineTags.Static.name) }

  /** Launch band, one home with the world: the suite pins the model to the shipped kernel. */
  test("the bug model tracks the shipped launch band") {
    assert(World.ReaverStaticLaunchBandPx == ReaverStaticHold.holdTiles * 32.0)
  }

  /** Scarab scarcity probe, same shape as the scale suite: the live hull carries five. */
  private class Ammo(cap: Int) {
    var spent: Int = 0

    def empty: Boolean = spent >= cap
  }

  /**
   * Hold cadence straight from the shipped kernel: walk at the static
   * past engine range (Chase parks at 256, outside the 232 band - the
   * shipped close walks at the pixel, not at reach), Hold one frame in
   * eight inside the band, re-attack between holds.
   */
  private class HoldCadencePilot(target: Body, uid: Int, ammo: Option[Ammo] = None) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      if (ammo.exists(_.empty)) return Hold

      val distTiles = me.dist(target) / 32.0
      if (ReaverStaticHold.holdLatched(uid, distTiles)) {
        if (ReaverStaticHold.holdFrame(world.frame, uid)) Hold else Chase(target)
      } else MoveTo(target.x, target.y)
    }
  }

  /** Stand permission straight from the shipped kernel: fresh hulls hold the line, holed hulls leave it. */
  private def standWoundHp(me: Body): Int = (me.maxHp - me.hp).toInt

  private class StandPermissionPilot(target: Body, uid: Int, ammo: Ammo, lastShot: () => Int) extends Pilot {
    private val cadence = new HoldCadencePilot(target, uid, Some(ammo))

    override def direct(me: Body, world: World): Directive = {
      val framesSinceShot = world.frame - lastShot()
      if (ReaverStaticStand.mayStand(standWoundHp(me), ammoLeft(me, ammo), framesSinceShot)) cadence.direct(me, world)
      else new Pilots.Flee(_ => world.foes).direct(me, world)
    }

    private def ammoLeft(me: Body, ammo: Ammo): Int =
      if (ammo.empty) 0 else 1
  }

  /** Ferry pilot: rejoin the mate on foot, then park on the designated ring spot and hold for the unload. */
  private class FerryPilot(reaver: Body, spot: (Double, Double)) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      if (reaver.transport.isEmpty) MoveTo(reaver.x, reaver.y)
      else {
        val dx = me.x - spot._1
        val dy = me.y - spot._2
        if (dx * dx + dy * dy > 32.0 * 32.0) MoveTo(spot._1, spot._2)
        else Hold
      }
    }
  }

  /** Designated ring spot through the shipped kernels: ring geometry plus berth legality, nearest our side. */
  private def ringSpot(staticPx: (Double, Double), homePx: (Double, Double)): (Double, Double) = {
    val candidates = ReaverDropSpot.ring((staticPx._1 / 32.0, staticPx._2 / 32.0)).map { case (x, y) =>
      val px = (x * 32.0, y * 32.0)
      val distStatic = Math.hypot(px._1 - staticPx._1, px._2 - staticPx._2) / 32.0
      val distHome = Math.hypot(px._1 - homePx._1, px._2 - homePx._2) / 32.0
      ReaverDropSpot.Candidate(x, y, distStatic, Double.PositiveInfinity, distHome)
    }
    val picked = ReaverDropSpot.pick(candidates)
    assert(picked.isDefined, "ring has no legal spot on an open plane")
    (picked.get.xTiles * 32.0, picked.get.yTiles * 32.0)
  }

  private def sunkenAt(x: Double, y: Double): Body =
    new Body(Zerg.SunkenColony, x, y, Zerg.SunkenColony.maxTotalHealth)

  private def trackShots(reaver: Body, ammo: Option[Ammo], lastShot: Array[Int], count: Array[Int], world: World): Unit = {
    val loaded = reaver.cooldownLeft == 0 && ammo.forall(! _.empty)
    world.step()
    if (loaded && reaver.cooldownLeft > 0) {
      count(0) += 1
      lastShot(0) = world.frame
      ammo.foreach(_.spent += 1)
    }
  }

  test("attack-move dies without firing: the map wall, reproduced", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    // No hold discipline: walk at the pixel the way the shipped close
    // does, stand wherever the engine claims range, never launch.
    val walkIn = new Pilot {
      override def direct(me: Body, world: World): Directive = MoveTo(sunken.x, sunken.y)
    }
    val pilots: Map[Body, Pilot] = Map(
      reaver -> walkIn,
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true

    var shots = 0
    while (world.frame < 12000 && sunken.alive && reaver.alive) {
      val loaded = reaver.cooldownLeft == 0
      world.step()
      if (loaded && reaver.cooldownLeft > 0) shots += 1
    }
    println(s"DIAG wall reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} shots=$shots frames=${world.frame}")

    assert(shots == 0, "attack-move must never launch outside the band")
    assert(sunken.alive, "the colony stands")
    assert( ! reaver.alive, "the hull dies walking at guns that never answer")
  }

  test("hold cadence kills the sunken: the user mechanism, proven", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new HoldCadencePilot(sunken, uid = 7101),
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true

    val count = Array(0)
    val lastShot = Array(-10000)
    while (world.frame < 12000 && sunken.alive && reaver.alive) {
      trackShots(reaver, None, lastShot, count, world)
    }
    println(s"DIAG cadence reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} shots=${count(0)} frames=${world.frame}")

    assert( ! sunken.alive, "the cadence kills the colony")
    assert(reaver.alive, "the hull lives")
    assert(count(0) > 0, "scarabs launched")
  }

  test("hold cadence kills the cannon too: both scoped statics", StarEngineTags.PvP) {
    val cannon = new Body(Protoss.PhotonCannon, 0.0, 0.0, Protoss.PhotonCannon.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new HoldCadencePilot(cannon, uid = 7102),
      cannon -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(cannon), pilots, Seq.empty)
    world.buggedStaticLaunch = true

    val count = Array(0)
    val lastShot = Array(-10000)
    while (world.frame < 12000 && cannon.alive && reaver.alive) {
      trackShots(reaver, None, lastShot, count, world)
    }
    println(s"DIAG cannon reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} cannon=${cannon.hp.toInt}/${cannon.maxHp.toInt} shots=${count(0)} frames=${world.frame}")

    assert( ! cannon.alive, "the cadence kills the cannon")
    assert(reaver.alive, "the hull lives")
  }

  test("a fresh hull stands through the permission and kills", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    val ammo = new Ammo(20)
    val lastShot = Array(-10000)
    val count = Array(0)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new StandPermissionPilot(sunken, uid = 7103, ammo, () => lastShot(0)),
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true

    while (world.frame < 12000 && sunken.alive && reaver.alive) {
      trackShots(reaver, Some(ammo), lastShot, count, world)
    }
    println(s"DIAG stand reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} shots=${count(0)} frames=${world.frame}")

    assert( ! sunken.alive, "the fresh hull kills the colony")
    assert(reaver.alive, "the hull lives")
  }

  test("a holed hull yields instead of standing: the withdraw half", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 700.0, Protoss.Reaver.maxTotalHealth)
    reaver.hp = reaver.maxHp - 41
    val ammo = new Ammo(20)
    val lastShot = Array(-10000)
    val count = Array(0)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new StandPermissionPilot(sunken, uid = 7104, ammo, () => lastShot(0)),
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true
    while (world.frame < 3000 && reaver.alive) {
      trackShots(reaver, Some(ammo), lastShot, count, world)
    }
    println(s"DIAG yield reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} shots=${count(0)} frames=${world.frame}")

    assert(reaver.alive, "the holed hull withdraws and lives")
    assert(sunken.alive, "yielding fires nothing")
    assert(count(0) == 0, "no standing, no shots")
  }

  test("the ring delivers the unload: designated spot, berth legal", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val home = (0.0, 2000.0)
    val spot = ringSpot((0.0, 0.0), home)
    val distStaticTiles = Math.hypot(spot._1, spot._2) / 32.0
    assert( ! DropSafety.vetoLanding(Some(distStaticTiles)), "the ring parks past the berth")

    val reaver = new Body(Protoss.Reaver, 0.0, 2000.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 2000.0, Protoss.Shuttle.maxTotalHealth)
    reaver.transport = Some(shuttle)
    val ammo = new Ammo(20)
    val lastShot = Array(-10000)
    val count = Array(0)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> new FerryPilot(reaver, spot),
      reaver -> new Pilot {
        override def direct(me: Body, world: World): Directive = {
          if (me.transport.isDefined) {
            if (Math.hypot(shuttle.x - spot._1, shuttle.y - spot._2) <= 32.0) Unload else Hold
          } else new HoldCadencePilot(sunken, uid = 7105, Some(ammo)).direct(me, world)
        }
      },
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver, shuttle), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true

    var unloaded = false
    while (world.frame < 12000 && sunken.alive && reaver.alive) {
      trackShots(reaver, Some(ammo), lastShot, count, world)
      if (reaver.transport.isEmpty) unloaded = true
    }
    println(s"DIAG ring reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} shots=${count(0)} unloaded=$unloaded frames=${world.frame}")

    assert(unloaded, "the ride unloads on the ring")
    assert( ! sunken.alive, "the unloaded hull kills the colony")
    assert(reaver.alive, "the hull lives")
  }

  test("the full loop wins: board, ferry, ring, unload, kill both", StarEngineTags.PvZ, StarEngineTags.Ling) {
    val sunkenA = sunkenAt(-160.0, 0.0)
    val sunkenB = sunkenAt(160.0, 0.0)
    val ling1 = new Body(Zerg.Zergling, -160.0, 480.0, Zerg.Zergling.maxTotalHealth)
    val ling2 = new Body(Zerg.Zergling, 160.0, 576.0, Zerg.Zergling.maxTotalHealth)
    val home = (0.0, 2000.0)
    val spot = ringSpot((-160.0, 0.0), home)
    val reaver = new Body(Protoss.Reaver, -320.0, 1600.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 320.0, 1700.0, Protoss.Shuttle.maxTotalHealth)
    val ammo = new Ammo(20)
    val lastShot = Array(-10000)
    val count = Array(0)
    val sunkens = Seq(sunkenA, sunkenB)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> new FerryPilot(reaver, spot),
      reaver -> new Pilot {
        private var boarded = false

        override def direct(me: Body, world: World): Directive = {
          if (me.transport.isDefined) {
            boarded = true
            if (Math.hypot(shuttle.x - spot._1, shuttle.y - spot._2) <= 32.0) Unload else Hold
          } else if ( ! boarded && me.dist(shuttle) <= 32.0) Board(shuttle)
          else {
            val foe = world.nearestFoeOf(me, world.foes, (a, b) => true).getOrElse(sunkenA)
            new HoldCadencePilot(foe, uid = 7106, Some(ammo)).direct(me, world)
          }
        }
      },
      ling1 -> new Pilots.ZombieAttack(_.ours, (0.0, 0.0)),
      ling2 -> new Pilots.ZombieAttack(_.ours, (0.0, 0.0)),
      sunkenA -> new Pilots.Hold,
      sunkenB -> new Pilots.Hold)
    val leash = new Constraints.StaticLeash(
      maxTiles = 15.0,
      nearTiles = 2.0,
      isBound = _.kind == Zerg.Zergling,
      isAnchor = _.kind.isStaticDefense)
    val world = new World(Seq(reaver, shuttle), Seq(ling1, ling2) ++ sunkens, pilots, Seq(leash))
    world.buggedStaticLaunch = true

    var sawLoaded = false
    var unloadedAfterLoad = false
    while (world.frame < 15000 && (sunkenA.alive || sunkenB.alive) && reaver.alive) {
      trackShots(reaver, Some(ammo), lastShot, count, world)
      if (reaver.transport.isDefined) sawLoaded = true
      else if (sawLoaded) unloadedAfterLoad = true
    }
    println(s"DIAG loop reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunkens=${sunkens.count(_.alive)}/2 lings=${Seq(ling1, ling2).count(_.alive)}/2 shots=${count(0)} frames=${world.frame}")

    assert(sawLoaded, "the hull boarded")
    assert(unloadedAfterLoad, "the ride unloaded on the ring")
    assert( ! sunkenA.alive && ! sunkenB.alive, "the loop clears both colonies")
    assert(reaver.alive, "the hull lives")
  }

  test("a stalled duel boards the waiting ride: the f290 moment", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    // f290 geometry: inside engine gun (256) but outside the launch
    // band (232) - the hull holds, the game never launches.
    val reaver = new Body(Protoss.Reaver, 0.0, 240.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 340.0, Protoss.Shuttle.maxTotalHealth)
    val lastShot = Array(0)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> new Pilots.Hold,
      reaver -> new Pilot {
        var closing = false

        override def direct(me: Body, world: World): Directive = {
          // The f290 moment, production order: a stalled duel boards the
          // waiting ride; once walking out of the gun the WalkToRide leg
          // finishes the converge instead of re-holding at the edge.
          val inGun = me.dist(sunken) <= me.rangeAgainst(sunken)
          if (inGun && world.frame - lastShot(0) >= 144 && me.dist(shuttle) <= 160.0) closing = true
          if (closing && me.transport.isEmpty) {
            if (me.dist(shuttle) <= 32.0) Board(shuttle)
            else MoveTo(shuttle.x, shuttle.y)
          }
          else Hold
        }
      },
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver, shuttle), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true
    while (world.frame < 2000 && reaver.transport.isEmpty && reaver.alive) {
      world.step()
    }
    println(s"DIAG stalled loaded=${reaver.transport.isDefined} frames=${world.frame} reaver=${reaver.hp.toInt}")

    assert(reaver.transport.contains(shuttle), "the stalled hull boards the waiting ride")
  }

  test("a progressing duel keeps fighting: the carve-out holds", StarEngineTags.PvZ) {
    val ling = new Body(Zerg.Zergling, 0.0, 200.0, Zerg.Zergling.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 0.0, 400.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 450.0, Protoss.Shuttle.maxTotalHealth)
    val klastShot = Array(0)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> new Pilots.Hold,
      reaver -> new Pilot {
        override def direct(me: Body, world: World): Directive = {
          val stalled = world.frame - klastShot(0) >= 144
          if (stalled && me.dist(shuttle) <= 160.0) Board(shuttle)
          else Chase(ling)
        }
      },
      ling -> new Pilots.ZombieAttack(_.ours, (0.0, 0.0)))
    val world = new World(Seq(reaver, shuttle), Seq(ling), pilots, Seq.empty)
    world.buggedStaticLaunch = true
    val kcount = Array(0)
    while (world.frame < 3000 && ling.alive) {
      val loaded = reaver.cooldownLeft == 0
      world.step()
      if (loaded && reaver.cooldownLeft > 0) klastShot(0) = world.frame
    }
    println(s"DIAG progressing loaded=${reaver.transport.isDefined} ling=${ling.hp.toInt}/${ling.maxHp.toInt} frames=${world.frame}")

    assert(reaver.transport.isEmpty, "the progressing hull keeps its duel")
    assert( ! ling.alive, "the duel progresses")
  }

  test("no ferry, no win: the control that makes the loop load-bearing", StarEngineTags.PvZ) {
    val sunken = sunkenAt(0.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 2000.0, Protoss.Shuttle.maxTotalHealth)
    val walkIn = new Pilot {
      override def direct(me: Body, world: World): Directive = MoveTo(sunken.x, sunken.y)
    }
    val pilots: Map[Body, Pilot] = Map(
      reaver -> walkIn,
      shuttle -> new Pilots.Hold,
      sunken -> new Pilots.Hold)
    val world = new World(Seq(reaver, shuttle), Seq(sunken), pilots, Seq.empty)
    world.buggedStaticLaunch = true
    world.run(12000)
    println(s"DIAG control reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt} sunken=${sunken.hp.toInt}/${sunken.maxHp.toInt} frames=${world.frame}")

    assert(sunken.alive, "without the loop the colony stands")
  }
}
