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
}
