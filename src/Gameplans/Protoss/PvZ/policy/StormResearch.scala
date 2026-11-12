package Gameplans.Protoss.PvZ.policy

/**
 * Pure Psionic Storm gate (no game access): never upfront, mandatory once
 * committed. Two lifetime Templars mean the plan is real; research starts
 * when a live caster exists and either the bank holds three-fifty gas or
 * two Templars stand already, incomplete ones counting.
 */
object StormResearch {

  /** Lifetime Templars that commit us to Storm. */
  val producedNeeded: Int = 2

  /** Live completed casters required before spending a frame on it. */
  val castersNeeded: Int = 1

  /** Bank that excuses a lone caster. */
  val bankNeeded: Int = 350

  /** Standing Templars, incomplete counting, that excuse a thin bank. */
  val standingNeeded: Int = 2

  def shouldResearch(produced: Int, complete: Int, extant: Int, gas: Int): Boolean =
    produced >= producedNeeded && complete >= castersNeeded &&
      (gas >= bankNeeded || extant >= standingNeeded)
}
