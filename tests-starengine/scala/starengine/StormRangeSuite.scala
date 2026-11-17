package starengine

import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Storm reach ladder: three dragoons plus one full-battery Templar meet
 * six holding Hydralisks with the Templar starting 6, 9 or 15 tiles out.
 * The pilot honors the shipped search cap (`PsionicStorm.stormSearchTiles`,
 * just inside cast range): in reach it storms through the shipped veto
 * kernels, past it it holds instead of walking into Hydralisk range - the
 * live bug was a three-tile leash past cast range, Templars marching out
 * to a pack they could not hit and dying without casting. Hydralisks hold
 * their ground so the starting distance stays the independent variable
 * (zombie pressure is covered by the other storm scenarios).
 */
class StormRangeSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Psionic.name + StarEngineTags.Templar.name) }

  private val searchPx: Double = PsionicStorm.stormSearchTiles * 32.0

  /** Search-capped Templar: storms the densest box in reach the shipped vetoes allow, holds past the cap. */
  private class SearchTemplar(leashPx: Double) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = world.foes.filter(f => f.alive && f.kind.canBeStormed)
      if (me.energy >= World.StormCostEnergy && live.nonEmpty) {
        val inLeash = live.filter(f => me.dist(f) <= leashPx)
        if (inLeash.nonEmpty) {
          var best: Option[Body] = None
          var bestFoes = 0
          inLeash.foreach(center => {
            val foes = live.count(f =>
              Math.abs(f.x - center.x) <= World.StormBoxHalfPx && Math.abs(f.y - center.y) <= World.StormBoxHalfPx)
            if (foes > bestFoes || (foes == bestFoes && best.exists(b => me.dist(center) < me.dist(b)))) {
              best = Some(center)
              bestFoes = foes
            }
          })
          best.foreach(center => {
            val friends = world.ours.count(o =>
              o.alive && Math.abs(o.x - center.x) <= World.StormBoxHalfPx && Math.abs(o.y - center.y) <= World.StormBoxHalfPx)
            if (Math.hypot(me.x - center.x, me.y - center.y) <= World.StormCastRangePx
              && ! PsionicStorm.vetoFriendlyHits(friends)
              && ! PsionicStorm.vetoThinPack(bestFoes, hasCrucialTarget = false, me.energy, enemyIsProtoss = false)
              && ! PsionicStorm.vetoOverlord(center.kind == Zerg.Overlord, me.energy)) {
              return CastStorm(center.x, center.y)
            }

          })
          world.nearestFoeOf(me, inLeash, (_, _) => true) match {
            case Some(foe) => return MoveTo(foe.x, foe.y)

            case None      =>
          }
        }
      }

      Hold
    }
  }

  private case class Outcome(storms: Int, hydrasLeft: Int, oursAlive: Int, htMovedPx: Double, killFrame: Option[Int])

  private def runAt(distPx: Double): Outcome = {
    val hydras = (0 until 6).map(i =>
      new Body(Zerg.Hydralisk, (i % 3) * 40.0 - 40.0, (i / 3) * 40.0, Zerg.Hydralisk.maxTotalHealth))
    val dragoons = Seq(-80.0, 0.0, 80.0).map(x =>
      new Body(Protoss.Dragoon, x, 200.0, Protoss.Dragoon.maxTotalHealth))
    val templar = new Body(Protoss.HighTemplar, 0.0, 40.0 + distPx, Protoss.HighTemplar.maxTotalHealth)
    templar.energy = 200
    val startX = templar.x
    val startY = templar.y
    val ours = dragoons :+ templar
    val pilots: Map[Body, Pilot] =
      dragoons.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => me.canHit(f), (0.0, 200.0))).toMap +
        (templar -> new SearchTemplar(searchPx)) ++
        hydras.map(_ -> new Pilots.Hold).toMap
    val world = new World(ours, hydras, pilots, Seq.empty)

    var killFrame: Option[Int] = None
    while (world.frame < 1500 && hydras.exists(_.alive) && ours.exists(_.alive)) {
      world.step()
      if (killFrame.isEmpty && hydras.forall(! _.alive)) killFrame = Some(world.frame)
    }
    Outcome(
      storms = world.stormsCast.getOrElse(templar, 0),
      hydrasLeft = hydras.count(_.alive),
      oursAlive = ours.count(_.alive),
      htMovedPx = Math.hypot(templar.x - startX, templar.y - startY),
      killFrame = killFrame)
  }

  test("six tiles out the templar storms from the first frame") {
    val o = runAt(6 * 32.0)
    println(s"DIAG 6 tiles: storms=${o.storms} hydrasLeft=${o.hydrasLeft} oursAlive=${o.oursAlive} killFrame=${o.killFrame}")
    assert(o.storms >= 1, "in-range templar never stormed")
    assert(o.hydrasLeft == 0, "hydra pack survived point-blank storms")
    assert(o.oursAlive == 4, "line took losses under instant storms")
  }

  test("eight tiles out the templar storms without closing in") {
    val o = runAt(8 * 32.0)
    println(s"DIAG 8 tiles: storms=${o.storms} hydrasLeft=${o.hydrasLeft} oursAlive=${o.oursAlive} moved=${o.htMovedPx.toInt}px killFrame=${o.killFrame}")
    assert(o.storms >= 1, "in-reach templar never stormed the pack")
    assert(o.hydrasLeft == 0, "hydra pack survived in-reach storms")
  }

  test("ten tiles out the templar holds instead of walking into Hydralisk range") {
    val o = runAt(10 * 32.0)
    println(s"DIAG 10 tiles: storms=${o.storms} hydrasLeft=${o.hydrasLeft} oursAlive=${o.oursAlive} moved=${o.htMovedPx.toInt}px killFrame=${o.killFrame}")
    assert(o.storms == 0, "past-search-cap templar marched out to storm")
    assert(o.htMovedPx <= 64.0, "past-search-cap templar left its post")
    assert(o.hydrasLeft > 0, "dragoons cleared the pack alone - the walk cost is unproven")
  }

  test("fifteen tiles out the templar holds past the search cap") {
    val o = runAt(15 * 32.0)
    println(s"DIAG 15 tiles: storms=${o.storms} hydrasLeft=${o.hydrasLeft} oursAlive=${o.oursAlive} moved=${o.htMovedPx.toInt}px killFrame=${o.killFrame}")
    assert(o.storms == 0, "past-search-cap templar marched out to storm")
    assert(o.htMovedPx <= 64.0, "past-search-cap templar left its post")
    assert(o.hydrasLeft > 0, "dragoons cleared the pack alone - the walk cost is unproven")
  }
}
