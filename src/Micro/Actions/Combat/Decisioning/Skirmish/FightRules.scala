package Micro.Actions.Combat.Decisioning.Skirmish

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
  pacifist    : Boolean = false,
  cantFight   : Boolean = false,
  cantFlee    : Boolean = false,
  static      : Boolean = false,
  committed   : Boolean = false,
  yolo        : Boolean = false,
  irradiated  : Boolean = false,
  safe        : Boolean = false,
  outOfContact: Boolean = false,
  detonated   : Boolean = false,
  hugTank     : Boolean = false,
  archon      : Boolean = false,
  coverFire   : Boolean = false,
  berzerk     : Boolean = false,
  cloaked     : Boolean = false,
  cloakable   : Boolean = false,
  lurking     : Boolean = false,
  towers      : Boolean = false,
  scourge     : Boolean = false,
  scarabless  : Boolean = false,
  birdless    : Boolean = false,
  noEnergy    : Boolean = false,
  disrupted   : Boolean = false,
  bideSiege   : Boolean = false,
  carrierVuln : Boolean = false,
  raze        : Boolean = false,
  miners      : Boolean = false,
  hallTargeted: Boolean = false,
  tankHunt    : Boolean = false,
  carrierBrawl: Boolean = false,
  reave       : Boolean = false,
  energized   : Boolean = false,
  dragoonBrave: Boolean = false)

object FightRules {

  /** No enemy within this many tiles beyond its firing range of any squad
    * unit means contact is lost: disengage unconditionally and reform. */
  val contactMarginTiles: Double = 1.5

  /**
   * First matching rule wins, in the historical priority order.
   * None means no rule fired and the battle forecast decides instead.
   * Groups partition the order unchanged; orElse keeps the priority.
   */
  def decide(f: FightFacts): Option[(Boolean, String)] =
    cannotFight(f)
      .orElse(willToFight(f))
      .orElse(contactState(f))
      .orElse(cloakAndTricks(f))
      .orElse(noAmmunition(f))
      .orElse(objectives(f))

  private def cannotFight(f: FightFacts): Option[(Boolean, String)] = {
    if (f.pacifist)         Some((false, "Pacifist"))
    else if (f.cantFight)   Some((false, "CantFight"))
    else if (f.cantFlee)    Some((true,  "CantFlee"))
    else if (f.static)      Some((true,  "Static"))
    else None
  }

  private def willToFight(f: FightFacts): Option[(Boolean, String)] = {
    if (f.committed)        Some((true,  "Committed"))
    else if (f.yolo)        Some((true,  "YOLO"))
    else if (f.irradiated)  Some((true,  "Irradiated"))
    else if (f.safe)        Some((true,  "Safe"))
    else if (f.tankHunt)     Some((true,  "TankHunt"))
    else if (f.carrierBrawl) Some((true,  "CarrierBrawl"))
    else if (f.dragoonBrave) Some((true,  "DragoonBrave"))
    else None
  }

  private def contactState(f: FightFacts): Option[(Boolean, String)] = {
    // Ordered onto the enemy hall, the fight is not over just because
    // nothing can hit us: walk the hall down instead of reforming.
    if (f.outOfContact && ! f.hallTargeted) Some((false, "OutOfContact"))
    else if (f.detonated)   Some((true,  "Detonated"))
    else if (f.hugTank)     Some((true,  "HugTank"))
    else if (f.archon)      Some((true,  "Archon"))
    else if (f.coverFire)   Some((true,  "CoverFire"))
    else if (f.berzerk)     Some((true,  "Berzerk"))
    else None
  }

  private def cloakAndTricks(f: FightFacts): Option[(Boolean, String)] = {
    if (f.cloaked)          Some((true,  "Cloaked"))
    else if (f.cloakable)   Some((true,  "Cloakable"))
    else if (f.lurking)     Some((true,  "Lurking"))
    else if (f.towers)      Some((true,  "Towers"))
    else if (f.scourge)     Some((false, "Scourge"))
    else None
  }

  private def noAmmunition(f: FightFacts): Option[(Boolean, String)] = {
    if (f.scarabless)       Some((false, "Scarabless"))
    else if (f.birdless)    Some((false, "Birdless"))
    else if (f.noEnergy)    Some((false, "NoEnergy"))
    else if (f.disrupted)   Some((false, "Disrupted"))
    else if (f.bideSiege)   Some((false, "BideSiege"))
    else if (f.carrierVuln) Some((false, "CarrierVuln"))
    else None
  }

  private def objectives(f: FightFacts): Option[(Boolean, String)] = {
    if (f.raze)             Some((true,  "Raze"))
    else if (f.miners)      Some((true,  "Miners"))
    else if (f.reave)       Some((true,  "Reave"))
    else if (f.energized)   Some((true,  "Energized"))
    else None
  }
}
