package starengine

import Micro.Actions.Protoss.ReaverShuttle.ReaverShuttleStaticAttack

/**
 * Pilots decide, the engine advances (SRP split): a pilot answers where
 * one body goes this frame, never moving it and never dealing damage.
 * New behaviors arrive as new pilots (OCP) - the engine loop stays shut.
 * Shared targeting rules stay real: range checks use the body's own
 * class, the standoff calls the shipped static-attack gate, the suicide
 * veto calls the shipped Focus vote.
 */
trait Pilot {

  def direct(me: Body, world: World): Directive
}

sealed trait Directive

case object Hold extends Directive

final case class MoveTo(x: Double, y: Double) extends Directive

final case class Chase(target: Body) extends Directive

/**
 * Board a transport: walk to its hull and embark once inside pickup
 * reach. Loaded cargo rides the transport, never shoots and cannot be
 * hit. Capacity follows the shipped UnitType slots.
 */
final case class Board(transport: Body) extends Directive

/** Leave the transport at its current position. */
case object Unload extends Directive

/** Storm cast at a point: resolves in the cast phase, never moves the caster that frame. */
final case class CastStorm(x: Double, y: Double) extends Directive

object Pilots {

  /** Hold position, always. Rides, observers, artillery that sits. */
  final class Hold extends Pilot {

    override def direct(me: Body, world: World): Directive = Hold
  }

  /**
   * Attack-move: chase the nearest valid foe, hold a mission goal with
   * nobody to fight. Static guns we outrange are held at our max reach
   * (the shipped advantage gate picks the standoff); everything else is
   * chased down - the engine stops the stride at reach for statics only.
   */
  final class AttackMove(
    enemiesOf: World => Seq[Body],
    isValidTarget: (Body, Body) => Boolean,
    fallback: (Double, Double)) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      world.nearestFoeOf(me, enemiesOf(world), isValidTarget) match {
        case Some(foe) if standoff(me, foe) =>
          val dx = me.x - foe.x
          val dy = me.y - foe.y
          val dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0) {
            val stand = me.rangeAgainst(foe)
            MoveTo(foe.x + dx / dist * stand, foe.y + dy / dist * stand)
          } else Hold
        case Some(foe) => Chase(foe)
        case None      => MoveTo(fallback._1, fallback._2)
      }
    }

    private def standoff(me: Body, foe: Body): Boolean =
      foe.speed == 0 && ReaverShuttleStaticAttack.advantageBackoff(
        ourRangeTiles = me.rangeAgainst(foe) / 32.0,
        staticRangeTiles = foe.rangeAgainst(me) / 32.0,
        distTiles = me.dist(foe) / 32.0)
  }

  /** Run from the nearest threat, full speed, no destination. */
  final class Flee(enemiesOf: World => Seq[Body]) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      world.nearestFoeOf(me, enemiesOf(world), (_, _) => true) match {
        case Some(foe) =>
          val dx = me.x - foe.x
          val dy = me.y - foe.y
          val dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
          else Hold
        case None => Hold
      }
    }
  }

  /**
   * Zombie attacker: always presses toward the enemy, chewing the
   * nearest most-wounded body. Nearest first, wounds break ties - the
   * default decent-bot pressure every scenario plays against. Never
   * retreats, never standoffs, so our restraint (vetoes, latches,
   * hold-fire floors) is what gets proven.
   */
  final class ZombieAttack(
    enemiesOf: World => Seq[Body],
    fallback: (Double, Double)) extends Pilot {

    override def direct(me: Body, world: World): Directive =
      ZombieAttack.pick(me, enemiesOf(world)) match {
        case Some(foe) => Chase(foe)
        case None      => MoveTo(fallback._1, fallback._2)
      }
  }

  object ZombieAttack {

    /** Nearest live foe, most wounded wins ties; nothing when nobody lives. */
    def pick(me: Body, foes: Seq[Body]): Option[Body] = {
      val live = foes.filter(_.alive)
      if (live.isEmpty) None else Some(live.minBy(f => (me.dist(f), - f.lossFraction)))
    }
  }
}
