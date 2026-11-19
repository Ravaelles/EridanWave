package Micro.Actions.Protoss.Zealot

/**
 * Zealot leash (no game access): a Zealot never operates further than
 * this from our Dragoons. A lone Zealot charging three Dragoons while
 * ours fall back is not pressure, it is a donation - the line fights
 * and retreats as one, and the Zealot's job is the Dragoons' flank,
 * not a solo duel a screen away.
 */
object ZealotLeash {

  /** Farthest a Zealot may operate from our Dragoons, in tiles. */
  val leashTiles: Double = 5.5

  /**
   * Release band, in tiles: a latched Zealot walks the leash all the way
   * back inside this before the slot leg may take it again, so the two
   * legs cannot trade the unit every frame on the boundary.
   */
  val leashReleaseTiles: Double = 4.5

  /**
   * Rejoin when the nearest Dragoon is past the leash. Pure numbers for
   * the suite; the caller measures, exempts scouts/commits/suicides, and
   * walks the leg.
   */
  def shouldRejoin(nearestDragoonTiles: Double): Boolean =
    nearestDragoonTiles > leashTiles

  /**
   * Ball radius, in tiles: blades hold shoulder-to-shoulder inside this.
   * Center-to-center cannot go below the unit diameter (collision), so
   * this is the tightest a ball gets - 0.3-tile spacing is physically
   * impossible, one tile is touching.
   */
  val ballTiles: Double = 1.0

  /** Glue once this many melee foes press the blade: fewer is a duel. */
  val ballSwarmFoes: Int = 2

  /**
   * Ball up with the nearest blade (pure numbers for the suite): with a
   * melee swarm on the line, a Zealot whose mate drifts past a tile
   * steps back to it instead of dueling alone - lone blades kiting in
   * opposite directions is how packs get picked apart. Releases at a
   * tile: the mates approach monotonically, so the edge never flaps.
   */
  def shouldBall(mateTiles: Double, meleeFoes: Int): Boolean =
    meleeFoes >= ballSwarmFoes && mateTiles > ballTiles
}
