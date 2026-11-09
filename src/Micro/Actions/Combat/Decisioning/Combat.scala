package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Combat.Tactics.Brawl
import Micro.Agency.Commander
import Micro.Coordination.Pushing.TrafficPriorities
import Micro.Targeting.Target
import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Group.GenericFriendlyUnitGroup
import Tactic.Squads.Group.TFriendlyUnitGroup
import Tactic.Squads.Group.UnitGroup
import Utilities.Time.{Forever, Seconds}
import Utilities.?

final class Combat(unit: FriendlyUnitInfo) extends Action {

  var restrained            : Boolean = false
  var restrainedFrames      : Double  = 0.0
  var technique             : Technique = Fight
  var framesUntilShot       : Int = Forever()
  var framesToPokeTarget    : Option[Int] = None
  var idealTargetDistance   : Double = _
  var hasSpacetimeToPoke    : Boolean = _
  var breakFormationToAttack: Boolean = _
  var trapped               : Boolean = _

  private[Decisioning] val techniques  = new CombatTechniques(this, unit)
  private[Decisioning] val distances   = new CombatDistances(this, unit)
  private[Decisioning] val maneuvers   = new CombatManeuvers(this, unit)
  private[Decisioning] val engagement  = new CombatEngagement(this, unit)

  private[Decisioning] def group                 : UnitGroup with TFriendlyUnitGroup = unit.squad.orElse(unit.friendlyTeam).getOrElse(new GenericFriendlyUnitGroup(unit))
  private[Decisioning] def target                : Option[UnitInfo]  = unit.agent.toAttack
  private[Decisioning] def squadEngagedUpon      : Boolean = unit.squad.map(_.engagedUpon).getOrElse(unit.matchups.engagedUpon)
  private[Decisioning] def squadEngagingOn       : Boolean = unit.squad.map(_.engagingOn).getOrElse(unit.matchups.engagingOn)
  private[Decisioning] def fightConsensus        : Boolean = unit.squad.map(_.fightConsensus).getOrElse(unit.agent.shouldFight)
  private[Decisioning] def assembling            : Boolean = false && unit.formationEngage.isDefined && ! group.engagedUpon && Math.max(0.25, group.confidence11) < 0.95 - group.restrainedFrames / Seconds(10)()
  private[Decisioning] def targetFleeing         : Boolean = target.exists(t => t.canMove && Maff.isTowards(unit.pixel, t.pixel, t.angleRadians))
  private[Decisioning] def targetDistanceHere    : Double  = target.map(unit.pixelDistanceEdge).getOrElse(0d)
  private[Decisioning] def targetDistanceThere   : Double  = target.map(unit.pixelDistanceEdgeFrom(_, unit.agent.destinationNext())).getOrElse(0d)
  private[Decisioning] def formationHelpsEngage  : Boolean = targetDistanceThere <= Math.min(targetDistanceHere, target.map(unit.pixelRangeAgainst).getOrElse(0d))
  private[Decisioning] def formationHelpsChase   : Boolean = targetDistanceThere <= targetDistanceHere
  // How much of the engage formation sits in its slots (1.0 = the line is formed).
  // Supplies FormationHold.shouldAdvanceInFormation; empty/missing formation counts as formed so it never blocks.
  private[Decisioning] def formationReadiness    : Double  = unit.formationEngage
    .map(f => FormationHold.fractionInSlot(f.placements.view.map(p => p._1.pixelDistanceCenter(p._2))))
    .getOrElse(1.0)
  private[Decisioning] def idealDistanceForward  : Double  = target.map(unit.pixelDistanceEdge(_) - idealTargetDistance).getOrElse(0d)
  private[Decisioning] def confidentEnoughToChase: Boolean = unit.confidence11 > confidenceChaseThreshold
  private[Decisioning] val confidenceChaseThreshold = 0.25
  private[Decisioning] def shouldChase           : Boolean = (idealDistanceForward > 0 && confidentEnoughToChase && targetFleeing) || (target.exists(_.isAny(Terran.SiegeTankSieged, Protoss.Reaver) && ! With.fingerprints.bio()) && ! unit.flying && ! Protoss.Reaver(unit))
  private[Decisioning] def allThreatsAbusable    : Boolean = unit.matchups.threats.forall(t => canAbuse(t) || t.pixelsToGetInRange(unit) > 24 * 5)
  private[Decisioning] def nudged                : Boolean = unit.agent.receivedPushPriority() > TrafficPriorities.Nudge
  private[Decisioning] def nudgedTowards         : Boolean = nudged && Maff.isTowards(unit.pixel, unit.agent.destinationNext(), unit.agent.receivedPushForce().radians)
  private[Decisioning] def pushThrough           : Boolean = shouldEngage && unit.formationEngage.isDefined && nudged && nudgedTowards && ! unit.flying
  private[Decisioning] def ground10              : Double  = ?(unit.flying, 0, 1)
  private[Decisioning] def shouldRetreat         : Boolean = techniqueIs(Fallback, Flee, Excuse)
  private[Decisioning] def shouldEngage          : Boolean = techniqueIs(Fight, Abuse, Scavenge)
  private[Decisioning] def canAbuse(t: UnitInfo) : Boolean = unit.canAttack(t) && unit.intent.canFight && ( ! t.canAttack(unit) || (unit.topSpeed > t.topSpeed && unit.pixelRangeAgainst(t) > t.pixelRangeAgainst(unit)))

  override def allowed(unit: FriendlyUnitInfo): Boolean = (unit.canMove || unit.canAttack) && unit.intent.canFight
  override def perform(unused: FriendlyUnitInfo = null): Unit = {
    restrained = false
    innerPerform()
    restrainedFrames += ?(restrained, 1.0, -0.5) * With.framesSince(unit.agent.lastFrame)
    restrainedFrames = Math.max(0, restrainedFrames)
  }
  def innerPerform(): Unit = {
    if (unit.agent.toAttack.isEmpty) {
      Target.choose(unit)
    }
    unit.agent.choosePerch()
    Commander.defaultEscalation(unit)

    framesUntilShot       = Maff.min((Seq(unit.pixel) ++ unit.agent.perch.pixel).view.flatMap(p =>  unit.matchups.threats.map(_.framesToGetInRange(unit, p)))).getOrElse(Forever())
    framesToPokeTarget    = target.map(unit.framesToLaunchAttack(_) + unit.unitClass.framesToPotshot + With.reaction.agencyMax + With.latency.latencyFrames)
    idealTargetDistance   = distances.getIdealTargetDistance
    hasSpacetimeToPoke    = framesToPokeTarget.exists(_ < framesUntilShot) && unit.agent.perch.pixel.forall(p => ! unit.matchups.threats.exists(t => t.inRangeToAttack(unit, p.project(t.pixel, 16))))
    lazy val safePassage  = unit.matchups.ignorant || unit.topSpeedTransported >= 0.9 * group.meanTopSpeed && unit.matchups.pixelsToThreatRange.forall(_ > 32 * ?(unit.agent.receivedPushPriority() < TrafficPriorities.Bump, 2, 16))

    techniques.selectBaseTechnique(safePassage)
    techniques.applyTransitions()
    techniques.syncConsensus()

    if (technique == Walk       && maneuvers.walk())        return
    if (technique == Aim        && engagement.aim())        return
    if (technique == Dodge      && maneuvers.dodge())       return
    if (technique == Fallback   && engagement.potshot())    return
    if (shouldRetreat           && Retreat(unit)) return
    if (shouldEngage            && Brawl(unit))   return
    if (technique == Abuse      && engagement.abuse())      return
    if (technique == SuperKite  && engagement.superkite())  return

    if (shouldEngage            && engagement.engage())      {}
  }

  private[Decisioning] def techniqueIs(candidates: Technique*): Boolean = candidates.contains(technique)
  private[Decisioning] def transition(newTechnique: Technique, predicate: => Boolean): Unit = {
    if (unit.ready && technique.canTransition(newTechnique) && predicate) {
      technique = newTechnique
      unit.agent.act(technique.toString)
    }
  }

  def tryDodge(): Boolean = maneuvers.tryDodge()
}
