package Micro.Formation

import Mathematics.Points.{Pixel, Tile}
import ProxyBwapi.UnitClasses.UnitClass

import scala.collection.mutable.ArrayBuffer

/**
 * Wall-gap geometry (pure kernel, no game access): extents, boxes, the
 * gap test between two obstacles, and the entrance deduplication. The
 * terrain snapshot (WalkTerrain, next to navigation) supplies `fits`;
 * everything here issuite-testable numbers.
 */
object WallGapGeometry {

  case class Box(left: Int, top: Int, right: Int, bottom: Int) {
    def center: Pixel = Pixel((left + right) / 2, (top + bottom) / 2)
    def expand(n: Int): Box = Box(left - n, top - n, right + n, bottom + n)
    def intersects(b: Box): Boolean = left < b.right && b.left < right && top < b.bottom && b.top < bottom
    def touches(b: Box): Boolean = left <= b.right && b.left <= right && top <= b.bottom && b.top <= bottom
    def contains(p: Pixel): Boolean = p.x >= left && p.x < right && p.y >= top && p.y < bottom
  }

  case class Body(left: Int, up: Int, right: Int, down: Int) {
    def at(p: Pixel): Box = Box(p.x - left, p.y - up, p.x + right, p.y + down)
    // Integer centers at which this body would collide with the obstacle.
    def forbidden(b: Box): Box = Box(b.left - right + 1, b.top - down + 1, b.right + left, b.bottom + up)
    def extent(x: Double, y: Double): Double = Math.abs(x) * (if (x < 0) left else right) + Math.abs(y) * (if (y < 0) up else down)
  }

  object Body {
    def of(kind: UnitClass): Body = Body(kind.dimensionLeft, kind.dimensionUp, kind.dimensionRightExclusive, kind.dimensionDownExclusive)
  }

  case class Defender(id: Int, body: Body, probe: Boolean, pixel: Pixel)

  case class Gap(first: Box, second: Box, a: Pixel, b: Pixel, nx: Double, ny: Double, outer: Double, inner: Double, doorway: Boolean) {
    val center: Pixel = a.midpoint(b)
    private val length = Math.hypot(b.x - a.x, b.y - a.y)
    val width: Int = Math.ceil(length).toInt
    val tx: Double = (b.x - a.x) / length
    val ty: Double = (b.y - a.y) / length
    def depth(p: Pixel): Double = (p.x - center.x) * nx + (p.y - center.y) * ny
    def across(p: Pixel): Double = (p.x - center.x) * tx + (p.y - center.y) * ty
    def at(across: Double, depth: Double): Pixel = Pixel(
      Math.round(center.x + across * tx + depth * nx).toInt,
      Math.round(center.y + across * ty + depth * ny).toInt)
  }

  def connectedTo(start: Box, blockers: Vector[Box]): Vector[Box] = {
    val reached = ArrayBuffer(start)
    val remaining = ArrayBuffer(blockers: _*)
    var next = 0
    while (next < reached.size) {
      val neighbors = remaining.filter(_.touches(reached(next))).toVector
      reached ++= neighbors
      remaining --= neighbors
      next += 1
    }
    reached.toVector
  }

  // The gap test between two obstacles: close enough for a Zergling to
  // consider, facing the base, traversable off-center. `fits` and the
  // hallway test ride in as functions so the math stays game-free.
  def between(
    a: Box,
    b: Box,
    inside: Pixel,
    door: Option[Pixel],
    obstacles: Vector[Box],
    hallway: Pixel => Boolean,
    ling: Body,
    fits: (Body, Pixel, Int) => Boolean): Option[Gap] = {
    def closest(lowA: Int, highA: Int, lowB: Int, highB: Int): (Int, Int) = {
      if (highA <= lowB) (highA, lowB)
      else if (highB <= lowA) (lowA, highB)
      else {
        val middle = (Math.max(lowA, lowB) + Math.min(highA, highB)) / 2
        (middle, middle)
      }
    }
    val (ax, bx) = closest(a.left, a.right, b.left, b.right)
    val (ay, by) = closest(a.top, a.bottom, b.top, b.bottom)
    val width = Math.hypot(bx - ax, by - ay)
    if (width == 0 || width > 96 || ling.forbidden(a).touches(ling.forbidden(b))) return None

    val center = Pixel((ax + bx) / 2, (ay + by) / 2)
    val delta = inside.subtract(center)
    val sign = if ((ay - by) * delta.x + (bx - ax) * delta.y >= 0) 1 else -1
    val nx = sign * (ay - by) / width
    val ny = sign * (bx - ax) / width
    // Ignore alleys along the back of the wall, such as between cannon rows.
    if (nx * delta.x + ny * delta.y < 0.45 * Math.hypot(delta.x, delta.y)) return None

    val diagonal = ax != bx && ay != by
    val depths = if (diagonal) Vector(0.0) else if (ax != bx)
      Vector(ny * (Math.max(a.top, b.top) - center.y), ny * (Math.min(a.bottom, b.bottom) - center.y))
    else Vector(nx * (Math.max(a.left, b.left) - center.x), nx * (Math.min(a.right, b.right) - center.x))
    val reservedDoor = door.isEmpty && (Math.floorDiv(Math.min(ax, bx), 32) to Math.floorDiv(Math.max(ax, bx), 32)).exists(x =>
      (Math.floorDiv(Math.min(ay, by), 32) to Math.floorDiv(Math.max(ay, by), 32)).exists(y => {
        val p = Tile(x, y).center
        hallway(p) && ! obstacles.exists(_.contains(p)) && p.pixelDistanceSquared(center) <= width * width / 4 + 32 * 32
      }))
    val g = Gap(a, b, Pixel(ax, ay), Pixel(bx, by), nx, ny, depths.min, depths.max,
      door.exists(p => Math.abs((p.x - center.x) * nx + (p.y - center.y) * ny) <= Math.max(32, depths.max - depths.min) &&
        Math.abs((p.x - center.x) * (bx - ax) / width + (p.y - center.y) * (by - ay) / width) < width / 2) || reservedDoor)
    // The Zergling origin is asymmetric; narrow passages may be traversable
    // off-center even when their geometric center line collides with a building.
    val crossLeft = -g.width / 2.0 + ling.extent(-g.tx, -g.ty)
    val crossRight = g.width / 2.0 - ling.extent(g.tx, g.ty)
    val crossings = if (diagonal) Vector(0.0) else
      (Math.ceil(crossLeft).toInt to Math.floor(crossRight).toInt).map(_.toDouble)
    if (crossings.exists(c => (g.outer.toInt - 8 to g.inner.toInt + 8 by 4).forall(d => fits(ling, g.at(c, d), 0)))) Some(g) else None
  }

  // A jagged cliff can produce several rectangles bordering the same passage.
  def dedupGaps(rawGaps: Vector[Gap]): Vector[Gap] = {
    val distinct = new ArrayBuffer[Gap]
    rawGaps.sortBy(g => (g.width, g.center.x, g.center.y)).foreach { g =>
      val duplicate = distinct.indexWhere(other =>
        (other.first == g.first || other.first == g.second || other.second == g.first || other.second == g.second) &&
        other.nx * g.nx + other.ny * g.ny > 0.9 &&
        Math.abs(other.across(g.center)) < Math.min(other.width, g.width) / 2.0 &&
        Math.abs(other.depth(g.center)) <= 128)
      if (duplicate < 0) distinct += g
      else if (g.doorway) distinct(duplicate) = distinct(duplicate).copy(doorway = true)
    }
    distinct.toVector.sortBy(g => (if (g.doorway) 0 else 1, -g.width, g.center.x, g.center.y))
  }
}
