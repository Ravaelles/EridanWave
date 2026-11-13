package Micro.Actions.Protoss.Shuttle

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.attack.SquadAttack
import Tactic.Squads.defense.SquadDefendBase

/**
 * Passenger adoption (plain Boolean guard for the BeShuttle chain): a
 * Shuttle with room picks up the neediest hailer, plus newborn Reavers
 * for an army squad. Returns whether anyone was adopted, so the chain
 * stops for the frame instead of questing on top of the pickup.
 */
object ShuttleAdoptPassenger {

  def adopt(shuttle: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Shuttle(shuttle)) return false

    val pickupCandidates =
      Maff.orElse(
        shuttle.alliesSquad.view.filter(shouldAdopt(shuttle, _)),
        shuttle.alliesBattle.view.filter(shouldAdopt(shuttle, _)),
        With.units.ours.filter(shouldAdopt(shuttle, _)))

    var acted = false
    Maff.maxBy(pickupCandidates)(c => pickupNeed(shuttle, c) / (1.0 + c.pixelDistanceSquared(shuttle))).foreach(passenger => {
      shuttle.agent.addPassenger(passenger)
      acted = true
    })

    // If we're not doing something fancy, pick up new Reavers as they're born
    if (shuttle.squad.exists(s => s.isInstanceOf[SquadAttack] || s.isInstanceOf[SquadDefendBase])) {
      val newborns = With.units.ours
        .filter(u => ! u.complete && Protoss.Reaver(u) && u.remainingCompletionFrames < shuttle.framesToTravelTo(u.pixel))
        .toVector

      Maff.minBy(newborns)(_.remainingCompletionFrames).foreach(newborn => {
        shuttle.agent.addPassenger(newborn)
        acted = true
      })
    }
    if (acted) shuttle.agent.act("ShuttleAdoptPassenger")

    acted
  }

  protected def shouldAdopt(shuttle: FriendlyUnitInfo, passenger: FriendlyUnitInfo): Boolean = (
    // Can we adopt this passenger?
    shuttle.canTransport(passenger)
      && shuttle.squad.forall {
        case drop: Tactic.Missions.core.MissionDrop => drop.assignedTransport(passenger).contains(shuttle)
        case _ => true
      }
      && passenger.transport.isEmpty
      && passenger.agent.ride.forall(shuttle==)
      && passenger.unitClass.spaceRequired + shuttle.agent.passengerSize <= shuttle.unitClass.spaceProvided
      // Do we want to adopt this passenger?
      && (passenger.matchups.targetedByScarab || Protoss.Reaver(passenger) || (Protoss.HighTemplar(passenger) && MacroFacts.techStarted(Protoss.PsionicStorm)))
      && (shuttle.agent.passengers.isEmpty || shuttle.agent.passengers.headOption.map(_.squad).getOrElse(shuttle.squad).forall(passenger.squad.contains))
    )

  protected def pickupNeed(shuttle: FriendlyUnitInfo, hailer: FriendlyUnitInfo): Double = {
    val endangered = hailer.matchups.framesOfSafety < shuttle.framesToTravelTo(hailer.pixel) + 2 * shuttle.unitClass.framesToTurn180
    val sojourning = hailer.agent.destinationFinal().pixelDistance(hailer.pixel) > 32.0 * 20
    var output = 1.0
    if (Protoss.Reaver(hailer))                         output *= 1000
    if (hailer.matchups.targetedByScarab)               output *= 100
    if (shuttle.squad.exists(hailer.squad.contains))    output *= 10
    if (shuttle.battle.exists(hailer.battle.contains))  output *= 5
    if (endangered)                                     output *= 10
    if (sojourning)                                     output *= 2
    output
  }
}
