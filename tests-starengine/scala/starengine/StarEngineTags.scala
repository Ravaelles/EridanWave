package starengine

import org.scalatest.Tag

/**
 * Topic tags for StarEngine scenarios (one home for the vocabulary):
 * `scripts/starengine.sh --filter reaver` runs every scenario tagged
 * `Reaver`, `--filter reaver,shuttle` runs scenarios carrying either.
 * Tags mark TOPICS, not verdicts - one scenario usually proves both
 * the allow and the deny leg of its rule (a board suite shows when
 * the hull embarks and when it stays out). Suites gain tags
 * incrementally, next to the scenarios they describe; never invent a
 * tag without tagging a scenario with it. `scripts/starengine-tags.py`
 * prints the catalog and fails on untagged suites or unused tags.
 */
object StarEngineTags {

  /** The Reaver half of the pair: boarding, fighting and unloading it. */
  object Reaver extends Tag("starengine.reaver")

  /** The Shuttle half: fetching, escorting, fleeing and extracting. */
  object Shuttle extends Tag("starengine.shuttle")

  /** Embark discipline: when the hull boards and when it stays out. */
  object Board extends Tag("starengine.board")

  /** Drop discipline: when the ride unloads and when it holds cargo. */
  object Drop extends Tag("starengine.drop")

  /** Dragoon duels: mirrors, packs, stands, kiting and carrier hunts. */
  object Dragoon extends Tag("starengine.dragoon")

  /** Zealot balls: mirrors, packs, leashes and static grinding. */
  object Zealot extends Tag("starengine.zealot")

  /** Scout doctrine: carrier hunting in pairs, static standoffs. */
  object Scout extends Tag("starengine.scout")

  /** Carrier doctrine: victim ladders, berths and tank brawls. */
  object Carrier extends Tag("starengine.carrier")

  /** Corsair hunts: Overlords, flee margins and muta pressure. */
  object Corsair extends Tag("starengine.corsair")

  /** High Templar play: storm leads, ranges and stands. */
  object Templar extends Tag("starengine.templar")

  /** Dark Templar raids: prey, detectors, mines and dither. */
  object DT extends Tag("starengine.dt")

  /** Observer play: fleeing hunters and detector standoffs. */
  object Observer extends Tag("starengine.observer")

  /** Psionic storms: leads, ranges, scenarios and storm stands. */
  object Psionic extends Tag("starengine.psionic")

  /** Marching dressed: columns, flaps and pack movement. */
  object March extends Tag("starengine.march")

  /** Formations: holds, dress waits, slots and standoffs. */
  object Formation extends Tag("starengine.formation")

  /** Cluster posture: shared Fight/HoldLine/Withdraw/Regroup verdicts. */
  object Posture extends Tag("starengine.posture")

  /** Retreat discipline: latches, floors, shock and scored flees. */
  object Retreat extends Tag("starengine.retreat")

  /** Cluster roles: spearheads, flanks, supports and rearguards. */
  object Roles extends Tag("starengine.roles")

  /** Static defense: keep-offs, standoffs, breaches and contains. */
  object Static extends Tag("starengine.static")

  /** Victim selection: ladders, chaff, spreads and picks. */
  object Targeting extends Tag("starengine.targeting")

  /** Hunts: prey drives, chases and contact hunts. */
  object Hunt extends Tag("starengine.hunt")

  /** Cohesion: packs, leashes, roams and drift rejoins. */
  object Cohesion extends Tag("starengine.cohesion")

  /** Early-mirror pressure: outranged handfuls break off. */
  object Pressure extends Tag("starengine.pressure")

  /** Wall breaches: sealed wall-ins chewed through. */
  object Wall extends Tag("starengine.wall")

  /** Contains: holding the line against trickles and packs. */
  object Contain extends Tag("starengine.contain")

  /** Raze details: expansion kills and suicide runs. */
  object Raze extends Tag("starengine.raze")

  /** Ferry trips: rides carrying cargo across the map. */
  object Ferry extends Tag("starengine.ferry")

  /** Rejoins: empty rides converging on mates left on foot. */
  object Rejoin extends Tag("starengine.rejoin")

  /** Artillery: siege shells stepped around, never through. */
  object Artillery extends Tag("starengine.artillery")

  /** Minefields: Spider Mines sidestepped, never swept. */
  object Mine extends Tag("starengine.mine")

  /** Detectors: eyes, overlords and the dither around them. */
  object Detector extends Tag("starengine.detector")

  /** Raid status: oldest pair raids, bleeding pairs support. */
  object Raid extends Tag("starengine.raid")
}
