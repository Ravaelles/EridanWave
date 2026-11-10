package Tactic

/**
 * Pure standing-guard staffing (no game access): from twenty warriors the
 * army spares one body, plus one per six after that. Guards never chase
 * staffing; the count only grows with the army and shrinks through lock
 * ablation when it bleeds.
 */
object BaseGuardStaffing {

  /** Below this army no guard is spared. */
  val armyNeeded: Int = 20

  /** One guard per this many warriors past the threshold. */
  val warriorsPerGuard: Int = 6

  def guardsFor(armyWarriors: Int): Int =
    if (armyWarriors <= armyNeeded) 0
    else 1 + (armyWarriors - armyNeeded) / warriorsPerGuard
}
