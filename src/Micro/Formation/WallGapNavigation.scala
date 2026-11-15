package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box}

import scala.collection.mutable.{ArrayBuffer, HashMap}

/**
 * Walk-terrain snapshot (prefix sums over the wall's surroundings, no game
 * access past the walkability function): constant-time footprint checks
 * plus the merged blocked rectangles the entrance search walks around.
 * One snapshot per foundation layout; navigation instances memoize on it.
 */
class WalkTerrain(boxes: Vector[Box], bounds: Box, walkable: (Int, Int) => Boolean) {

  // Prefix sums make footprint/terrain collision checks constant-time. The extra
  // margin includes reinforcement positions and Zergling contact samples.
  val minX = Math.floorDiv(bounds.left - 160, 8)
  val minY = Math.floorDiv(bounds.top - 160, 8)
  val width = Math.floorDiv(bounds.right + 160, 8) - minX + 1
  val height = Math.floorDiv(bounds.bottom + 160, 8) - minY + 1
  private val terrain = Array.tabulate(width * height)(i => walkable(minX + i % width, minY + i / width))
  private val stride = width + 1
  private val blocked = Array.fill(stride * (height + 1))(0)
  for (y <- 0 until height; x <- 0 until width) {
    val i = x + 1 + (y + 1) * stride
    blocked(i) = (if (terrain(x + y * width)) 0 else 1) + blocked(i - 1) + blocked(i - stride) - blocked(i - stride - 1)
  }
  private def terrainOpen(x: Int, y: Int): Boolean = terrain(x - minX + (y - minY) * width)

  def fits(body: Body, p: Pixel, clearance: Int = 1): Boolean = {
    // Movement flush against buildings or terrain can stop short; leave one pixel.
    val b = body.at(p).expand(clearance)
    if (boxes.exists(_.intersects(b))) return false

    val x0 = Math.floorDiv(b.left, 8) - minX
    val y0 = Math.floorDiv(b.top, 8) - minY
    val x1 = Math.floorDiv(b.right - 1, 8) - minX + 1
    val y1 = Math.floorDiv(b.bottom - 1, 8) - minY + 1
    x0 >= 0 && y0 >= 0 && x1 <= width && y1 <= height &&
      blocked(x1 + y1 * stride) - blocked(x0 + y1 * stride) - blocked(x1 + y0 * stride) + blocked(x0 + y0 * stride) == 0
  }

  // Merge walk cells into rectangles so a straight cliff is one side of one gap.
  def blockedBoxes: Vector[Box] = {
    val result = new ArrayBuffer[Box]
    var active = Map.empty[(Int, Int), Int]
    for (y <- Math.floorDiv(bounds.top, 8) to Math.floorDiv(bounds.bottom, 8)) {
      val next = new HashMap[(Int, Int), Int]
      var x = Math.floorDiv(bounds.left, 8)
      val end = Math.floorDiv(bounds.right, 8)
      while (x <= end) {
        if (terrainOpen(x, y)) x += 1
        else {
          val start = x
          while (x <= end && ! terrainOpen(x, y)) x += 1
          val key = (start, x)
          val index = active.getOrElse(key, result.size)
          if (index == result.size) result += Box(8 * start, 8 * y, 8 * x, 8 * (y + 1))
          else result(index) = result(index).copy(bottom = 8 * (y + 1))
          next(key) = index
        }
      }
      active = next.toMap
    }
    result.toVector
  }

  private val navigations = new HashMap[Body, WallGapNavigation]
  def navigationFor(body: Body, inside: Pixel): WallGapNavigation =
    navigations.getOrElseUpdate(body,
      new WallGapNavigation(body, minX * 8, minY * 8, width, height, fits(_, _, _), inside))
}

/**
 * Wall-gap navigation (pure grid BFS, no game access): a fitting pixel can
 * still be trapped behind a neck narrower than the defender, so approach
 * paths match the game grid and exact pixels are only the final step.
 * The terrain snapshot supplies `fits`; the grid frame rides in as plain
 * numbers so this stays suite-testable.
 */
class WallGapNavigation(
  body: Body,
  originXPx: Int,
  originYPx: Int,
  columns: Int,
  rows: Int,
  fits: (Body, Pixel, Int) => Boolean,
  inside: Pixel) {

  private val step = 8
  private val count = columns * rows
  private def pixel(i: Int): Pixel = Pixel(originXPx + i % columns * step, originYPx + i / columns * step)
  private val open = Array.tabulate(count)(i => fits(body, pixel(i), 1))
  private val reached = Array.fill(count)(false)
  private val queue = new Array[Int](count)
  private def neighbors(i: Int)(visit: Int => Unit): Unit = {
    if (i % columns > 0) visit(i - 1)
    if (i % columns + 1 < columns) visit(i + 1)
    if (i >= columns) visit(i - columns)
    if (i + columns < count) visit(i + columns)
  }
  private val seed: Option[Int] = {
    val seeds = open.indices.filter(i => open(i))
    if (seeds.isEmpty) None else Some(seeds.minBy(i => pixel(i).pixelDistanceSquared(inside)))
  }
  seed.foreach { start =>
    var head = 0
    var tail = 1
    queue(0) = start
    reached(start) = true
    while (head < tail) {
      val i = queue(head)
      head += 1
      neighbors(i)(j => {
        if (open(j) && ! reached(j)) {
          reached(j) = true
          queue(tail) = j
          tail += 1
        }
      })
    }
  }
  private def clear(from: Pixel, to: Pixel): Boolean = {
    val length = Math.max(1, Math.ceil(from.pixelDistance(to)).toInt)
    (0 to length).forall(i => fits(body, Pixel(
      Math.round(from.x + (to.x - from.x).toDouble * i / length).toInt,
      Math.round(from.y + (to.y - from.y).toDouble * i / length).toInt), 1))
  }
  private def node(p: Pixel): Option[Int] = {
    val x = Math.floorDiv(p.x - originXPx, step)
    val y = Math.floorDiv(p.y - originYPx, step)
    (for (dy <- 0 to 1; dx <- 0 to 1 if x + dx >= 0 && y + dy >= 0 && x + dx < columns && y + dy < rows)
      yield x + dx + (y + dy) * columns)
      .filter(i => reached(i) && clear(p, pixel(i))).sortBy(i => pixel(i).pixelDistanceSquared(p)).headOption
  }
  def reachable(p: Pixel): Boolean = node(p).isDefined
  private val routes = new HashMap[(Int, Pixel), Vector[Pixel]]
  private def route(from: Pixel, to: Pixel): Vector[Pixel] = {
    val start = node(from)
    val end = node(to)
    if (start.isEmpty || end.isEmpty) return Vector.empty

    val parent = Array.fill(count)(-1)
    var head = 0
    var tail = 1
    queue(0) = start.get
    parent(start.get) = start.get
    while (head < tail && parent(end.get) < 0) {
      val i = queue(head)
      head += 1
      neighbors(i)(j => {
        if (reached(j) && parent(j) < 0) {
          parent(j) = i
          queue(tail) = j
          tail += 1
        }
      })
    }
    if (parent(end.get) < 0) return Vector.empty

    val reverse = scala.collection.mutable.ArrayBuffer(pixel(end.get))
    var i = end.get
    while (i != start.get) {
      i = parent(i)
      reverse += pixel(i)
    }
    val path = reverse.reverse.toVector
    // Keep corners only; the final segment retains the exact sub-tile goal.
    path.indices.filter(i => i == 0 || i + 1 == path.size ||
      path(i).subtract(path(i - 1)) != path(i + 1).subtract(path(i))).map(i => path(i)).toVector :+ to
  }
  def waypoint(id: Int, from: Pixel, to: Pixel): Pixel = {
    if ( ! reachable(from)) return to

    def visible(path: Vector[Pixel]): Option[Pixel] = path.reverseIterator
      .find(p => p.pixelDistanceSquared(from) > 4 && clear(from, p))
    val next = if (clear(from, to)) to else {
      val key = (id, to)
      visible(routes.getOrElseUpdate(key, route(from, to))).orElse {
        // A spawning unit may initially overlap a building or be outside the grid.
        routes(key) = route(from, to)
        visible(routes(key))
      }.getOrElse(to)
    }
    // Short moves keep the game from replanning around a building's tile bounds.
    from.projectUpTo(next, 64)
  }
}
