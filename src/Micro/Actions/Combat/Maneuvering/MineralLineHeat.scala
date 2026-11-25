package Micro.Actions.Combat.Maneuvering

/**
 * Mineral-line heat (pure flags, no game access): miners bleeding with no
 * visible enemy means an unseen killer works the line - a DT, a Lurker,
 * a mine, a storm. Memory and range gates cannot see it (without
 * detection a blade is never sighted at all), so the line evacuates on
 * the damage itself and stays out until a full quiet window passes.
 * Frames for the suite; the hall-keyed latch lives on the seam.
 */
object MineralLineHeat {

  /** Miner damage this recent counts as hot, in frames. */
  val damageWindowFrames: Int = 48

  /** No miner damage for this long reopens the line, in frames. */
  val calmFrames: Int = 480

  /** Heat fires on fresh miner damage with no visible enemy around. */
  def heating(minerDamagedRecently: Boolean, visibleEnemyNear: Boolean): Boolean =
    minerDamagedRecently && ! visibleEnemyNear

  /** Calm releases only after a full quiet window. */
  def calm(framesSinceMinerDamage: Int): Boolean =
    framesSinceMinerDamage > calmFrames
}
