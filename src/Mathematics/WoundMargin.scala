package Mathematics

/**
 * Wound-scaled margin (pure numbers, no game access): a low..high band in
 * tiles that lerps with the wound, read in tiles or pixels. One home for
 * every retreat/kite/flee berth so the tile/pixel conversion lives at a
 * single seam (rule 12) instead of a `* 32.0` at every call site.
 */
case class WoundMargin(lowTiles: Double, highTiles: Double) {

  def tiles(wound01: Double): Double =
    Maff.woundBonus(wound01, lowTiles, highTiles)

  def pixels(wound01: Double): Double =
    tiles(wound01) * 32.0
}
