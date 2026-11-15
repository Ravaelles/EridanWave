package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box, Defender, Gap}
import Micro.Formation.WallGapSlots.Slot

import scala.collection.mutable.{ArrayBuffer, HashMap}

/**
 * Wall-gap allocator (the sealing plan, no game access): which defenders
 * take which slots, how many the wall needs, and where arrivals join the
 * rank. Runs on the entrances with the slot search; the game seam
 * (WallGaps) only feeds it buildings and defenders.
 */
class WallGapAllocator(
  gaps: Vector[Gap],
  ling: Body,
  zealot: Body,
  slots: WallGapSlots,
  reachable: (Body, Pixel) => Boolean,
  fits: (Body, Pixel, Int) => Boolean) {

  import WallGapGeometry.connectedTo

  private case class Planned(probe: Boolean, body: Body, slot: Slot, gap: Gap)
  private var previousComposition : Vector[(Boolean, Body)] = Vector.empty
  private var plan                : Vector[Planned]         = Vector.empty
  private var previousIds         : Set[Int]                = Set.empty
  private var allocatedGaps       : Vector[(Planned, Int)]   = Vector.empty
  private val arrived = new HashMap[Int, Gap]

  // Count hypothetical assignments without changing unit identities or arrival order.
  lazy val minimumDefenders: Option[Int] = countMinimumDefenders()

  private def countMinimumDefenders(): Option[Int] = {
    var count = 0
    var stalled = 0
    var previousFront = Set.empty[(Gap, Box)]
    while (stalled < Math.max(1, gaps.size)) {
      val placements = allocate(Vector.fill(count)((false, zealot)))
      val sealedWall = gaps.forall(g => connectedTo(ling.forbidden(g.first), placements.map(_.slot.blocked))
        .exists(_.touches(ling.forbidden(g.second))))
      if (sealedWall) return Some(count)
      if (placements.size < count) return None

      val front = placements.filter(_.slot.front).map(p => (p.gap, p.slot.box)).toSet
      // Allocation visits every gap once per pass. If a whole pass adds only
      // reserves, additional Zealots cannot fill the remaining front openings.
      stalled = if (count > 0 && front == previousFront) stalled + 1 else 0
      previousFront = front
      count += 1
    }
    None
  }

  def assign(units: Vector[Defender]): Map[Int, Pixel] = {
    val byId = units.map(u => u.id -> u).toMap
    arrived.retain((id, gap) => byId.contains(id))
    val composition: Vector[(Boolean, Body)] = units.map(u => (u.probe, u.body)).sortBy { case (probe, b) => (probe, b.left, b.up, b.right, b.down) }
    if (composition != previousComposition) {
      val next = allocate(composition)
      val probes = plan.filter(_.probe)
      val zealots = next.filterNot(_.probe)
      // After casualties, leave reserves clear of the Zealots advancing to seal the wall.
      val keepProbes = composition.count( ! _._1) < previousComposition.count( ! _._1) &&
        composition.filter(_._1) == previousComposition.filter(_._1) &&
        probes.groupBy(_.gap).map { case (g, ps) => g -> ps.size } ==
          next.filter(_.probe).groupBy(_.gap).map { case (g, ps) => g -> ps.size } &&
        gaps.forall(g => connectedTo(ling.forbidden(g.first), zealots.filter(_.gap == g).map(_.slot.blocked))
          .exists(_.touches(ling.forbidden(g.second))))
      plan = if (keepProbes) zealots ++ probes else next
      previousComposition = composition
      previousIds = Set.empty
    }
    val ids = units.map(_.id).toSet
    val remaining = ArrayBuffer(units: _*)
    // Fill the slots furthest from the arriving group first, so a defender
    // already at the entrance can lead onward instead of blocking reinforcements.
    val approach = Pixel(units.map(_.pixel.x).sum / Math.max(1, units.size), units.map(_.pixel.y).sum / Math.max(1, units.size))
    if (ids != previousIds) {
      previousIds = ids
      allocatedGaps = plan.sortBy(p => -p.slot.pixel.pixelDistanceSquared(approach)).map { placement =>
        val unit = remaining.filter(u => u.probe == placement.probe && u.body == placement.body)
          .minBy(u => (u.pixel.pixelDistanceSquared(placement.slot.pixel), u.id))
        remaining -= unit
        (placement, unit.id)
      }
    }
    val nearest: Vector[(Planned, Defender)] = allocatedGaps.map { case (placement, id) => (placement, byId(id)) }
    nearest.groupBy { case (placement, _) => (placement.gap, placement.probe, placement.body) }
      .values.flatMap(assignGap).toMap
  }

  private def arrivalDepth(unit: Defender, gap: Gap, rankBoxes: Vector[Box]): Double = {
    val depth = gap.depth(unit.pixel)
    // Units approaching around a building join from that mouth of the gap,
    // even if their current position is alongside an existing rank.
    if (arrived.get(unit.id).contains(gap) || rankBoxes.exists(_.intersects(unit.body.at(unit.pixel))) ||
      Math.abs(gap.across(unit.pixel)) <= gap.width / 2.0) depth
    else if (depth > gap.inner) Double.PositiveInfinity else Double.NegativeInfinity
  }

  private def assignGap(group: Vector[(Planned, Defender)]): Vector[(Int, Pixel)] = {
    val available = ArrayBuffer[Defender](group.map(_._2): _*)
    val placements = group.map(_._1)
    val rankBoxes = placements.map(_.slot.box)
    // Preserve physical order so newcomers do not walk through the front rank.
    placements.sortBy(p => (-p.slot.depth, p.slot.pixel.x, p.slot.pixel.y)).map(placement => {
      // Keep single-file replacements in order, but do not cross neighbors in a row.
      val tolerance = if (placements.count(p => Math.abs(p.slot.depth - placement.slot.depth) <= 8) > 1) 8 else 0
      val inward = available.map(unit => arrivalDepth(unit, placement.gap, rankBoxes)).max
      val unit = available.filter(u => arrivalDepth(u, placement.gap, rankBoxes) >= inward - tolerance)
        .minBy(u => (u.pixel.pixelDistanceSquared(placement.slot.pixel), u.id))
      available -= unit
      if (unit.pixel == placement.slot.pixel) arrived(unit.id) = placement.gap
      unit.id -> placement.slot.pixel
    })
  }

  private def allocate(composition: Vector[(Boolean, Body)]): Vector[Planned] = {
    val remaining = ArrayBuffer(composition: _*)
    val occupied = new ArrayBuffer[Box]
    val output = new ArrayBuffer[Planned]
    val placed = new HashMap[Gap, Vector[Planned]]
    val fronts = new HashMap[Gap, Vector[Slot]]
    var progressed = true
    while (remaining.nonEmpty && progressed) {
      progressed = false
      gaps.foreach { g =>
        val previous: Vector[Planned] = placed.getOrElse(g, Vector.empty)
        val blockers = previous.map(_.slot.blocked)
        val fromA = connectedTo(ling.forbidden(g.first), blockers)
        val fromB = connectedTo(ling.forbidden(g.second), blockers)
        val sealedAlready = fromA.exists(_.touches(ling.forbidden(g.second)))
        val zealotBack: Option[Double] = previous.filterNot(_.probe)
          .map(p => p.slot.depth + p.body.extent(g.nx, g.ny)).reduceOption((left, right) => Math.max(left, right))
        // One unit per gap per pass. Zealots consume the outer slots before Probes.
        val kinds = remaining.distinct
        val choice: Option[Planned] = kinds.iterator.map { case (probe, body) =>
          def legal(s: Slot): Boolean =
            ( ! probe || zealotBack.forall(_ <= s.depth - body.extent(-g.nx, -g.ny))) &&
              ! occupied.exists(_.intersects(s.box))
          val options = slots.candidates(g, body)
          // Close remaining lanes before reinforcing an already sealed passage.
          // This also puts a too-large defender in the inner mouth of a tiny gap.
          val slot: Option[Slot] = if (sealedAlready) {
            val front = fronts.getOrElseUpdate(g, previous.map(_.slot))
            val reserves = slots.reinforcementCandidates(g, body, front)
            val unbacked = front.filterNot(f => previous.drop(front.size).exists(p =>
              Math.abs(g.across(p.slot.pixel) - g.across(f.pixel)) <= 2))
            // Back each sealing unit once, then spread into the nearby side pockets.
            val backup = if (unbacked.isEmpty) None else reserves.find(s => legal(s) && unbacked.exists(f =>
              s.depth > f.depth && Math.abs(g.across(s.pixel) - g.across(f.pixel)) <= 2))
            backup.orElse(reserves.find(legal))
          } else (2 to 0 by -1).iterator.map(sides => {
            // Build an incomplete seal from the sides of the opening; a sheltered
            // central slot can leave no room for the next defender beside it.
            val ordered = if (sides == 1) options.sortBy(s => (s.depth, -s.across, s.exposure, s.pixel.x, s.pixel.y)) else options
            ordered.find(s => (s.front || probe && zealotBack.isDefined) && legal(s) &&
              (if (fromA.exists(_.touches(s.blocked))) 1 else 0) + (if (fromB.exists(_.touches(s.blocked))) 1 else 0) == sides)
          }).collectFirst { case Some(candidate) => candidate }
          slot.map(s => Planned(probe, body, s, g))
        }.collectFirst { case Some(placement) => placement }
        choice.foreach(placement => {
          output += placement
          occupied += placement.slot.box
          placed(g) = previous :+ placement
          remaining -= ((placement.probe, placement.body))
          progressed = true
        })
      }
    }
    centerUnpaired(output.toVector)
  }

  private def centerUnpaired(placements: Vector[Planned]): Vector[Planned] = {
    val output = ArrayBuffer[Planned](placements: _*)
    output.indices.foreach(i => {
      val others = output.indices.filter(_ != i).map(j => output(j)).toVector
      output(i) = centerUnpaired(output(i), others)
    })
    output.toVector
  }

  private def centerUnpaired(placement: Planned, others: Vector[Planned]): Planned = {
    val gap = placement.gap
    val body = placement.body
    val slot = placement.slot
    val neighbors = others.filter(_.gap == gap)
    val alone = ! neighbors.exists(p =>
      p.slot.depth - p.body.extent(-gap.nx, -gap.ny) < slot.depth + body.extent(gap.nx, gap.ny) &&
      slot.depth - body.extent(-gap.nx, -gap.ny) < p.slot.depth + p.body.extent(gap.nx, gap.ny))
    if ( ! alone) return placement

    val left = -gap.width / 2.0 + zealot.extent(-gap.tx, -gap.ty) + 1
    val right = gap.width / 2.0 - zealot.extent(gap.tx, gap.ty) - 1
    if (left > right) return placement

    // Balance the clearance around the collision box, including its asymmetric origin.
    val across = (body.extent(-gap.tx, -gap.ty) - body.extent(gap.tx, gap.ty)) / 2
    val center = gap.at(across, slot.depth)
    val box = body.at(center)
    if ( ! fits(body, center, 1) || ! reachable(body, center) || others.exists(_.slot.box.intersects(box))) return placement

    val pair = (Math.ceil(left).toInt to Math.floor(right).toInt).map(c => gap.at(c, slot.depth))
      .filter(p => fits(zealot, p, 1) && reachable(zealot, p))
    if (pair.isEmpty || zealot.at(pair.head).intersects(zealot.at(pair.last))) return placement

    // Centering must not open a passage already sealed by staggered defenders.
    val blockers = neighbors.map(_.slot.blocked)
    def sealedWith(blocker: Box): Boolean = connectedTo(ling.forbidden(gap.first), blockers :+ blocker)
      .exists(_.touches(ling.forbidden(gap.second)))
    if (sealedWith(slot.blocked) && ! sealedWith(ling.forbidden(box))) return placement

    placement.copy(slot = slot.copy(pixel = center, box = box, blocked = ling.forbidden(box),
      depth = gap.depth(center), across = Math.abs(gap.across(center))))
  }
}
