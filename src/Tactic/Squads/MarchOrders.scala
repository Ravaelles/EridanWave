package Tactic.Squads

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Formation.{FormationStyleDisengage, FormationStyleGuard}
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{LoneMarch, MainMarch, MarchDress, NoTankMarch, SquadRally, StrongMarch, SuicideMission, ToothlessMarch}
import Utilities.?

/**
 * March orders (extracted from SquadAutomation): where each squad unit
 * walks. One reason to change: the march rules (rally stragglers, close
 * ranks, dress leaders, send Reavers home, never hold a suicide charge).
 * Targeting stays in SquadAutomation, formations in Micro.Formation.
 */
object MarchOrders {

  def sendUnits(squad: Squad, units: Iterable[FriendlyUnitInfo], defaultReturn: Option[Pixel] = None): Unit = {
    // Common march reference: the pack mean distance to the squad goal, in tiles.
    // Leaders ahead of it wait (MarchDress); laggards march or rally back.
    val meanDistToGoalTiles = Maff.mean(units.view.map(_.pixelDistanceCenter(squad.vicinity))) / 32.0
    units.foreach(unit => sendUnit(squad, unit, meanDistToGoalTiles, defaultReturn))
  }

  private def sendUnit(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel]): Unit = {
    val finalReturn = getReturn(unit, squad, defaultReturn)
    val assault = SuicideMission.isActive(unit)
    // A toothless Terran fields nothing worth forming up against: combat
    // units skip every formation leg, including the trip home, and walk
    // straight at the goal instead.
    val toothless = ToothlessMarch.shouldMarchDirectly(
      unit.unitClass.isWarrior,
      With.enemies.exists(_.isTerran),
      With.units.enemy.exists(t => Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t)),
      With.units.enemy.exists(Terran.Vulture(_)))
    val (terminus, marchLabel) = marchTerminus(squad, unit, meanDistToGoalTiles, defaultReturn)
    Debugging.CherryVis.CherryVis.decision(unit.id,
      ?(squad.fightConsensus || assault || toothless, marchLabel, "Returning to slot"))
    unit.intend(squad)
      .setTerminus(?(squad.fightConsensus || assault || toothless, terminus, finalReturn))
      .setRedoubt(finalReturn)
  }

  // The march rules, innermost first: rallying stragglers beat slots,
  // closing ranks beats marching alone, dressing beats everything but
  // a suicide charge (which skips the dressing room entirely).
  // Dark Templars run their own hunt (visible or remembered contacts only):
  // they never dress, rally, or take formation slots, so they march
  // straight at the squad goal instead of posing with the line.
  private case class MarchGate(label: String, applies: (Squad, FriendlyUnitInfo) => Boolean)

  // First claim wins, in order; every gate marches straight at the squad
  // goal, so the terminus needs no per-gate branch. Suicide units commit
  // above (every unit, every frame) before the gates are even asked.
  private val marchGates: Seq[MarchGate] = Seq(
    // Dark Templars run their own hunt (visible or remembered contacts
    // only): they never dress, rally, or take formation slots.
    MarchGate("DarkTemplarHunt", (_, unit) => Protoss.DarkTemplar(unit)),
    // Main march: past 90 supply a unit sitting on our doorstep never
    // poses, rallies or walks to a slot - the column just walks out, so
    // the base stops choking on its own formation dance. No bases (UMS
    // micro maps) means no doorstep: skip quietly instead of asking
    // geography for a main that isn't there.
    MarchGate("MainMarch", (_, unit) => With.geography.ourBases.nonEmpty && MainMarch.shouldSkip(
      With.self.supplyUsed200,
      unit.pixel.groundPixels(With.geography.ourMain.heart.center) * Maff.inv32)),
    // Suicide mission: a ground warrior inside the enemy main never poses,
    // rallies or walks home - it commits (no retreat, attack only) and
    // marches straight at the squad goal past every formation leg below.
    MarchGate("SuicideMission", (_, unit) => SuicideMission.isActive(unit)),
    // Pair ride: a loaded Shuttle belongs to its pair, never to the pack -
    // routing cargo to a rally slot or a formation slot strands it while
    // the park/raid brains wait for a march that never comes. Empty hulls
    // still march the legs (they await the next ride, nothing to strand).
    MarchGate("PairRide", (_, unit) => Protoss.Shuttle(unit) && unit.loadedUnitCount > 0),
    // Strong march: a banked stock or a maxed strong army never poses -
    // it walks straight at the squad goal instead of rallying to the pack
    // centroid or marching to a formation slot.
    MarchGate("StrongMarch", (_, unit) => StrongMarch.shouldMarchDirectly(
      With.self.minerals,
      With.battles.globalAttack.us.skimStrengthTotal,
      With.self.supplyUsed200)),
    // No tanks, no dressing: Dragoons walk straight at a Terran without
    // siege lines instead of posing for guns that are not there.
    MarchGate("NoTankMarch", (_, unit) => NoTankMarch.shouldMarchDirectly(
      Protoss.Dragoon(unit),
      With.enemies.exists(_.isTerran),
      With.units.enemy.exists(t => Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t)))),
    // Toothless Terran, no formations at all: without Vultures or Tanks
    // combat units skip every march leg, including the trip home - the
    // pack just walks at the goal.
    MarchGate("ToothlessMarch", (_, unit) => ToothlessMarch.shouldMarchDirectly(
      unit.unitClass.isWarrior,
      With.enemies.exists(_.isTerran),
      With.units.enemy.exists(t => Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t)),
      With.units.enemy.exists(Terran.Vulture(_)))),
    // Lone march: with no friendly warrior inside ten tiles there is no
    // line to pose in, so the unit skips rallying to the pack centroid
    // and slot-marching and walks straight at the squad goal instead of
    // milling alone, and never while retreating (a falling-back unit
    // walks home, not at the squad goal). Latched (10.5 out, 9.5 back): the terminus on
    // either side lives hundreds of pixels apart, so a sharp gate jerks
    // the destination for units breathing on the boundary.
    MarchGate("LoneMarch", (squad, unit) =>
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
    MarchGate("StaticPush", (_, unit) =>
      unit.agent.shouldFight &&
      MarchDress.pushPastStatics(unit.id, Maff.minBy(unit.matchups.enemies.view.filter(e =>
        e.likelyStillThere
        && e.unitClass.isStaticDefense
        && e.unitClass.canAttack(unit)))(unit.tileDistanceEdge).map(unit.tileDistanceEdge).getOrElse(Double.PositiveInfinity))))

  private def marchTerminus(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel]): (Pixel, String) = {
    val suicide = SuicideMission.assign(unit)
    marchGates.collectFirst { case gate if gate.applies(squad, unit) => gate.label } match {
      case Some(label) => (squad.vicinity, label)
      case None =>
        val finalTravel = getTravel(unit, squad, defaultReturn)
        marchLegs(squad, unit, meanDistToGoalTiles, finalTravel, suicide)
    }
  }

  // The march legs: rallying stragglers beat slots, closing ranks beats
  // marching alone, dressing beats everything but a gated march above.
  private def marchLegs(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, finalTravel: Pixel, suicide: Boolean): (Pixel, String) = {
    val (finalMarch, rallying) = rallyLeg(squad, unit, finalTravel, suicide)
    val joinPixel              = joinLeg(squad, unit, suicide)
    val (holding, homing)      = dressLeg(squad, unit, meanDistToGoalTiles, suicide)
    val pack                   = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val terminus = ?(holding, unit.pixel, joinPixel.getOrElse(?(homing, pack, finalMarch)))
    val label    = ?(holding, "Waiting for pack", ?(rallying || homing, "Rallying to pack centroid", "Marching to formation slot"))
    (terminus, label)
  }

  // Straggler rally (Stardust): a unit lagging behind the marching pack
  // rejoins the pack centroid first instead of walking alone to its slot.
  // Returning units keep their own slots - they are meant to disperse.
  // Slow hulls (Reavers) rally on a longer leash so they keep their slot
  // and their shot instead of chasing the pack all march.
  private def rallyLeg(squad: Squad, unit: FriendlyUnitInfo, finalTravel: Pixel, suicide: Boolean): (Pixel, Boolean) = {
    val pack     = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val leash    = SquadRally.rallyRadiusFor(Protoss.Reaver(unit))
    val rallying = ! suicide && squad.fightConsensus && SquadRally.isRallying(unit.pixel, pack, leash)
    (?(rallying, pack, finalTravel), rallying)
  }

  // Close ranks on the march: a unit adrift from its nearest ally
  // rejoins instead of milling alone. Consensus-only, so guards and
  // defenders keep their posts instead of bunching up.
  private def joinLeg(squad: Squad, unit: FriendlyUnitInfo, suicide: Boolean): Option[Pixel] = {
    val nearestAlly  = Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel))
    val joiningAlly  = ! suicide && squad.fightConsensus && nearestAlly.exists(a => SquadRally.shouldJoinAlly(unit.tileDistanceCenter(a.pixel)))
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
    val waiting      = ! suicide && squad.fightConsensus && enemyNear && ! skipCompactStrong && ! staticNear && MarchDress.holdDress(unit.id, meanDistToGoalTiles - unit.tileDistanceCenter(squad.vicinity))
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
