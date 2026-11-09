package Micro.Coordination.Pushing.Explosions

import Micro.Coordination.Pushing.{LinearPush, TrafficPriorities, TrafficPriority}
import Mathematics.Points.Pixel
import ProxyBwapi.Bullets.BulletInfo

class ExplosionLurkerNow(spine: BulletInfo) extends LinearPush {
  override protected def source: Pixel = spine.pixel

  override protected def destination: Pixel = spine.targetPixel.getOrElse(source)

  override protected def sourceWidth: Double = 20

  override val priority: TrafficPriority = TrafficPriorities.Shove
}
