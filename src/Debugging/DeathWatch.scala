package Debugging

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.UnitInfo

import scala.collection.mutable

/**
 * Death safety valve for the units we cannot afford to lose blindly:
 * every frame it polls the engine orders of watched units (Observer,
 * Shuttle, Dark Templar, Reaver) and records their decision chains;
 * on death it dumps the last twenty of each to the debug log, so the
 * story reads off one block instead of needing a trace hunt. Quiet by
 * construction: unwatched units cost one set lookup, watched ones log
 * on change only, and deaths are rare. To watch more classes, extend
 * the set below.
 */
object DeathWatch {

  val watched: Set[UnitClass] = Set(
    Protoss.Observer,
    Protoss.Shuttle,
    Protoss.DarkTemplar,
    Protoss.Reaver)

  private val depth = 20
  private val orderRings = new mutable.HashMap[Int, mutable.Queue[(Int, String)]]()
  private val behavedRings = new mutable.HashMap[Int, mutable.Queue[(Int, String)]]()

  def reset(): Unit = {
    orderRings.clear()
    behavedRings.clear()
  }

  def update(): Unit = {
    With.units.ours.view.filter(u => watched(u.unitClass)).foreach(trackOrders)
  }

  def recordBehavior(unitId: Int, label: String): Unit = {
    push(behavedRings, unitId, (With.frame, label))
  }

  def reportIfWatched(unit: UnitInfo): Unit = {
    if ( ! watched(unit.unitClass)) return

    val orders = orderRings.getOrElse(unit.id, mutable.Queue.empty).map(p => f"f${p._1} #${p._2}").mkString("; ")
    val behaved = behavedRings.getOrElse(unit.id, mutable.Queue.empty).map(p => f"f${p._1} ${p._2}").mkString("; ")
    // Deaths are rare and the whole point is reading them in the main log,
    // so mirror to stdout like the game summary box (debug logging alone
    // stays in the file when the tournament config mutes stdout).
    System.out.println(f"DEATHWATCH $unit died f${With.frame} at ${unit.pixel}")
    System.out.println(f"DEATHWATCH orders: $orders")
    System.out.println(f"DEATHWATCH behaved: $behaved")
    With.logger.debug(f"DEATHWATCH $unit died f${With.frame} at ${unit.pixel}")
    With.logger.debug(f"DEATHWATCH orders: $orders")
    With.logger.debug(f"DEATHWATCH behaved: $behaved")
    orderRings.remove(unit.id)
    behavedRings.remove(unit.id)
  }

  private def trackOrders(unit: UnitInfo): Unit = {
    push(orderRings, unit.id, (With.frame, unit.order))
  }

  private def push(rings: mutable.HashMap[Int, mutable.Queue[(Int, String)]], unitId: Int, entry: (Int, String)): Unit = {
    val ring = rings.getOrElseUpdate(unitId, new mutable.Queue[(Int, String)]())
    if (ring.lastOption.exists(_._2 == entry._2)) return

    ring.enqueue(entry)
    while (ring.size > depth) ring.dequeue()
  }
}
