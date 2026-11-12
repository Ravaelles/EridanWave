package Micro.Actions.Protoss.Dragoon

import Micro.Coordination.Pushing.{TrafficPriorities, TrafficPriority}
import Utilities._

/**
 * How close a Dragoon may sit to slow Zealots with N shots of slack.
 * Pure policy: the margin math never touches the game, so the stand-or-run
 * line can be argued with in a test instead of in a duel. Pixels in, tiles
 * out only where the name says so (rule 10); the engine reports pixels.
 */
object DragoonSlowlotPolicy {

  /** Shots of slack before the next hit kills us. */
  def hitsLeft(totalHealth: Int): Int =
    totalHealth / 14

  /** Tiles of slack plus shots of slack: how safe this margin is. */
  def safety(marginPixels: Int, totalHealth: Int): Int =
    marginPixels / 32 + hitsLeft(totalHealth)

  /** Priority follows safety: dying Dragoons shove, comfortable ones pardon. */
  def priorityForSafety(safety: Int): TrafficPriority =
    ?(safety < 3, TrafficPriorities.Shove,
    ?(safety < 5, TrafficPriorities.Bump,
    ?(safety < 7, TrafficPriorities.Nudge,
                  TrafficPriorities.Pardon)))

  /** How many pixels of margin N shots of slack buy. */
  def marginRequiredPixels(totalHealth: Int): Int =
    Math.max(32, 128 - 8 * hitsLeft(totalHealth))

  /** Positive means hold, negative means run. */
  def marginDeltaPixels(marginPixels: Int, totalHealth: Int): Int =
    marginPixels - marginRequiredPixels(totalHealth)
}
