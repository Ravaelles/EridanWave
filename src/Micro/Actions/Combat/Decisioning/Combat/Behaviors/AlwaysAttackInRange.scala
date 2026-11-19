package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Always-attack victims in reach: a grounded warrior shoots the targets
 * that must die first, regardless of anything else the unit is doing -
 * even retreating. Air victims need anti-air guns, ground victims need
 * anti-ground ones; air always outranks ground and each half walks its
 * victim down inside the chase margin instead of watching it leave -
 * only fast runners (Scourge, Wraith showing its engines) are let go.
 * Fixed priority: suicide Scourge nearest, then the weakest Carrier,
 * Science Vessel, Battlecruiser, Wraith, detected Observer, Reaver,
 * Dark Templar, High Templar. The pick always lands; the shot only
 * fires when the duel will not run this frame (retreating, or no
 * advance) - an advancing duel owns its frame and kites on cooldown,
 * so a standing shot here would pin it in place and starve the kite.
 * Returns true when it shot or is already on the victim.
 */
object AlwaysAttackInRange {

  /** One hunt row: which victims, of what kind. UnitClasses double as the matcher. */
  private final case class Prey(matches: UnitInfo => Boolean, kind: VictimKind)

  // The hunt list - the whole doctrine on one screen, race by race.
  // Only rows of a shown race are even walked: no Terran on the board
  // means no Vessel/BC/Wraith scans. Presence is read off the hittable
  // targets, not the lobby slot - mixed maps field several races while
  // only one ever shows up there.
  private val zergAir: Seq[Prey] = Seq(
    Prey(Zerg.Scourge, Scourge))

  private val terranAir: Seq[Prey] = Seq(
    Prey(Terran.ScienceVessel, Vessel),
    Prey(Terran.Battlecruiser, Battlecruiser),
    Prey(Terran.Wraith, Wraith))

  private val protossAir: Seq[Prey] = Seq(
    Prey(Protoss.Carrier, Carrier),
    Prey(o => Protoss.Observer(o) && ! o.effectivelyCloaked, Observer))

  private val protossGround: Seq[Prey] = Seq(
    Prey(Protoss.Reaver, Reaver),
    Prey(Protoss.DarkTemplar, DarkTemplar),
    Prey(Protoss.HighTemplar, HighTemplar))

  /**
   * @param willRetreat the duel will not run (technique owns survival)
   * @param advance     the duel will run (posture advances the line)
   */
  def run(unit: FriendlyUnitInfo, willRetreat: Boolean, advance: Boolean): Boolean = {
    if (unit.flying) return false

    if ( ! unit.unitClass.isWarrior) return false

    val hits = unit.matchups.targets.view.filter(_.visible).toSeq
    if (hits.isEmpty) return false

    val zergShown    = hits.exists(_.unitClass.isZerg)
    val terranShown  = hits.exists(_.unitClass.isTerran)
    val protossShown = hits.exists(_.unitClass.isProtoss)

    val air: Seq[(UnitInfo, VictimKind)] =
      if ( ! unit.unitClass.attacksAir) Seq.empty
      else victimsOf(unit,
        (if (zergShown) zergAir else Seq.empty) ++
          (if (terranShown) terranAir else Seq.empty) ++
          (if (protossShown) protossAir else Seq.empty),
        hits,
        flying = true)
    val ground: Seq[(UnitInfo, VictimKind)] =
      if ( ! unit.unitClass.attacksGround || ! protossShown) Seq.empty
      else victimsOf(unit, protossGround, hits, flying = false)
    val victims = (air ++ ground)
      // Dark Templars duel on terms: a nearly-ready gun, and room to
      // stand for ranged hulls - planting feet on top of the swipe
      // trades the volley for DT hits. Melee answers whenever warm.
      .filterNot { case (t, _) => Protoss.DarkTemplar(t) && ! CrucialAirSnipe.duelsDarkTemplar(
        unit.cooldownLeft,
        unit.unitClass.melee,
        unit.pixelDistanceEdge(t) * Maff.inv32) }
      .map { case (t, _) => t }.toVector
    if (victims.isEmpty) return false

    val marks = (air ++ ground).collect { case (t, kind) if victims.contains(t) => Mark(
      distTiles = unit.pixelDistanceEdge(t) * Maff.inv32,
      kind = kind,
      totalHealth = t.totalHealth) }
    val pick = CrucialAirSnipe.pick(marks.toSeq)
    pick.foreach(i => unit.agent.toAttack = Some(victims(i)))
    if (willRetreat || ! advance) {
      pick.foreach(i => NearestShot.take(unit, "AlwaysAttack", Seq(victims(i))))

      true
    } else false
  }

  /** Victims of one half of the sky: the row decides the ladder, the
    * Atlantis margin decides the walk-in. In range or at most a margin
    * past it counts; the kernel vetoes only fast runners. */
  private def victimsOf(
    unit: FriendlyUnitInfo,
    rows: Seq[Prey],
    hits: Seq[UnitInfo],
    flying: Boolean): Seq[(UnitInfo, VictimKind)] =
    rows.flatMap(row =>
      hits.view.filter(t => t.flying == flying && row.matches(t) && CrucialAirSnipe.inSnipeReach(
        row.kind,
        unit.inRangeToAttack(t),
        unit.canAttackWithMargin(CrucialAirSnipe.chaseMarginTiles, t),
        unit.isOtherUnitFacingUs(t))).map(t => (t, row.kind))).toSeq
}
