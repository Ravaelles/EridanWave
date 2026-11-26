package Micro.Actions.Protoss.Zealot

import Utilities.?

/**
 * Zealot duel verdict: four branches that used to write fightReason and
 * shouldFight each on their own now flow through one decision. Pure
 * priority, so the duel table can be argued with in a test; the game
 * readings stay in BeZealotVsZealot.
 */
object DuelDecision {

  /** Attackers on one hull that rout it (ladder): two blades on you is a donation. */
  val outnumberedLadder: Int = 1

  /** UMS double fight margin (owner order): micro maps tolerate twice
    * the press - four blades to rout, never two. Retreats still fire
    * when truly mobbed, at double the count. */
  val outnumberedUms: Int = 3

  def outnumberedLimit(umsMode: Boolean): Int =
    ?(umsMode, outnumberedUms, outnumberedLadder)

  def outnumberedBy(attackers: Int, umsMode: Boolean): Boolean =
    attackers > outnumberedLimit(umsMode)

  /** Hull hp that reads dying (ladder): below this a bleeding blade runs. */
  val dyingHpLadder: Int = 14

  /** UMS double fight margin (owner order): half the hp to call it dying. */
  val dyingHpUms: Int = 7

  def dyingBelowHp(umsMode: Boolean): Int =
    ?(umsMode, dyingHpUms, dyingHpLadder)

  sealed trait DuelVerdict

  case object Outnumbered extends DuelVerdict

  case object Dying extends DuelVerdict

  case object Duelling extends DuelVerdict

  case object Outnumbering extends DuelVerdict

  case object Undecided extends DuelVerdict

  final case class DuelFacts(
    outnumbered : Boolean,
    dying       : Boolean,
    duelling    : Boolean,
    outnumbering: Boolean)

  /** First matching rule wins, in the historical priority order. */
  def decide(f: DuelFacts): DuelVerdict = {
    if (f.outnumbered)        Outnumbered
    else if (f.dying)         Dying
    else if (f.duelling)      Duelling
    else if (f.outnumbering)  Outnumbering
    else                      Undecided
  }
}
