package Micro.Actions.Protoss.DarkTemplar

/**
 * Dark Templar duel bravery (no game access): no duel without a local
 * edge. A lone Templar walking into a pack that outguns it dies
 * unconnected - the live bill is 1-7 HP dealt. The duel is hp-weighted
 * DPS both sides (Lanchester-flavored power); only a strictly winning
 * duel is taken. Zero incoming DPS is a free kill (cloak holds: nothing
 * that cannot see the Templar can answer it); zero outgoing DPS never
 * duels. The caller sums the powers - its own hull plus nearby friends
 * against the local pack - so wounds, numbers and backup all move the
 * same comparison instead of growing thresholds.
 */
object DarkTemplarBravery {

  /**
   * Local pack radius around the pick, in tiles: the guns that answer
   * the dive. Distant contacts are other duels, not this one.
   */
  val packTiles: Double = 6.0

  /**
   * Take the duel (pure numbers): ours strictly outpowers the pack.
   * A tie holds - an even duel is exactly the no-edge case.
   */
  def shouldDuel(ourHp: Double, ourDps: Double, foeHp: Double, foeDps: Double): Boolean =
    if (ourDps <= 0.0) false
    else if (foeDps <= 0.0) true
    else ourHp * ourDps > foeHp * foeDps
}
