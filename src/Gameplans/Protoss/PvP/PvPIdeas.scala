package Gameplans.Protoss.PvP

import Information.Battles.ArmyStrength
import Information.Geography.Types.Base
import Lifecycle.With
import Macro.Actions.{ExpandForbidden, MacroActions}
import Macro.Requests.RequestUnit
import Mathematics.Maff
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
          // One base scrambles at home, never at the unheld natural: two
          // cannons toward the main choke and one over the minerals - a
          // DT that walks around a forward cluster kills the line free.
          requestTower(Protoss.PhotonCannon, 2, With.geography.ourMain,   DefendEntrance)
          requestTower(Protoss.PhotonCannon, 1, With.geography.ourMain,   DefendEntrance)
          requestTower(Protoss.PhotonCannon, 1, With.geography.ourMain,   DefendHall)
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
            requestTower(Protoss.Pylon,       1, With.geography.ourFoyer,   DefendHall,     pylonMinStartFrame)
          }
          val n = Maff.clamp(enemies(Protoss.DarkTemplar) + 1, 2, 4)
          // Spread pair (owner order): one cannon holds the base itself,
          // the rest watch the lane toward the main choke - a DT that
          // walks around a single cluster kills the line for free. One
          // base keeps both cannons home (minerals + main choke side);
          // the foyer split only defends a natural we actually hold.
          requestTower(Protoss.PhotonCannon, 1, With.geography.ourMain, DefendHall, cannonMinStartFrame)
          val chokeBase = ?(bases > 1, With.geography.ourFoyer, With.geography.ourMain)
          requestTower(Protoss.PhotonCannon, n - 1, chokeBase, DefendEntrance, cannonMinStartFrame)
        } else if (injectPylon) {
          requestTower(Protoss.Pylon,         1, With.geography.ourFoyer,   DefendHall,     ?(naturalPylonNow, 0, pylonMinStartFrame))
        }
      }
    }

    goObserver
  }
  private def requestTower(unitClass: UnitClass, quantity: Int, base: Base, label: PlaceLabel, startFrame: Int = 0): Unit = {
    get(RequestUnit(
      unitClass,
      quantity,
      minStartFrameArg = startFrame,
      placementQueryArg = Some(new PlacementQuery(unitClass)
        .requireBase(base)
        .requireLabelYes(Defensive)
        .preferLabelYes(Wall, DefendGround, label))))
  }
}


