package Micro.Actions.Combat.Spells

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Mathematics.Shapes.Ring
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Heuristics.{SpellTargetAOE, SpellTargetSingle}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

abstract class TargetedSpell extends Action {
  
  protected def casterClass       : UnitClass
  protected def tech              : Tech
  protected def aoe               : Boolean
  protected def castRangeTiles    : Int
  protected def thresholdValue    : Double
  protected def lookaheadPixels   : Int = 0
  protected def pixelWidth        : Int = 96
  protected def pixelHeight       : Int = 96
  protected def bonusSearchPixels : Int = 32

  final def castRangePixels: Int = Maff.x32(castRangeTiles)
  
  protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double

  // Box-level veto (storms over our own army): individual values stay
  // soft so a great trade still tempts, but a box the veto rejects never
  // casts no matter how red the neighborhood is.
  protected def vetoBox(units: Iterable[UnitInfo], caster: FriendlyUnitInfo): Boolean = false

  /** Confirm radius, in tiles: a fresh pick this close re-affirms the intent. */
  val intentConfirmTiles: Double = 2.0

  /** Intent expiry, in frames: a spot unreachable this long goes stale. */
  val intentExpiryFrames: Int = 10 * 24

  def hasEnoughEnergy(unit: UnitInfo): Boolean = unit.energy >= tech.energyCost
  def canCast(unit: UnitInfo): Boolean = casterClass(unit) && unit.player.hasTech(tech) && hasEnoughEnergy(unit)
  final override def allowed(unit: FriendlyUnitInfo): Boolean = canCast(unit) && unit.matchups.enemies.nonEmpty && additionalConditions(unit)
  
  protected def additionalConditions(unit: FriendlyUnitInfo): Boolean = true
  
  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    val safeDistance  = Math.max(0, -unit.matchups.pixelsEntangled)
    val totalRange    = safeDistance + 32.0 * castRangeTiles + bonusSearchPixels // The margin is just some encouragement
    
    if (aoe) {
      // Committed cast point: out of range we hold the intent and walk to
      // it instead of re-picking every frame; in range we recalculate and
      // cast only a spot that still holds, otherwise the intent dies and
      // the next frame picks a new spot or stands down.
      def pick(): Option[Pixel] =
        new SpellTargetAOE().chooseTargetPixel(unit, totalRange, thresholdValue, valueTarget, pixelWidth = pixelWidth, pixelHeight = pixelHeight, vetoBox = vetoBox(_, unit))
      def inCastRange(target: Pixel): Boolean =
        unit.pixelDistanceCenter(target) <= castRangePixels
      unit.agent.spellIntent.filter(_._1 == tech).filterNot(intent => TargetedSpell.intentExpired(With.frame - intent._3, intentExpiryFrames)) match {
        case Some((_, spot, _)) if inCastRange(spot) =>
          pick() match {
            case Some(fresh) if TargetedSpell.confirmSpot(fresh.pixelDistance(spot) * Maff.inv32, intentConfirmTiles) =>
              Commander.useTechOnPixel(unit, tech, fresh)
              onCast(unit, fresh)
              unit.agent.spellIntent = None
            case _ =>
              unit.agent.spellIntent = None
          }
        case Some((_, spot, _)) =>
          moveInRange(unit, spot)
        case None =>
          pick() match {
            case Some(target) if inCastRange(target) =>
              Commander.useTechOnPixel(unit, tech, target)
              onCast(unit, target)
            case Some(target) =>
              unit.agent.spellIntent = Some((tech, target, With.frame))
              moveInRange(unit, target)
            case None =>
          }
      }
    } else {
      val targetUnit = SpellTargetSingle.chooseTarget(unit, totalRange, thresholdValue, valueTarget)
      targetUnit.foreach(target => {
        if (unit.pixelDistanceEdge(target) <= castRangePixels) {
          targetUnit.foreach(Commander.useTechOnUnit(unit, tech, _))
        } else {
          moveInRange(unit, target.pixel)
        }
      })
    }
  }
  
  // Event handler for when the unit issues a cast
  protected def onCast(caster: FriendlyUnitInfo, target: Pixel): Unit = {}

  private def moveInRange(caster: FriendlyUnitInfo, target: Pixel): Unit ={
    if (caster.airborne) {
      caster.agent.decision.set(target)
    } else {
      caster.agent.decision.set(groundApproachPoint(caster, target))
    }
    Commander.move(caster)
  }

  // Grounded casters walk to the nearest ring point in range, not the target itself.
  private def groundApproachPoint(caster: FriendlyUnitInfo, target: Pixel) =
    Maff.minBy(Ring(castRangeTiles).map(target.tile.add).map(_.center))(caster.pixelDistanceTravelling)
}

object TargetedSpell {

  def confirmSpot(distTiles: Double, confirmTiles: Double): Boolean =
    distTiles <= confirmTiles

  def intentExpired(framesSinceIntent: Int, expiryFrames: Int): Boolean =
    framesSinceIntent > expiryFrames
}
