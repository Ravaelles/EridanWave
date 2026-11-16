package Micro.Matchups

/**
 * Miner threat (pure numbers, no game access): a worker mining in a metro
 * we don't own threatens us if we're close enough to punish it - our edge
 * to its minerals plus our edge to it, inside two ground ranges plus
 * three tiles (96px). The metro/worker scan stays in MatchupAnalysis; only the verdict
 * is pinned here.
 */
object MinerThreat {

  def threatened(
    metroOurs: Boolean,
    gathering: Boolean,
    approachPixels: Double,
    rangeGroundPixels: Double): Boolean =
    ! metroOurs && gathering && approachPixels < 96 + 2 * rangeGroundPixels
}
