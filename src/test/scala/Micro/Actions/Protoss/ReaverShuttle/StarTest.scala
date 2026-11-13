package Micro.Actions.Protoss.ReaverShuttle

import scala.collection.mutable.ArrayBuffer

/**
 * StarTest: a minimal frame stepper for doctrine duels (test scope only).
 * Movers hold tile positions and constant speeds; each frame ours walk
 * their goals while foes walk at us, and a foe in range with a ready
 * weapon deals its damage instantly. No pathing, no cooldown trickery -
 * just enough world to prove a maneuver takes no hits it should not.
 */
object StarTest {

  case class Mover(
    var x: Double,
    var y: Double,
    speedTiles: Double,
    var hp: Double,
    maxHp: Double,
    rangeTiles: Double,
    dmgPerShot: Double,
    cooldownFrames: Int) {

    var cooldownLeft: Int = 0
    var goal: Option[(Double, Double)] = None

    def alive: Boolean = hp > 0

    def wound01: Double = (maxHp - hp) / maxHp

    def hpLossFraction: Double = (maxHp - hp) / maxHp

    def distTiles(other: Mover): Double = {
      val dx = x - other.x
      val dy = y - other.y
      Math.sqrt(dx * dx + dy * dy)
    }

    def stepToward(tx: Double, ty: Double): Unit = {
      val dx = tx - x
      val dy = ty - y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) {
        val stride = Math.min(speedTiles, dist)
        x += dx / dist * stride
        y += dy / dist * stride
      }
    }
  }

  class World(val ours: Seq[Mover], val foes: Seq[Mover]) {

    var frame: Int = 0
    val hits: ArrayBuffer[(Int, Double)] = ArrayBuffer.empty

    private def nearestOur(foe: Mover): Option[Mover] = {
      val live = ours.filter(_.alive)
      if (live.isEmpty) None else Some(live.minBy(foe.distTiles))
    }

    def step(): Unit = {
      ours.filter(_.alive).foreach(o => {
        nearestFoe(o) match {
          // Can hit it: hold and shoot. Cannot reach it: close in.
          // Unarmed or no contact: fly the mission goal.
          case Some(f) if o.dmgPerShot > 0 && o.rangeTiles > 0 && o.distTiles(f) <= o.rangeTiles =>
          case Some(f) if o.dmgPerShot > 0 && o.rangeTiles > 0 => o.stepToward(f.x, f.y)
          case _ => o.goal.foreach(g => o.stepToward(g._1, g._2))
        }
      })
      foes.filter(_.alive).foreach(f => nearestOur(f).foreach(n => f.stepToward(n.x, n.y)))
      foes.filter(_.alive).foreach(f => {
        if (f.cooldownLeft > 0) {
          f.cooldownLeft -= 1
        } else {
          nearestOur(f).filter(n => f.distTiles(n) <= f.rangeTiles).foreach(victim => {
            victim.hp = Math.max(0, victim.hp - f.dmgPerShot)
            hits += ((frame, f.dmgPerShot))
            f.cooldownLeft = f.cooldownFrames
          })
        }
      })
      ours.filter(_.alive).foreach(o => {
        if (o.cooldownLeft > 0) {
          o.cooldownLeft -= 1
        } else {
          nearestFoe(o).filter(f => o.dmgPerShot > 0 && o.distTiles(f) <= o.rangeTiles).foreach(victim => {
            victim.hp = Math.max(0, victim.hp - o.dmgPerShot)
            hits += ((frame, -o.dmgPerShot))
            o.cooldownLeft = o.cooldownFrames
          })
        }
      })
      frame += 1
    }

    private def nearestFoe(o: Mover): Option[Mover] = {
      val live = foes.filter(_.alive)
      if (live.isEmpty) None else Some(live.minBy(o.distTiles))
    }

    def run(frames: Int): Unit = {
      var remaining = frames
      while (remaining > 0 && ours.exists(_.alive)) {
        step()
        remaining -= 1
      }
    }
  }
}
