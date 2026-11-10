package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Points
import Micro.Actions.Idle
import Utilities.Time.{Minutes, Seconds}

/**
 * Drop run-loop plumbing: target filtering, itinerary renewal, finished
 * and withdrawn states, destination setup and the state machine that
 * routes each frame to a phase. The phase bodies live in DropPhases.
 */
trait DropLifecycle { self: MissionDrop =>

  private[Missions] final def transition(newState: DropState): Unit = {
    if (state != newState) {
      With.logger.debug(f"$this transitioning from $state to $newState")
      pickupTargets.clear()
    }
    state = newState
  }

  private[Missions] def ignore(base: Base): Boolean = {
    if (base.owner.isFriendly) return true
    if (isScoutingTarget(base)) return false
    if (!base.isEnemy && base.heart.tileDistanceFast(Points.tileMiddle) < 32 && ! base.zone.island) return true
    if (targetCleared(base)) return true
    if ( ! base.owner.isEnemy && base.units.exists(u => u.likelyStillThere && u.isEnemy && u.canAttack && u.canMove && ! u.unitClass.isWorker)) return true
    if ( ! additionalItineraryConditions(base)) return true

    With.framesSince(base.lastFrameScoutedByUs) < Seconds(90)() && ! base.owner.isEnemy
  }

  private def scoutTactics = Vector(With.tactics.scoutExpansions, With.tactics.acePilots, With.tactics.scoutWithOverlord, With.tactics.monitor, With.tactics.darkTemplar)

  private[Missions] final def skipBase(base: Base): Boolean = {
    if (ignore(base)) return true
    if (isScoutingTarget(base)) return false
    if ((state == Evacuating || state == Escaping) && units.exists(_.base.contains(base))) return true
    if (base.isNeutral && itinerary.size > 1 && With.frame < Minutes(10)()) return true

    if (base.isNeutral) {
      val maxCost = if (itinerary.size > 1 && With.frame < Minutes(10)()) 0
        else if (scoutTactics.exists(t => With.recruiter.lockedBy(t).nonEmpty)) 32  * 24
        else 32 * 48
      val here  = transports.headOption.map(_.pixel).getOrElse(centroidAir)
      val there = base.townHallArea.center
      val next  = itinerary.drop(1).headOption.map(_.townHallArea.center).getOrElse(there)
      val cost = here.pixelDistance(there) + there.pixelDistance(next) - here.pixelDistance(next)
      if (cost > maxCost) return true
    }

    if (shouldStopRaiding && (units.exists(_.base.contains(base)) || (defaultCombatLanding && destinationBase.contains(base)))) return true

    false
  }

  private[Missions] def renewItinerary(expired: Boolean = false): Boolean = {
    val renewed = !returningHome && !shouldGoHome && (itinerary.isEmpty || expired) && destinationBase.exists(continueItinerary)
    if (renewed) itineraryFrame = With.frame
    renewed
  }

  private[Missions] def finishDrop(reason: String): Unit = {
    releasePassengers(passengers.filter(p => !requiredPassenger(p) && !p.loaded).toSet)
    // Releasing a recruitment lock does not replace an installed micro action.
    // Clear drop orders immediately, including when no squad orders a transport.
    (transports ++ passengers).filter(u => u.client == this || u.squad.contains(this)).foreach(u => {
      u.agent.commit = false
      u.agent.toBoard = None
      u.setSquad(None)
      val intent = u.intend(this).setAction(Idle).setTerminus(u.agent.defaultHome).setStation(None).setRedoubt(None).setBoard(None)
      intent.targets = None
      u.agent.resetDestinations()
    })
    terminate(reason)
  }

  private[Missions] def finishWithdrawal(): Boolean = {
    if (state != Escaping || !allAboard || transports.exists(t => !safeToRelease(t)) ||
      (!allHome && transports.exists(t => destinationBase.exists(t.base.contains)))) return false
    finishDrop("Recovered crew and left final target")
    true
  }

  private[Missions] def beginDestination(): Unit = {
    destinationBase = itinerary.headOption
    destinationFrame = With.frame
    initiallyLanded.clear()
    raidingTransports.clear()
    routes.clear()
    onDestinationChanged()
    destinationBase.foreach(base => {
      vicinity =
        if (base.owner.isEnemy) base.heart.center
        else if (isScoutingTarget(base)) base.townHallArea.center
        else if (itinerary.size > 1) base.townHallArea.center.project(itinerary(1).townHallArea.center, Maff.max(transports.view.map(_.sightPixels)).getOrElse(0).toDouble)
        else base.heart.center
    })
  }

  private[Missions] def updateState(goHome: Boolean): Unit = {
    // Micro can finish or reject the final stop between squad updates, too.
    if (!goHome) renewItinerary()
    returningHome ||= goHome || itinerary.isEmpty
    if (destinationBase.isEmpty && !returningHome) beginDestination()
    // Keep the old vicinity throughout pickup; a new destination never orders
    // grounded passengers to walk away from the Shuttle collecting them.
    if (returningHome || destinationBase != itinerary.headOption) {
      if (state != Evacuating && state != Escaping) transition(Evacuating)
    }
    if (state == Assembling && allAboard) transition(Travelling)
    if (state == Travelling && !allAboard) transition(Evacuating)
    if ((state == Travelling || state == Landing) && transports.exists(rejectApproach)) {
      if (itinerary.headOption == destinationBase && itinerary.nonEmpty) itinerary.dequeue()
      renewItinerary()
      returningHome ||= itinerary.isEmpty
      transition(Evacuating)
    }
    if (state == Travelling && transports.exists(t => t.base.exists(b => destinationBase.contains(b) && canLandAt(b)) || (defaultCombatLanding && readyToRaid(t)))) transition(Landing)
    if (state == Landing) {
      if (defaultCombatLanding) {
        transports.filterNot(raidingTransports).filterNot(rejectApproach).filter(readyToRaid).foreach(t => {
          raidingTransports += t
          onRaidHandoff(t)
          With.logger.debug(s"$this handing $t and its crew to default combat at ${t.pixel}")
        })
        if (transports.forall(t => raidingTransports(t) || !passengersFor(t).exists(requiredPassenger))) transition(Raiding)
      } else {
        initiallyLanded ++= passengers.filterNot(_.loaded)
        if (allLanded) transition(Raiding)
      }
    }
    if (state == Escaping && !allAboard) transition(Evacuating)
    if (state == Evacuating && allAboard) {
      returningHome ||= !canContinueAfterLeaving
      if (returningHome) transition(Escaping) else {
        beginDestination()
        transition(Travelling)
      }
    }
  }
}
