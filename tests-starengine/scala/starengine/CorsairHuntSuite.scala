package starengine

import Micro.Actions.Combat.Maneuvering.AirFlee
import Micro.Actions.Combat.Maneuvering.CornerAntiStuckGuard
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Corsairs against Overlords, with and without Hydralisk cover: the
 * brain flees ranged threats on the shipped wound-scaled margin first
 * (the same RangedAvoidance instance the production Corsair holds),
 * then hunts the nearest Overlord. Harmless eyes never trigger the
 * flee - the turn-away bug ends here - while real guns keep the hulls
 * out of reach and the Overlords still die.
 */
class CorsairHuntSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Corsair.name + StarEngineTags.Hunt.name) }

  /** Production order: flee ranged threats first, hunt the nearest Overlord second. */
  private class CorsairHunt extends Pilot {
    var flees = 0

    override def direct(me: Body, world: World): Directive = {
      val ranged = world.foes.view.filter(f => f.alive && f.kind.canAttack(me.kind)).toSeq
      AirFlee.fleePixel(
        Mathematics.Points.Pixel(me.x.toInt, me.y.toInt),
        ranged.map(f => Mathematics.Points.Pixel(f.x.toInt, f.y.toInt)),
        me.lossFraction, 8192, 8192,
        AirFlee.marginLowTiles, AirFlee.marginHighTiles,
        AirFlee.triggerTiles, CornerAntiStuckGuard.corsairTiles).foreach(dest => {
        flees += 1

        return MoveTo(dest.x, dest.y)
      })
      world.nearestFoeOf(me, world.foes, (_, _) => true) match {
        case Some(foe) => Chase(foe)
        case None      => Hold
      }
    }
  }

  test("corsairs close harmless overlords instead of turning away") {
    val corsairs = Seq(
      new Body(Protoss.Corsair, 0.0, 500.0, Protoss.Corsair.maxTotalHealth),
      new Body(Protoss.Corsair, 80.0, 500.0, Protoss.Corsair.maxTotalHealth))
    val overlords = Seq(
      new Body(Zerg.Overlord, 0.0, 100.0, Zerg.Overlord.maxTotalHealth),
      new Body(Zerg.Overlord, 120.0, 80.0, Zerg.Overlord.maxTotalHealth),
      new Body(Zerg.Overlord, -120.0, 80.0, Zerg.Overlord.maxTotalHealth))
    val hunts = corsairs.map(_ -> new CorsairHunt).toMap
    val world = new World(corsairs, overlords,
      hunts ++ overlords.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    println(s"DIAG corsair speed=${corsairs.head.speed} range=${corsairs.head.rangeAgainst(overlords.head)} " +
      s"dmg=${corsairs.head.damageAgainst(overlords.head)} overlord hp=${Zerg.Overlord.maxTotalHealth}")
    while (world.frame < 4000 && overlords.exists(_.alive) && corsairs.exists(_.alive)) {
      world.step()
    }

    val fled = hunts.values.map(_.flees).sum
    println(s"DIAG overlordsLeft=${overlords.count(_.alive)} fled=$fled")
    assert(overlords.forall(! _.alive), "corsairs never finished the overlords")
    assert(corsairs.forall(_.alive), "corsairs died to harmless eyes")
    assert(fled == 0, "flee fired with nothing that can hit back")
  }

  test("corsairs keep out of hydra reach while the overlords die") {
    val corsairs = Seq(
      new Body(Protoss.Corsair, 0.0, 500.0, Protoss.Corsair.maxTotalHealth),
      new Body(Protoss.Corsair, 80.0, 500.0, Protoss.Corsair.maxTotalHealth))
    val overlords = Seq(
      new Body(Zerg.Overlord, 0.0, 100.0, Zerg.Overlord.maxTotalHealth),
      new Body(Zerg.Overlord, 120.0, 80.0, Zerg.Overlord.maxTotalHealth))
    val hydras = Seq(
      new Body(Zerg.Hydralisk, 400.0, 100.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 460.0, 100.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 520.0, 100.0, Zerg.Hydralisk.maxTotalHealth))
    val hunts = corsairs.map(_ -> new CorsairHunt).toMap
    val foes = overlords ++ hydras
    val world = new World(corsairs, foes,
      hunts ++ foes.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    val hydraRange = hydras.head.rangeAgainst(corsairs.head)
    var minDist = Double.PositiveInfinity
    while (world.frame < 4000 && overlords.exists(_.alive) && corsairs.exists(_.alive)) {
      world.step()
      corsairs.foreach(c => hydras.foreach(h =>
        if (h.alive) minDist = Math.min(minDist, c.dist(h))))
    }

    val fled = hunts.values.map(_.flees).sum
    println(s"DIAG hydra range=$hydraRange minDist=${minDist.toInt}px overlordsLeft=${overlords.count(_.alive)} fled=$fled")
    assert(overlords.forall(! _.alive), "covered overlords survived")
    assert(corsairs.forall(_.alive), "hydras caught a corsair")
    assert(minDist > hydraRange, "corsairs flew inside hydra reach")
    assert(fled > 0, "flee never fired with real guns around")
  }
}
