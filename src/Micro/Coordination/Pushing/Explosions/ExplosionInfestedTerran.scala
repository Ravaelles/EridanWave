package Micro.Coordination.Pushing.Explosions

import Micro.Coordination.Pushing.{CircularPush, TrafficPriorities}
import Mathematics.Physics.Force
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

class ExplosionInfestedTerran(infested: UnitInfo) extends CircularPush(TrafficPriorities.Dodge, infested.pixel, 60) {
  override def force(recipient: FriendlyUnitInfo): Option[Force] = {
    if(recipient.flying) None else super.force(recipient)
  }
}
