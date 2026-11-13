package Micro.Actions.Protoss.ReaverShuttle

import Micro.Actions.Protoss.ReaverShuttle.StarTest._
import org.scalatest.funsuite.AnyFunSuite

class StarTestSuite extends AnyFunSuite {

  private def dropRun(foeAt: Option[(Double, Double)] = None): World = {
    val shuttle = Mover(x = 0.0, y = 30.0, speedTiles = 2.0, hp = 200.0, maxHp = 200.0, rangeTiles = 0.0, dmgPerShot = 0.0, cooldownFrames = 0)
    val reaver = Mover(x = 0.0, y = 30.0, speedTiles = 2.0, hp = 180.0, maxHp = 180.0, rangeTiles = 8.0, dmgPerShot = 100.0, cooldownFrames = 60)
    shuttle.goal = Some((0.0, 0.0))
    reaver.goal = Some((0.0, 0.0))
    val foes = foeAt.toSeq.map(p => Mover(x = p._1, y = p._2, speedTiles = 0.5, hp = 40.0, maxHp = 40.0, rangeTiles = 4.0, dmgPerShot = 10.0, cooldownFrames = 15))
    new World(Seq(shuttle, reaver), foes)
  }

  test("an unattacked drop run takes no hits") {
    val world = dropRun()
    world.run(100)
    assert(world.hits.isEmpty)
    assert(world.ours.forall(_.hpLossFraction == 0.0))
  }

  test("contact deals damage instantly on the touching frame") {
    val world = dropRun(foeAt = Some((0.0, 24.0)))
    world.run(100)
    assert(world.hits.nonEmpty)
    assert(world.hits.head._1 < 20)
  }

  test("a longer gun kills the approach before taking a hit") {
    val reaver = Mover(x = 0.0, y = 30.0, speedTiles = 2.0, hp = 180.0, maxHp = 180.0, rangeTiles = 8.0, dmgPerShot = 100.0, cooldownFrames = 60)
    val foe = Mover(x = 0.0, y = 20.0, speedTiles = 0.5, hp = 40.0, maxHp = 40.0, rangeTiles = 4.0, dmgPerShot = 10.0, cooldownFrames = 15)
    val world = new World(Seq(reaver), Seq(foe))
    world.run(100)
    assert( ! foe.alive, "reaver never killed the approach")
    assert(reaver.hp == reaver.maxHp, "reaver took hits it outranges")
  }

  test("a reaver drop clears its ling escort before the sunkens") {
    // Drop validation without a game: a Reaver plus its Shuttle walk into
    // two Zerglings screening a Sunken Colony. Speeds and reaches are the
    // matchup contract - lings 0.5 tiles/frame, ours 2.0, sunkens 7 tiles
    // against our 8 - so the only acceptable outcome is lings first (they
    // are nearest, the doctrine shoots nearest), then the static, with the
    // ride never taking a hit. Step manually to record the kill order.
    val sunken = Mover(x = 0.0, y = 0.0, speedTiles = 0.0, hp = 300.0, maxHp = 300.0, rangeTiles = 7.0, dmgPerShot = 25.0, cooldownFrames = 32, isStatic = true, isBuilding = true, isStaticDefense = true)
    val ling1 = Mover(x = 0.0, y = 15.0, speedTiles = 0.5, hp = 35.0, maxHp = 35.0, rangeTiles = 0.5, dmgPerShot = 5.0, cooldownFrames = 15)
    val ling2 = Mover(x = 3.0, y = 18.0, speedTiles = 0.5, hp = 35.0, maxHp = 35.0, rangeTiles = 0.5, dmgPerShot = 5.0, cooldownFrames = 15)
    val reaver = Mover(x = 0.0, y = 32.0, speedTiles = 2.0, hp = 180.0, maxHp = 180.0, rangeTiles = 8.0, dmgPerShot = 100.0, cooldownFrames = 60)
    val shuttle = Mover(x = 0.0, y = 32.0, speedTiles = 2.0, hp = 200.0, maxHp = 200.0, rangeTiles = 0.0, dmgPerShot = 0.0, cooldownFrames = 0)
    reaver.goal = Some((0.0, 0.0))
    shuttle.goal = Some((0.0, 32.0))
    val world = new World(Seq(shuttle, reaver), Seq(ling1, ling2, sunken))

    var lingsDeadFrame: Option[Int] = None
    var sunkenDeadFrame: Option[Int] = None
    while (world.frame < 1200 && (sunken.alive || ling1.alive || ling2.alive) && reaver.alive) {
      world.step()
      if (lingsDeadFrame.isEmpty && ! ling1.alive && ! ling2.alive) lingsDeadFrame = Some(world.frame)
      if (sunkenDeadFrame.isEmpty && ! sunken.alive) sunkenDeadFrame = Some(world.frame)
    }

    assert(lingsDeadFrame.isDefined, "reaver never cleared the ling escort")
    assert(sunkenDeadFrame.isDefined, "reaver never killed the sunken")
    assert(lingsDeadFrame.get < sunkenDeadFrame.get, "reaver shot the static before clearing its escort")
    assert(reaver.alive, "reaver died clearing one sunken with an escort")
    assert(shuttle.hp == shuttle.maxHp, "shuttle took hits holding off the statics")
    assert(reaver.hpLossFraction < 0.15, f"reaver lost ${reaver.hpLossFraction * 100}%.1f%% - the 8v7 outrange is broken")
  }

  test("a suicide squad razes the base after the workers") {
    // Suicide targeting repro: two Dragoons walk into a base with two
    // fleeing SCVs screening a Pylon. Workers die first (nearest), then
    // the base itself must fall - but the production in-range vote vetoes
    // unassigned non-static buildings, so the squad stands on the Pylon
    // forever. The pilot calls the real kernel, so this fails until the
    // shipped rule changes, not when the harness does.
    val pylon = Mover(x = 0.0, y = 0.0, speedTiles = 0.0, hp = 300.0, maxHp = 300.0, rangeTiles = 0.0, dmgPerShot = 0.0, cooldownFrames = 1, isStatic = true, isBuilding = true)
    val scv1 = Mover(x = 0.0, y = 20.0, speedTiles = 0.5, hp = 60.0, maxHp = 60.0, rangeTiles = 0.0, dmgPerShot = 0.0, cooldownFrames = 1, flees = true)
    val scv2 = Mover(x = 4.0, y = 23.0, speedTiles = 0.5, hp = 60.0, maxHp = 60.0, rangeTiles = 0.0, dmgPerShot = 0.0, cooldownFrames = 1, flees = true)
    val dragoon1 = Mover(x = 0.0, y = 30.0, speedTiles = 2.0, hp = 180.0, maxHp = 180.0, rangeTiles = 4.0, dmgPerShot = 20.0, cooldownFrames = 30)
    val dragoon2 = Mover(x = 3.0, y = 30.0, speedTiles = 2.0, hp = 180.0, maxHp = 180.0, rangeTiles = 4.0, dmgPerShot = 20.0, cooldownFrames = 30)
    dragoon1.goal = Some((0.0, 0.0))
    dragoon2.goal = Some((0.0, 0.0))
    val world = new World(Seq(dragoon1, dragoon2), Seq(scv1, scv2, pylon), suicideMission = true)

    var workersDeadFrame: Option[Int] = None
    var pylonDeadFrame: Option[Int] = None
    var firstBlood: Option[String] = None
    while (world.frame < 1500 && (pylon.alive || scv1.alive || scv2.alive) && dragoon1.alive && dragoon2.alive) {
      world.step()
      if (firstBlood.isEmpty) {
        if ( ! scv1.alive) firstBlood = Some("scv1")
        else if ( ! scv2.alive) firstBlood = Some("scv2")
        else if ( ! pylon.alive) firstBlood = Some("pylon")
      }
      if (workersDeadFrame.isEmpty && ! scv1.alive && ! scv2.alive) workersDeadFrame = Some(world.frame)
      if (pylonDeadFrame.isEmpty && ! pylon.alive) pylonDeadFrame = Some(world.frame)
    }

    assert(workersDeadFrame.isDefined, "dragoons never cleared the fleeing workers")
    assert(pylonDeadFrame.isDefined, "dragoons never razed the pylon they stand on")
    assert(firstBlood.exists(_ != "pylon"), "dragoons shot the base before touching its workers")
    assert(dragoon1.alive && dragoon2.alive, "dragoons died to unarmed workers and a pylon")
  }
}
