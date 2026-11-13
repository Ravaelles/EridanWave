package starengine

/**
 * Constraints adjust the world after movement, before shooting (SRP: the
 * engine advances, constraints correct). New map rules arrive as new
 * constraints (OCP) - leashes, bounds, teleports - without touching the
 * loop or the pilots.
 */
trait Constraint {

  def apply(world: World): Unit
}

object Constraints {

  /**
   * Static leash, UMS-faithful: bound bodies never stray past maxTiles
   * from the nearest anchor static; crossed the line, they teleport back
   * to its neighborhood (nearTiles out, along their incoming heading).
   * Zerglings on the sunkens map live on exactly this rule.
   */
  final class StaticLeash(
    maxTiles: Double,
    nearTiles: Double,
    isBound: Body => Boolean,
    isAnchor: Body => Boolean) extends Constraint {

    override def apply(world: World): Unit = {
      val anchors = (world.ours ++ world.foes).filter(a => a.alive && isAnchor(a))
      if (anchors.isEmpty) return

      world.ours.foreach(bound => leash(bound, anchors))
      world.foes.foreach(bound => leash(bound, anchors))
    }

    private def leash(bound: Body, anchors: Seq[Body]): Unit = {
      if ( ! bound.alive || ! isBound(bound)) return

      val anchor = anchors.minBy(bound.dist)
      val distTiles = bound.dist(anchor) / 32.0
      if (distTiles <= maxTiles) return

      val dx = bound.x - anchor.x
      val dy = bound.y - anchor.y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) {
        bound.x = anchor.x + dx / dist * nearTiles * 32.0
        bound.y = anchor.y + dy / dist * nearTiles * 32.0
      } else {
        bound.x = anchor.x + nearTiles * 32.0
        bound.y = anchor.y
      }
    }
  }
}
