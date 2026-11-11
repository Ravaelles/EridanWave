package testkit

import Lifecycle.With
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import bwapi.{Game, Player, Position, UnitType, Unit => BwUnit}
import org.mockito.Mockito._

import scala.collection.JavaConverters._

/**
 * Minimal game-free world for suites: seeded map dims and frame plus
 * Mockito-stubbed bwapi units. Mockito (test scope, offline repo) answers
 * unstubbed calls with false/0/null, so stub everything the constructor
 * path reads. Suites that need deeper world state (matchups, squads) stay
 * out - thin seams only.
 */
object TestKit {

  def seedWorld(frame: Int = 0, mapTiles: Int = 128): Unit = {
    With.frame = frame
    With.mapTileWidth = mapTiles
    With.mapTileHeight = mapTiles
    val player = mock(classOf[Player])
    val game = mock(classOf[Game])
    when(game.getPlayers).thenReturn(List(player).asJava)
    With.game = game
    With.proxy = new ProxyBwapi.ProxyBWAPI
    With.units = new ProxyBwapi.UnitTracking.UnitTracker
  }

  def mockUnit(
    unitType: UnitType,
    x: Int = 0,
    y: Int = 0,
    hitPoints: Int = 100,
    shields: Int = 0): BwUnit = {
    val unit = mock(classOf[BwUnit])
    when(unit.getType).thenReturn(unitType)
    when(unit.getHitPoints).thenReturn(hitPoints)
    when(unit.getShields).thenReturn(shields)
    when(unit.getPosition).thenReturn(new Position(x, y))
    when(unit.exists()).thenReturn(true)
    when(unit.isCompleted).thenReturn(true)
    when(unit.getPlayer).thenReturn(mock(classOf[Player]))
    when(unit.getOrder).thenReturn(bwapi.Order.Stop)
    unit
  }

  def friendly(unitType: UnitType, id: Int, x: Int = 0, y: Int = 0): FriendlyUnitInfo = {
    val unit = new FriendlyUnitInfo(mockUnit(unitType, x, y), id)
    unit.changeUnitType(ProxyBwapi.UnitClasses.UnitClasses.get(unitType))
    unit
  }
}
