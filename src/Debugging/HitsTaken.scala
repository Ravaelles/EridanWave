package Debugging

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Hits-taken ledger (kite discipline, owner direction: a dueling Dragoon
 * is never hit more than a few times): frames where a warrior's total
 * hit points drop count as hits taken, one per frame no matter how many
 * guns landed. Regen never erases the count - a healed-over swing still
 * happened. Counted per unit, reported once - the owner reads it off
 * every UMS validation game next to the waste ledger.
 */
object HitsTaken extends PerGame {

  private val hitsTaken = new mutable.HashMap[Int, Int]()
  private val classById = new mutable.HashMap[Int, String]()
  private val lastHp    = new mutable.HashMap[Int, Int]()

  override def reset(): Unit = {
    hitsTaken.clear()
    classById.clear()
    lastHp.clear()
  }

  /** An hp drop since the last frame is a landed hit, no matter the guns. */
  def tookHit(wasHp: Option[Int], nowHp: Int): Boolean =
    wasHp.exists(_ > nowHp)

  def bump(unitId: Int, className: String, totalHp: Int): Unit = {
    if (tookHit(lastHp.get(unitId), totalHp))
      hitsTaken(unitId) = hitsTaken.getOrElse(unitId, 0) + 1
    classById(unitId) = className
    lastHp(unitId) = totalHp
  }

  def summary(maxRows: Int = 3): String = {
    if (hitsTaken.isEmpty) return "### Hits taken: none"

    val top = hitsTaken.toSeq.sortBy(-_._2).take(maxRows)
      .map { case (id, hits) => s"${classById.getOrElse(id, "?")} #$id $hits" }
      .mkString(", ")
    f"### Hits taken: $top"
  }

  /** Worst single-unit total, for suites. */
  def worstTotal: Int =
    if (hitsTaken.isEmpty) 0 else hitsTaken.values.max
}
