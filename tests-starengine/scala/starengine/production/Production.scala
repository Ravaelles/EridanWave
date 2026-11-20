package starengine.production

import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import starengine._

import scala.collection.mutable

/**
 * Standing production orders (harness mirror of the shipped requests):
 * re-issued every frame by the producer pilot, latest wins per
 * structure - like scheduler requests expiring when not renewed.
 */
object ProductionOrder {

  sealed trait Standing

  /** Exactly this many hulls ever, then silence (`BuildOnce`). */
  final case class Once(kind: UnitClass, count: Int) extends Standing

  /** Continuous until this many hulls started (`pump` maximum). */
  final case class Pump(kind: UnitClass, maxTotal: Int) extends Standing

  /** While fewer than this many are live or training (`get` total). */
  final case class Want(kind: UnitClass, total: Int) extends Standing
}

/**
 * Minimal production (test harness only, never shipped): a free
 * gateway, power and resources, nothing more. Pilots issue standing
 * orders, the engine advances busy timers through the constraint seam,
 * bodies emerge. No workers, no income, no supply - the scenario sets
 * stockpiles and the harness only spends them.
 *
 * Start rules, all required: the structure lives and builds, the
 * ordered kind trains there (a Pylon trains nothing), Protoss
 * structures produce only under power (any live Pylon stands in for
 * coverage, documented approximation), and the stockpile covers the
 * table cost. Costs and build frames come from the unit tables, like
 * every other engine number. Deduction lands at start, BW-faithful.
 */
final class Production(var minerals: Int, var gas: Int) {

  import ProductionOrder._

  private val orders = new mutable.HashMap[Body, Standing]()
  private val busy = new mutable.HashMap[Body, (UnitClass, Int)]()
  private val startedEver = new mutable.HashMap[UnitClass, Int]().withDefaultValue(0)

  /** Emerged hulls, in order: the parity surface scenarios assert on. */
  val emerged = new mutable.ListBuffer[Body]()

  /** Standing order for a structure; latest wins. */
  def order(structure: Body, standing: Standing): Unit =
    orders(structure) = standing

  /** Build frames left on a structure, zero when free. */
  def busyLeft(structure: Body): Int =
    busy.get(structure).map(_._2).getOrElse(0)

  /** Hulls of this kind ever started. */
  def started(kind: UnitClass): Int =
    startedEver(kind)

  def step(world: World, own: Seq[Body]): Unit = {
    orders.retain((structure, _) => structure.alive)
    orders.foreach { case (structure, standing) =>
      if ( ! structure.kind.isBuilding) ()
      else {
        // Complete, then start the next in the same step: a standing
        // order is the building's queue, and a queued hull starts the
        // frame its predecessor leaves - cycles run gapless at exactly
        // table rate, never overlapping. See the parity suite.
        if (busy.contains(structure)) tick(structure)
        if ( ! busy.contains(structure)) nextKind(structure, standing, own).foreach(start(structure, _))
      }
    }
  }

  private def tick(structure: Body): Unit = {
    val (kind, left) = busy(structure)
    if (left <= 1) {
      busy.remove(structure)
      emerged.append(new Body(kind, structure.x, structure.y, kind.maxTotalHealth))
    } else busy(structure) = (kind, left - 1)
  }

  private def nextKind(structure: Body, standing: Standing, own: Seq[Body]): Option[UnitClass] = {
    val kind = standing match {
      case Once(k, count) => if (startedEver(k) < count) Some(k) else None
      case Pump(k, max)   => if (startedEver(k) < max) Some(k) else None
      case Want(k, total) => if (liveEmerging(k) < total) Some(k) else None
    }
    kind.filter(k => trainsHere(structure, k, own) && affordable(k))
  }

  private def liveEmerging(kind: UnitClass): Int =
    emerged.count(b => b.kind == kind && b.alive) +
      busy.count { case (_, (k, _)) => k == kind }

  /** Right structure, powered when Protoss - before any mineral moves. */
  private def trainsHere(structure: Body, kind: UnitClass, own: Seq[Body]): Boolean =
    kind.whatBuilds._1 == structure.kind && powered(structure, own)

  private def powered(structure: Body, own: Seq[Body]): Boolean =
    ! structure.kind.isProtoss ||
      own.exists(b => b.alive && b.kind == Protoss.Pylon)

  private def affordable(kind: UnitClass): Boolean =
    minerals >= kind.mineralPrice && gas >= kind.gasPrice

  private def start(structure: Body, kind: UnitClass): Unit = {
    minerals -= kind.mineralPrice
    gas -= kind.gasPrice
    startedEver(kind) = startedEver(kind) + 1
    busy(structure) = (kind, kind.buildFrames)
  }
}
