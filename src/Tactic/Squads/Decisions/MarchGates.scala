package Tactic.Squads.Decisions

/**
 * The march gate table (pure predicates over facts, no game access):
 * first claim wins, in order. Every gate marches straight at the squad
 * goal, so the terminus needs no per-gate branch. Suicide units commit
 * above (every unit, every frame) before the gates are even asked.
 * Retune by moving whole gates, never by tweaking one gate's thresholds
 * against another's - order is the contract, and the order table suite
 * pins it row by row.
 */
object MarchGates {

  private case class MarchGate(label: MarchLabel, applies: (MarchFacts, MarchUnit) => Boolean)

  private val gates: Seq[MarchGate] = Seq(
    // Dark Templars run their own hunt (visible or remembered contacts
    // only): they never dress, rally, or take formation slots.
    MarchGate(MarchLabel.DarkTemplarHunt, (_, unit) => unit.isDarkTemplar),

    // Corsairs run their own hunt (flee, spread, dance, snipe): they
    // never dress, rally, or take formation slots, or they mill in
    // place instead of killing Overlords.
    MarchGate(MarchLabel.CorsairHunt, (_, unit) => unit.isCorsair),
    // Main march: past 90 supply a unit sitting on our doorstep never
    // poses, rallies or walks to a slot - the column just walks out, so
    // the base stops choking on its own formation dance. No bases (UMS
    // micro maps) means no doorstep: skip quietly instead of asking
    // geography for a main that isn't there.
    MarchGate(MarchLabel.MainMarch, (facts, unit) => unit.distHomeTiles.exists(dist =>
      MainMarch.shouldSkip(facts.supplyUsed, dist))),
    // Suicide mission: a ground warrior inside the enemy main never poses,
    // rallies or walks home - it commits (no retreat, attack only) and
    // marches straight at the squad goal past every formation leg below.
    MarchGate(MarchLabel.SuicideMission, (_, unit) => unit.isSuicide),
    // Pair ride: a loaded Shuttle belongs to its pair, never to the pack -
    // routing cargo to a rally slot or a formation slot strands it while
    // the park/raid brains wait for a march that never comes. Empty hulls
    // still march the legs (they await the next ride, nothing to strand).
    MarchGate(MarchLabel.PairRide, (_, unit) => unit.isLoadedShuttle),
    // Strong march: a banked stock or a maxed strong army never poses -
    // it walks straight at the squad goal instead of rallying to the pack
    // centroid or marching to a formation slot.
    MarchGate(MarchLabel.StrongMarch, (facts, _) => StrongMarch.shouldMarchDirectly(
      facts.bankedMinerals,
      facts.armyStrength,
      facts.supplyUsed)),
    // Toothless Terran, no formations at all: without Vultures or Tanks
    // combat units skip every march leg, including the trip home - the
    // pack just walks at the goal.
    MarchGate(MarchLabel.ToothlessMarch, (facts, unit) => ToothlessMarch.shouldMarchDirectly(
      unit.isWarrior,
      facts.enemyIsTerran,
      facts.enemyHasTanks,
      facts.enemyHasVultures)),
    // Early Dragoon roam: our Dragoons in a mirror with no enemy Dragoon
    // scouted skip every march leg, including the trip home - there is
    // nothing ranged to dress against, so they roam the map instead of
    // posing at the spawn. The permission closes on first recognition.
    MarchGate(MarchLabel.DragoonRoam, (facts, unit) => DragoonRoaming.mayRoam(
      unit.isDragoon,
      facts.enemyIsProtoss,
      facts.enemyDragoonSeen)),
    // Lone march: with no friendly warrior inside ten tiles there is no
    // line to pose in, so the unit skips rallying to the pack centroid
    // and slot-marching and walks straight at the squad goal instead of
    // milling alone, and never while retreating (a falling-back unit
    // walks home, not at the squad goal). Latched (10.5 out, 9.5 back): the terminus on
    // either side lives hundreds of pixels apart, so a sharp gate jerks
    // the destination for units breathing on the boundary.
    MarchGate(MarchLabel.LoneMarch, (_, unit) =>
      unit.shouldFight &&
      unit.nearestWarriorTiles.map(t => LoneMarch.marchAlone(unit.unitId, t)).getOrElse(true)),
    // Static push: with an enemy static defense that can hit us inside
    // ten tiles there is no pack to rally to and no slot to march to -
    // milling in their reach trades the line for nothing, so the unit
    // walks straight at the squad goal instead. Latched per unit: the
    // gate itself used to flap with Rallying and Marching on the
    // boundary until the unit died milling instead of attacking.
    MarchGate(MarchLabel.StaticPush, (_, unit) =>
      unit.shouldFight &&
      MarchDress.pushPastStatics(unit.unitId, unit.nearestStaticTiles.getOrElse(Double.PositiveInfinity))))

  /** First claim wins, in order; nothing when no gate fires. */
  def select(facts: MarchFacts, unit: MarchUnit): Option[MarchLabel] =
    gates.collectFirst { case gate if gate.applies(facts, unit) => gate.label }

  /**
   * Gate order as data (one explicit list): the sequence below is the
   * contract, pinned beside the behavioral rows by MarchGateOrderSuite.
   * Reorder by moving a MarchGate line, disable by commenting one out.
   * March gates arbitrate one domain (the march label), so they carry
   * no duel bands - first claim wins, in this order.
   */
  def order: Seq[MarchLabel] =
    gates.map(_.label)
}
