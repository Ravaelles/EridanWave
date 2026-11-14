package Micro.Actions.Combat.Spells.Protoss

import Lifecycle.With
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Dying-Templar storm (pure gates plus one cast): a High Templar with a
 * full storm's energy, a chewed hull and fresh incoming damage spends
 * the battery on the pack chewing it instead of carrying it to the
 * grave. The cast point is the median of the enemies around it, so one
 * swipe lands centered no matter which side the pack closes from. Fires
 * before the valued storm path, which would hold a dying caster's fire
 * for a pack that never comes.
 */
object SuicidePsionic {

  /** Suicide needs a full storm: the same floor that holds all fire. */
  val suicideEnergy: Int = PsionicStorm.packFloorEnergy

  /** Hull below this, in hit points, counts as dying. */
  val suicideHitPoints: Int = 45

  /** Incoming damage inside this window, in frames, arms the trade. */
  val attackedWindowFrames: Int = 10

  /** Enemies counted around the dying Templar, in tiles. */
  val gatherTiles: Double = 7.0

  def shouldSuicide(energy: Int, hitPoints: Int, framesSinceAttacked: Int): Boolean =
    energy >= suicideEnergy && hitPoints < suicideHitPoints && framesSinceAttacked <= attackedWindowFrames

  /** Median enemy pixel: centered on the pack however it surrounds. */
  def medianTarget(pixels: Seq[Pixel]): Option[Pixel] = {
    if (pixels.isEmpty) return None

    val xs = pixels.map(_.x).sorted
    val ys = pixels.map(_.y).sorted
    Some(new Pixel(xs(xs.size / 2), ys(ys.size / 2)))
  }

  def act(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.HighTemplar(unit)) return false
    if ( ! shouldSuicide(unit.energy, unit.hitPoints, With.framesSince(unit.lastFrameTakingDamage))) return false

    val foes = unit.matchups.enemies.view.filter(e =>
      unit.pixelDistanceCenter(e.pixel) <= gatherTiles * 32.0).map(_.pixel).toSeq
    medianTarget(foes).exists(pixel => {
      if (unit.pixelDistanceCenter(pixel) > PsionicStorm.castRangePixels) return false

      Commander.useTechOnPixel(unit, Protoss.PsionicStorm, pixel)
      unit.agent.act("SuicidePsionic")

      true
    })
  }
}
