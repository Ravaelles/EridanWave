package Lifecycle

import ProxyBwapi.Players.PlayerInfo

/**
 * Engine-owned world state (the input half of With): frame, players and
 * map dimensions. Only Lifecycle writes it (With.onStart, EridanWave,
 * UmsMode); everyone else reads through With's forwarders, so the call
 * sites keep compiling while "who may mutate what" has an answer.
 */
object WorldState {

  var frame             : Int                 = 0
  var self              : PlayerInfo          = _
  var neutral           : PlayerInfo          = _
  var enemies           : Vector[PlayerInfo]  = _
  var friendlies        : Vector[PlayerInfo]  = _
  var mapFileName       : String              = _
  var mapCleanName      : String              = _
  var mapClock          : String              = _
  var umsMode           : Boolean             = false
  var startNanoTime     : Long                = 0
  var mapTileWidth      : Int                 = 0
  var mapTileHeight     : Int                 = 0
  var mapTileArea       : Int                 = 0
  var mapPixelWidth     : Int                 = 0
  var mapPixelHeight    : Int                 = 0
  var mapPixelPerimeter : Int                 = 0
  var mapWalkWidth      : Int                 = 0
  var mapWalkHeight     : Int                 = 0
  var mapWalkArea       : Int                 = 0
  // Session counter for carry-over detection (ghosts): ids and handles
  // restart every game, so frame-zero units are only stale from game two on.
  var gameCount         : Int                 = 0
}
