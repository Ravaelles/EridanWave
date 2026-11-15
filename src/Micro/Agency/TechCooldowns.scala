package Micro.Agency

import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.Techs.Tech

/**
 * Per-tech cooldowns (pure table, no game access): a new tech stops
 * editing the command facade and only adds a row here. Recast guards
 * the repeat order (Stim), sleep paces the chain after the cast.
 */
object TechCooldowns {

  /** Frames before the same tech may be ordered again (0: no guard). */
  def recastFrames(tech: Tech): Int =
    if (tech == Terran.Stim) 24 else 0

  /** Frames to sleep after issuing the cast; None keeps the caller's pace. */
  def after(tech: Tech): Option[Int] =
    if (tech == Protoss.ArchonMeld || tech == Protoss.DarkArchonMeld) Some(48)
    else if (tech == Terran.SpiderMinePlant) Some(12)
    else None
}
