package Micro.Coordination.Pushing.Explosions

import Micro.Coordination.Pushing.{CircularPush, TrafficPriorities}
import Mathematics.Physics.Force
import ProxyBwapi.Bullets.BulletInfo
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

class ExplosionEMP(emp: BulletInfo) extends CircularPush(TrafficPriorities.Dodge, emp.targetPixel.getOrElse(emp.pixel), 96) {
  override def force(unit: FriendlyUnitInfo): Option[Force] = {
    if(unit.energy <= 5 && unit.shieldPoints == 20) None else super.force(unit)
  }
}
