package Gameplans.Protoss.PvZ.reactive

/**
 * Weights behind the PvZ army mix: what the Zerg has been seen with, what
 * the scouting expects, and how much of our army should answer each of those
 * units. Pure, so the mix can be argued with in a test instead of a ladder
 * game.
 */
object PvZArmyWeights {

  /** What we see and expect, all read once by the caller. */
  case class Seen(
    zerglings             : Int,
    hydralisks            : Int,
    mutalisks             : Int,
    sunkenColonies        : Int,
    confidenceDefending01 : Double,
    highTemplars          : Int,
    gas                   : Int,
    hydralisksLikely      : Boolean,
    mutalisksLikely       : Boolean)

  /** Normalized needs per unit type, plus the raw counts the goals use. */
  case class Mix(
    zerglings       : Int,
    hydralisks      : Int,
    mutalisks       : Int,
    sunkenColonies  : Double,
    zealots         : Double,
    dragoons        : Double,
    reavers         : Double,
    archons         : Double,
    corsairs        : Double,
    scouts          : Double)

  /**
   * Unseen Hydralisks are assumed at four, unseen Mutalisks at eight unless
   * Hydralisks are expected: a Hydralisk opening makes Mutalisks unlikely and
   * the other way around. Sunken Colonies only count once we are defending.
   */
  def of(seen: Seen): Mix = {
    val zerglings  = Math.max(seen.zerglings,  if (seen.hydralisksLikely) 0 else Math.max(8, 4 + seen.zerglings))
    val hydralisks = Math.max(seen.hydralisks, if (seen.hydralisksLikely) 4 else 0)
    val mutalisks  = Math.max(seen.mutalisks,  if (seen.mutalisksLikely && ! seen.hydralisksLikely) 8 else 0)
    val sunkens    = seen.sunkenColonies * 2 * Math.max(0, seen.confidenceDefending01 - 0.5)
    val total      = Math.max(0.01, 0.25 * zerglings + 1.0 * hydralisks + 2.0 * mutalisks + 4.0 * sunkens)
    val weightZerglings  = 0.25 * zerglings  / total
    val weightHydralisks = 1.0  * hydralisks / total
    val weightMutalisks  = 2.0  * mutalisks  / total
    val weightSunkens    = 4.0  * sunkens    / total

    // An unmatched High Templar plus every 150 gas past the first hundred.
    val templarWeight = Math.max(0, seen.highTemplars % 2 + (seen.gas - 100) / 150)

    Mix(zerglings, hydralisks, mutalisks, sunkens,
      zealots  = weightZerglings  + weightSunkens,
      dragoons = weightHydralisks + weightMutalisks,
      reavers  = weightHydralisks + weightSunkens,
      archons  = weightZerglings  + weightMutalisks + templarWeight,
      corsairs = weightMutalisks,
      scouts   = weightZerglings  + weightSunkens)
  }
}
