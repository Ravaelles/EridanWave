package starengine

/**
 * The StarEngine world: two sides, one frame at a time. Owns the loop
 * only - pilots decide, constraints adjust, and this object moves bodies
 * and resolves shots. Nearest-with-predicate is a query, not a decision:
 * pilots and the gun resolution share it instead of each copying it.
 *
 * Order per frame (documented, fixed): pilots move ours, then foes, then
 * constraints adjust, then foes shoot, then ours shoot. Foes shoot first
 * so outcomes never depend on who the engine favors.
 */
final class World(
  val ours: Seq[Body],
  val foes: Seq[Body],
  val pilots: Map[Body, Pilot],
  val constraints: Seq[Constraint]) {

  var frame: Int = 0

  def nearestFoeOf(me: Body, among: Seq[Body], valid: (Body, Body) => Boolean): Option[Body] = {
    val live = among.view.filter(b => b.alive && valid(me, b))
    if (live.isEmpty) None else Some(live.minBy(me.dist))
  }

  def step(): Unit = {
    val orders = (ours ++ foes).view.filter(_.alive)
      .map(b => b -> pilots.get(b).map(_.direct(b, this)).getOrElse(Hold)).toMap
    moveSide(ours, orders)
    moveSide(foes, orders)
    constraints.foreach(_.apply(this))
    shootSide(foes, ours, orders)
    shootSide(ours, foes, orders)
    frame += 1
  }

  def run(frames: Int): Unit = {
    var remaining = frames
    while (remaining > 0 && ours.exists(_.alive)) {
      step()
      remaining -= 1
    }
  }

  private def moveSide(side: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(_.alive).foreach(body => {
      orders.getOrElse(body, Hold) match {
        case Hold => ()
        case MoveTo(x, y) => body.stepToward(x, y)
        case Chase(target) =>
          if (target.alive) {
            // Statics stop the stride at reach (no jumping the guns);
            // mobiles are chased full-speed, or a capped stride lets
            // anything faster hold the rim forever.
            val stopEdge = if (target.speed == 0) body.rangeAgainst(target) else 0.0
            val dist = body.dist(target)
            if (dist > stopEdge) {
              val stride = Math.min(body.speed, dist - stopEdge)
              val dx = target.x - body.x
              val dy = target.y - body.y
              body.x += dx / dist * stride
              body.y += dy / dist * stride
            }
          }
      }
    })
  }

  private def shootSide(side: Seq[Body], victims: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(_.alive).foreach(body => {
      if (body.cooldownLeft > 0) {
        body.cooldownLeft -= 1
      } else {
        // The chase target is the committed pick (mirrors toAttack): shoot
        // it while it holds, otherwise fall back to the nearest hittable.
        val committed = orders.get(body).collect {
          case Chase(target) if target.alive && body.canHit(target) && body.dist(target) <= body.rangeAgainst(target) => target
        }
        val victim = committed.orElse(nearestFoeOf(body, victims, (me, foe) => me.canHit(foe) && me.dist(foe) <= me.rangeAgainst(foe)))
        victim.foreach(v => {
          v.hp = Math.max(0, v.hp - body.damageAgainst(v))
          body.cooldownLeft = body.cooldownAgainst(v)
        })
      }
    })
  }
}
