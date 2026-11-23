package Micro.Actions.Combat.Decisioning

/**
 * Volley discipline (owner order: never yield ground for free): one
 * verdict for loaded guns, shared by every mover that could walk past
 * a volley. A warrior whose gun is loaded with something hittable in
 * reach holds the volley instead of walking - attack if the shot is
 * ready, hold if it is still aiming. Retreating hulls keep their way
 * out: the fallback owns their shots, never this gate. Pure flags
 * for the suite; each caller keeps its own movement math and its own
 * attack-vs-hold split, but the question "shoot or walk" has exactly
 * one answer now - the dance rung, the charge, the terminal, the
 * scavenge leg, the dress step and the dying hold all ask here, so
 * no two classes can disagree about a loaded volley again.
 */
object VolleyDiscipline {

  /**
   * Loaded volley owns the frame (pure flags for the suite): not
   * retreating, gun loaded, something hittable in reach. Cooldown and
   * contact still gate the callers; this answers only the last mile.
   */
  def shouldVolley(retreating: Boolean, cooldownLeft: Int, hasHittable: Boolean): Boolean =
    ! retreating && cooldownLeft <= 0 && hasHittable
}
