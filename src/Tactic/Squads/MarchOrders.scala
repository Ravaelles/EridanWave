package Tactic.Squads

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Formation.{FormationStyleDisengage, FormationStyleGuard}
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{DragoonRoaming, LoneMarch, MainMarch, MarchDress, NoTankMarch, SquadRally, StrongMarch, SuicideMission, ToothlessMarch}
import Utilities.?

/**
 * March orders (extracted from SquadAutomation): where each squad unit
 * walks. One reason to change: the march rules (rally stragglers, close
 * ranks, dress leaders, send Reavers home, never hold a suicide charge).
 * Targeting stays in SquadAutomation, formations in Micro.Formation.
 */
object MarchOrders {

  /**
   * March labels, one per decision (gates, legs, trip home): a sealed
   * family instead of string literals, so a typo cannot silently fork
   * the trace. The rendered strings stay byte-for-byte identical.
   */
  sealed abstract class MarchLabel(val label: String) {
    override def toString: String = label
  }

  object MarchLabel {

    case object DarkTemplarHunt extends MarchLabel("DarkTemplarHunt")

    case object CorsairHunt extends MarchLabel("CorsairHunt")

    case object MainMarch extends MarchLabel("MainMarch")

    case object SuicideMission extends MarchLabel("SuicideMission")

    case object PairRide extends MarchLabel("PairRide")

    case object StrongMarch extends MarchLabel("StrongMarch")

    case object NoTankMarch extends MarchLabel("NoTankMarch")

    case object ToothlessMarch extends MarchLabel("ToothlessMarch")

    case object DragoonRoam extends MarchLabel("DragoonRoam")

    case object LoneMarch extends MarchLabel("LoneMarch")

    case object StaticPush extends MarchLabel("StaticPush")

    case object WaitingForPack extends MarchLabel("Waiting for pack")

    case object RallyingToPack extends MarchLabel("Rallying to pack centroid")

    case object MarchingToSlot extends MarchLabel("Marching to formation slot")

    case object ReturningToSlot extends MarchLabel("Returning to slot")
  }

  /**
   * World facts the gates read, measured once per `sendUnits` instead of once
   * per gate per unit: ten gates over forty units used to mean hundreds of
   * full enemy-list scans a frame on the march path.
   */
  private final case class MarchFacts(
    enemyIsTerran    : Boolean,
    enemyIsProtoss   : Boolean,
    enemyHasTanks    : Boolean,
    enemyHasVultures : Boolean,
    enemyDragoonSeen : Boolean,
    bankedMinerals   : Int,
    armyStrength     : Double,
    supplyUsed       : Int,
    home             : Option[Pixel]) {

    /** A toothless Terran fields nothing worth forming up against. */
    def enemyIsToothless: Boolean = enemyIsTerran && ! enemyHasTanks && ! enemyHasVultures
  }

  private def readFacts(): MarchFacts = {
    val home = if (With.geography.ourBases.isEmpty) None else Some(With.geography.ourMain.heart.center)
    MarchFacts(
      // Races from units on the board, not lobby slots: UMS maps field
      // several enemy races at once while only one ever shows up, and a
      // phantom race flips every race-gated march rule for the whole game.
      // Dragoon recognition is memory, not board: a scouted Dragoon stays
      // scouted, so the roaming permission closes once and for all.
      enemyIsTerran    = With.units.enemy.exists(_.unitClass.isTerran),
      enemyIsProtoss   = With.units.enemy.exists(_.unitClass.isProtoss),
      enemyHasTanks    = With.units.enemy.exists(t => Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t)),
      enemyHasVultures = With.units.enemy.exists(Terran.Vulture(_)),
      enemyDragoonSeen = MacroFacts.enemyHasShown(Protoss.Dragoon),
      bankedMinerals   = With.self.minerals,
      armyStrength     = With.battles.globalAttack.us.skimStrengthTotal,
      supplyUsed       = With.self.supplyUsed200,
      home             = home)
  }

  def sendUnits(squad: Squad, units: Iterable[FriendlyUnitInfo], defaultReturn: Option[Pixel] = None): Unit = {
    // Common march reference: the pack mean distance to the squad goal, in tiles.
    // Leaders ahead of it wait (MarchDress); laggards march or rally back.
    val meanDistToGoalTiles = Maff.mean(units.view.map(_.pixelDistanceCenter(squad.vicinity))) / 32.0
    val facts               = readFacts()
    units.foreach(unit => sendUnit(squad, unit, meanDistToGoalTiles, defaultReturn, facts))
  }

  private def sendUnit(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel], facts: MarchFacts): Unit = {
    val finalReturn = getReturn(unit, squad, defaultReturn)
    val assault = SuicideMission.isActive(unit)
    // A toothless Terran fields nothing worth forming up against: combat
    // units skip every formation leg, including the trip home, and walk
    // straight at the goal instead.
    val toothless = ToothlessMarch.shouldMarchDirectly(
      unit.unitClass.isWarrior,
      facts.enemyIsTerran,
      facts.enemyHasTanks,
      facts.enemyHasVultures)
    val (terminus, marchLabel) = marchTerminus(squad, unit, meanDistToGoalTiles, defaultReturn, facts)
    // Roaming Dragoons march like the toothless columns: with no enemy
    // Dragoon scouted there is nothing to dress against, so they walk at
    // the goal instead of posing, rallying or returning - and the march
    // never sends them home for lack of a consensus they do not need.
    val roam = marchLabel == MarchLabel.DragoonRoam
    Debugging.CherryVis.CherryVis.decision(unit.id,
      ?(squad.fightConsensus || assault || toothless || roam, marchLabel, MarchLabel.ReturningToSlot).label)
    Debugging.Probe.note("March", unit.id,
      f"${?(squad.fightConsensus || assault || toothless || roam, marchLabel, MarchLabel.ReturningToSlot)} terminus=${terminus.tx},${terminus.ty}")
    unit.intend(squad)
      .setTerminus(?(squad.fightConsensus || assault || toothless || roam, terminus, finalReturn))
      .setRedoubt(finalReturn)
      // The march-home verdict rides the frame-fresh intention: a unit the
      // march sent home must not re-engage in the unit phase. Consensus
      // reads agree within a frame, but the verdict can change hands
      // between the phases (re-squadding, HuntContact mode), and one
      // overwrite frame is enough to feed a charge into a retreat.
      .setMarchHome( ! (squad.fightConsensus || assault || toothless || roam))
  }

  // The march rules, innermost first: rallying stragglers beat slots,
  // closing ranks beats marching alone, dressing beats everything but
  // a suicide charge (which skips the dressing room entirely).
  // Dark Templars run their own hunt (visible or remembered contacts only):
  // they never dress, rally, or take formation slots, so they march
  // straight at the squad goal instead of posing with the line.
  private case class MarchGate(label: MarchLabel, applies: (MarchFacts, Squad, FriendlyUnitInfo) => Boolean)

  // First claim wins, in order; every gate marches straight at the squad
  // goal, so the terminus needs no per-gate branch. Suicide units commit
  // above (every unit, every frame) before the gates are even asked.
  private val marchGates: Seq[MarchGate] = Seq(
    // Dark Templars run their own hunt (visible or remembered contacts
    // only): they never dress, rally, or take formation slots.
    MarchGate(MarchLabel.DarkTemplarHunt, (_, _, unit) => Protoss.DarkTemplar(unit)),

    // Corsairs run their own hunt (flee, spread, dance, snipe): they
    // never dress, rally, or take formation slots, or they mill in
    // place instead of killing Overlords.
    MarchGate(MarchLabel.CorsairHunt, (_, _, unit) => Protoss.Corsair(unit)),
    // Main march: past 90 supply a unit sitting on our doorstep never
    // poses, rallies or walks to a slot - the column just walks out, so
    // the base stops choking on its own formation dance. No bases (UMS
    // micro maps) means no doorstep: skip quietly instead of asking
    // geography for a main that isn't there.
    MarchGate(MarchLabel.MainMarch, (facts, _, unit) => facts.home.exists(home => MainMarch.shouldSkip(
      facts.supplyUsed,
      unit.pixel.groundPixels(home) * Maff.inv32))),
    // Suicide mission: a ground warrior inside the enemy main never poses,
    // rallies or walks home - it commits (no retreat, attack only) and
    // marches straight at the squad goal past every formation leg below.
    MarchGate(MarchLabel.SuicideMission, (_, _, unit) => SuicideMission.isActive(unit)),
    // Pair ride: a loaded Shuttle belongs to its pair, never to the pack -
    // routing cargo to a rally slot or a formation slot strands it while
    // the park/raid brains wait for a march that never comes. Empty hulls
    // still march the legs (they await the next ride, nothing to strand).
    MarchGate(MarchLabel.PairRide, (_, _, unit) => Protoss.Shuttle(unit) && unit.loadedUnitCount > 0),
    // Strong march: a banked stock or a maxed strong army never poses -
    // it walks straight at the squad goal instead of rallying to the pack
    // centroid or marching to a formation slot.
    MarchGate(MarchLabel.StrongMarch, (facts, _, unit) => StrongMarch.shouldMarchDirectly(
      facts.bankedMinerals,
      facts.armyStrength,
      facts.supplyUsed)),
    // No tanks, no dressing: Dragoons walk straight at a Terran without
    // siege lines instead of posing for guns that are not there.
    MarchGate(MarchLabel.NoTankMarch, (facts, _, unit) => NoTankMarch.shouldMarchDirectly(
      Protoss.Dragoon(unit),
      facts.enemyIsTerran,
      facts.enemyHasTanks)),
    // Toothless Terran, no formations at all: without Vultures or Tanks
    // combat units skip every march leg, including the trip home - the
    // pack just walks at the goal.
    MarchGate(MarchLabel.ToothlessMarch, (facts, _, unit) => ToothlessMarch.shouldMarchDirectly(
      unit.unitClass.isWarrior,
      facts.enemyIsTerran,
      facts.enemyHasTanks,
      facts.enemyHasVultures)),
    // Early Dragoon roam: our Dragoons in a mirror with no enemy Dragoon
    // scouted skip every march leg, including the trip home - there is
    // nothing ranged to dress against, so they roam the map instead of
    // posing at the spawn. The permission closes on first recognition.
    MarchGate(MarchLabel.DragoonRoam, (facts, _, unit) => DragoonRoaming.mayRoam(
      Protoss.Dragoon(unit),
      facts.enemyIsProtoss,
      facts.enemyDragoonSeen)),
    // Lone march: with no friendly warrior inside ten tiles there is no
    // line to pose in, so the unit skips rallying to the pack centroid
    // and slot-marching and walks straight at the squad goal instead of
    // milling alone, and never while retreating (a falling-back unit
    // walks home, not at the squad goal). Latched (10.5 out, 9.5 back): the terminus on
    // either side lives hundreds of pixels apart, so a sharp gate jerks
    // the destination for units breathing on the boundary.
    MarchGate(MarchLabel.LoneMarch, (_, squad, unit) =>
      unit.agent.shouldFight &&
      Maff.minBy(squad.units.view.filter(u => u != unit && u.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
        .map(a => LoneMarch.marchAlone(unit.id, unit.pixelDistanceCenter(a.pixel) * Maff.inv32))
        .getOrElse(true)),
    // Static push: with an enemy static defense that can hit us inside
    // ten tiles there is no pack to rally to and no slot to march to -
    // milling in their reach trades the line for nothing, so the unit
    // walks straight at the squad goal instead. Latched per unit: the
    // gate itself used to flap with Rallying and Marching on the
    // boundary until the unit died milling instead of attacking.
    MarchGate(MarchLabel.StaticPush, (_, _, unit) =>
      unit.agent.shouldFight &&
      MarchDress.pushPastStatics(unit.id, Maff.minBy(unit.matchups.enemies.view.filter(e =>
        e.likelyStillThere
        && e.unitClass.isStaticDefense
        && e.unitClass.canAttack(unit)))(unit.tileDistanceEdge).map(unit.tileDistanceEdge).getOrElse(Double.PositiveInfinity))))

  private def marchTerminus(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel], facts: MarchFacts): (Pixel, MarchLabel) = {
    val suicide = SuicideMission.assign(unit)
    marchGates.collectFirst { case gate if gate.applies(facts, squad, unit) => gate.label } match {
      case Some(label) => (squad.vicinity, label)
      case None =>
        val finalTravel = getTravel(unit, squad, defaultReturn)
        marchLegs(squad, unit, meanDistToGoalTiles, finalTravel, suicide)
    }
  }

  // The march legs: rallying stragglers beat slots, closing ranks beats
  // marching alone, dressing beats everything but a gated march above.
  private def marchLegs(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, finalTravel: Pixel, suicide: Boolean): (Pixel, MarchLabel) = {
    val (finalMarch, rallying) = rallyLeg(squad, unit, finalTravel, suicide)
    val joinPixel              = joinLeg(squad, unit, suicide)
    val (holding, homing)      = dressLeg(squad, unit, meanDistToGoalTiles, suicide)
    val pack                   = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val terminus = ?(holding, unit.pixel, joinPixel.getOrElse(?(homing, pack, finalMarch)))
    val label    = ?(holding, MarchLabel.WaitingForPack, ?(rallying || homing, MarchLabel.RallyingToPack, MarchLabel.MarchingToSlot))
    (terminus, label)
  }

  // Straggler rally (Stardust): a unit lagging behind the marching pack
  // rejoins the pack centroid first instead of walking alone to its slot.
  // Returning units keep their own slots - they are meant to disperse.
  // Slow hulls (Reavers) rally on a longer leash so they keep their slot
  // and their shot instead of chasing the pack all march.
  // A retreating warrior never rallies: it walks its own way out instead
  // of rejoining the pack centroid it just fled - the line it left is
  // where the guns are. Non-warriors keep their legs (no verdict gates
  // what never fights).
  private def rallyLeg(squad: Squad, unit: FriendlyUnitInfo, finalTravel: Pixel, suicide: Boolean): (Pixel, Boolean) = {
    val pack     = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val leash    = SquadRally.rallyRadiusFor(Protoss.Reaver(unit))
    val rallying = ! suicide && squad.fightConsensus && SquadRally.mayRally(unit.unitClass.isWarrior, unit.agent.shouldFight) && SquadRally.isRallyingLatched(
      unit.id,
      unit.pixelDistanceCenter(pack) * Maff.inv32,
      leash * Maff.inv32)
    (?(rallying, pack, finalTravel), rallying)
  }

  // Close ranks on the march: a unit adrift from its nearest ally
  // rejoins instead of milling alone. Consensus-only, so guards and
  // defenders keep their posts instead of bunching up.
  private def joinLeg(squad: Squad, unit: FriendlyUnitInfo, suicide: Boolean): Option[Pixel] = {
    val nearestAlly  = Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel))
    val joiningAlly  = ! suicide && squad.fightConsensus && SquadRally.mayRally(unit.unitClass.isWarrior, unit.agent.shouldFight) && nearestAlly.exists(a => SquadRally.shouldJoinAlly(unit.tileDistanceCenter(a.pixel)))
    if (joiningAlly) nearestAlly.map(_.pixel) else None
  }

  // March dressing: a leader running ahead of the pack holds position so
  // the line arrives together instead of trickling in. Waiting zeroes the
  // stuck counter (tryingToMove is false), so Unstick stays out of it.
  // Reavers never hold: a slow hull waiting ahead of a stalled pack
  // freezes next to the enemy, so it walks back to the pack instead.
  private def dressLeg(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, suicide: Boolean): (Boolean, Boolean) = {
    // Fresh reinforcements never dress on an empty battlefield: with no
    // enemy inside twenty tiles there is no contact to dress for, so the
    // line marches instead of posing at the spawn. A strong compact pack
    // with no armed enemy in sight marches through for the same reason.
    val enemyNear = unit.matchups.enemies.exists(e =>
      e.likelyStillThere && MarchDress.enemyNear(unit.pixelDistanceEdge(e) * Maff.inv32))
    val skipCompactStrong = MarchDress.shouldSkipWait(
      squad.warriorsCasters.map(_.subjectiveValue).sum,
      Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel)).map(_.pixelDistanceCenter(unit.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      With.units.enemy.exists(e => e.visible && e.unitClass.canAttack))
    val staticNear = unit.matchups.enemies.exists(e =>
      e.likelyStillThere && e.unitClass.isStaticDefense && MarchDress.staticNear(unit.tileDistanceEdge(e)))
    // A crowd needs no line: with six friendlies inside four tiles the
    // formation already stands, so the leader marches instead of posing
    // for stragglers that are not straggling.
    val massed = MarchDress.isMassed(squad.units.count(u =>
      u != unit && unit.pixelDistanceCenter(u.pixel) <= MarchDress.massedTiles * 32.0))
    val waiting      = ! suicide && MarchDress.shouldHoldForPack(
      squad.fightConsensus,
      enemyNear,
      skipCompactStrong,
      staticNear,
      massed,
      MarchDress.holdDress(unit.id, meanDistToGoalTiles - unit.tileDistanceCenter(squad.vicinity)),
      unit.matchups.targetsInRange.nonEmpty)
    val reaverToArmy = Protoss.Reaver(unit) && MarchDress.holdDress(unit.id, meanDistToGoalTiles - unit.tileDistanceCenter(squad.vicinity))
    (waiting && ! reaverToArmy, reaverToArmy)
  }

  def getReturn(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel = squad
    .formations
    .find(f => f.style == FormationStyleGuard || f.style == FormationStyleDisengage)
    .filter(_.placements.contains(unit))
    .map(_.placements(unit))
    .orElse(defaultReturn)
    .getOrElse(squad.homeConsensus)

  def getTravel(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel =
    ?(squad.hasGround, None, Some(squad.vicinity))
      .orElse(
        squad
          .formations
          .headOption
          .find(_.placements.contains(unit))
          .map(_.placements(unit)))
      .getOrElse(?(squad.fightConsensus, squad.vicinity, getReturn(unit, squad, defaultReturn)))
}
