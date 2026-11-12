package Micro.Actions.Protoss.Zealot

import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
import Utilities.UnitFilters.IsWarrior

/**
 * How a rushing Zealot values the units around it: positive for what it can
 * beat, negative for what beats it. Pure scoring, no orders and no writes,
 * so the weights can be read (and tuned) apart from the decision flow.
 */
object ZealotRushScoring {

  private val typeStrength: Map[UnitClass, Double] = Map(
    (Terran.SCV,            0.3),
    (Terran.Marine,         0.4),
    (Terran.Vulture,        1.0),
    (Terran.Firebat,        0.6),
    (Terran.Bunker,         2.0),

    (Protoss.Probe,         0.3),
    (Protoss.Zealot,        1.0),
    (Protoss.Dragoon,       0.75),
    (Protoss.PhotonCannon,  1.75),

    (Zerg.Drone,            0.3),
    (Zerg.Zergling,         0.35),
    (Zerg.Hydralisk,        0.65),
    (Zerg.SunkenColony,     3.5)
  )

  /** Total value of the ground fight around the Zealot; above zero means we win it. */
  def fieldScore(zealot: FriendlyUnitInfo): Double =
    zealot.matchups
      .allUnits
      .filter(_.unitClass.attacksGround)
      .map(score(zealot, _))
      .sum

  def score(zealot: FriendlyUnitInfo, u: UnitInfo): Double = {
    val sign        = ?(u.isFriendly, 1, ?(u.isEnemy, -1, 0))
    val range       = proximity(zealot, u)
    val health      = u.totalHealth / u.unitClass.maxTotalHealth.toDouble
    val strength    = typeStrength.getOrElse(u.unitClass, u.unitClass.subjectiveValue / Protoss.Zealot.subjectiveValue)
    val fighting    = u.matchups.allies.filter(IsWarrior).exists(_.matchups.threatsInRange.nonEmpty)
    sign * range * health * strength * hysteresis(u, fighting) * busyness(u) * assisting(zealot)
  }

  /** 1 at our own range, falling to 0 four tiles past it. */
  private def proximity(zealot: FriendlyUnitInfo, u: UnitInfo): Double =
    Math.max(0, 1 - Math.max(0, u.pixelsToGetInRange(zealot) - zealot.matchups.pixelsToTargetRange.getOrElse(0d)) / 196.0)

  private def busyness(u: UnitInfo): Double =
    ?(u.gathering || u.constructing, 0.5, 1.0)

  /** A Zealot with more health than the weakest ally nearby is worth sending in. */
  private def assisting(zealot: FriendlyUnitInfo): Double = {
    val lowAllyHP = Maff.min(
      zealot.matchups.allies
        .filter(Protoss.Zealot)
        .filter(_.matchups.threatsInPixels(8).nonEmpty)
        .map(_.totalHealth.toDouble))
      .getOrElse(1000.0)
    Maff.clamp(zealot.totalHealth / Math.max(1.0, lowAllyHP), 1.0, 2.0)
  }

  /**
   * Stickiness on the enemy types this Zealot has been burned by: a ball
   * already trading is worth more than its raw strength, which keeps a
   * winning Zealot in the fight instead of re-picking targets every frame.
   */
  private def hysteresis(u: UnitInfo, fighting: Boolean): Double = {
    val marine    = u.matchups.threats.exists(_.isAny(Terran.Marine, Terran.Vulture))
    val zergling  = u.matchups.threats.exists(Zerg.Zergling)
    val dragoon   = u.matchups.threats.exists(Protoss.Dragoon)
    ?(u.isOurs,
      ?(marine,
        ?(fighting, 1.5, 0.8),
      ?(zergling,
        ?(fighting, 1.2, 0.8),
      ?(dragoon,
        ?(fighting, 3.0, 0.7),
        ?(fighting, 1.1, 0.9)))),
      1.0)
  }
}
