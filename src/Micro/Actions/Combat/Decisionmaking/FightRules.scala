package Micro.Actions.Combat.Decisionmaking

/**
 * Flat snapshot of everything the fight-or-flee table looks at.
 *
 * Each field is one rule's whole condition, computed up front from game
 * state. Splitting fact-gathering (game-coupled, reviewed by reading) from
 * the decision (pure, covered by FightRulesSuite) is what makes the table
 * testable: the priority order is load-bearing for every skirmish, so it
 * gets pinned by tests instead of living only in a 30-line if-chain.
 */
final case class FightFacts(
  pacifist    : Boolean,
  cantFight   : Boolean,
  cantFlee    : Boolean,
  static      : Boolean,
  committed   : Boolean,
  yolo        : Boolean,
  irradiated  : Boolean,
  safe        : Boolean,
  detonated   : Boolean,
  hugTank     : Boolean,
  archon      : Boolean,
  coverFire   : Boolean,
  berzerk     : Boolean,
  cloaked     : Boolean,
  cloakable   : Boolean,
  lurking     : Boolean,
  towers      : Boolean,
  scourge     : Boolean,
  scarabless  : Boolean,
  birdless    : Boolean,
  noEnergy    : Boolean,
  disrupted   : Boolean,
  bideSiege   : Boolean,
  carrierVuln : Boolean,
  raze        : Boolean,
  miners      : Boolean,
  reave       : Boolean,
  energized   : Boolean)

object FightRules {

  /**
   * First matching rule wins, in the historical priority order.
   * None means no rule fired and the battle forecast decides instead.
   */
  def decide(f: FightFacts): Option[(Boolean, String)] = {
    if (f.pacifist)         Some((false, "Pacifist"))
    else if (f.cantFight)   Some((false, "CantFight"))
    else if (f.cantFlee)    Some((true,  "CantFlee"))
    else if (f.static)      Some((true,  "Static"))
    else if (f.committed)   Some((true,  "Committed"))
    else if (f.yolo)        Some((true,  "YOLO"))
    else if (f.irradiated)  Some((true,  "Irradiated"))
    else if (f.safe)        Some((true,  "Safe"))
    else if (f.detonated)   Some((true,  "Detonated"))
    else if (f.hugTank)     Some((true,  "HugTank"))
    else if (f.archon)      Some((true,  "Archon"))
    else if (f.coverFire)   Some((true,  "CoverFire"))
    else if (f.berzerk)     Some((true,  "Berzerk"))
    else if (f.cloaked)     Some((true,  "Cloaked"))
    else if (f.cloakable)   Some((true,  "Cloakable"))
    else if (f.lurking)     Some((true,  "Lurking"))
    else if (f.towers)      Some((true,  "Towers"))
    else if (f.scourge)     Some((false, "Scourge"))
    else if (f.scarabless)  Some((false, "Scarabless"))
    else if (f.birdless)    Some((false, "Birdless"))
    else if (f.noEnergy)    Some((false, "NoEnergy"))
    else if (f.disrupted)   Some((false, "Disrupted"))
    else if (f.bideSiege)   Some((false, "BideSiege"))
    else if (f.carrierVuln) Some((false, "CarrierVuln"))
    else if (f.raze)        Some((true,  "Raze"))
    else if (f.miners)      Some((true,  "Miners"))
    else if (f.reave)       Some((true,  "Reave"))
    else if (f.energized)   Some((true,  "Energized"))
    else None
  }
}
