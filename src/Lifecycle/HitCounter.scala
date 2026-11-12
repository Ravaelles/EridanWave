package Lifecycle

import ProxyBwapi.UnitInfo.UnitInfo

import scala.collection.mutable

/**
 * Counts how many times our units take damage, Atlantis parity: a hit is a
 * frame in which a unit's hit points plus shields drop versus the previous
 * frame (AUnit.isUnderAttack(2) compares current hp against last frame's).
 * The old rising edge on the engine isUnderAttack flag counted one per
 * engagement, so a lost Dragoon showed a single hit; damage ticks count
 * every frame the shot lands. Only our own hit-point drops count, so our
 * attacks can never double-count. Reset every game; read by GameSummary.
 */
object HitCounter {

  private val lastHp = new mutable.HashMap[Int, Int]
  var total: Int = 0

  def reset(): Unit = {
    lastHp.clear()
    total = 0
  }

  def onFrame(units: Iterable[UnitInfo]): Unit = {
    val current = units.view.filter(_.alive).map(u => (u.id, u.hitPoints + u.shieldPoints)).toSeq
    val (hits, next) = countHits(lastHp.toMap, current)
    total += hits
    lastHp.clear()
    lastHp ++= next
  }

  /** Pure kernel: frames with a hit-point drop count once each; newcomers,
    * healed units and the dead never count. Returns the hits and the map
    * the next frame compares against.
    */
  def countHits(previous: Map[Int, Int], current: Seq[(Int, Int)]): (Int, Map[Int, Int]) = {
    var hits = 0
    current.foreach({ case (id, hp) =>
      if (previous.get(id).exists(_ > hp)) {
        hits += 1
      }
    })

    (hits, current.toMap)
  }
}
