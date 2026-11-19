package Micro.Actions.Protoss.ReaverShuttle

import Utilities.Time.Seconds

/**
 * Static stand permission (no game access): a fresh hull with a shot to
 * spend holds its gun line under a defensive building instead of
 * stepping out - walk in, shoot, withdraw after, not before. The
 * keep-off floor below would bleed the whole magazine away: a hull
 * that yields before firing never fires, while one that tanks a few
 * hits lands its scarab and leaves on the cooldown. Past the shield
 * wound, dry, or just fired, the floor resumes and the hull steps out.
 */
object ReaverStaticStand {

  /** Stand with shields holed up to this, in hit points. */
  val shieldWoundMaxHp: Int = 40

  /** A shot to spend means quiet this long, in frames. */
  val quietFrames: Int = Seconds(6)()

  def mayStand(shieldWoundHp: Int, scarabs: Int, framesSinceShot: Int): Boolean =
    shieldWoundHp <= shieldWoundMaxHp && scarabs > 0 && framesSinceShot >= quietFrames
}
