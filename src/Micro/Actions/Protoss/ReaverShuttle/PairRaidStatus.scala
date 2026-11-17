package Micro.Actions.Protoss.ReaverShuttle

import Utilities.Latch

/**
 * Raid status memory (no game access, no frames): which pairs raid and
 * which support. The sharp health lines would flip the pair's orders on
 * every wound crossing (shields regenerate, damage jumps), so the
 * 0.5-out / 0.65-back deadband holds the current assignment instead -
 * start raiding past the return line, stop below the retreat line,
 * never flap inside the band. One bit per hull id, reset with the rest
 * of the latches every game. The Zerg veto (never raid into Zerg) stays
 * with the pair doctrine that reads the enemy, not here.
 */
object PairRaidStatus {

  private val raiding = new Latch

  def reset(): Unit = raiding.reset()

  def update(unitId: Int, isOldestReaver: Boolean, ownHealth01: Double, mateHealth01: Double): ReaverShuttleStatus.PairStatus = {
    val raid = raiding(
      unitId,
      fires    = ReaverShuttleStatus.shouldRaid(isOldestReaver, ownHealth01, mateHealth01),
      releases = ReaverShuttleStatus.shouldSupport(isOldestReaver, ownHealth01, mateHealth01))
    if (raid) ReaverShuttleStatus.RaidBase else ReaverShuttleStatus.SupportArmy
  }
}
