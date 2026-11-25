package Gameplans.Protoss.PvP

import Information.Battles.ArmyStrength
import Information.Geography.Chokes
import Information.Geography.Types.Base
import Lifecycle.With
import Macro.Actions.{ExpandForbidden, MacroActions}
import Macro.Requests.RequestUnit
import Mathematics.Maff
import Mathematics.Points.Tile
import Placement.Access.PlaceLabels._
import Placement.Access.PlacementQuery
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.{Frames, GameTime, Minutes, Seconds}

object PvPIdeas extends MacroActions {

  def attackFirstZealot: Boolean = trackRecordLacks(With.fingerprints.twoGate, With.fingerprints.proxyGateway)

  def enemyContained: Boolean = With.geography.enemyBases.nonEmpty && With.geography.enemyBases.forall(b => (Seq(b) ++ b.natural).exists(With.scouting.weControl))

  def riskingFirstExpansion: Boolean = (
    With.scouting.weExpandedFirst
    && ! enemyStrategy(With.fingerprints.nexusFirst, With.fingerprints.forgeFe, With.fingerprints.gatewayFe)
    && ! (employing(PvP3GateGoon, PvP4GateGoon) && With.fingerprints.robo())
    && ! (PvPRobo() && With.fingerprints.dtRush())
    && With.framesSince(With.scouting.firstExpansionFrameUs) < Minutes(3)())

  def monitorSafely(): Unit = {
    With.blackboard.monitorBases.set(
      ! enemyRobo
      && ! enemyDarkTemplarLikely
      && (enemyStrategy(With.fingerprints.fourGateGoon, With.fingerprints.threeGateGoon) || ! enemyRecentStrategy(With.fingerprints.dtRush)))
  }

  def dtBraveryAbroad : Boolean = unitsComplete(Protoss.DarkTemplar) > 0 && With.frame < With.scouting.earliestCompletion(Protoss.Observer)
  def dtBraveryHome   : Boolean = unitsComplete(Protoss.DarkTemplar) > 0 && With.frame < With.scouting.earliestArrival(Protoss.Observer)
  def pvpSafeToMoveOut: Boolean = {
    var output    = safePushing
    output      ||= dtBraveryAbroad
    output      &&= ! enemiesHave(Protoss.DarkTemplar) || haveComplete(Protoss.Observer)
    output
  }
  def pvpSafeAtHome: Boolean = {
    var output    = safeDefending
    output      ||= dtBraveryHome
    output      &&= ! enemiesHave(Protoss.DarkTemplar) || haveComplete(Protoss.Observer, Protoss.PhotonCannon)
    output
  }

  val lateOneBaseDTFrame  : Int = GameTime(7, 30)()
  val twoBaseDTFrame      : Int = GameTime(15, 30)()
  val cannonSafetyFrames  : Int = Seconds(10)()

  /**
   * Absolute-first Forge vs suspected DT (owner order 2026-11-25): the
   * moment DT is likely, Forge jumps the whole scheduler queue - 150
   * minerals, and every frame behind army pumps risks the game (BetaStar
   * 2026-11-25: suspicion ~3:12, Forge f6030). Called first in every PvP
   * executeBuild, so MacroSim inserts it before anything that could
   * steal the minerals. On our own DT plan the Citadel and Archives
   * follow in the same order - race kept, defense first.
   */
  def urgentForgeVsDT(): Unit = {
    if (AntiDTCannon.urgentForgeNow(enemyDarkTemplarLikely, have(Protoss.Forge))) get(Protoss.Forge)
    else if (AntiDTCannon.urgentCitadelNow(enemyDarkTemplarLikely, PvPDT(), have(Protoss.Forge), have(Protoss.CitadelOfAdun))) get(Protoss.CitadelOfAdun)
    else if (AntiDTCannon.urgentArchivesNow(enemyDarkTemplarLikely, PvPDT(), have(Protoss.Forge), have(Protoss.CitadelOfAdun), have(Protoss.TemplarArchives))) get(Protoss.TemplarArchives)
  }

  /**
   * Choke cannon anchor (owner order 2026-11-25): the anti-DT cannon sits
   * five tiles off the main choke center toward the main base - close
   * enough to cover the choke, inside to cover the mineral line.
   */
  val chokeCannonTiles: Double = AntiDTCannon.chokeCannonTiles

  private def chokeCannonTile: Option[Tile] =
    Chokes.mainChoke.map(choke =>
      choke.pixelCenter.project(With.geography.ourMain.heart.center, chokeCannonTiles * 32.0).walkableTile)

  /**
   * Choke cannon order (owner order): one cannon five tiles off the main
   * choke toward the main, base-scoped to ourMain. Lifts only the choke
   * ban, so eligibility is today's set plus the near-choke slots - it
   * cannot fail where today's query succeeds, it just prefers closer.
   * The query is frame-stable (fixed anchor, radius and tile), so the
   * production matcher never sees it as a new request.
   */
  private def requestChokeCannon(quantity: Int, startFrame: Int = 0): Unit = {
    val query = new PlacementQuery(Protoss.PhotonCannon)
      .requireBase(With.geography.ourMain)
      .requireLabelYes(Defensive)
      .allowMainChoke()
      .preferLabelYes(Defensive)
    // Hard ring (owner order 2026-11-25): five to six tiles off the main
    // choke, so the placer cannot wander off to a random slot. Fixed
    // anchor and radii, so the query stays frame-stable for the matcher.
    Chokes.mainChoke.foreach(choke => {
      val anchor = choke.pixelCenter.walkableTile
      query.requireFar(anchor, AntiDTCannon.chokeRingNearTiles)
      query.requireNear(anchor, AntiDTCannon.chokeRingFarTiles)
    })
    chokeCannonTile.foreach(query.preferTile(_))
    // Placement tripwire: the ring is hard, so an empty ring means this
    // map has no slot there - lands on the board instead of stalling
    // silently while the Hall cannon covers alone.
    if (query.foundations.isEmpty) status("ChokeCannonNoSlot")
    get(RequestUnit(Protoss.PhotonCannon, quantity, minStartFrameArg = startFrame, placementQueryArg = Some(query)))
  }

  private def makeObservers(): Unit = {
    pump(Protoss.Observer, ?(enemyHasShown(Protoss.DarkTemplar), 2, 1))
    // If we got caught with our pants down -- DT at our base and no Observer at home -- make another one
    if (With.units.enemy.filter(Protoss.DarkTemplar).filter(_.visible).exists(dt =>
        With.units.ours.filter(Protoss.RoboticsFacility).exists(robo =>
          With.units.ours.filter(Protoss.Observer).forall(obs =>
            obs.framesToTravelTo(robo.pixel) > dt.framesToGetInRange(robo) + Protoss.Observer.buildFrames)))) {
      status("DTPantsDown")
      pump(Protoss.Observer, maximumConcurrently = 1)
      cancel(Protoss.Shuttle, Protoss.Reaver)
    }
  }

  def preferObserverForDetection  : Boolean = detectWithObserverOrCannon(auditing = true)
  def requireTimelyDetection()    : Boolean = detectWithObserverOrCannon(auditing = false)

  /**
   * Second-base veto (one home for "would we cancel it right after
   * ordering"): the ordering sites and the cancel below read the same
   * predicate with the same live inputs, so a forbidden base never
   * gets ordered and an allowed one never gets cancelled for economy.
   * The Dark Templar clause is the only canceller: an unanswered blade
   * with no Forge money kills the morphing base for free, so the refund
   * funds detection instead. The army score never cancels - it breathes,
   * and breathing must not eat bases.
   */
  def forbidSecondBase(): Boolean = {
    val veto = ExpandForbidden.check(
      minutes             = With.frame / 24.0 / 60.0,
      armyStrengthUnits   = With.battles.globalAttack.us.skimStrengthUnits,
      enemyStronger       = ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthTotal,
        With.battles.globalAttack.enemy.skimStrengthTotal) < 100.0,
      darkTemplarShown    = enemyHasShown(Protoss.DarkTemplar),
      detectionReady      = With.units.existsOurs(Protoss.Observer) ||
        With.units.ours.exists(u => Protoss.PhotonCannon(u) && u.complete),
      forgeAffordable     = With.self.minerals >= Protoss.Forge.mineralPrice)
    if (veto) status("ForbidSecondBase")
    veto
  }

  /**
   * Shown DT plus a morphing base that the veto still forbids: cancel
   * the Nexus so the refund funds detection instead of donating 400
   * minerals to the first swipe. The veto already checked the clock,
   * the backbone and the answers, so this fires only on a genuine
   * fresh threat - and every firing lands in the log with its reason.
   */
  def cancelBaseVsDarkTemplar(): Unit = {
    val morphing = With.units.ours.exists(u => Protoss.Nexus(u) && ! u.complete)
    if (morphing && ExpandForbidden.shouldCancelBase(
      minutes         = With.frame / 24.0 / 60.0,
      morphing        = morphing,
      darkTemplarShown = enemyHasShown(Protoss.DarkTemplar),
      detectionReady  = With.units.existsOurs(Protoss.Observer) ||
        With.units.ours.exists(u => Protoss.PhotonCannon(u) && u.complete),
      forgeAffordable = With.self.minerals >= Protoss.Forge.mineralPrice)) {
      status("DTCancelBase")
      With.logger.debug(f"CancelNexus dtShown=true detection=false forgeAffordable=${With.self.minerals >= Protoss.Forge.mineralPrice} frame=${With.frame}")
      cancel(Protoss.Nexus)
    }
  }

  def enemyDarkTemplarPossible: Boolean = enemyDarkTemplarLikely || ( ! enemyRobo && ! With.fingerprints.fourGateGoon())

  /**
   * Cannon-backed expo (owner order): a complete enemy cannon on enemy
   * ground means static detection is up - any DT plan behind it is dead.
   * Location disambiguates from cannon rushes (which sit on our ground)
   * and from contain cannons (which sit on nobody's).
   */
  def foeCannonedExpo: Boolean =
    With.units.enemy.exists(e => e.alive && Protoss.PhotonCannon(e) && e.complete &&
      e.base.exists(b => With.geography.enemyBases.contains(b)))

  private def detectWithObserverOrCannon(auditing: Boolean): Boolean = {
    // Performance shortcut
    if (With.units.existsOurs(Protoss.Observer, Protoss.Observatory)) {
      if ( ! auditing) { makeObservers() }
      return true
    }

    var expectEarliestArrival = enemyDarkTemplarLikely || With.fingerprints.rampBlock()
    if (have(Protoss.Forge)) {
      expectEarliestArrival ||= enemyRecentStrategy(With.fingerprints.dtRush)
      expectEarliestArrival ||= ! With.fingerprints.dragoonRange()
    }
    lazy val dtArePossibility    = enemyDarkTemplarPossible
    lazy val earliestArrival     = With.scouting.earliestArrival(Protoss.DarkTemplar)
    lazy val expectedArrival     = if (expectEarliestArrival) earliestArrival else if (enemyContained || With.fingerprints.proxyGateway()) lateOneBaseDTFrame else twoBaseDTFrame
    lazy val framesUntilArrival  = expectedArrival - With.frame
    lazy val framesUntilObserver = framesUntilUnit(Protoss.Observer)
    lazy val dtTimingConfirmed   = enemiesHave(Protoss.CitadelOfAdun, Protoss.TemplarArchives, Protoss.DarkTemplar)
    lazy val dtPrecedesCannon    = framesUntilArrival - cannonSafetyFrames  < framesUntilUnit(Protoss.PhotonCannon) && dtTimingConfirmed
    lazy val dtPrecedesObserver  = framesUntilArrival                       < framesUntilObserver                   && dtTimingConfirmed
    lazy val cannonsAreReady = (
      haveComplete(Protoss.PhotonCannon)
      && With.units.ours.filter(Protoss.PhotonCannon).forall(_.complete)
      && Some(With.geography.ourFoyer)
        .filter(_.isOurs)
        .getOrElse(With.geography.ourMain)
        .ourUnits
        .exists(u => Protoss.PhotonCannon(u)
          && u.complete
          && ! With.units.enemy.filter(Protoss.DarkTemplar).exists(_.pixelDistanceTravelling(With.geography.ourMain.heart) < u.pixel.groundPixels(With.geography.ourMain.heart))))
    lazy val goObserver = (
      With.units.existsOurs(Protoss.RoboticsFacility) // It's part of our plan already
        || ( ! dtPrecedesObserver && ! PvPDT()) // Observers are just better if we can swing them
        // Imminent DT with no Forge wants the emergency cannon spam below,
        // not a Robo chain from scratch: the chain (Robo+Tory+Obs, gas-hungry
        // beside our own DT tech) never materializes before the swipe, while
        // the spam branch orders Forge and cannons immediately. Observed
        // 2026-11-25 vs BetaStar: Citadel seen f5018, zero Robo/Obs ever built,
        // first cannon f7890 after the DT was already inside.
        || (dtPrecedesCannon && ! With.units.existsOurs(Protoss.Forge) && With.units.existsOurs(Protoss.RoboticsFacility)) // Cannons are awful once DTs are already inside your base; Obs is better
        || (cannonsAreReady && enemyHasShown(Protoss.DarkTemplar) && ! With.geography.ourFoyer.ourUnits.exists(Protoss.PhotonCannon))) // We need to leave our base sooner rather than later

    if (auditing) return goObserver

    status(enemyContained,          "Containing")
    status(enemyDarkTemplarLikely, f"ExpectDT@${Frames(expectedArrival)}")

    if (goObserver) {
      if (enemyDarkTemplarLikely) {
        val observerMinStartFrame     = expectedArrival          - Protoss.Observer.buildFrames         - Seconds(10)() // Some margin for error/travel time
        val observatoryMinStartFrame  = observerMinStartFrame    - Protoss.Observatory.buildFramesFull
        val roboMinStartFrame         = observatoryMinStartFrame - Protoss.RoboticsFacility.buildFramesFull
        status(f"Obs4DT@${Frames(roboMinStartFrame)}-${Frames(observatoryMinStartFrame)}-${Frames(observerMinStartFrame)}")
        get(RequestUnit(Protoss.RoboticsFacility, 1, minStartFrameArg = roboMinStartFrame))
        get(RequestUnit(Protoss.Observatory,      1, minStartFrameArg = observatoryMinStartFrame))
        makeObservers()

        if (units(Protoss.RoboticsFacility) == 0 && enemyHasShown(Protoss.DarkTemplar, Protoss.TemplarArchives, Protoss.Arbiter, Protoss.ArbiterTribunal)) {
          val framesUntilGas = With.accounting.framesToMineGas(Protoss.RoboticsFacility.gasPrice - With.self.gas)
          if (framesUntilGas + framesUntilObserver > expectedArrival) {
            cancel(Protoss.Dragoon)
          }
        }
        if ( ! have(Protoss.Observer) && With.units.ours.filter(_.isAny(Protoss.Shuttle, Protoss.Reaver)).forall(_.remainingCompletionFrames > framesUntilUnit(Protoss.Observatory))) {
          cancel(Protoss.Shuttle, Protoss.Reaver)
        }
      }
    } else if (dtArePossibility && (PvPDT() || PvPCoreExpand() || PvP3GateGoon() || PvP4GateGoon())) {
      // If DTs are already here, spam cannons and pray one sticks
      if (enemyDarkTemplarLikely && (framesUntilArrival < 120 || (With.geography.ourBases :+ With.geography.ourNatural).exists(_.enemies.exists(Protoss.DarkTemplar)))) {
        get(Protoss.Forge)
        val bestBase    = Some(With.geography.ourFoyer).filter(_.ourUnits.exists(u => Protoss.PhotonCannon(u) && u.complete)).getOrElse(With.geography.ourMain)
        val holdNatural = bestBase.naturalOf.isDefined && ! With.strategy.isMoneyMap
        val bestTile    = ?(holdNatural, bestBase.zone.exitNowOrHeart, ?(cannonsAreReady, bestBase.zone.exitNowOrHeart, bestBase.heart))
        val label       = ?(holdNatural || cannonsAreReady, DefendEntrance, DefendHall)
        status(f"DTHere-Hold${?(holdNatural, "Nat", "Main")}-${?(cannonsAreReady, "Prepared", "Scrambling")}")
        get(RequestUnit(Protoss.PhotonCannon, ?(enemiesHaveComplete(Protoss.DarkTemplar, Protoss.TemplarArchives), ?(cannonsAreReady, 3, 4), 1),
          placementQueryArg = Some(new PlacementQuery(Protoss.PhotonCannon)
            .allowMainChoke()
            .preferBase(bestBase)
            .preferTile(bestTile)
            .preferLabelYes(Defensive, DefendGround, label))))

      // If DTs will arrive before cannons, spam to maximize survival
      } else if (enemyDarkTemplarLikely && dtPrecedesCannon) {
        status("DTPrecedesCannon")
        get(Protoss.Forge)

        if (bases > 1) {
          requestTower(Protoss.PhotonCannon, 4, With.geography.ourFoyer,  DefendEntrance)
        } else {
          // One base scrambles at home, never at the unheld natural: three
          // cannons toward the main choke (five tiles off it) and one over
          // the minerals from the front - a DT that walks around a forward
          // cluster kills the line free.
          requestChokeCannon(3)
          requestTower(Protoss.PhotonCannon, 1, With.geography.ourMain,   DefendHall, 0, hallFrontTile)
        }

      // Take reasonable precautions
      } else {
        val cannonMinStartFrame = expectedArrival - Protoss.PhotonCannon.buildFramesFull - cannonSafetyFrames
        val forgeMinStartFrame  = cannonMinStartFrame - Protoss.Forge.buildFramesFull
        val pylonMinStartFrame  = cannonMinStartFrame - Protoss.Pylon.buildFramesFull
        val naturalPylonNow     = With.units.ours.filter(Protoss.Pylon).forall(_.complete) && With.scouting.weControlOurFoyer && With.units.ours.count(Protoss.Pylon) > 3
        val injectPylon         = ! With.strategy.isMoneyMap
        // Suspicion opens the queue, not the confirmation: a likely DT
        // (dtRush prints, speedlots with no robo answer) starts Forge
        // and cannon on the arrival timing instead of waiting for a
        // shown Citadel - waiting donated the whole game to the first
        // swipe. The minStartFrames below still pace the spend, so an
        // early scare never bankrupts the build.
        val doForgeCannon       = dtArePossibility && (PvPDT() || enemyDarkTemplarLikely || enemyHasShown(Protoss.CitadelOfAdun) || enemies(Protoss.DarkTemplar) > 0)

        status(f"DTAfterCannon@${Frames(forgeMinStartFrame)}-${Frames(pylonMinStartFrame)}-${Frames(cannonMinStartFrame)}")
        if (naturalPylonNow) status("NaturalPylon")

        if (doForgeCannon) {
          status("DoForgeCannon")
          get(RequestUnit(Protoss.Forge,                                 minStartFrameArg = forgeMinStartFrame))
          if (injectPylon) {
            requestTower(Protoss.Pylon,       1, ?(bases > 1, With.geography.ourFoyer, With.geography.ourMain),   DefendHall,     pylonMinStartFrame)
          }
          val n = Maff.clamp(enemies(Protoss.DarkTemplar) + 1, 2, 4)
          // Choke first, hall second (owner order): the first cannon built
          // is the choke cannon; confirmed timing builds it now, suspicion
          // paces it on the arrival. The hall cannon covers the minerals
          // from the front of the base, never from its back.
          if (bases > 1) {
            requestTower(Protoss.PhotonCannon, n - 1, With.geography.ourFoyer, DefendEntrance, cannonMinStartFrame)
          } else {
            requestChokeCannon(n - 1, ?(dtTimingConfirmed, 0, cannonMinStartFrame))
          }
          requestTower(Protoss.PhotonCannon, 1, With.geography.ourMain, DefendHall, cannonMinStartFrame, hallFrontTile)
        } else if (injectPylon) {
          requestTower(Protoss.Pylon,         1, With.geography.ourFoyer,   DefendHall,     ?(naturalPylonNow, 0, pylonMinStartFrame))
        }
      }
    }

    goObserver
  }
  private def requestTower(unitClass: UnitClass, quantity: Int, base: Base, label: PlaceLabel, startFrame: Int = 0, anchorTile: Option[Tile] = None): Unit = {
    val query = new PlacementQuery(unitClass)
      .requireBase(base)
      .requireLabelYes(Defensive)
      .preferLabelYes(Wall, DefendGround, label)
    anchorTile.foreach(query.preferTile(_))
    get(RequestUnit(
      unitClass,
      quantity,
      minStartFrameArg = startFrame,
      placementQueryArg = Some(query)))
  }

  /**
   * Hall-front tile: the main hall pushed toward the choke, so the
   * mineral-line cannon sits at the front of the base, never at its back.
   */
  private def hallFrontTile: Option[Tile] =
    Chokes.mainChoke.map(choke =>
      AntiDTCannon.frontAnchor(With.geography.ourMain.heart.center, choke.pixelCenter, AntiDTCannon.hallFrontTiles).walkableTile)
}


