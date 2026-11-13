package Micro.Actions.Protoss.Shuttle

import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitClasses.UnitClasses
import bwapi.UnitType

/**
 * Pure Reaver-drop simulator (no game access): a Shuttle carrying a Reaver
 * against Hydralisks, stepped frame by frame. Speeds, ranges and pools
 * come from live BWAPI statics (see SimParams.production), so the sim
 * cannot drift from the engine - only deliberately documented
 * simplifications may: point units (dimensions folded into ranges),
 * homing Scarabs (damage lands if the target lives), instant turns.
 *
 * Hydralisks run the owner's script exactly: every frame each Hydra walks
 * at full speed at whichever of Shuttle/Reaver is nearer (ties go to the
 * Reaver) and hits for full damage when in range. The drop side runs the
 * doctrine under test: pick a landing zone off the Hydra pack, unload
 * before their reach plus the preemptive band touches us, shoot, and
 * reboard the moment they close - the sim asserts the landing happens and
 * nobody ever lands a hit on us.
 *
 * The point is debugging the dance, not the engine: tune the policy here
 * in milliseconds instead of three-minute UMS games.
 */
object ReaverDropSim {

  /** Production numbers, read off BWAPI statics so they cannot rot. */
  case class SimParams(
    shuttleSpeedPx   : Double,
    hydraSpeedPx     : Double,
    reaverSpeedPx    : Double,
    scarabSpeedPx    : Double,
    hydraRangePx     : Double,
    reaverRangePx    : Double,
    hydraDmg         : Int,
    hydraCdFrames    : Int,
    scarabDmg        : Int,
    reaverCdFrames   : Int,
    unloadFrames     : Int,
    loadFrames       : Int,
    loadRadiusPx     : Double,
    shuttleHp        : Int,
    shuttleShields   : Int,
    reaverHp         : Int,
    reaverShields    : Int,
    hydraHp          : Int,
    preemptBaseTiles : Double,
    preemptWoundTiles: Double,
    boardTriggerTiles: Double,
    dropStandoffTiles: Double)

  object SimParams {

    /** Mirror of production: Shuttle/Reaver/Hydralisk statics + doctrine bands. */
    def production(): SimParams = {
      val shuttle = UnitClasses.get(UnitType.Protoss_Shuttle)
      val hydra   = UnitClasses.get(UnitType.Zerg_Hydralisk)
      val reaver  = UnitClasses.get(UnitType.Protoss_Reaver)
      val scarab  = UnitClasses.get(UnitType.Protoss_Scarab)
      SimParams(
        shuttleSpeedPx    = shuttle.topSpeed,
        hydraSpeedPx      = hydra.topSpeed,
        reaverSpeedPx     = reaver.topSpeed,
        scarabSpeedPx     = scarab.topSpeed,
        hydraRangePx      = hydra.pixelRangeGround,
        reaverRangePx     = reaver.pixelRangeGround,
        hydraDmg          = hydra.effectiveGroundDamage,
        hydraCdFrames     = hydra.groundDamageCooldown,
        scarabDmg         = 100,
        reaverCdFrames    = 60,
        unloadFrames      = 5,
        loadFrames        = 10,
        loadRadiusPx      = 2 * 32.0,
        shuttleHp         = shuttle.maxHitPoints,
        shuttleShields    = shuttle.maxShields,
        reaverHp          = reaver.maxHitPoints,
        reaverShields     = reaver.maxShields,
        hydraHp           = hydra.maxHitPoints,
        preemptBaseTiles  = ShuttleFleeGeometry.baseFleeMarginTiles,
        preemptWoundTiles = ShuttleFleeGeometry.woundFleeBonusTiles,
        // Board while the pack is still outside Hydra range plus volley
        // slack: loading takes 10 frames under the guns that just closed.
        boardTriggerTiles = 7.0,
        dropStandoffTiles = 9.0)
    }
  }

  case class P(x: Double, y: Double) {
    def dist(o: P): Double = Math.hypot(x - o.x, y - o.y)
    def towards(o: P, d: Double): P = {
      val d0 = dist(o)
      if (d0 == 0) this else P(x + (o.x - x) / d0 * d, y + (o.y - y) / d0)
    }
    def away(o: P, d: Double): P = towards(P(2 * x - o.x, 2 * y - o.y), d)
  }

  class Hydra(var pos: P, var hp: Int, var cdLeft: Int = 0)

  class Scarab(var pos: P, val target: Hydra, val arrival: Int)

  /** Landing-zone pick: the base corner/edge farthest from the Hydra pack. */
  def chooseDropPoint(base: P, halfTiles: Double, hydras: Seq[P], shuttle: P): P = {
    val r = halfTiles * 32.0
    val candidates = Seq(
      P(base.x - r, base.y - r), P(base.x + r, base.y - r),
      P(base.x - r, base.y + r), P(base.x + r, base.y + r),
      P(base.x - r, base.y), P(base.x + r, base.y),
      P(base.x, base.y - r), P(base.x, base.y + r))
    candidates.maxBy(c => (c hydraGap hydras, -c.dist(shuttle)))
  }

  private implicit class GapOps(c: P) {
    def hydraGap(hydras: Seq[P]): Double =
      if (hydras.isEmpty) Double.PositiveInfinity else hydras.map(c.dist).min
  }

  class World(
    val p: SimParams,
    val base: P,
    val baseHalfTiles: Double,
    var shuttle: P,
    var shuttleHp: (Int, Int),
    var reaver: P,
    var reaverHp: (Int, Int),
    var loaded: Boolean,
    var unloadLeft: Int = 0,
    var loadLeft: Int = 0,
    var scarabCd: Int = 0,
    var orderBoard: Boolean = false,
    var dropPoint: P,
    val hydras: Seq[Hydra],
    var scarabs: Seq[Scarab] = Seq.empty,
    var frame: Int = 0,
    val hits: collection.mutable.ArrayBuffer[(Int, String, Int)] = collection.mutable.ArrayBuffer.empty,
    var reversals: Int = 0,
    var unloads: Int = 0,
    var scarabsFired: Int = 0,
    var escaping: Boolean = false) {

  private val escapeExtraPx: Double = 400.0

    private var lastDir: Option[(Double, Double)] = None

    def preemptPx: Double =
      (p.preemptBaseTiles + p.preemptWoundTiles * 0.0) * 32.0

    def nearestHydra(pos: P): Option[Hydra] =
      if (hydras.isEmpty) None else Some(hydras.minBy(_.pos.dist(pos)))

    def step(): Unit = {
      // Shuttle: dash pickup overrides everything; a loaded hull that
      // tasted the pack escapes with commitment - no turning back into
      // trailers - and then picks a FRESH landing zone far from them.
      // Fixed drop points plus trailers mean dancing on the old spot.
      if (loaded) {
        val gap = nearestHydra(shuttle).map(h => shuttle.dist(h.pos)).getOrElse(Double.PositiveInfinity)
        if (gap < p.hydraRangePx + preemptPx) escaping = true
        else if (gap > p.hydraRangePx + preemptPx + escapeExtraPx) {
          if (escaping) {
            dropPoint = chooseDropPoint(base, baseHalfTiles, hydras.map(_.pos), shuttle)
            escaping = false
          }
        }
        if (escaping) {
          nearestHydra(shuttle).foreach(h => moveShuttle(shuttle.away(h.pos, p.shuttleSpeedPx)))
        } else if (shuttle.dist(dropPoint) > 32.0) routeToDrop()
        else if (safeToUnload && unloadLeft == 0) unloadLeft = p.unloadFrames
      } else if (orderBoard) {
        moveShuttle(reaver)
        if (loadLeft == 0 && shuttle.dist(reaver) < p.loadRadiusPx) loadLeft = p.loadFrames
      } else if (shuttle.dist(dropPoint) > 32.0) routeToDrop()
      if (unloadLeft > 0) {
        unloadLeft -= 1
        if (unloadLeft == 0) { loaded = false; reaver = shuttle; unloads += 1 }
      }
      if (loadLeft > 0) {
        loadLeft -= 1
        if (loadLeft == 0) { loaded = true; orderBoard = false; reaver = shuttle }
      }
      if (loaded) reaver = shuttle

      // Reaver: shoot the nearest Hydra in reach on cooldown; call the
      // Shuttle back the moment one closes to boarding range.
      if ( ! loaded) {
        if (scarabCd > 0) scarabCd -= 1
        nearestHydra(reaver).foreach(h => {
          if (reaver.dist(h.pos) < p.boardTriggerTiles * 32.0) orderBoard = true
          if (scarabCd == 0 && reaver.dist(h.pos) <= p.reaverRangePx) {
            scarabCd = p.reaverCdFrames
            scarabsFired += 1
            scarabs :+= new Scarab(reaver, h, frame + (reaver.dist(h.pos) / p.scarabSpeedPx).toInt + 1)
          }
        })
      }

      // Scarabs home; Hydras walk at the nearer hull and hit in range.
      val arrived = scarabs.filter(_.arrival <= frame)
      arrived.foreach(s => if (hydras.contains(s.target) && s.target.hp > 0) {
        s.target.hp -= p.scarabDmg
      })
      scarabs = scarabs.filterNot(arrived.contains)
      hydras.filter(_.hp > 0).foreach(h => {
        if (h.cdLeft > 0) h.cdLeft -= 1
        // A loaded Reaver rides inside the hull: only the Shuttle exists.
        val targetIsReaver = ! loaded && reaver.dist(h.pos) <= shuttle.dist(h.pos)
        val targetPos = if (targetIsReaver) reaver else shuttle
        if (h.pos.dist(targetPos) <= p.hydraRangePx) {
          if (h.cdLeft == 0) {
            h.cdLeft = p.hydraCdFrames
            deal(targetIsReaver, p.hydraDmg)
          }
        } else {
          h.pos = h.pos.towards(targetPos, Math.min(p.hydraSpeedPx, h.pos.dist(targetPos)))
        }
      })

      frame += 1
    }

    private def safeToUnload: Boolean =
      nearestHydra(shuttle).forall(h => shuttle.dist(h.pos) >= p.hydraRangePx + preemptPx)

    /** Threat-aware approach: goal pull blended with a repulsor off the
      * nearest Hydra inside the skirt. Nearest-only on purpose: a summed
      * pack outpulls the goal and stalls the approach in equilibrium. */
    private def routeToDrop(): Unit = {
      val toGoal = sub(dropPoint, shuttle)
      var dx = toGoal._1
      var dy = toGoal._2
      val skirt = p.hydraRangePx + preemptPx + 160.0
      nearestHydra(shuttle).foreach(h => {
        val d = shuttle.dist(h.pos)
        if (d < skirt && d > 0) {
          val w = 4.0 * (1 - d / skirt)
          dx += (shuttle.x - h.pos.x) / d * w
          dy += (shuttle.y - h.pos.y) / d * w
        }
      })
      val n = Math.hypot(dx, dy)
      if (n == 0) return

      val step = Math.min(p.shuttleSpeedPx, shuttle.dist(dropPoint))
      val dir = (dx / n, dy / n)
      lastDir.foreach(ld => if (ld._1 * dir._1 + ld._2 * dir._2 < -0.5) reversals += 1)
      lastDir = Some(dir)
      shuttle = P(shuttle.x + dir._1 * step, shuttle.y + dir._2 * step)
    }

    private def sub(a: P, b: P): (Double, Double) = {
      val d = a.dist(b)
      if (d == 0) (0.0, 0.0) else ((a.x - b.x) / d, (a.y - b.y) / d)
    }

    private def moveShuttle(to: P): Unit = {
      val d = shuttle.dist(to)
      if (d == 0) return

      val step = Math.min(p.shuttleSpeedPx, d)
      val dir = ((to.x - shuttle.x) / d, (to.y - shuttle.y) / d)
      lastDir.foreach(ld => if (ld._1 * dir._1 + ld._2 * dir._2 < -0.5) reversals += 1)
      lastDir = Some(dir)
      shuttle = shuttle.towards(to, step)
    }

    private def deal(toReaver: Boolean, dmg: Int): Unit = {
      if (toReaver) {
        val (hp, sh) = reaverHp
        val sHit = Math.min(sh, dmg)
        reaverHp = (hp - (dmg - sHit), sh - sHit)
        hits += ((frame, "Reaver", dmg))
      } else {
        val (hp, sh) = shuttleHp
        val sHit = Math.min(sh, dmg)
        shuttleHp = (hp - (dmg - sHit), sh - sHit)
        hits += ((frame, "Shuttle", dmg))
      }
    }

    def run(maxFrames: Int): Unit =
      while (frame < maxFrames && hydras.exists(_.hp > 0) && reaverHp._1 > 0 && shuttleHp._1 > 0) step()
  }
}
