package Micro.Targeting

import Mathematics.Maff
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Pure Carrier target priority (no game access) - a pre-`Target`
 * doctrine pick, not a second scoring ladder (conventions rule 15):
 * `BeCarrier` applies it to leashed hittable enemies before the
 * generic `Target.choose` chain, because interceptors are wasted on
 * whatever the squad ladder scores highest. AA first, then the guns
 * that can answer, then builders and workers. Nearest inside each
 * tier; anything else hittable falls through to the generic pick.
 * Distances in tiles.
 */
object CarrierTargets {

  final case class Mark(
    distTiles: Double,
    isTurret: Boolean,
    isGoliath: Boolean,
    isSiegedTank: Boolean,
    isUnsiegedTank: Boolean,
    isMarine: Boolean,
    isBunker: Boolean,
    isMedic: Boolean,
    isBuilderWorker: Boolean,
    isWorker: Boolean)

  def tier(mark: Mark): Int =
    if (mark.isTurret) 0
    else if (mark.isGoliath) 1
    else if (mark.isSiegedTank) 2
    else if (mark.isUnsiegedTank) 3
    else if (mark.isMarine) 4
    else if (mark.isBunker) 5
    else if (mark.isMedic) 6
    else if (mark.isBuilderWorker) 7
    else if (mark.isWorker) 8
    else 9

  /** Nearest inside the best non-empty tier; nothing when nobody qualifies. */
  def pick(marks: Seq[Mark]): Option[Int] =
    if (marks.isEmpty) None
    else Some(marks.indices.minBy(i => (tier(marks(i)), marks(i).distTiles)))

  /** Game seam: one Mark per candidate, plain data out. */
  def mark(carrier: FriendlyUnitInfo, target: UnitInfo): Mark =
    Mark(
      carrier.pixelDistanceEdge(target) * Maff.inv32,
      Terran.MissileTurret(target),
      Terran.Goliath(target),
      Terran.SiegeTankSieged(target),
      Terran.SiegeTankUnsieged(target),
      Terran.Marine(target),
      Terran.Bunker(target),
      Terran.Medic(target),
      target.unitClass.isWorker && target.constructing,
      target.unitClass.isWorker)
}
