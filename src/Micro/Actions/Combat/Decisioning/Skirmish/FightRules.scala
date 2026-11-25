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
  dragoonBrave: Boolean = false,
  dragoonPack: Boolean = false,
  outmatchedGoon: Boolean = false,
  loneScout     : Boolean = false,
  staticOutgunned: Boolean = false,
  homeStand   : Boolean = false,
  tanksAtNatural : Boolean = false,
  reaverBrave : Boolean = false,
  sunkenTrap  : Boolean = false)

object FightRules {

  /** No enemy within this many tiles beyond its firing range of any squad
    * unit means contact is lost: disengage unconditionally and reform. */
  val contactMarginTiles: Double = 1.5

  /** Static cover counts this far past its own reach, in tiles: a duel
    * about to step under the guns reads as covered before it bleeds. */
  val staticCoverMarginTiles: Double = 1.5

  /**
   * Sunken trap, in tiles: a blade this close to a live Sunken is dead -
   * the colony outranges the swing, so standing trades the hull for
   * nothing and the line walks home instead.
   */
  val sunkenTrapTiles: Double = 7.1

  def sunkenTrap(melee: Boolean, sunkenTiles: Double): Boolean =
    melee && sunkenTiles < sunkenTrapTiles

  /**
   * First matching rule wins, in the historical priority order.
   * None means no rule fired and the battle forecast decides instead.
   * Groups partition the order unchanged; orElse keeps the priority.
   */
  def decide(f: FightFacts): Option[(Boolean, String)] =
    cannotFight(f)
      .orElse(willToFight(f))
      .orElse(outmatched(f))
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
    // Overlaps dragoonBrave on all-Marine contact (both true); first wins,
    // same verdict either way - the pack rule only widens the conditions.
    else if (f.dragoonPack) Some((true,  "DragoonPack"))
    else if (f.homeStand)   Some((true,  "HomeStand"))
    else if (f.tanksAtNatural) Some((true,  "TanksAtNatural"))
    else if (f.reaverBrave) Some((true,  "ReaverBrave"))
    else None
  }

  /**
   * Outmatched early mirrors stand down: an outnumbered handful of
   * Dragoons facing enemy Dragoons breaks off instead of trading into
   * a superior line; a lone Scout in a Protoss mirror holds with the
   * line instead of feeding a Carrier alone. A lone hull trading equal
   * range under enemy static cover breaks off too - without a reach
   * edge the cover's DPS decides every trade (BetaStar: 1v2 goons under
   * cannons). Range advantage still fights (goons over Marines); explicit
   * fight reasons keep their say below as above - these only stop the
   * anonymous advance.
   */
  private def outmatched(f: FightFacts): Option[(Boolean, String)] =
    if (f.outmatchedGoon) Some((false, "OutmatchedGoon"))
    else if (f.loneScout) Some((false, "LoneScout"))
    else if (f.staticOutgunned) Some((false, "StaticOutgunned"))
    // A lone blade under guns it cannot outrange does not wait for the
    // pack math: the static cover above needs outnumbering, but a solo
    // hull chewing a Sunken donates itself either way.
    else if (f.sunkenTrap) Some((false, "SunkenTrap"))
    else None

  private def contactState(f: FightFacts): Option[(Boolean, String)] = {    // Ordered onto the enemy hall, the fight is not over just because
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
