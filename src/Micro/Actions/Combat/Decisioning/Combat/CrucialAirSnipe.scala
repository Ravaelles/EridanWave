package Micro.Actions.Combat.Decisioning

/**
 * Crucial air-victim snipe (no game access): ground anti-air shoots the
 * targets that must die first, regardless of anything else the unit is
 * doing - even retreating. Fixed priority: suicide Scourge nearest,
 * then the weakest Carrier, Science Vessel, Battlecruiser, Wraith.
 * Distances in tiles, health pooled.
 */
object CrucialAirSnipe {

  sealed trait VictimKind

  case object Scourge extends VictimKind

  case object Carrier extends VictimKind

  case object Vessel extends VictimKind

  case object Battlecruiser extends VictimKind

  case object Wraith extends VictimKind

  final case class Mark(distTiles: Double, kind: VictimKind, totalHealth: Double)

  /** First the nearest Scourge, then the weakest of each capital in order. */
  def pick(marks: Seq[Mark]): Option[Int] = {
    if (marks.isEmpty) return None

    def nearest(kind: VictimKind): Option[Int] = {
      val at = marks.indices.filter(i => marks(i).kind == kind)
      if (at.isEmpty) None else Some(at.minBy(i => marks(i).distTiles))
    }

    def weakest(kind: VictimKind): Option[Int] = {
      val at = marks.indices.filter(i => marks(i).kind == kind)
      if (at.isEmpty) None else Some(at.minBy(i => marks(i).totalHealth))
    }

    nearest(Scourge)
      .orElse(weakest(Carrier))
      .orElse(weakest(Vessel))
      .orElse(weakest(Battlecruiser))
      .orElse(weakest(Wraith))
  }
}
