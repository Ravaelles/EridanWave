package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Scout static keep-off: like every other air hull, a Scout dueling near
 * an anti-air static holds outside its reach instead of posing inside it.
 * Shoots the target from the hold point when the gun reaches; otherwise
 * holds the edge instead of closing under the guns. Steals only the
 * approach, never the retreat - a fleeing Scout still runs for safety.
 */
object ScoutStaticKeepOff {

  /** Standoff margin past the static's reach, in tiles. */
  val marginTiles: Double = 1.0

  /**
   * Hold point (pure geometry): the point on the static->target line at
   * box edge plus reach plus margin. The edge matters: a 2x2 static
   * measured from its center donates the whole margin tile back to the
   * guns. When even that point sits past the shooter's weapon range the
   * target shelters under the umbrella and the duel stays normal -
   * diving is the caller's decision, not this hold's.
   */
  def holdPoint(
    staticX: Double, staticY: Double, staticTileWidth: Int, staticTileHeight: Int,
    staticRangePixels: Double,
    targetX: Double, targetY: Double,
    weaponRangePixels: Double): Option[(Double, Double)] = {
    val dx = targetX - staticX
    val dy = targetY - staticY
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist <= 0.0) return None

    // Box exit along the ray: the line starts where the static ends.
    val ux = dx / dist
    val uy = dy / dist
    val halfW = staticTileWidth * 16.0
    val halfH = staticTileHeight * 16.0
    val exitX = if (ux == 0.0) Double.PositiveInfinity else halfW / Math.abs(ux)
    val exitY = if (uy == 0.0) Double.PositiveInfinity else halfH / Math.abs(uy)
    val stand = Math.min(exitX, exitY) + staticRangePixels + marginTiles * 32.0
    val hx = staticX + ux * stand
    val hy = staticY + uy * stand
    val toTarget = Math.sqrt((targetX - hx) * (targetX - hx) + (targetY - hy) * (targetY - hy))
    if (toTarget > weaponRangePixels) None else Some((hx, hy))
  }

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! Protoss.Scout(unit)) return false
    if (combat.shouldRetreat) return false
    if (With.yolo.active) return false

    val target = combat.target
    if (target.isEmpty) return false

    val static = Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && e.unitClass.isStaticDefense && e.canAttack(unit)))(
      unit.pixelDistanceEdge(_))
    if (static.isEmpty) return false

    holdPoint(
      staticX = static.get.pixel.x, staticY = static.get.pixel.y,
      staticTileWidth = static.get.unitClass.tileWidth, staticTileHeight = static.get.unitClass.tileHeight,
      staticRangePixels = static.get.pixelRangeAgainst(unit),
      targetX = target.get.pixel.x, targetY = target.get.pixel.y,
      weaponRangePixels = unit.pixelRangeAgainst(target.get)) match {
      case None => false
      case Some((hx, hy)) =>
        // Outside the static's reach plus margin there is nothing to hold.
        if (unit.pixelDistanceEdge(static.get) > static.get.pixelRangeAgainst(unit) + marginTiles * 32.0) return false

        unit.agent.act("ScoutStandoff")
        if (unit.inRangeToAttack(target.get)) Commander.attack(unit, target.get)
        else {
          unit.agent.forceTo(Pixel(hx.toInt, hy.toInt).walkablePixel)
          Commander.move(unit)
        }

        true
    }
  }
}
