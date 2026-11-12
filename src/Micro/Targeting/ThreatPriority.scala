package Micro.Targeting

/**
 * Pure snipe priority (no game access): inside our firing range the army
 * kills the force multipliers first - Defiler, then Lurker, then Mutalisk.
 * A tier fires only on visible targets; what we cannot see keeps the normal
 * ladder instead of sending the army after ghosts. Most wounded among
 * equals comes from the ladder's score ranking, and targets guaranteed to
 * die keep the accelerates-demise passes like every other stratum.
 */
object ThreatPriority {

  /** Snipe order inside our firing range. Lower fires first. */
  val defilerTier : Int = 0
  val lurkerTier  : Int = 1
  val mutaliskTier: Int = 2
  val otherTier   : Int = 3

  /** Tier of a target described by its class flags. */
  def tier(isDefiler: Boolean, isLurker: Boolean, isMutalisk: Boolean): Int =
    if (isDefiler) defilerTier
    else if (isLurker) lurkerTier
    else if (isMutalisk) mutaliskTier
    else otherTier

  /** Only a visible target of the wanted tier diverts the ladder. */
  def snipable(visible: Boolean, unitTier: Int, wantTier: Int): Boolean =
    visible && unitTier == wantTier
}
