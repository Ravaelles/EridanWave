package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Physics.ForceMath
import Micro.Coordination.Pushing.{TrafficPriorities, TrafficPriority}
import Performance.{Cache, KeyedCache}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Traffic (extracted from Agent): the unit's push priority and the pushes
 * it receives. One reason to change: shove order. Agent keeps one-line
 * delegates so call sites do not move.
 */
final class Traffic(val unit: FriendlyUnitInfo) {

  var priority: TrafficPriority = TrafficPriorities.Freedom

  def escalatePriority(newPriority: TrafficPriority): Unit = if (priority < newPriority && ! unit.flying) priority = newPriority

  val receivedPushForces = new Cache(() => With.coordinator.pushes
    .get(unit)
    .map(p => (p, p.force(unit)))
    .filter(_._2.exists(_.lengthSquared > 0))
    .map(p => (p._1, p._2.get))
    .toVector)
  val receivedPushForce = new KeyedCache(() => ForceMath.sum(receivedPushForces().view.filter(_._1.priority >= priority).map(_._2)), () => priority)
  val receivedPushPriority = new KeyedCache(() => Maff.max(receivedPushForces().view.map(_._1.priority)).getOrElse(TrafficPriorities.Freedom), () => priority)
}
