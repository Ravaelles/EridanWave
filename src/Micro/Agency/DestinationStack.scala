package Micro.Agency

import Information.Geography.Pathfinding.Types.TilePath
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Tile}
import Micro.Agency.Destinations.DestinationLevel
import Performance.Cache
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

trait DestinationStack {
  val unit: FriendlyUnitInfo

  val destinations: Vector[Destination] = Destinations.All.map(new Destination(unit, _))
  var path: Option[TilePath] = None

  private def at  (destinationLevel: DestinationLevel)                    : Destination = destinations(destinationLevel.level - destinations.head.level)
  private def set (destinationLevel: DestinationLevel, p: Pixel)          : Destination = at  (destinationLevel).set(p)
  private def set (destinationLevel: DestinationLevel, t: Tile)           : Destination = set (destinationLevel, t.center)
  private def set (destinationLevel: DestinationLevel, p: Option[Pixel])  : Destination = at  (destinationLevel).set(p)
  private def sett(destinationLevel: DestinationLevel, t: Option[Tile])   : Destination = set (destinationLevel, t.map(_.center))

  val home      : Destination = at(Destinations.Home).set(unit.pixel)
  val redoubt   : Destination = at(Destinations.Redoubt)
  val terminus  : Destination = at(Destinations.Terminus).set(unit.pixel)
  val perch     : Destination = at(Destinations.Perch)
  val station   : Destination = at(Destinations.Station)
  val forced    : Destination = at(Destinations.Forced)
  val decision  : Destination = at(Destinations.Decision)

  def destinationsDefined : Seq[Destination]  = destinations.view.filter(_.pixel.isDefined)
  def origin              : Destination       = destinationsDefined.reverseIterator.find(_.level < 0).getOrElse(home)
  def destinationFinal    : Destination       = Maff.minBy(destinationsDefined)(d => ?(d.level < 0, 100, 0) + d.level).getOrElse(terminus)
  // Computed winner, read-only outside Agency: doctrines override through
  // forceTo/forceAway/forceWaypoint below, never by writing this - it
  // addresses whichever level currently wins, so the same line edits a
  // different slot depending on what ran earlier in the frame.
  def destinationNext     : Destination       = Maff.minBy(destinationsDefined)(d => ?(d.level < 0, 100, 0) - d.level).getOrElse(terminus)

  /**
   * Doctrine override (the only destination write outside Agency): a
   * locally-known point that outranks squad routing but yields to the
   * final decision. Sites with their own back-off math keep it and pass
   * the computed point here instead of addressing the winner.
   */
  def forceTo(p: Pixel): Unit = { forced.set(p) }
  def forceTo(p: Option[Pixel]): Unit = { forced.set(p) }
  def forceWaypoint(p: Pixel): Unit = { forced.setWaypoint(p) }
  def forceWaypoint(p: Option[Pixel]): Unit = { forced.setWaypoint(p) }

  /**
   * Doctrine retreat from a threat through the shared separation rule.
   * Only for sites that already used it; everything else keeps its own
   * math and routes the result through forceTo.
   */
  def forceAway(from: Pixel, threat: Pixel, tiles: Double): Unit =
    forceTo(Mathematics.Separation.awayFrom(from, threat, tiles))

  def resetDestinations(): Unit = {
    destinations.foreach(_.clear())
    path = None
    home.set(defaultHome)
    terminus.set(unit.pixel)
    unit.squadIntent.redoubt                                 .foreach(redoubt.set)
    unit.stationDisengage                               .foreach(redoubt.set) // Formation supersedes squadIntent; squadIntent is used to provide a default value
    unit.squadIntent.toNuke                                  .foreach(terminus.set)
    unit.squadIntent.toAttack      .map(_.pixel)             .foreach(terminus.set)
    unit.squadIntent.toGather      .map(_.pixel)             .foreach(terminus.set)
    unit.squadIntent.toHeal        .map(_.pixel)             .foreach(terminus.set)
    unit.squadIntent.toFinish      .map(_.pixel)             .foreach(terminus.set)
    unit.squadIntent.toBoard       .map(_.pixel)             .foreach(terminus.set)
    unit.squadIntent.toBuildActive .map(_.tile.topLeftPixel) .foreach(terminus.set)
    unit.squadIntent.toScoutTiles.headOption                 .foreach(terminus.set)
    unit.squadIntent.terminus                                .foreach(terminus.set) // Explicit squadIntent supersedes implicit squadIntent
    unit.squadIntent.station                                 .foreach(station.set)
    unit.stationEngage                                  .foreach(station.set) // Formation supersedes squadIntent; squadIntent is used to provide a default value
  }

  def safety: Pixel = unit.agent
    .ride.filterNot(unit.transport.contains).map(_.pixel)
    .getOrElse(origin())

  def defaultHome: Pixel = _defaultHome()
  private val _defaultHome = new Cache[Pixel](() =>
    Maff.minBy(
      With.geography.ourBases.filter(base =>
        base.scoutedByEnemy
          || base.naturalOf.exists(_.scoutedByEnemy)
          || base.metro == With.geography.ourMetro))(base =>
      unit.pixelDistanceTravelling(base.heart)
        // Retreat into main
        + ?(base.naturalOf.filter(_.isOurs).exists(_.heart.altitude >= base.heart.altitude) && unit.battle.exists(_.enemy.attackCentroidGround.base.contains(base)), 32 * 40, 0))
      .map(_.heart.center)
      .getOrElse(With.geography.home.center))

  override def toString: String = destinations.filter(_.pixel.isDefined).reverseIterator.mkString("; ")
}
