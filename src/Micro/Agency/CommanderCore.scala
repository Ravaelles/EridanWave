package Micro.Agency
import Debugging.ErrorLedger
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points, Tile}
import Micro.Coordination.Pushing._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
/** See Commander: role-grouped delegates behind the facade. */
object CommanderCore {
  private[Micro] val tryingToMoveThreshold = 32
  private[Micro] def defaultEscalation(unit: FriendlyUnitInfo): Unit = {
    lazy val buildDistance = unit.pixelDistanceCenter(unit.squadIntent.toBuildActive.map(b => b.unitClass.tileArea.add(b.tile).center).get)
    if (unit.flying) return

    if (unit.squadIntent.toScoutTiles.nonEmpty)                      unit.agent.escalatePriority(TrafficPriorities.Pardon)
    if (unit.squadIntent.toFinish.isDefined)                         unit.agent.escalatePriority(TrafficPriorities.Bump)
    if (unit.squadIntent.toHeal.isDefined)                           unit.agent.escalatePriority(TrafficPriorities.Bump)
    if (unit.squadIntent.toBuild.nonEmpty)                           unit.agent.escalatePriority(?(buildDistance < 128, TrafficPriorities.Shove, TrafficPriorities.Bump))
    if (unit.agent.toAttack.exists( ! unit.inRangeToAttack(_))) unit.agent.escalatePriority(TrafficPriorities.Nudge)
    if (unit.battle.isDefined && ! unit.agent.shouldFight) {    unit.agent.escalatePriority(TrafficPriorities.Pardon)
      if (unit.matchups.pixelsEntangled > -80)                  unit.agent.escalatePriority(TrafficPriorities.Nudge)
      if (unit.matchups.pixelsEntangled > -48)                  unit.agent.escalatePriority(TrafficPriorities.Bump)
      if (unit.matchups.pixelsEntangled > -16)                  unit.agent.escalatePriority(TrafficPriorities.Shove)
    }
  }
  private[Micro] def push(unit: FriendlyUnitInfo,  minimumPriority: TrafficPriority, makePush: => Push): Unit = {
    if (unit.flying) return

    unit.agent.escalatePriority(minimumPriority)
    defaultEscalation(unit)
    if (unit.agent.priority > TrafficPriorities.Freedom) {
      With.coordinator.pushes.put(makePush)
    }
  }
  private[Micro] def pushAway(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, radius: Double = 48): Unit = {
    push(unit, minimumPriority, new CircularPush(unit.agent.priority, to, radius, unit))
  }
  private[Micro] def pushThrough(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, maxDistance: Double = 96): Unit = {
    push(unit, minimumPriority, new UnitLinearGroundPush(unit.agent.priority, unit, ?(unit.pixelDistanceCenter(to) > maxDistance, unit.pixel.project(to, maxDistance), to)))
  }
  private[Micro] def autoUnburrow(unit: FriendlyUnitInfo): Boolean = {
    if (unit.unready) return false
    if ( ! unit.burrowed) return false

    CommanderAbilities.unburrow(unit)
    true
  }
  private[Micro] def autoUnload(unit: FriendlyUnitInfo): Boolean = {
    if (unit.unready) return false
    if (unit.transport.isEmpty) return false

    CommanderTransport.requestUnload(unit)
    true
  }

  /**
   * Per-frame issued-order fuse (Stardust DUPLICATE ORDER, phase 0,
   * log-only): records every BWAPI issuance on the unit and counts a
   * second issuance in the same frame in the error ledger instead of
   * failing silently. Log-only on purpose, and it stays that way:
   * blocking a duplicate drops intent no dedup owns (unload-then-move
   * has no sleep between them, sites are shared across actions), so a
   * flip needs per-pair evidence that does not exist yet. Callers
   * still issue; the ledger only names who doubled.
   */
  private[Micro] def claimOrder(unit: FriendlyUnitInfo, issuer: OrderIssuer.Issuer): Unit = {
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(unit.orderClaim, With.frame, issuer)
    unit.orderClaim = stored
    if (duplicate) {
      val first = stored.map(_.issuer).getOrElse(OrderIssuer.MoveStep)
      ErrorLedger.note(
        IssuedOrderFuse.duplicateKey(first, issuer, unit.unitClass.toString),
        ErrorLedger.Priority.Medium,
        f"unit ${unit.id} ${unit.unitClass} frame ${With.frame} first $first second $issuer")
    }
  }

  /**
   * The only BWAPI door (phase 1): every issuance in Commander* runs
   * through here. Direct mode (outside the engage capture window) claims
   * the fuse and issues at once, exactly like before. Capture mode
   * (engage pipeline) defers: the call plus its adjacent bookkeeping
   * (sleep, flap note) is stored as a UnitIntention bid under the
   * entering stage's reason and runs once, only if the bid wins the
   * resolve. Unready units post nothing - a slept frame owns no orders,
   * and the starvation tripwire beside the resolve counts it instead.
   */
  /**
   * Drill guard (owner order, extraordinary formation): while this hull
   * drills pre-contact it holds a formation order - attack issuances
   * never leave this door. Blocked attempts land in the ledger (Low:
   * the drill working as designed, not an error), so every override
   * stays countable instead of silent.
   */
  private def drillBlocks(unit: FriendlyUnitInfo, issuer: OrderIssuer.Issuer): Boolean = {
    if (issuer != OrderIssuer.MoveAttackMove && issuer != OrderIssuer.EngageAttack) return false

    if ( ! Tactic.Squads.MarchOrders.assemblyHolding(unit)) return false

    ErrorLedger.note(
      "drillBlocksAttack.at." + unit.unitClass.toString,
      ErrorLedger.Priority.Low,
      f"unit ${unit.id} frame ${With.frame} door ${issuer.key}")

    true
  }

  private[Micro] def issueOrder(unit: FriendlyUnitInfo, issuer: OrderIssuer.Issuer)(order: => Unit): Unit = {
    // No readiness gate here on purpose: direct mode keeps Step-0
    // semantics exactly (the formation bypass issues while unready, and
    // the fuse names it if it doubles). Only capture drops slept frames.
    if (drillBlocks(unit, issuer)) return

    if (unit.agent.capturingBids) {
      if (unit.unready) return

      val reason = unit.agent.bidReason.getOrElse({
        ErrorLedger.note(
          f"bidReasonFallback.at.${issuer.key}%s",
          ErrorLedger.Priority.Low,
          f"unit ${unit.id} ${unit.unitClass} frame ${With.frame}")
        UnitIntentionReason.Duel
      })
      unit.agent.bids += UnitIntention(reason, issuer, () => order)
    } else {
      claimOrder(unit, issuer)
      order
    }
  }

  /**
   * Ends the engage capture window: picks the highest-ranked bid (first
   * posted wins ties) and issues it through the normal door - so the
   * fuse, the sleep and the flap note all see one issuance, not two.
   * Losers count themselves in the ledger (Low: arbitration working as
   * designed, not an error). A direct issuance that already claimed this
   * frame keeps today's semantics outright: the bids stand down and name
   * the direct winner instead.
   */
  private[Micro] def resolveBids(unit: FriendlyUnitInfo): Unit = {
    unit.agent.capturingBids = false
    val bids = unit.agent.bids.toVector
    unit.agent.bids.clear()
    unit.agent.bidReason = None
    if (bids.isEmpty) return

    // The drill guard binds the capture window too: attack bids posted
    // while this hull drills never run - the line finishes first.
    val live = bids.filterNot(b =>
      (b.issuer == OrderIssuer.MoveAttackMove || b.issuer == OrderIssuer.EngageAttack) &&
      Tactic.Squads.MarchOrders.assemblyHolding(unit))
    if (live.isEmpty) {
      ErrorLedger.note(
        "drillBlocksAttack.at." + unit.unitClass.toString,
        ErrorLedger.Priority.Low,
        f"unit ${unit.id} frame ${With.frame} bids ${bids.size} stood down")

      return
    }

    unit.orderClaim.filter(_.frame == With.frame) match {
      case Some(claim) =>
        ErrorLedger.note(
          UnitIntentionRank.stoodDownKey(claim.issuer, unit.unitClass.toString),
          ErrorLedger.Priority.Low,
          f"unit ${unit.id} ${unit.unitClass} frame ${With.frame} direct ${claim.issuer} stood down ${bids.size}")
      case None =>
        val winner = UnitIntentionRank.winnerIndex(live)
        live.zipWithIndex.foreach { case (bid, i) =>
          if (i == winner) bid.order()
          else {
            ErrorLedger.note(
              UnitIntentionRank.outrankedKey(bid.reason, live(winner).reason, unit.unitClass.toString),
              ErrorLedger.Priority.Low,
              f"unit ${unit.id} ${unit.unitClass} frame ${With.frame} loser ${bid.issuer} winner ${live(winner).issuer}")
          }
        }
    }
  }

  /**
   * Readiness ladder (one home for the command prologue): readiness,
   * unburrow, optional unload, in that order. `build` passes unload=false -
   * loaded Reavers build Scarabs through their own process, never by
   * stepping out of the Shuttle.
   */
  private[Micro] def ready(unit: FriendlyUnitInfo, unload: Boolean = true)(body: => Unit): Unit = {
    if (unit.unready) return

    if (autoUnburrow(unit)) return

    if (unload && autoUnload(unit)) return

    body
  }

  private[Micro] def autoUnburrowUnlessLurkerInRangeOf(unit: FriendlyUnitInfo, to: Pixel): Boolean = {
    ( ! Zerg.Lurker(unit) || unit.pixelDistanceCenter(to) > unit.pixelRangeGround) && autoUnburrow(unit)
  }
  private[Micro] def sleepAttack(unit: FriendlyUnitInfo): Unit = {
    sleep(unit, AttackDelay.framesToWaitAfterIssuingAttackOrder(unit))
  }
  private[Micro] def sleepBuild(unit: FriendlyUnitInfo): Unit = {
    // Adapted from https://github.com/tscmoo/tsc-bwai/blame/master/src/unit_controls.h#L1497
    // In practice (1 + 2 * With.latency.latencyFrames) was too low
    sleep(unit, 3 + 2 * With.latency.latencyFrames)
  }
  private[Micro] def sleepReturnCargo(unit:FriendlyUnitInfo): Unit = {
    // ReturnMinerals/ReturnGas are processed by Brood War's slow-order loop,
    // which runs once every nine frames. A retry after eight frames can reset
    // the order after it selects a depot but before it processes the deposit.
    // Wait one full processing interval plus one frame while retaining the
    // pathing and recovery benefits of periodically reissuing the order.
    sleep(unit, 10)
  }
  private[Micro] def sleep(unit: FriendlyUnitInfo, requiredDelay: Int = 2): Unit = {
    unit.sleepUntil = Maff.vmax(
      With.frame + requiredDelay,
      With.frame + With.latency.turnSize,
      unit.sleepUntil)
    if (With.configuration.trackUnit && (unit.selected || unit.transport.exists(_.selected))) {
      With.configuration = With.configuration.copy(trackUnit = false) // This is where you want the trackUnit breakpoint
    }
  }
  private[Micro] def leadFollower(unit: FriendlyUnitInfo, todo: (FriendlyUnitInfo) => Unit): Unit = {
    if (unit.ready) {
      unit.agent.leadFollower = todo
    }
  }
}

