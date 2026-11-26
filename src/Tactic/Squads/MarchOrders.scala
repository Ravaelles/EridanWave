package Tactic.Squads

import Information.Geography.Chokes
import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import Micro.Agency.Commander
import Micro.Formation.{FormationStyleDisengage, FormationStyleGuard}
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{AttackModes, CatchUpMarch, ClusterPosture, FormationReadiness, MarchDress, MarchFacts, MarchGates, MarchLabel, MarchUnit, MotionVerdict, RetreatExtraPenalties, RetreatVerdict, SlotDiscipline, SquadRally, SuicideMission, ToothlessMarch, UmsHold, WithdrawAnchor}
import Utilities.?

/**
 * March orders: where each squad unit walks. One reason to change: the
 * march rules (rally stragglers, close ranks, dress leaders, send Reavers
 * home, never hold a suicide charge). For target strata see
 * SquadTargeting, for arcs Micro.Formation.
 */
object MarchOrders {

  private def readFacts(): MarchFacts = {
    val home = if (With.geography.ourBases.isEmpty) None else Some(With.geography.ourMain.heart.center)
    // Catch-up anchor: the nearest ground enemy to the alpha centroid,
    // measured once - every reinforcement compares against this one pixel.
    // No ground enemies, no anchor, no catch-up: the legs already march
    // when there is nothing to dress for.
    val alphaCentroid = With.tactics.attackSquad.centroidGround
    val catchUpAnchor = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && ! e.flying))(_.pixelDistanceCenter(alphaCentroid)).map(_.pixel)
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
      armyUnitsScore   = Information.Battles.ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthUnits,
        With.battles.globalAttack.enemy.skimStrengthUnits),
      supplyUsed       = With.self.supplyUsed200,
      proxyRush        = With.enemies.exists(_.isTerran) && With.fingerprints.proxyRax(),
      home             = home,
      catchUpAnchor    = catchUpAnchor,
      alphaAnchorTiles = catchUpAnchor.map(a => alphaCentroid.pixelDistance(a) * Maff.inv32).getOrElse(Double.PositiveInfinity))
  }

  def sendUnits(squad: Squad, units: Iterable[FriendlyUnitInfo], defaultReturn: Option[Pixel] = None): Unit = {
    // Common march reference: the pack mean distance to the squad goal, in tiles.
    // Leaders ahead of it wait (MarchDress); laggards march or rally back.
    val meanDistToGoalTiles = Maff.mean(units.view.map(_.pixelDistanceCenter(squad.vicinity))) / 32.0
    val facts               = readFacts()
    units.foreach(unit => sendUnit(squad, unit, meanDistToGoalTiles, defaultReturn, facts))
  }

  private def sendUnit(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel], facts: MarchFacts): Unit = {
    // Single writer per frame: during re-squadding a unit sits in two squads'
    // lists and both would march it - two March probes, last-writer-wins
    // destination. Only the current owner sends; unassigned hulls march.
    if (unit.squad.exists(_ != squad)) return

    val finalReturn = getReturn(unit, squad, defaultReturn)
    val assault = SuicideMission.held(unit)
    // A toothless Terran fields nothing worth dressing against: the
    // posture vote below attacks instead of posing, and the legs walk
    // the line out instead of trickling it.
    val toothless = ToothlessMarch.shouldForceAttack(
      unit.unitClass.isWarrior,
      facts.enemyIsTerran,
      facts.enemyHasTanks,
      facts.enemyHasVultures)
    val (terminus, marchLabel) = marchTerminus(squad, unit, meanDistToGoalTiles, defaultReturn, facts)
    // Posture marches: an attacking or regrouping cluster walks out
    // dressed instead of sitting on its return slots - the Roam vote
    // below is posture now, and the legs stay inert without consensus,
    // so the column moves without dancing.
    val marching = squad.fightConsensus || postureMarches(squad) || assault || toothless
    // Pair shuttles never march home: an empty ride that awaits, escorts
    // or rejoins belongs with the army, and marching it to a slot strands
    // the next Reaver exactly like the old fetch bug did. Loaded hulls
    // already march at the goal via PairRide; the legs dress the rest
    // near the pack instead. (Bait rides still bait: the brain's forceTo
    // outranks the march terminus either way.)
    val holdShuttle = Protoss.Shuttle(unit) && unit.loadedUnitCount == 0
    // Honest labels: marching home with no fight is not returning to a
    // slot. The slot return only names the trip home under contact.
    val returning = ! marching && ! holdShuttle && unit.matchups.enemies.exists(e =>
      e.likelyStillThere && MarchDress.enemyNear(unit.pixelDistanceEdge(e) * Maff.inv32))
    val marchHome = ?(marching || holdShuttle, marchLabel, ?(returning, MarchLabel.ReturningToSlot, MarchLabel.MarchingHome))
    // Retreats carry their reason in parens on the visible march label:
    // a stronger army routing with no enemy in front reads what it
    // voted and at what odds, instead of a bare Retreating.
    val marchReason =
      if (MarchLabel.isRetreating(marchHome))
        "Retreating(" + RetreatVerdict.retreatLabel(
          unit.agent.fightReason,
          With.battles.globalAttack.us.skimStrengthUnits,
          With.battles.globalAttack.enemy.skimStrengthUnits) + ")"
      else marchHome.label
    Debugging.CherryVis.CherryVis.decision(unit.id, marchReason)
    Debugging.Probe.note("March", unit.id,
      f"$marchHome terminus=${terminus.tx},${terminus.ty}")
    unit.squadIntend(squad)
      .setTerminus(?(marching || holdShuttle, terminus, finalReturn))
      .setRedoubt(finalReturn)
      // The march-home verdict rides the frame-fresh intention: a unit the
      // march sent home must not re-engage in the unit phase. Consensus
      // reads agree within a frame, but the verdict can change hands
      // between the phases (re-squadding, HuntContact mode), and one
      // overwrite frame is enough to feed a charge into a retreat.
      // Retreating counts as marching home even when the consensus
      // marches on: the label already says home, so the flag must agree
      // with it instead of opening the duel dispatch behind its back.
      .setMarchHome( ! marching || MarchLabel.isRetreating(marchLabel))
    // The opening stand owns the frame outright: the line holds instead
    // of walking, dueling or retreating, so the chain carries the
    // owner's name instead of the vote it overruled. One credit, from
    // the march that owns the destination - legs stay silent behind it.
  }

  // The march rules, innermost first: rallying stragglers beat slots,
  // closing ranks beats marching alone, dressing beats everything but
  // a gated march above. The gate table holds travel overrides (lone
  // and main-march columns walk at the goal instead of rallying home),
  // missions (suicide, pair rides) and the slotless hunters - battle
  // squadIntent lives in the cluster posture now, not in gate-skipped legs.
  // Order is load-bearing (first gate wins, like every first-claims
  // chain in the tree).
  // The gate table lives in Decisions.MarchGates (pure predicates over
  // MarchFacts + MarchUnit, first claim wins); the order table suite
  // pins the sequence row by row.

  /**
   * Far dress-and-wait probe (UMS mirrors, no reavers): arriving at five
   * tiles outside gun reach the line dresses and waits for the enemy
   * ball instead of trickling into it. Cheap gates first so ladder
   * games pay nothing: the scans run only while UMS Protoss stands.
   */
  /**
   * Far dress-and-wait read, shared with the technique: the dispatch
   * pauses the duel (advance) while the march dresses the line, so the
   * legs and the technique never split the frame between waiting and
   * charging. Same latch the march ticks - stateless per tick, so a
   * second read in the unit phase agrees by construction.
   */
  def farWaitHolding(unit: FriendlyUnitInfo): Boolean = {
    // Player records stay empty on UMS (nothing ever assigns them), so
    // read the race off seen units instead: blind at spawn, exact later.
    // Eyes and guns never dress the line: the gap reads warriors only,
    // so an observer (or a cannon) never holds a column that should
    // march past it.
    if ( ! With.umsMode) return false
    if ( ! With.units.enemy.exists(_.unitClass.isProtoss)) return false
    if (With.units.enemy.exists(e => e.alive && Protoss.Reaver(e))) return false

    val gapTiles = Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.unitClass.isWarrior))(_.pixelDistanceEdge(unit))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity)

    MarchDress.holdFarWait(
      unit.id,
      unit.unitClass.isWarrior,
      umsMode = true,
      enemyProtoss = true,
      enemyReaverKnown = false,
      gapTiles,
      unit.pixelRangeMax * Maff.inv32)
  }

  /** Creep step toward the nearest enemy, every couple dozen frames. */
  private def farCreepPixel(unit: FriendlyUnitInfo): Pixel =
    if (MarchDress.creepNow(With.frame))
      Maff.minBy(With.units.enemy.view.filter(_.alive))(_.pixelDistanceEdge(unit))
        .map(foe => unit.pixel.project(foe.pixel, MarchDress.creepTiles * 32.0).walkablePixel)
        .getOrElse(unit.pixel)
    else unit.pixel

  /**
   * Assembly read, shared by the march, the motion verdict and the
   * technique: while the squad drills (Forming) every hull ahead of the
   * rear rank falls back to it and the rear holds the anchor, so the
   * line meets the enemy shoulder-to-shoulder instead of trickling in.
   * Stragglers behind the rear keep their rally, contact hulls keep
   * their duel, sacred ground and flanks never assemble. True while
   * this hull drills (movement or hold), false when another leg owns it.
   */
  def assemblyHolding(unit: FriendlyUnitInfo): Boolean =
    unit.squad.exists(squad => assembleFor(squad, unit)._1)

  private def assembleFor(squad: Squad, unit: FriendlyUnitInfo): (Boolean, Pixel, Boolean) = {
    if ( ! unit.unitClass.isWarrior || ! unit.canMove || unit.agent.isScout || unit.flying) return (false, unit.pixel, false)

    if (Protoss.DarkTemplar(unit)) return (false, unit.pixel, false)

    if (Micro.Actions.Combat.Decisioning.Skirmish.NoRetreat.holdsAny(unit)) return (false, unit.pixel, false)

    if (SuicideMission.assign(unit)) return (false, unit.pixel, false)

    if (unit.formationEngage.exists(_.flanking)) return (false, unit.pixel, false)

    if (FormationReadiness.update(squad, With.frame) != FormationReadiness.Forming) return (false, unit.pixel, false)

    if (unit.matchups.targetsInRange.nonEmpty || unit.matchups.threatsInRange.nonEmpty) return (false, unit.pixel, false)

    val mates = squad.units.filter(o => o.alive && o.unitClass.isWarrior && ! o.flying)
    if (mates.size < 2) return (false, unit.pixel, false)

    val rearCoord = mates.map(_.tileDistanceCenter(squad.vicinity)).max
    val myCoord = unit.tileDistanceCenter(squad.vicinity)
    if (myCoord > rearCoord + FormationReadiness.rearBandTiles) return (false, unit.pixel, false)

    val step = FormationReadiness.backStepTiles(myCoord, rearCoord)
    if (step <= 0) {
      unit.agent.act("Forming")

      return (true, unit.pixel, false)
    }

    Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_)) match {
      case None      => (true, unit.pixel, false)
      case Some(foe) =>
        unit.agent.act("Forming")
        val away = foe.pixel.radiansTo(unit.pixel)
        (true, unit.pixel.radiateRadians(away, step * 32.0).walkablePixel, true)
    }
  }
  private def marchTerminus(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel], facts: MarchFacts): (Pixel, MarchLabel) = {
    val suicide = SuicideMission.assign(unit)
    // The verdict postdates the read (it needs the armed scan below),
    // so the gate reads it through a copy: the duel and retreat facts
    // ride the same unit, never two vintages.
    val marchUnit = MarchUnit.read(squad, unit, facts, verdictRetreat = false)
    // First verdict reader (step 2): the march ticks the shared bus with
    // the unified raw inputs and obeys the held verdict instead of
    // re-deriving retreat. Same vintage as before (shouldFight arrives
    // from the unit phase); only the inputs unify - a committed hull or
    // a storm-holding Templar no longer marches home under a technique
    // that walks or duels beside it.
    // Weak far expedition (owner order): a thin force deep out counts
    // three quarters of its skim, so it walks home instead of trading.
    // Same penalized score on all three arms, or the trio drifts apart.
    val expeditionScore = RetreatExtraPenalties.expeditionScore(facts.armyUnitsScore)
    RetreatVerdict.update(
      unit.id,
      (RetreatVerdict.shouldHold(
        unit.unitClass.isWarrior,
        Protoss.Reaver(unit),
        unit.squadIntent.canFlee,
        Protoss.HighTemplar(unit) && PsionicStorm.canCast(unit),
        unit.agent.shouldFight,
        unit.totalHealth,
        expeditionScore,
        marchUnit.nearestArmedTiles,
        facts.proxyRush,
        MacroFacts.enemyHardCounterKnown,
        Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
          .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity)) ||
        // PvZ handful retreats to the natural choke: below ten hulls and
        // two hundred skim the line falls back and receives the wave
        // there instead of trading in the open. Same gate on both arms.
        // A weak handful holds home outright, enemy sighted or not:
        // marching blind out of the base donates the wave before it
        // arrives, and no consensus outranks the empty roster - only
        // recovery releases it. Ladder homes only: UMS duels are always
        // joined, never judged.
        ! With.umsMode && (Tactic.Squads.Decisions.NaturalChokeRetreat.shouldRetreat(
          With.enemies.exists(_.isZerg),
          With.units.ours.count(u => u.alive && u.unitClass.isWarrior),
          facts.armyUnitsScore,
          marchUnit.nearestArmedTiles) ||
          Tactic.Squads.Decisions.NaturalChokeRetreat.shouldHoldHome(
            With.enemies.exists(_.isZerg),
            With.units.ours.count(u => u.alive && u.unitClass.isWarrior),
            facts.armyUnitsScore))) &&
        // PvP proxy defense owns both verdict arms: with few hulls home
        // and an enemy ground army already inside, neither phase walks
        // the line home - the march and the technique read the same gate.
        ! Micro.Actions.Combat.Decisioning.Skirmish.NoRetreat.proxyDefense() &&
        // Forbidden retreat owns them too: an enemy on or past our
        // natural choke with our hull inside dueling range fights where
        // it stands instead of walking home through the enemy.
        ! Micro.Actions.Combat.Decisioning.Skirmish.NoRetreat.retreatForbidden(unit) &&
        ! RetreatVerdict.suppressUmsOpening(With.umsMode, With.frame),
      With.frame)
    // An overwhelming line marches instead of going home: the same
    // release that frees the technique owns the march, or the two
    // phases split the frame between home and the duel.
    val verdictRetreat = RetreatVerdict.heldArmy(With.frame) &&
      ! ClusterPosture.overrulesRetreat(squad.battlePosture, facts.armyUnitsScore)
    // Stage 2 motion verdict: tick the desired owner from the fresh duel
    // and dressing scans. The gates read the latched side instead of
    // re-deriving their own; the legs always march, the technique duels
    // through its own orders. Upgrades preempt sharp (contact answers
    // the same frame the march sees it), downgrades wait out the
    // release, so a one-frame target flicker never marches the duel
    // out of the map.
    // Catch-up (nadrzędna): a reinforcement trailing the alpha joins at
    // full speed - a far assigned target is a march, not a duel, so the
    // verdict reads Advance and both phases march instead of dancing.
    // Nearby teeth still own the frame (hittable in reach, fire incoming,
    // statics owning their reach); retreat outranks everything. Same
    // predicate the gate reads, ticked once here: the latches are
    // idempotent within the frame, so gate and verdict agree.
    val catchUpFullSpeed = marchUnit.shouldFight && ! verdictRetreat && ! marchUnit.hasContact &&
      marchUnit.nearestStaticTiles.forall(_ > MarchDress.staticDressTiles) &&
      CatchUpMarch.catchUp(marchUnit.unitId, marchUnit.behindAlphaTiles)
    val wanted = MotionVerdict.desired(verdictRetreat, marchUnit.hasDuel && ! catchUpFullSpeed, farWaitHolding(unit) || assemblyHolding(unit))
    MotionVerdict.update(unit.id, wanted, With.frame)
    val verdictDuel = MotionVerdict.held(unit.id, With.frame) == MotionVerdict.Duel
    val latched = marchUnit.copy(verdictRetreat = verdictRetreat, verdictDuel = verdictDuel)
    val (terminus, marchLabel) = MarchGates.select(facts, latched) match {
      // A lone hull kites better from a set line than from a march: at
      // five tiles outside gun reach it dresses and waits instead of
      // walking into the enemy corner, where there is nowhere left to
      // kite to. Same gate as the dressed far-wait, same creep.
      case Some(MarchLabel.LoneMarch) if farWaitHolding(unit) => (farCreepPixel(unit), MarchLabel.WaitingForPack)
      case Some(label) => (squad.vicinity, label)
      case None if verdictRetreat =>
        // Retreat outranks every formation rule below: the verdict already
        // sent this hull home, so no gate and no leg gets asked - the line
        // to the Guard slot is the whole order for this frame. A forbidden
        // retreat (strong and clear, or proxy answered, unless dying)
        // falls through to the legs instead, so the line marches instead
        // of milling home.
        (getReturn(unit, squad, defaultReturn), MarchLabel.Retreating(RetreatVerdict.tokenOrThrow(With.frame)))
      case None =>
        val finalTravel = getTravel(unit, squad, defaultReturn)
        marchLegs(squad, unit, facts, meanDistToGoalTiles, finalTravel, suicide, verdictRetreat)
    }
    // Stage 2 motion audit: the desired owner, the latched verdict the
    // technique obeys, and what the march actually ordered. The change-only
    // probe keeps quiet maps quiet; want-vs-held disagreements inside the
    // release are the flicker the bus absorbs. Duels own no march label:
    // the legs always march, the technique duels through its own orders -
    // so have=Duel never appears and want=Duel against have=Advance is
    // the healthy split, not a flap.
    val actual = marchLabel match {
      case _: MarchLabel.Retreating => MotionVerdict.Withdraw
      case MarchLabel.WaitingForPack => MotionVerdict.Dress
      case _                        => MotionVerdict.Advance
    }
    // L1 rivalry tripwire: the march retreats a held duel - the line walks
    // the fight out of the map. (Was compared against the mapped motion
    // verdict, which can never equal a march label - the tripwire never
    // fired. It reads the march label now, as intended.)
    if (MarchLabel.isRetreating(marchLabel) && MotionVerdict.held(unit.id, With.frame) == MotionVerdict.Duel) {
      Debugging.MotionMismatch.bumpMarch(unit.id, unit.unitClass.toString, With.frame)
    }

    Debugging.Probe.note("Motion", unit.id, s"want=${wanted.label} have=${actual.label} held=${MotionVerdict.held(unit.id, With.frame).label}")
    (terminus, marchLabel)
  }

  // The march legs: rallying stragglers beat slots, closing ranks beats
  // marching alone, dressing beats everything but a gated march above.
  // Every leg answers to the slot discipline: a firing gun, fresh wounds
  // or a held retreat vetoes the pose that frame, but contact alone never
  // does - the line only exists if it forms while the enemy is out there.
  private def marchLegs(squad: Squad, unit: FriendlyUnitInfo, facts: MarchFacts, meanDistToGoalTiles: Double, finalTravel: Pixel, suicide: Boolean, verdictRetreat: Boolean): (Pixel, MarchLabel) = {
    val slotOpen             = SlotDiscipline.maySlot(
      firingNow         = unit.matchups.targetsInRange.nonEmpty && unit.cooldownLeft <= 0,
      framesSinceDamage = With.framesSince(unit.lastFrameTakingDamage),
      verdictHeld       = verdictRetreat)
    // Dressing veto scans (same vintage as the legs): buildings veto by
    // plausibility (a wall depot still counts, a confirmed-rubble hall
    // does not), melee vetoes only by warriors that could still be
    // there - workers and Overlords never stopped a blade from dressing,
    // and a remembered ghost must not pin the line.
    val nearEnemyBuildingTiles = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.isBuilding))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    val nearEnemyTiles       = Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    val dressOpen            = SlotDiscipline.mayDress(
      firingNow         = unit.matchups.targetsInRange.nonEmpty && unit.cooldownLeft <= 0,
      framesSinceDamage = With.framesSince(unit.lastFrameTakingDamage),
      verdictHeld       = verdictRetreat,
      nearEnemyBuildingTiles = nearEnemyBuildingTiles,
      isMelee           = unit.unitClass.melee,
      nearEnemyTiles    = nearEnemyTiles)
    val (finalMarch, rallying) = rallyLeg(squad, unit, finalTravel, suicide, slotOpen)
    val joinPixel              = joinLeg(squad, unit, suicide, slotOpen)
    val (holding, homing)      = dressLeg(squad, unit, meanDistToGoalTiles, suicide, dressOpen)
    val (umsHolding, umsPixel) = umsHoldLeg(squad, unit, facts)
    val (assembling, assemblePixel, assembleStepped) = assembleFor(squad, unit)
    // Extraordinary enforcement (owner order): a drilling hull holds a
    // formation order every frame - the anchor plants HoldPosition
    // outright (no arrived-sleep keeps a stale spawn attack-move alive),
    // the fallback owns an explicit back-step the march below executes.
    if (assembling) {
      if (assembleStepped) unit.agent.decision.set(assemblePixel)
      else Commander.hold(unit)
    }
    // Far dress-and-wait (UMS mirrors, no reavers): arriving at five
    // tiles outside gun reach the line dresses and waits for the enemy
    // ball instead of trickling into it, creeping a step every couple
    // dozen frames so a waiting enemy still gets met. Warriors only -
    // eyes keep their own march. Contact still duels first (above), and
    // a retreating hull never waits - it walks its way out.
    val farWait = ! verdictRetreat && farWaitHolding(unit)
    val farCreep = farCreepPixel(unit)
    val pack                   = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val terminus = ?(assembling, assemblePixel, ?(umsHolding, umsPixel, ?(farWait, farCreep, ?(holding, unit.pixel, joinPixel.getOrElse(?(homing, pack, finalMarch))))))
    val label    = ?(assembling, MarchLabel.WaitingForPack, ?(umsHolding, MarchLabel.UmsHold, ?(farWait || holding, MarchLabel.WaitingForPack, ?(rallying || homing, MarchLabel.RallyingToPack, MarchLabel.MarchingToSlot))))
    (terminus, label)
  }

  /**
   * UmsHold (thin game seam over the pure pace): on UMS the line stands
   * in formation and shrinks the range instead of freezing or
   * trickling. The stride scales with the strength score and issues at
   * most every third frame, and only while four in five hulls stand
   * still - between strides the formation stands motionless. Warriors
   * only, and only toward known warriors: eyes and guns never set the
   * pace. No known warriors, no stride - the line stands.
   */
  private def umsHoldLeg(squad: Squad, unit: FriendlyUnitInfo, facts: MarchFacts): (Boolean, Pixel) = {
    if ( ! With.umsMode || ! unit.unitClass.isWarrior) return (false, unit.pixel)

    val foe = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.isWarrior))(_.pixelDistanceEdge(unit.pixel))
    if (foe.isEmpty) return (true, unit.pixel)

    val settled = UmsHold.settled(
      squad.units.count(u => u.alive && ! u.moving),
      squad.units.count(_.alive))
    if (settled && UmsHold.strideNow(unit.id, With.frame)) {
      val step = foe.get.pixel
      (true, unit.pixel.project(step, UmsHold.strideTiles(facts.armyUnitsScore) * 32.0).walkablePixel)
    } else (true, unit.pixel)
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
  private def rallyLeg(squad: Squad, unit: FriendlyUnitInfo, finalTravel: Pixel, suicide: Boolean, slotOpen: Boolean): (Pixel, Boolean) = {
    val pack     = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val leash    = SquadRally.rallyRadiusFor(Protoss.Reaver(unit))
    val rallying = ! suicide && slotOpen && squad.fightConsensus && SquadRally.mayRally(unit.unitClass.isWarrior, unit.agent.shouldFight) && SquadRally.isRallyingLatched(
      unit.id,
      unit.pixelDistanceCenter(pack) * Maff.inv32,
      leash * Maff.inv32)
    (?(rallying, pack, finalTravel), rallying)
  }

  // Close ranks on the march: a unit adrift from its nearest ally
  // rejoins instead of milling alone. Consensus-only, so guards and
  // defenders keep their posts instead of bunching up.
  private def joinLeg(squad: Squad, unit: FriendlyUnitInfo, suicide: Boolean, slotOpen: Boolean): Option[Pixel] = {
    val nearestAlly  = Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel))
    val joiningAlly  = ! suicide && slotOpen && squad.fightConsensus && SquadRally.mayRally(unit.unitClass.isWarrior, unit.agent.shouldFight) && nearestAlly.exists(a => SquadRally.shouldJoinAlly(unit.tileDistanceCenter(a.pixel)))
    if (joiningAlly) nearestAlly.map(_.pixel) else None
  }

  // March dressing: a leader running ahead of the pack holds position so
  // the line arrives together instead of trickling in. Waiting zeroes the
  // stuck counter (tryingToMove is false), so Unstick stays out of it.
  // Reavers never hold: a slow hull waiting ahead of a stalled pack
  // freezes next to the enemy, so it walks back to the pack instead.
  private def dressLeg(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, suicide: Boolean, slotOpen: Boolean): (Boolean, Boolean) = {
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
    val waiting      = ! suicide && slotOpen && MarchDress.shouldHoldForPack(
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
    // No slot and no override: the middle choke anchors the withdrawal
    // instead of bleeding home through the open map - the choke narrows
    // what can chase, and the march back out is short. Home when the
    // choke is unknown or the ground path there does not exist.
    .getOrElse(withdrawAnchor(unit, squad))

  private def withdrawAnchor(unit: FriendlyUnitInfo, squad: Squad): Pixel = {
    val home = squad.homeConsensus
    // PvZ handful retreats to the natural choke, not the middle: below
    // ten hulls and two hundred skim the line receives the wave at the
    // wall instead of bleeding home through the open map - or worse,
    // trickling out to meet it past the middle. Same gate the verdict
    // arms read, so the vote and the feet never disagree.
    // Ladder homes only (see the verdict gates above).
    val weakPvZ = ! With.umsMode && Tactic.Squads.Decisions.NaturalChokeRetreat.shouldRetreat(
      With.enemies.exists(_.isZerg),
      With.units.ours.count(u => u.alive && u.unitClass.isWarrior),
      Information.Battles.ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthUnits,
        With.battles.globalAttack.enemy.skimStrengthUnits),
      Maff.minBy(With.units.enemy.view.filter(e =>
        e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)))(_.pixelDistanceCenter(unit.pixel))
        .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity))
    val choke = Chokes.middleChoke.map(_.pixelCenter)
    val natural = Chokes.naturalChoke.map(_.pixelCenter)
    if (weakPvZ && natural.exists(n => With.paths.groundPathExists(unit.tile, n.tile)))
      return natural.get.walkablePixel

    val (ax, ay) = WithdrawAnchor.anchor(
      home.x.toDouble, home.y.toDouble,
      choke.map(c => (c.x.toDouble, c.y.toDouble)),
      choke.exists(c => With.paths.groundPathExists(unit.tile, c.tile)))
    new Pixel(ax.toInt, ay.toInt).walkablePixel
  }

  /**
   * Posture marching (one predicate for the send and the travel legs):
   * an attacking cluster walks at the goal and a regrouping one dresses
   * out to it - both move without consensus, and the legs stay inert
   * without it, so the column walks instead of dancing. Squads without
   * a posture keep the consensus gate untouched.
   */
  private def postureMarches(squad: Squad): Boolean =
    squad.battlePosture.exists(p => ClusterPosture.advances(p) || p == ClusterPosture.Regroup)

  def getTravel(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel =
    ?(squad.hasGround, None, Some(squad.vicinity))
      .orElse(
        squad
          .formations
          .headOption
          .find(_.placements.contains(unit))
          .map(_.placements(unit)))
      .getOrElse(?(squad.fightConsensus || postureMarches(squad), squad.vicinity, getReturn(unit, squad, defaultReturn)))
}
