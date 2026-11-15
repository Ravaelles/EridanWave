package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box, Gap}

import scala.collection.mutable.{HashMap, Queue}

object WallGapSlots {
  case class Slot(pixel: Pixel, box: Box, blocked: Box, exposure: Int, depth: Double, across: Double, front: Boolean)
}

/**
 * Wall-gap slots (pure search over the entrances, no game access): sealing
 * candidates ordered by exposure, plus flood-filled reserves behind the
 * front. The terrain snapshot supplies `fits` and reachability; both ride
 * in as functions so the search stays suite-testable.
 */
class WallGapSlots(
  ling: Body,
  fits: (Body, Pixel, Int) => Boolean,
  reachable: (Body, Pixel) => Boolean) {

  import WallGapSlots.Slot

  private val slots = new HashMap[(Gap, Body), Vector[Slot]]
  def candidates(g: Gap, body: Body): Vector[Slot] = slots.getOrElseUpdate((g, body), {
    val diagonal = g.tx != 0 && g.ty != 0
    // Cardinal corridors can shelter the whole front plus melee reach. At an offset
    // corner there may be no such recess; search the inner mouth for the best seal.
    val crossLeft = -g.width / 2.0 + body.extent(-g.tx, -g.ty) + 1
    val crossRight = g.width / 2.0 - body.extent(g.tx, g.ty) - 1
    val through = crossLeft <= crossRight && ! diagonal &&
      g.inner - g.outer >= body.extent(-g.nx, -g.ny) + body.extent(g.nx, g.ny)
    // A body too wide for the corridor must clear both inner building edges,
    // including an offset edge beyond the overlapping part of the passage.
    val mouth = if (through || diagonal) g.inner else Vector(g.first, g.second).flatMap(b =>
      Vector(Pixel(b.left, b.top), Pixel(b.right, b.top), Pixel(b.left, b.bottom), Pixel(b.right, b.bottom))).map(g.depth).max
    val frontLimit = if (through) g.inner - body.extent(g.nx, g.ny) - 1 else mouth + body.extent(-g.nx, -g.ny) + 1 + (if (diagonal) 16 else 0)
    val first = if (diagonal) 0 else Math.ceil(Math.min(frontLimit, g.outer + body.extent(-g.nx, -g.ny) + 16)).toInt
    val last = Math.ceil(g.inner + 256).toInt
    val step = if (diagonal) 2 else 4
    // Keep sealing candidates aligned with the entrance.
    val cross = if (crossLeft > crossRight) Vector((crossLeft + crossRight) / 2) else
      ((Math.ceil(crossLeft).toInt to Math.floor(crossRight).toInt by step).map(_.toDouble) ++ Vector(crossLeft, crossRight)).distinct
    // Include exact obstacle contacts as well as the search lattice: a legal
    // inner-mouth seal can have only one pixel of clearance.
    val depths = ((first to last by step).map(_.toDouble) ++ Vector(
      Math.ceil(mouth + body.extent(-g.nx, -g.ny) + 1), Math.floor(g.inner - body.extent(g.nx, g.ny) - 1)))
      .filter(d => d >= first && d <= last).distinct
    depths.flatMap(d => cross.map(c => g.at(c, d))).distinct.filter(p => fits(body, p, 1) && reachable(body, p)).map { p =>
      val b = body.at(p)
      // Count possible Zergling contact positions around the perimeter. Fewer
      // contacts means less exposed surface, including at staggered corners.
      val contacts =
        (b.left to b.right by 8).flatMap(x => Vector(Pixel(x, b.top - ling.down), Pixel(x, b.bottom + ling.up))) ++
        (b.top to b.bottom by 8).flatMap(y => Vector(Pixel(b.left - ling.right, y), Pixel(b.right + ling.left, y)))
      Slot(p, b, ling.forbidden(b), contacts.count(fits(ling, _, 0)), g.depth(p), Math.abs(g.across(p)), g.depth(p) <= frontLimit + 0.5)
    }.toVector.sortBy(s => (s.exposure, s.depth, s.across, s.pixel.x, s.pixel.y))
  })

  private val reserves = new HashMap[(Gap, Body, Vector[Pixel]), Vector[Slot]]
  def reinforcementCandidates(g: Gap, body: Body, front: Vector[Slot]): Vector[Slot] =
    reserves.getOrElseUpdate((g, body, front.map(_.pixel)), {
      // A side pocket behind the shorter building can sit ahead of an inner-mouth plug.
      val frontDepth = Math.min(front.map(_.depth).min, g.inner + body.extent(-g.nx, -g.ny) + 1)
      def open(p: Pixel): Boolean = g.depth(p) >= frontDepth && fits(body, p, 1)
      val distance = new HashMap[Pixel, Int]
      val queue = new Queue[Pixel]
      val offsets = (for (x <- -32 to 32 by 4; y <- -32 to 32 by 4) yield (x, y))
        .sortBy { case (x, y) => Math.abs(x) + Math.abs(y) }
      front.foreach { s =>
        // Probes may need a wider starting point than the Zealot they back up.
        offsets.map { case (x, y) => s.pixel.add(x, y) }.find(p => open(p) && reachable(body, p)).foreach { p =>
          val d = Math.abs(p.x - s.pixel.x) + Math.abs(p.y - s.pixel.y)
          if (d < distance.getOrElse(p, Int.MaxValue)) {
            distance(p) = d
            queue.enqueue(p)
          }
        }
      }
      // Flood inward and sideways from the sealing units. Walking distance keeps
      // reserves near a replacement route instead of behind an intervening cannon.
      while (queue.nonEmpty) {
        val p = queue.dequeue()
        for (dx <- -1 to 1; dy <- -1 to 1 if dx != 0 || dy != 0) {
          val next = p.add(4 * dx, 4 * dy)
          val d = distance(p) + (if (dx == 0 || dy == 0) 4 else 6)
          if (d <= 256 && d < distance.getOrElse(next, Int.MaxValue) && open(next) &&
            (dx == 0 || dy == 0 || (1 to 3).forall(i => open(p.add(i * dx, i * dy))))) {
            distance(next) = d
            queue.enqueue(next)
          }
        }
      }
      distance.keys.toVector.sortBy(p => (distance(p), g.depth(p), Math.abs(g.across(p)), p.x, p.y)).map { p =>
        val box = body.at(p)
        Slot(p, box, ling.forbidden(box), 0, g.depth(p), Math.abs(g.across(p)), front = false)
      }
    })
}
