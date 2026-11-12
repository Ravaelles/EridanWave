package Micro.Actions.Protoss.Zealot

/**
 * Zealot duel verdict: four branches that used to write fightReason and
 * shouldFight each on their own now flow through one decision. Pure
 * priority, so the duel table can be argued with in a test; the game
 * readings stay in BeZealotVsZealot.
 */
object DuelDecision {

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
