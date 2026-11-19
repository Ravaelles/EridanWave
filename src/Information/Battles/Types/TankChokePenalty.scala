package Information.Battles.Types

/**
 * Tank-choke penalty (pure numbers, no game access): a push whose
 * centroid holds inside eight tiles of a choke with sieged tanks inside
 * twenty fights the verdict bar hiked by 0.3 - on the -1..1 scale the
 * fight needs a significant advantage to say yes. Shells across a lane
 * are the number one army killer; the simulation shows how the push
 * dies, this bar decides whether to try. Unlike the choke modifier this
 * is verdict-only, so it stays on regardless of the collision sim.
 */
object TankChokePenalty {

  /** Sieged tanks this close, in tiles, cover the lane. */
  val tankRadiusTiles: Double = 20.0

  /** ...while our centroid holds this close to a choke, in tiles. */
  val chokeRadiusTiles: Double = 8.0

  /** Fight bar hike while the guns cover the lane, on the verdict scale. */
  val penaltyDelta: Double = 0.3

  def shouldPenalize(tankDistTiles: Double, chokeDistTiles: Double): Boolean =
    tankDistTiles <= tankRadiusTiles && chokeDistTiles < chokeRadiusTiles
}
