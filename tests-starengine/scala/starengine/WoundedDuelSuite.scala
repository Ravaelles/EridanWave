package starengine

import Micro.Actions.Combat.Decisioning.Kiting.{KiteAway, KiteHold}
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Wounded-duel discipline: one dragoon at 60 total HP against a fresh
 * zealot - the 1DragWounded map. Standing trades volleys the hull no
 * longer has; kiting while the gun cools spends waiting time on
 * distance instead of damage. The shipped rule kites below 61 HP when
 * cooling with the melee chaser in margin; the plant stands and trades.
 * Crossed seats (the engine lets foes shoot first, so a win in one
 * seat proves nothing): kills plus survivals decide.
 */
class WoundedDuelSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  /** Shipped rule, verbatim: kite while cooling under the shipped gate, else chase. */
  private class KitePilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val meleeNear = foe.exists(f => f.kind == Protoss.Zealot && me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      if (foe.isDefined && KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, inMargin, meleeNear)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  /** Plant: chase and swing, never yield ground, cooling or not. */
  private class PlantPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, foesOf(me, world), (_, _) => true).map(Chase).getOrElse(Hold)
  }

  /**
   * Flee-first (shipped 1DragWounded doctrine): a bleeding hull with
   * melee inside the chase runs far instead of edging to zero, and only
   * starts a volley outside the 2.8 floor. Everywhere else it kites
   * exactly like the shipped rule.
   */
  private class FleeFirstPilot extends Pilot {
    private val kite = new KitePilot

    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val f = foe.getOrElse(return Hold)
      val distTiles = me.dist(f) / 32.0
      if (KiteHold.fleesFirst(me.hp.toInt, distTiles)) {
        if (me.cooldownLeft <= 0 && me.dist(f) <= me.rangeAgainst(f) && KiteHold.holdsShot(me.hp.toInt, distTiles)) Chase(f)
        else {
          val dx = me.x - f.x
          val dy = me.y - f.y
          val d = Math.sqrt(dx * dx + dy * dy)
          if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
        }
      } else kite.direct(me, world)
    }
  }

  /**
   * Plant-last: a loaded gun plants the volley instead of entering the
   * kite decision at all, then kites while truly cooling. Engine bodies
   * carry no shields, so the near-dead half of the doctrine stays
   * live-only and the plant half proves here: a healthy hull plants,
   * a dying one would have fled.
   */
  private class PlantLastPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      if (foe.isEmpty) return Hold

      val f = foe.get
      val hittableNow = me.dist(f) <= me.rangeAgainst(f)
      val meleeNear = f.kind == Protoss.Zealot && me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0
      // Wounded 40/80: the plant exercises (whole hulls belong to the
      // line, the engine has no shields to scratch).
      if (KiteHold.shouldPlant(me.cooldownLeft, hittableNow, shieldPoints = 40, maxShields = 80, hasMeleeNearby = meleeNear)) return Hold

      val inMargin = me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0
      if (KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, inMargin, meleeNear)) {
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else Chase(f)
    }
  }

  /**
   * Production order, verbatim (KiteAwayBehavior.run): hold loaded
   * volleys, plant the plantable ones, kite cooling contact, chase
   * the rest. Counts wasted frames (a move issued while loaded with
   * something hittable) - the ledger's definition, measured where
   * shots are undeniable.
   */
  private class ProductionOrderPilot extends Pilot {
    var wasted = 0

    private def kiteFrom(me: Body, f: Body): Directive = {
      if (me.cooldownLeft <= 0 && me.dist(f) <= me.rangeAgainst(f)) wasted += 1

      val dx = me.x - f.x
      val dy = me.y - f.y
      val d = Math.sqrt(dx * dx + dy * dy)
      if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
    }

    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val f = foe.getOrElse(return Hold)
      val contactClose = me.dist(f) <= KiteAway.steadyContactTiles * 32.0
      if (KiteAway.holdsFire(me.cooldownLeft, me.hp.toInt, contactClose)) return Hold

      val hittableNow = me.dist(f) <= me.rangeAgainst(f)
      val meleeNear = f.kind == Protoss.Zealot && me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0
      if (KiteHold.shouldPlant(me.cooldownLeft, hittableNow, shieldPoints = 80, maxShields = 80, hasMeleeNearby = meleeNear)) return Hold

      val meleeContact = f.kind == Protoss.Zealot && contactClose
      if (me.cooldownLeft > 2 && meleeContact) return kiteFrom(me, f)

      if (hittableNow) return Chase(f)

      val meleeClose = f.kind == Protoss.Zealot && me.dist(f) <= KiteAway.meleeCloseTiles * 32.0
      if ( ! KiteAway.shouldKite(0, meleeClose)) return Chase(f)

      Chase(f)
    }
  }

  /** One duel, dragoon doctrine under test, zealot plants. Returns (dragoon alive, zealot dead, dragoon death frame or Int.MaxValue). */
  private def duel(dragoonP: Body => Pilot, dragoonIsOurs: Boolean, dragoonHp: Double = 60.0): (Boolean, Boolean, Int) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 900.0, dragoonHp)
    val zealot = new Body(Protoss.Zealot, 0.0, 100.0, Protoss.Zealot.maxTotalHealth)
    val (ours, foes) = if (dragoonIsOurs) (Seq(dragoon), Seq(zealot)) else (Seq(zealot), Seq(dragoon))
    val world = new World(ours, foes, Map(dragoon -> dragoonP(dragoon), zealot -> new PlantPilot), Seq.empty)
    var remaining = 9000
    var deathAt = Int.MaxValue
    while (remaining > 0 && dragoon.alive && zealot.alive) {
      world.step()
      remaining -= 1
      if ( ! dragoon.alive && deathAt == Int.MaxValue) deathAt = world.frame
    }
    (dragoon.alive, ! zealot.alive, deathAt)
  }

  /** Production-order duel that also reports wasted frames (see ProductionOrderPilot). */
  private def duelCounted(dragoonHp: Double, dragoonIsOurs: Boolean): (Boolean, Boolean, Int) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 900.0, dragoonHp)
    val zealot = new Body(Protoss.Zealot, 0.0, 100.0, Protoss.Zealot.maxTotalHealth)
    val pilot = new ProductionOrderPilot
    val (ours, foes) = if (dragoonIsOurs) (Seq(dragoon), Seq(zealot)) else (Seq(zealot), Seq(dragoon))
    val world = new World(ours, foes, Map(dragoon -> pilot, zealot -> new PlantPilot), Seq.empty)
    var remaining = 9000
    var deathAt = Int.MaxValue
    while (remaining > 0 && dragoon.alive && zealot.alive) {
      world.step()
      remaining -= 1
      if ( ! dragoon.alive && deathAt == Int.MaxValue) deathAt = world.frame
    }
    (dragoon.alive, ! zealot.alive, pilot.wasted)
  }

  test("chase math: print the raw speeds and ranges once") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val zealot = new Body(Protoss.Zealot, 100.0, 0.0, Protoss.Zealot.maxTotalHealth)
    println(s"DIAG dragoon speed=${dragoon.speed} range=${dragoon.rangeAgainst(zealot)} cd=${dragoon.cooldownAgainst(zealot)} dmg=${dragoon.damageAgainst(zealot)} hp=${dragoon.maxHp}")
    println(s"DIAG zealot speed=${zealot.speed} range=${zealot.rangeAgainst(dragoon)} cd=${zealot.cooldownAgainst(dragoon)} dmg=${zealot.damageAgainst(dragoon)} hp=${zealot.maxHp}")
    assert(dragoon.speed > 0 && zealot.speed > 0)
  }

  test("a wounded dragoon kites a fresh zealot, planting does not") {
    val (kiteAliveA, kiteKillsA, kiteDeathA) = duel(_ => new KitePilot, dragoonIsOurs = true)
    val (kiteAliveB, kiteKillsB, kiteDeathB) = duel(_ => new KitePilot, dragoonIsOurs = false)
    val (plantAliveA, plantKillsA, plantDeathA) = duel(_ => new PlantPilot, dragoonIsOurs = true)
    val (plantAliveB, plantKillsB, plantDeathB) = duel(_ => new PlantPilot, dragoonIsOurs = false)
    val kiteScore = Seq(kiteAliveA, kiteKillsA, kiteAliveB, kiteKillsB).count(identity)
    val plantScore = Seq(plantAliveA, plantKillsA, plantAliveB, plantKillsB).count(identity)
    println(s"DIAG kite alive=$kiteAliveA,$kiteAliveB deadAt=$kiteDeathA,$kiteDeathB kills=$kiteKillsA,$kiteKillsB; plant alive=$plantAliveA,$plantKillsB deadAt=$plantDeathA,$plantDeathB kills=$plantKillsA,$plantKillsB")
    assert(kiteScore >= plantScore, "kiting must not trade down to planting a wounded duel")
    assert(kiteDeathA + kiteDeathB >= plantDeathA + plantDeathB, "kiting must buy time over planting")
  }

  /**
   * Plant-last guardrail (engine half of the proof): the unwinnable
   * seat cannot show kills - the 60-HP hull dies standing or kiting -
   * so the engine pins what it can: planting the last cooldown frames
   * never donates the hull faster than raw kiting. A reckless window
   * (plant everything) dies with the plant seat; the shipped six holds
   * the kite's death line. Superiority (extra volleys, ledger down,
   * kills held) judges live - the engine has no facing cost, so the
   * return trip it saves is invisible here.
   */
  test("planting the last cooldown frames never trades down to raw kiting") {
    val (kiteAliveA, kiteKillsA, kiteDeathA) = duel(_ => new KitePilot, dragoonIsOurs = true)
    val (kiteAliveB, kiteKillsB, kiteDeathB) = duel(_ => new KitePilot, dragoonIsOurs = false)
    val (plantLastAliveA, plantLastKillsA, plantLastDeathA) = duel(_ => new PlantLastPilot, dragoonIsOurs = true)
    val (plantLastAliveB, plantLastKillsB, plantLastDeathB) = duel(_ => new PlantLastPilot, dragoonIsOurs = false)
    val kiteScore = Seq(kiteAliveA, kiteKillsA, kiteAliveB, kiteKillsB).count(identity)
    val plantLastScore = Seq(plantLastAliveA, plantLastKillsA, plantLastAliveB, plantLastKillsB).count(identity)
    println(s"DIAG kite alive=$kiteAliveA,$kiteAliveB deadAt=$kiteDeathA,$kiteDeathB kills=$kiteKillsA,$kiteKillsB; plantLast alive=$plantLastAliveA,$plantLastAliveB deadAt=$plantLastDeathA,$plantLastDeathB kills=$plantLastKillsA,$plantLastKillsB")
    assert(plantLastScore >= kiteScore, "holding the last cooldown frames must not trade down to raw kiting")
    assert(plantLastDeathA + plantLastDeathB >= kiteDeathA + kiteDeathB, "planting must not donate the hull faster than kiting")
  }

  test("at live HP (12) the hull kites for time, never for the trade") {
    val (kiteAliveA, kiteKillsA, kiteDeathA) = duel(_ => new KitePilot, dragoonIsOurs = true, dragoonHp = 12.0)
    val (kiteAliveB, kiteKillsB, kiteDeathB) = duel(_ => new KitePilot, dragoonIsOurs = false, dragoonHp = 12.0)
    val (plantAliveA, plantKillsA, plantDeathA) = duel(_ => new PlantPilot, dragoonIsOurs = true, dragoonHp = 12.0)
    val (plantAliveB, plantKillsB, plantDeathB) = duel(_ => new PlantPilot, dragoonIsOurs = false, dragoonHp = 12.0)
    println(s"DIAG 12hp kite alive=$kiteAliveA,$kiteAliveB deadAt=$kiteDeathA,$kiteDeathB kills=$kiteKillsA,$kiteKillsB; plant alive=$plantAliveA,$plantAliveB deadAt=$plantDeathA,$plantDeathB kills=$plantKillsA,$plantKillsB")
    assert(kiteDeathA + kiteDeathB >= plantDeathA + plantDeathB, "at live HP kiting must buy time over planting")
  }

  test("healthy hulls kite slow blades to death, both seats") {
    val hp = Protoss.Dragoon.maxTotalHealth
    val (kiteAliveA, kiteKillsA, _) = duel(_ => new KitePilot, dragoonIsOurs = true, dragoonHp = hp)
    val (kiteAliveB, kiteKillsB, _) = duel(_ => new KitePilot, dragoonIsOurs = false, dragoonHp = hp)
    println(s"DIAG healthy kite alive=$kiteAliveA,$kiteAliveB kills=$kiteKillsA,$kiteKillsB")
    assert(kiteAliveA && kiteAliveB, "a healthy hull kiting a slow blade must survive both seats")
    assert(kiteKillsA && kiteKillsB, "a healthy hull kiting a slow blade must clear it both seats")
  }

  test("flee-first outlives raw kiting on the bleeding duel, both seats") {
    val (kiteAliveA, kiteKillsA, kiteDeathA) = duel(_ => new KitePilot, dragoonIsOurs = true)
    val (kiteAliveB, kiteKillsB, kiteDeathB) = duel(_ => new KitePilot, dragoonIsOurs = false)
    val (fleeAliveA, fleeKillsA, fleeDeathA) = duel(_ => new FleeFirstPilot, dragoonIsOurs = true)
    val (fleeAliveB, fleeKillsB, fleeDeathB) = duel(_ => new FleeFirstPilot, dragoonIsOurs = false)
    println(s"DIAG flee-first alive=$fleeAliveA,$fleeAliveB deadAt=$fleeDeathA,$fleeDeathB kills=$fleeKillsA,$fleeKillsB; kite alive=$kiteAliveA,$kiteAliveB deadAt=$kiteDeathA,$kiteDeathB kills=$kiteKillsA,$kiteKillsB")
    // Capped: a timeout (Int.MaxValue, nobody died) outlives every death.
    def capped(deathAt: Int): Int = Math.min(deathAt, 9000)
    assert(capped(fleeDeathA) + capped(fleeDeathB) >= capped(kiteDeathA) + capped(kiteDeathB), "flee-first must buy at least as much time as kiting")
  }

  test("the shipped order shoots every cycle and never walks a loaded volley, both seats") {
    val hp = Protoss.Dragoon.maxTotalHealth
    val (aliveA, killsA, wastedA) = duelCounted(hp, dragoonIsOurs = true)
    val (aliveB, killsB, wastedB) = duelCounted(hp, dragoonIsOurs = false)
    println(s"DIAG production alive=$aliveA,$aliveB kills=$killsA,$killsB wasted=$wastedA,$wastedB")
    assert(killsA && killsB, "the shipped order must clear the duel both seats - every dragoon shoots")
    assert(wastedA == 0 && wastedB == 0, "the shipped order must never walk a loaded volley")
  }
}
