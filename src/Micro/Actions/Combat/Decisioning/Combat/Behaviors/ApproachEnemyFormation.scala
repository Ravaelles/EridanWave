package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Formation.OverwhelminglyStronger
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Latch

/**
 * Formation approach (owner order: fight positions, never charges):
 * a warrior the duel sends to fight with nothing hittable and no
 * contact dresses the line instead of attack-moving - slots when
 * scattered, hold when slotted, creep when dressed with a target past
 * the gun. Melee closes anyway, retreats march home, active chases
 * stay with the duel, overwhelming lines charge as today. Returns true
 * when it moved or held the unit.
 */
object ApproachEnemyFormation {

  /**
   * Dress step, in tiles: walk to a slot only this near - anything
   * farther means the formation moved on without this hull, and
   * chasing it strings the line out instead of dressing it. The rally
   * owns stragglers past the leash; inside it the hull holds.
   */
  val dressStepTiles: Double = 2.0

  /**
   * Formation distance, in tiles (owner order): dress the line once a
   * seen enemy stands inside it - fifteen against Terran siege tanks
   * (their shells outrange the line), twelve against everyone else.
   * Farther out the march owns the walk.
   */
  def formationRangeTiles(terranSieged: Boolean): Double =
    if (terranSieged) 15.0 else 12.0

  /**
   * Metronome advance, in tiles, inside a move window each cycle: the
   * held line steps together toward the nearest known enemy instead of
   * freezing in place - a dressed line that never advances parks the
   * map. Every warrior steps, ranged included: outranged frames belong
   * to the formation, and with no hittable target and no contact there
   * is no volley to donate - standing still only trades the map for
   * nothing. The window re-issues the step until the hull actually
   * moves (a single step frame after holds never executes - the
   * order-change skips movement), so the line walks instead of
   * twitching. The duel takes over the moment anything is hittable or
   * in contact, so the steps never walk a gun into blades.
   */
  val advanceEveryFrames: Int = 20
  val advanceWindowFrames: Int = 8
  val advanceStepTiles: Double = 0.85

  /**
   * Stall nudge, in tiles: a dressed hull that has not changed position
   * in this many frames steps forward instead of holding forever. The
   * back line's window steps die in the order-change skip, so without
   * the nudge the far hulls pose while the near ones walk. Self-limiting
   * (a stepped hull moves, so the nudge sleeps until the next stall) -
   * no latch, no flap band.
   */
  val stallFrames: Int = 4
  val stallStepTiles: Double = 1.5

  /**
   * Nearest seen enemy warrior (game seam for the steps below): visible
   * foes only - blind hulls hold, never walk at ghosts. The eligible gate
   * already requires a seen foe inside the dress range, so this always
   * finds one on the reachable path; None means hold.
   */
  private def nearestKnownFoe(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
  }

  /** Dress release margin, in tiles: the line holds formation past the
   * edge it formed at, instead of flapping on sight flicker. */
  val releaseMarginTiles: Double = 1.5

  /** Pack contact, in tiles: an enemy this close to any blade means the pack brawls. */
  val packContactTiles: Double = 2.0

  /** Pack release, in tiles: the dress resumes only once every blade stands this clear. */
  val packReleaseTiles: Double = 3.5

  /**
   * Pack fight signs (pure booleans for the suite): one fighting mate
   * releases the dress - firing, engaged upon, holding a hittable
   * target, standing inside an enemy gun, or with an enemy inside
   * contact range. The 2.0 edge is the entry; the all-clear below is
   * the release.
   */
  def packMateFights(
    fired: Boolean,
    engagedUpon: Boolean,
    hasHittable: Boolean,
    hasThreatInRange: Boolean,
    nearestFoeTiles: Double): Boolean =
    fired || engagedUpon || hasHittable || hasThreatInRange || nearestFoeTiles <= packContactTiles

  /**
   * Pack all-clear (pure booleans for the suite): the dress resumes
   * only with no fight sign on the mate and every foe past the release
   * band - the 2.0/3.5 hysteresis never flaps the line on the edge.
   */
  def packClear(
    fired: Boolean,
    engagedUpon: Boolean,
    hasHittable: Boolean,
    hasThreatInRange: Boolean,
    nearestFoeTiles: Double): Boolean =
    ! fired && ! engagedUpon && ! hasHittable && ! hasThreatInRange && nearestFoeTiles > packReleaseTiles

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! eligible(unit, combat)) return false

    unit.agent.act("Dress")
    // Metronome window (owner order): frames 0-8 of every 20-frame
    // cycle each warrior steps 0.85 tiles at the nearest seen enemy,
    // staggered by id so the line walks without reissuing every order
    // on the same frame. Blind hulls hold (no seen foe, no step).
    if ((With.frame + unit.id) % advanceEveryFrames <= advanceWindowFrames) {
      nearestKnownFoe(unit).foreach { foe =>
        val stepped = unit.pixel.project(foe.pixel, advanceStepTiles * 32.0).walkablePixel
        unit.agent.decision.set(stepped)
        combat.maneuvers.move()
        unit.agent.act("DressAdvance")

        return true
      }
    }
    // Nearby slots are walked to, never the far goal: chasing a
    // formation across the map strings the line out instead of
    // dressing it. A slot past the leash means the pack moved on
    // without this hull: hold, the rally owns stragglers. Slotless
    // hulls hold where they stand.
    unit.formationEngage.flatMap(_(unit)) match {
      case Some(slot) if unit.pixelDistanceCenter(slot) * Maff.inv32 <= dressStepTiles =>
        unit.agent.decision.set(slot)
        combat.maneuvers.move()

        true
      case _ =>
        // Stall nudge (owner order): a dressed hull with a Formation
        // order that stood still for four frames steps 1.5 tiles at
        // the nearest seen enemy instead of holding forever - the back
        // line's window steps die in the order-change skip, so without
        // the nudge the far hulls pose while the near ones walk. Reads
        // the per-frame position memory (a moved-and-returned hull counts
        // as moved); self-limiting, no latch.
        if (unit.lastChangedPosition >= stallFrames) {
          nearestKnownFoe(unit).foreach { foe =>
            val stepped = unit.pixel.project(foe.pixel, stallStepTiles * 32.0).walkablePixel
            unit.agent.decision.set(stepped)
            combat.maneuvers.move()
            unit.agent.act("DressAdvance")

            return true
          }
        }
        // Explicit hold, not the stationary skip: a standing hull keeps
        // whatever order it already holds (map attack-moves, stale
        // marches), so the freeze must own the order outright.
        if (unit.order != ProxyBwapi.Orders.HoldPosition) {
          Micro.Agency.CommanderCore.claimOrder(unit, "ApproachEnemyFormation.hold")
          unit.bwapiUnit.holdPosition()
        }
        Commander.hold(unit)

        true
    }
  }

  /**
   * Seen enemy warrior inside formation range: the line dresses for it
   * (fifteen against Terran siege tanks, twelve against everyone
   * else), never blind and never across the map. Shared with the Close
   * outer gate, so the duel never charges what the line should dress.
   */
  def seenNearFoe(unit: FriendlyUnitInfo): Boolean = {
    val terranSieged = With.enemies.exists(_.isTerran) &&
      With.units.enemy.exists(e => e.alive && e.likelyStillThere && Terran.SiegeTankSieged(e))
    Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32).exists(_ <= formationRangeTiles(terranSieged))
  }
  /**
   * Formation gate (shared with the Hunt stage): the dressed line owns
   * outranged frames, so a ghost hunt never walks it out of formation.
   */
  def eligible(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.unitClass.isWarrior || ! unit.canMove) return false
    if (unit.agent.isScout) return false
    // Hittable frames belong to the duel, contact belongs to the kite.
    if (unit.matchups.targetsInRange.nonEmpty) return false
    if (unit.matchups.threatsInRange.nonEmpty) return false
    // Lone warriors duel, never dress: with no pack there are no slots
    // to hold, and holding hands the chaser a free walk-in to our wall.
    if (isLone(unit)) return false
    // Retreats march home; visible runners stay with the duel, ghosts
    // and approachers dress instead of attack-moving at stale pixels.
    // Let them walk into the dressed line instead of meeting them
    // halfway - only a visibly fleeing target keeps its chase.
    if (combat.shouldRetreat) return false
    if (combat.shouldChase && combat.target.exists(t => t.visible && t.likelyStillThere && t.speedApproaching(unit) < 0)) return false

    // Stragglers rally, they never dress: past the leash the march owns
    // the frame (rally, catch-up), inside it the line owns it. Same six
    // tiles the kite leash reads - one distance for pack cohesion.
    // Squadless hulls march: without a pack there is nothing to dress.
    if (unit.squad.isEmpty) return false
    if (farFromPack(unit)) return false
    // The pack brawls together (owner order): a blade whose mate already
    // fights never dresses - one hull brawling while the rest pose is a
    // donation in detail. Blades only: ranged lines hold their arc while
    // individuals kite. Entry on any fight sign, release only once every
    // blade stands clear (2.0/3.5 band, no boundary flap).
    if (unit.unitClass.melee && packFighting(unit)) return false
    // Dress on a seen enemy inside formation range, and hold the dress
    // past its edge: sight flicker at the boundary must not flap the
    // line between dressing and charging every other frame. Farther
    // out the march owns the walk. Overwhelming lines charge as today.
    if ( ! dressLatched(unit)) return false
    if (OverwhelminglyStronger.skipFormation(unit)) return false

    true
  }

  /**
   * Dress latch (sight hysteresis): forms up on a seen enemy inside
   * the formation range and releases past it with margin - or the
   * moment the duel owns the frame. Shared with the Close outer gate,
   * so the duel never charges what the line should dress.
   */
  private val dressLatch = new Latch

  private def dressLatched(unit: FriendlyUnitInfo): Boolean = {
    val terranSieged = With.enemies.exists(_.isTerran) &&
      With.units.enemy.exists(e => e.alive && e.likelyStillThere && Terran.SiegeTankSieged(e))
    val nearestSeenFoeTiles = Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity)
    dressLatch(
      unit.id,
      fires = nearestSeenFoeTiles <= formationRangeTiles(terranSieged),
      releases = nearestSeenFoeTiles > formationRangeTiles(terranSieged) + releaseMarginTiles)
  }

  /** Past the pack leash: the straggler rejoins instead of dressing. */
  private def farFromPack(unit: FriendlyUnitInfo): Boolean =
    unit.squad.exists(squad => {
      val pack = if (unit.flying) squad.centroidAir else squad.centroidGround
      unit.pixelDistanceCenter(pack) * Maff.inv32 > KiteAway.leashTiles
    })

  /**
   * Pack fight latch (sight hysteresis shape): one fighting mate holds
   * the whole blade line out of the dress until every blade stands
   * clear - the release band absorbs the kill-edge flicker the way the
   * dress latch absorbs sight flicker.
   */
  private val packFightLatch = new Latch

  private def packFighting(unit: FriendlyUnitInfo): Boolean = {
    val mates = unit.squad.map(_.units.toSeq).getOrElse(Seq(unit))
    packFightLatch(
      unit.id,
      fires    = mates.exists(mateFights),
      releases = mates.forall(mateClear))
  }

  private def mateFights(mate: FriendlyUnitInfo): Boolean =
    packMateFights(
      fired             = mate.cooldownLeft > 0,
      engagedUpon       = mate.matchups.engagedUpon,
      hasHittable       = mate.matchups.targetsInRange.nonEmpty,
      hasThreatInRange  = mate.matchups.threatsInRange.nonEmpty,
      nearestFoeTiles   = Maff.minBy(mate.matchups.threats.view.filter(_.likelyStillThere))(
        mate.pixelDistanceEdge(_))
        .map(mate.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity))

  private def mateClear(mate: FriendlyUnitInfo): Boolean =
    packClear(
      fired             = mate.cooldownLeft > 0,
      engagedUpon       = mate.matchups.engagedUpon,
      hasHittable       = mate.matchups.targetsInRange.nonEmpty,
      hasThreatInRange  = mate.matchups.threatsInRange.nonEmpty,
      nearestFoeTiles   = Maff.minBy(mate.matchups.threats.view.filter(_.likelyStillThere))(
        mate.pixelDistanceEdge(_))
        .map(mate.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity))

  /** No pack, no line: warriors without another living warrior beside them. */
  private def isLone(unit: FriendlyUnitInfo): Boolean =
    ! unit.squad.exists(_.units.exists(o => o.id != unit.id && o.alive && o.unitClass.isWarrior))
}
