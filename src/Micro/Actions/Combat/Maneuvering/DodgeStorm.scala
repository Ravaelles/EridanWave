package Micro.Actions.Combat.Maneuvering

import Lifecycle.{EridanEventListener, With}
import Mathematics.Points.Pixel
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import bwapi.BulletType

import scala.collection.JavaConverters._

/**
 * Psionic Storm dodge, ported from Atlantis AvoidPsionicStorm: any mobile
 * non-building unit with a storm closer than fleeTiles steps away from
 * the storm center instead of melting in it. Storm positions are scanned
 * once per game frame and shared by all units.
 */
object DodgeStorm extends Action {

  /** Run from storm centers closer than this (tiles). */
  var fleeTiles: Double = 4.0

  private var stormsGame: Int = -1
  private var stormsFrame: Int = -1
  private var stormsCache: Seq[Pixel] = Seq.empty

  def storms: Seq[Pixel] = {
    if (stormsGame != EridanEventListener.gameCount || stormsFrame != With.frame) {
      stormsGame = EridanEventListener.gameCount
      stormsFrame = With.frame
      stormsCache = With.game.getBullets.asScala.view
        .filter(bullet => bullet.getType == BulletType.Psionic_Storm && bullet.isVisible())
        .map(bullet => Pixel(bullet.getPosition.getX, bullet.getPosition.getY))
        .toSeq
    }
    stormsCache
  }

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.canMove && ! unit.unitClass.isBuilding

  override def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.ready) {
      storms
        .map(storm => (storm, unit.pixelDistanceCenter(storm)))
        .filter(_._2 < fleeTiles * 32)
        .sortBy(_._2)
        .headOption
        .foreach(storm => {
          unit.agent.decision.set(unit.pixel.project(storm._1, -(fleeTiles + 2) * 32).clamp())
          unit.agent.act("DodgeStorm")
          Commander.move(unit)
        })
    }
    unit.unready
  }
}
