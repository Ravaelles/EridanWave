package Gameplans.Protoss.PvZ.policy

/**
 * Pure Templar-before-Robo gate (no game access): a standing Templar
 * Archives with fewer than two lifetime Dark Templars outranks the first
 * Robotics Facility while the bank is thin and no Lurkers are shown or
 * likely - the sunk Archives must earn before the next tech path opens.
 * The bank clause mirrors the affordability logic: past it both paths
 * are payable and normal rules apply. Callers queue the Dark Templar
 * themselves when this fires, so the release condition keeps moving.
 */
object TemplarArchivesOrRoboticsFacility {

  /** Lifetime Dark Templars owed before the first Robotics Facility. */
  val templarsBeforeRobo: Int = 2

  /** Past this bank both tech paths are affordable; stop prioritizing. */
  val roboGasBank: Int = 260

  def shouldPrioritizeTemplarsOverFirstRoboticsFacility(
    haveArchives : Boolean,
    hasRobo      : Boolean,
    dtProduced   : Int,
    gas          : Int,
    lurkersShown : Boolean,
    lurkersLikely: Boolean): Boolean =
    haveArchives &&
      ! hasRobo &&
      dtProduced < templarsBeforeRobo &&
      gas < roboGasBank &&
      ! lurkersShown &&
      ! lurkersLikely
}
