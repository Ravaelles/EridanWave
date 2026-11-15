package Micro.Matchups

/**
 * Volley discipline (pure numbers, no game access): a volley must not
 * decrease our DPS relative to theirs, and must not let them open a gap
 * against us. The unit reads stay in MatchupAnalysis; the algebra - with
 * its strict-vs-lazy edges - is pinned here.
 */
object VolleyDiscipline {

  def wants(
    cooldownMaxAgainstTarget: Int,
    potshotFrames: Int,
    rangeVsTarget: Double,
    rangeDeepestVsMe: Double,
    rangeNearestVsMe: Double,
    speed: Double,
    speedDeepest: Double): Boolean =
    (cooldownMaxAgainstTarget > potshotFrames ||
      (rangeVsTarget > rangeDeepestVsMe && speed > speedDeepest)) &&
      (rangeVsTarget >= rangeDeepestVsMe ||
        rangeVsTarget >= rangeNearestVsMe ||
        speed > speedDeepest)
}
