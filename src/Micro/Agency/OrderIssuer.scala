package Micro.Agency

/**
 * Every BWAPI door (owner order, phase 1): the compile-checked identity
 * of each Commander issuance site. Behaviors never pass raw strings -
 * a typo'd issuer used to silently fork the ledger; now a wrong door
 * does not compile. The dotted key keeps the ledger greppable and
 * stable (it is the old call-site name, verbatim).
 */
object OrderIssuer {

  sealed trait Issuer { def key: String }

  case object MoveStop extends Issuer { val key = "CommanderMove.stop" }

  case object MoveHold extends Issuer { val key = "CommanderMove.hold" }

  case object MoveAttackMove extends Issuer { val key = "CommanderMove.attackMove" }

  case object MovePatrol extends Issuer { val key = "CommanderMove.patrol" }

  case object MoveStep extends Issuer { val key = "CommanderMove.move" }

  case object EngageAttack extends Issuer { val key = "CommanderEngage.issueAttack" }

  case object EngageTech extends Issuer { val key = "CommanderEngage.useTech" }

  case object EngageTechOnUnit extends Issuer { val key = "CommanderEngage.useTechOnUnit" }

  case object EngageTechOnPixel extends Issuer { val key = "CommanderEngage.useTechOnPixel" }

  case object TransportRightClick extends Issuer { val key = "CommanderTransport.rightClick" }

  case object TransportUnload extends Issuer { val key = "CommanderTransport.unload" }

  case object WorkRepair extends Issuer { val key = "CommanderWork.repair" }

  case object WorkReturnCargo extends Issuer { val key = "CommanderWork.returnCargo" }

  case object WorkGather extends Issuer { val key = "CommanderWork.gather" }

  case object WorkBuild extends Issuer { val key = "CommanderWork.build" }

  case object WorkTech extends Issuer { val key = "CommanderWork.tech" }

  case object WorkUpgrade extends Issuer { val key = "CommanderWork.upgrade" }

  case object WorkCancel extends Issuer { val key = "CommanderWork.cancel" }

  case object WorkRally extends Issuer { val key = "CommanderWork.rally" }

  case object WorkAddon extends Issuer { val key = "CommanderWork.addon" }

  case object AbilitiesScarab extends Issuer { val key = "CommanderAbilities.buildScarab" }

  case object AbilitiesInterceptor extends Issuer { val key = "CommanderAbilities.buildInterceptor" }

  case object AbilitiesCloak extends Issuer { val key = "CommanderAbilities.cloak" }

  case object AbilitiesDecloak extends Issuer { val key = "CommanderAbilities.decloak" }

  case object AbilitiesBurrow extends Issuer { val key = "CommanderAbilities.burrow" }

  case object AbilitiesUnburrow extends Issuer { val key = "CommanderAbilities.unburrow" }

  case object AbilitiesLift extends Issuer { val key = "CommanderAbilities.lift" }

  case object AbilitiesLand extends Issuer { val key = "CommanderAbilities.land" }

  case object FormationHold extends Issuer { val key = "ApproachEnemyFormation.hold" }

  /** Every door, for the suite: keys must stay distinct and stable. */
  val all: Vector[Issuer] = Vector(
    MoveStop, MoveHold, MoveAttackMove, MovePatrol, MoveStep,
    EngageAttack, EngageTech, EngageTechOnUnit, EngageTechOnPixel,
    TransportRightClick, TransportUnload,
    WorkRepair, WorkReturnCargo, WorkGather, WorkBuild, WorkTech,
    WorkUpgrade, WorkCancel, WorkRally, WorkAddon,
    AbilitiesScarab, AbilitiesInterceptor, AbilitiesCloak, AbilitiesDecloak,
    AbilitiesBurrow, AbilitiesUnburrow, AbilitiesLift, AbilitiesLand,
    FormationHold)
}
