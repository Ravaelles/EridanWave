package Micro.Actions.Combat.Decisioning

/**
 * Always-attack victims in range (no game access) - a pre-`Target` doctrine pick
 * (conventions rule 15): a fixed priority that outranks retreat, so it
 * stays one documented ladder instead of joining `Target`'s scoring.
 *
 * Grounded warriors shoot the
 * targets that must die first, regardless of anything else the unit is
 * doing - even retreating. Fixed priority: suicide Scourge nearest,
 * then the weakest Carrier, Science Vessel, Battlecruiser, Wraith,
 * detected Observer, Reaver, Dark Templar, High Templar.
 * Distances in tiles, health pooled.
 */
object CrucialAirSnipe {

  sealed trait VictimKind

  case object Scourge extends VictimKind

  case object Carrier extends VictimKind

  case object Vessel extends VictimKind

  case object Battlecruiser extends VictimKind

  case object Wraith extends VictimKind

  case object Observer extends VictimKind

  case object Reaver extends VictimKind

  case object DarkTemplar extends VictimKind

  case object HighTemplar extends VictimKind

  final case class Mark(distTiles: Double, kind: VictimKind, totalHealth: Double)

  /**
   * Chase margin past our own range, in tiles: a must-kill victim this
   * close gets walked down instead of watched leave - a visible Observer
   * a tile and a half past a six-tile gun still dies for the walk.
   */
  val chaseMarginTiles: Double = 1.5

  /**
   * Fast victims we never chase off the line: a Scourge that turned home
   * and a Wraith showing its engines are gone either way - the line
   * would trade formation for a kill it cannot catch. Slow capitals,
   * observers and ground support always get walked down inside the
   * margin (allowAttackingUnitsRunningAwayFromUs is false only here).
   */
  def chasesAway(kind: VictimKind): Boolean = kind == Scourge || kind == Wraith

  /**
   * Dark Templar duel terms, in frames and tiles: answer the swipe only
   * with a nearly-ready gun, and as a ranged hull only with room to
   * spare - planting feet inside 2.6 tiles trades the volley for DT
   * swipes on the stop-fire frames. Melee hulls have no kite to protect,
   * so they answer whenever the gun is warm.
   */
  val dtCooldownFrames: Int = 4

  val dtRangedStandTiles: Double = 2.6

  def duelsDarkTemplar(cooldownLeft: Int, melee: Boolean, distTiles: Double): Boolean =
    cooldownLeft <= dtCooldownFrames && (melee || distTiles >= dtRangedStandTiles)

  /**
   * Snipe reach (pure flags for the suite): in range always counts; past
   * the range a victim inside the chase margin counts unless it is a
   * fast one running away - capitals, observers and ground support get
   * walked down, runners do not.
   */
  def inSnipeReach(kind: VictimKind, inRange: Boolean, inMargin: Boolean, facingUs: Boolean): Boolean =
    inRange || (inMargin && (facingUs || ! chasesAway(kind)))

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
      .orElse(weakest(Observer))
      .orElse(weakest(Reaver))
      .orElse(weakest(DarkTemplar))
      .orElse(weakest(HighTemplar))
  }
}
