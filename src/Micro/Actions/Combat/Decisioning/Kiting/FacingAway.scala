package Micro.Actions.Combat.Decisioning.Kiting

import Mathematics.Maff

/**
 * Pure facing reads (no game access): a melee enemy showing its back
 * neither chases nor hits us, so it must not trigger kiting. Angles in
 * radians; the difference wraps through pi, so a foe at 3.0 facing -3.0
 * looks straight at us. Naming follows the picture, not Atlantis.
 */
object FacingAway {

  /** Inside this half-angle around us the enemy looks at us. */
  val facingHalfAngle: Double = Math.PI / 4

  /** Past this angle the enemy shows its back. */
  val backAngle: Double = 3 * Math.PI / 4

  def angleDiff(foeAngle: Double, radiansFoeToUs: Double): Double =
    Math.abs(Maff.normalizePiToPi(radiansFoeToUs - foeAngle))

  def facingUs(foeAngle: Double, radiansFoeToUs: Double): Boolean =
    angleDiff(foeAngle, radiansFoeToUs) <= facingHalfAngle

  def showingBack(foeAngle: Double, radiansFoeToUs: Double): Boolean =
    angleDiff(foeAngle, radiansFoeToUs) >= backAngle

  /** Turned-away melee is no kite trigger; everything else still is. */
  def dismissMelee(isMelee: Boolean, foeShowsBack: Boolean): Boolean =
    isMelee && foeShowsBack
}
