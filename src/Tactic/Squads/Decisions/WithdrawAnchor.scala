package Tactic.Squads.Decisions

/**
 * Withdrawal anchor (no game access): a withdrawing cluster reforms at
 * the middle choke instead of bleeding home through the open map - the
 * choke narrows what can chase, and the march back out is short. Falls
 * back to the home point when the choke is unknown or the ground path
 * there does not exist (sealed in means reforming inside, not walking
 * into the seal).
 */
object WithdrawAnchor {

  def anchor(
    homeX: Double, homeY: Double,
    choke: Option[(Double, Double)],
    reachesChoke: Boolean): (Double, Double) =
    if (reachesChoke) choke.getOrElse((homeX, homeY)) else (homeX, homeY)
}
