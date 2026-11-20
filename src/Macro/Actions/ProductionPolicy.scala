package Macro.Actions

import Gameplans.Protoss.PvP.DragoonsBeforeReavers
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass

/**
 * Production policy sink (one home for "may this be requested at all"):
 * every production request flows through `once`/`pump`/`get`, and each
 * consults the fold below before reaching the scheduler - so a gate
 * cannot be bypassed by a sibling production path the way the first-
 * Reaver gate was. Veto by *what*, not by *who*: rules key on the
 * produced class, never on the requesting gameplan. How many, in what
 * order, and when to cancel stay with the owners; the sink only gates
 * new requests, never kills in-flight ones.
 *
 * Registered rules (one per commit, each with its suite):
 * - backbone before the first Reaver hull or Bay (PvP only).
 * The Shuttle veto keeps its own lines until it migrates in as the
 * second rule; committed play (hulls already out) always flows.
 */
object ProductionPolicy {

  /** A veto carries its reason: the enum is the probe taxonomy. */
  sealed trait Veto {
    def label: String
  }

  /** First Reaver waits for the backbone - count and bar travel along. */
  final case class BackboneFirst(dragoons: Int, need: Int) extends Veto {
    override def label: String = s"dragoons $dragoons < $need"
  }

  /** Game-read facts the fold decides on; the seam gathers them. */
  final case class Facts(
    dragoons: Int,
    enemyIsProtoss: Boolean,
    enemyCannonKnown: Boolean,
    haveReaver: Boolean,
    haveBay: Boolean)

  /**
   * Veto for a requested class (pure fold for the suite): unregistered
   * classes always flow; the first Reaver hull or Bay in a mirror waits
   * for the dragoon backbone or a scouted Cannon, committed play
   * (hulls or a Bay already out) excepted.
   */
  def veto(unit: UnitClass, facts: Facts): Option[Veto] = {
    if ( ! facts.enemyIsProtoss) None
    else if (unit == Protoss.Reaver || unit == Protoss.RoboticsSupportBay) {
      if (facts.haveReaver || facts.haveBay) None
      else if (DragoonsBeforeReavers.allowReavers(facts.dragoons, facts.enemyCannonKnown)) None
      else Some(BackboneFirst(facts.dragoons, DragoonsBeforeReavers.minDragoons))
    } else None
  }
}
