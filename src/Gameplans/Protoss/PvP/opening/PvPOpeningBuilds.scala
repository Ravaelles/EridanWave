package Gameplans.Protoss.PvP.opening

import Gameplans.Protoss.PvP.PvPOpening
import Gameplans.Protoss.PvP.PvPIdeas
import Lifecycle.With
import Macro.Actions.{Enemy, Flat}
import Mathematics.Maff
import Placement.Access.PlaceLabels
import ProxyBwapi.Races.Protoss
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.{GameTime, Minutes, Seconds}
import Utilities.UnitFilters.{IsComplete, IsWarrior}

/**
 * Opening build orders: scouting, Zealot rush, shared opener, DT
 * detection, proxy/9-9 defense, FE response and the main 2-Gate /
 * 1-Gate-Core branches. Each method issues orders for one scenario.
 */
trait PvPOpeningBuilds { self: PvPOpening =>

  private[PvP] def orderScouting(): Unit = {
    //////////////
    // Scouting //
    //////////////

    if ( ! enemiesHave(Protoss.Dragoon)
      && ! With.fingerprints.proxyGateway()
      && ! With.units.enemy.filter(Protoss.CyberneticsCore).exists(With.frame - _.completionFrame > Protoss.Dragoon.buildFrames - Seconds(5)())) {
      if (starts > 3 || enemyRecentStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate99)) {
        scoutOn(Protoss.Gateway)
      } else if ( ! zBeforeCore || ! PvPIdeas.attackFirstZealot) {
        if (PvP1012()) {
          scoutOn(Protoss.Gateway, quantity = 2)
        } else {
          scoutOn(Protoss.CyberneticsCore)
        }
      }
    }
  }

  private[PvP] def orderZealotRush(): Unit = {
    /////////////////
    // Zealot rush //
    /////////////////

    if (PvP1012()) {
      if (enemyStrategy(With.fingerprints.twoGate, With.fingerprints.proxyGateway)
        || enemies(Protoss.Zealot) > Math.min(unitsComplete(Protoss.Zealot), 2)) {
        //With.blackboard.pushKiters.set(false)
        With.units.ours.foreach(_.agent.commit = false)
        commitZealots = false
      } else if (frame < GameTime(4, 15)() && ! enemiesHaveComplete(Protoss.PhotonCannon)) {
        // Wait until we have at least three Zealots together; then go in hard
        aggression(0.75)
        val zealots = With.units.ours.filter(u => Protoss.Zealot(u) && u.battle.exists(_.us.units.count(Protoss.Zealot) > 2)).toVector
        commitZealots ||= zealots.size >= (if (PvP3Zealot()) 3 else 5)
        if (commitZealots) {
          With.units.ours.filter(Protoss.Zealot).filter(_.complete).foreach(_.agent.commit = true)
        }
      }
    }
  }

  private[PvP] def orderSharedOpener(): Unit = {
    /////////////////////////
    // Execute build order //
    /////////////////////////

    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(10, Protoss.Probe)
    once(Protoss.Gateway)
  }

  private[PvP] def orderDetection(): Unit = {
    //////////////////////
    // React against DT //
    //////////////////////

    if ( ! greedyDT) {
      PvPIdeas.requireTimelyDetection()
    }
  }

  private[PvP] def orderProxyDefense(): Unit = {
    //////////////////////
    // React against 9-9 //
    //////////////////////

    if (enemyStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate99)
      && With.frame < Minutes(7)()
      && unitsComplete(IsWarrior) < 7
      // If they just make a few Zealots and stop, we don't want to overreact and die to, say, a DT swap
      && ! enemiesHave(Protoss.CyberneticsCore, Protoss.Dragoon)) {
      status("99Defense")
      gasLimitCeiling(200)
      if (units(Protoss.Gateway) < 2 || unitsComplete(Protoss.Probe) < 15) {
        cancel(Protoss.Assimilator, Protoss.CyberneticsCore, Protoss.DragoonRange, Protoss.AirDamage)
        gasWorkerCeiling(0)
      } else if ( ! have(Protoss.CyberneticsCore)) {
        gasWorkerCeiling(0)
      } else if ( ! haveComplete(Protoss.CyberneticsCore)) {
        if ( ! have(Protoss.Dragoon)) {
          gasLimitCeiling(50)
        } else {
          gasWorkerCeiling(2)
        }
      }
      once(8, Protoss.Probe)
      once(Protoss.Pylon)
      once(10, Protoss.Probe)
      once(Protoss.Gateway)
      once(11, Protoss.Probe)
      once(2, Protoss.Gateway)
      autosupply()
      pumpWorkers(oversaturate = true)
      pump(Protoss.Dragoon)
      pump(Protoss.Zealot)
      get(Protoss.ShieldBattery)
      if (With.units.enemy.exists(u => Protoss.Gateway(u) && u.proximity > 0.7)) {
        get(3, Protoss.Gateway)
      }
      get(Protoss.Assimilator)
      get(Protoss.CyberneticsCore)
      get(3, Protoss.Gateway)
      get(Protoss.DragoonRange)
    }
  }

  /**
   * FE response. Returns true when an FE build fired, in which case the
   * main builds below must stay silent: in the original these branches
   * form one if-else chain.
   */
  private[PvP] def orderForgeExpandResponse(): Boolean = {
    //////////////////////
    // React against FE //
    //////////////////////

    lazy val proxyRoboPlace = With.placement.get(PlaceLabels.ProxyForwardRobo).find(foundation => With.geography.mains.minBy(_.groundPixels(foundation.tile)).isEnemy)
    lazy val proxyGatePlace = Maff.minBy(With.placement.get(PlaceLabels.Proxy4Gate))(_.tile.groundTiles(With.scouting.enemyHome))
    lazy val canProxyRobo   = proxyRoboPlace.isDefined
    lazy val canProxyGate   = proxyGatePlace.isDefined

    val shouldFESafe    = shouldExpand && (With.fingerprints.forgeFe() || With.fingerprints.gatewayFe()) && (enemiesHave(Protoss.PhotonCannon) || enemyBases >= 2)
    val shouldFEGreedy  = shouldFESafe && With.fingerprints.forgeFe() && (enemies(Protoss.PhotonCannon) > 1 || enemyBases >= 2)
    val shouldFE        = shouldFESafe || shouldFEGreedy
    if (shouldFE && unitsEver(Protoss.Nexus) < 2 && minerals < 300) {
      cancel(Protoss.Assimilator, Protoss.CyberneticsCore, Protoss.DragoonRange)
    }
    if (shouldFEGreedy) {
      get(15, Protoss.Probe)
      get(2, Protoss.Nexus)
      get(2, Protoss.Pylon)
      get(16, Protoss.Probe)
      get(Protoss.Assimilator)
      get(17, Protoss.Probe)
      get(Protoss.CyberneticsCore)
      true
    } else if (shouldFESafe) {
      get(12, Protoss.Probe)
      get(2, Protoss.Pylon)
      get(13, Protoss.Probe)
      get(Protoss.Zealot)
      get(15, Protoss.Probe)
      get(2, Protoss.Zealot)
      get(17, Protoss.Probe)
      get(2, Protoss.Nexus)
      get(18, Protoss.Probe)
      get(3, Protoss.Zealot)
      get(19, Protoss.Probe)
      get(Protoss.Assimilator)
      get(20, Protoss.Probe)
      get(Protoss.CyberneticsCore)
      true
    } else false
  }

  private[PvP] def orderMainBuilds(feHandled: Boolean): Unit = {
    ////////////
    // 2-Gate //
    ////////////

    if (feHandled) {
      // FE build above already fired this frame; the original if-else chain ends here
    } else if (PvP1012()) { // https://liquipedia.net/starcraft/2_Gate_(vs._Protoss)
      once(12,  Protoss.Probe)
      once(2,   Protoss.Gateway)
      once(13,  Protoss.Probe)
      once(1,   Protoss.Zealot)
      once(2,   Protoss.Pylon)
      once(15,  Protoss.Probe)
      once(3,   Protoss.Zealot)

      ///////////////
      // 5+ Zealot //
      ///////////////

      if (PvP5Zealot()) { // https://tl.net/forum/bw-strategy/380852-pvp-2-gate-5-zealot-expand
        once(16,  Protoss.Probe)
        once(3,   Protoss.Pylon)
        once(17,  Protoss.Probe)
        once(5,   Protoss.Zealot)
        once(18,  Protoss.Probe)
        if (With.fingerprints.proxyGateway()) {
          pump(Protoss.Probe, 12)
          pumpRatio(Protoss.Zealot, 3, 5, Seq(Flat(2.0), Enemy(Protoss.Zealot, 1.0)))
          pump(Protoss.Probe, 18)
        }
        once(4, Protoss.Pylon)
        once(Protoss.Assimilator)
        once(19, Protoss.Probe)
        once(Protoss.CyberneticsCore)
        if (sevenZealot) {
          once(7,  Protoss.Zealot)
        } else if (PvP4GateGoon()) {
          once(4,  Protoss.Gateway)
        }
        once(21, Protoss.Probe)
        once(3,  Protoss.Gateway)
        once(2,  Protoss.Dragoon)
        once(Protoss.DragoonRange)

      //////////////
      // 3-Zealot //
      //////////////

      } else { // https://tl.net/forum/bw-strategy/567442-pvp-bonyth-style-2-gate-3-zealot-21-gas-guide
        once(Protoss.Assimilator)
        once(17, Protoss.Probe)
        once(Protoss.CyberneticsCore)
        once(18, Protoss.Probe)
        once(3,  Protoss.Pylon)
        once(20, Protoss.Probe)
        once(4,  Protoss.Pylon) // On paper this build requires losing the Zealots to free supply, but with mineral optimization we can easily afford the Pylon
        once(2,  Protoss.Dragoon)
        once(21, Protoss.Probe)
        once(Protoss.DragoonRange)
        once(22, Protoss.Probe)
        once(3,  Protoss.Gateway) // Also not in the build but we can afford it so let's
        once(4,  Protoss.Dragoon)
        once(23, Protoss.Probe)
        once(5,  Protoss.Pylon)
        once(24, Protoss.Probe)
        once(6,  Protoss.Dragoon)
      }

    /////////////////
    // 1 Gate Core //
    /////////////////

    } else {
      once(12, Protoss.Probe)
      once(Protoss.Assimilator)
      once(13, Protoss.Probe)

      /////////////
      // 17 Core //
      /////////////

      if (zBeforeCore) { // https://liquipedia.net/starcraft/1_Gate_Core_(vs._Protoss)
        once(Protoss.Zealot)
        once(14, Protoss.Probe)
        once(2,  Protoss.Pylon)
        once(15, Protoss.Probe)
        once(Protoss.CyberneticsCore)
        once(16, Protoss.Probe)
        if (zAfterCore) {
          once(2, Protoss.Zealot)
          if (gcgate) { // https://liquipedia.net/starcraft/2_Gate_Reaver_(vs._Protoss
            once(18, Protoss.Probe)
            once(3,  Protoss.Pylon)
            once(19, Protoss.Probe)
            once(Protoss.Dragoon)
            once(20, Protoss.Probe)
            once(2,  Protoss.Gateway)
            once(21, Protoss.Probe)
            once(3,  Protoss.Dragoon)
            once(3,  Protoss.Dragoon)
            once(Protoss.DragoonRange)
            once(4,  Protoss.Pylon)
            once(21, Protoss.Probe)
            once(4,  Protoss.Dragoon)
          }
        }
        once(17, Protoss.Probe)

      /////////////
      // 13 Core //
      /////////////

      } else {
        // https://liquipedia.net/starcraft/1_Gate_Core_(vs._Protoss)
        once(Protoss.CyberneticsCore)
        if (zAfterCore) {
          // https://namu-wiki.translate.goog/w/21%ED%88%AC%EA%B2%8C%EC%9D%B4%ED%8A%B8?_x_tr_sl=ko&_x_tr_tl=en&_x_tr_pto=ajax,sc,elem
          once(14, Protoss.Probe)
          once(Protoss.Zealot)
          once(2,  Protoss.Pylon)
          once(16, Protoss.Probe)
          once(Protoss.Dragoon)

          ///////////////////////
          // Robo before range //
          ///////////////////////

          if (gctech) {
            once(17, Protoss.Probe)
            if      (PvPDT())   sneakyCitadel()
            else if (PvPRobo()) sneakyRobo()
            else                get(Protoss.DragoonRange)
            once(18, Protoss.Probe)
            once(2,  Protoss.Dragoon)
            once(3,  Protoss.Pylon)
            once(19, Protoss.Probe)
            once(3,  Protoss.Dragoon)

          /////////////////
          // Range-first //
          /////////////////

          } else {
            once(Protoss.DragoonRange)
            once(17, Protoss.Probe)
            if (gcgate) {
              once(2,  Protoss.Gateway)
              once(2,  Protoss.Dragoon)
              once(18, Protoss.Probe)
              once(3,  Protoss.Pylon)
            } else {
              once(2,  Protoss.Dragoon)
              once(18, Protoss.Probe)
            }
          }
        } else {
          once(15, Protoss.Probe)
          once(2,  Protoss.Pylon)
          once(17, Protoss.Probe)
          once(Protoss.Dragoon)
          once(Protoss.DragoonRange)
          once(18, Protoss.Probe)
          once(3,  Protoss.Pylon)
          once(19, Protoss.Probe)
          once(2,  Protoss.Dragoon)
          once(20, Protoss.Probe)
        }
      }
    }
  }

  private def sneakyRobo(): Unit = {
    doSneakyRobo &&= units(Protoss.Probe) < 17 || scoutCleared
    if (doSneakyRobo) {
      status("SneakyRobo")
      get(Protoss.RoboticsFacility)
    } else {
      ?(With.strategy.isRamped,
        get(2, Protoss.Gateway),
        get(Protoss.DragoonRange))
    }
  }
}
