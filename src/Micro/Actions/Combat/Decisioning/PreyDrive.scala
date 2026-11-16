package Micro.Actions.Combat.Decisioning

/**
 * Prey drive (pure numbers, no game access): no formation-holding
 * against helpless prey. When everything hittable nearby is a worker,
 * or the local skim runs three-to-one or better, the hull hunts
 * instead of dressing the line or walking its march home - a column
 * that poses against miners mills until the miners leave, then walks
 * home, then poses again. The duel logic keeps it safe (kiting and
 * retreat still outrank the chase); this only opens the attempt.
 */
object PreyDrive {

  /** Hunt instead of posing at this local skim ratio or better. */
  val overwhelmRatio: Double = 3.0

  def shouldHunt(workersOnly: Boolean, usSkim: Double, foeSkim: Double): Boolean =
    workersOnly || (foeSkim > 0 && usSkim / foeSkim >= overwhelmRatio)
}
