package Lifecycle

import Debugging.Visualizations.{Animations, Viewport, Visualization}
import Debugging.{Camera, LambdaQueue, Logger, Storyteller}
import Information.Battles.Battles
import Information.Battles.Prediction.Simulation.Simulation
import Information.Counting._
import Information.Fingerprinting.Fingerprints
import Information.GameSense.GameSensor
import Information.Geography.Geography
import Information.Geography.NeoGeo.MapIdentifier
import Information.Geography.Pathfinding.Paths
import Information.Grids.Grids
import Information.Scouting.{Scouting, UnitsShown}
import Lifecycle.Configure.{BwapiData, Configuration, ConfigurationLoader}
import Macro.Allocation._
import Macro.Gathering.Gathering
import Macro.Scheduling.Scheduler
import Macro.Scheduling.Simulation.MacroSim
import Mathematics.Points.Tile
import Micro.Agency.Agency
import Micro.Coordination.Coordinator
import Micro.Matchups.MatchupGraph
import Performance.TaskQueue.{TaskQueueGlobal, TaskQueueParallel}
import Placement.Placement
import Planning.Proxy.Proxies
import Planning.{Blackboard, Yolo}
import ProxyBwapi.Bullets.Bullets
import ProxyBwapi.Players.{PlayerInfo, Players}
import ProxyBwapi.ProxyBWAPI
import ProxyBwapi.UnitTracking.UnitTracker
import Strategery.History.History
import Tactic.Missions.evaluation.MissionHistory
import Strategery.Strategist
import Tactic.Squads.Squads
import Tactic.Tactician
import _root_.Performance.{Latency, PerformanceMonitor, ReactionTimes}
import _root_.Placement.Architecture.Architecture
import bwapi.Flag

import scala.collection.JavaConverters._

object With {

  var frame0ms          : Long                = 0
  // Engine-owned world state lives in WorldState; these forwarders keep
  // every call site compiling while writes stay a Lifecycle affair.
  def frame             : Int                 = WorldState.frame
  def frame_=(value     : Int                ): Unit = { WorldState.frame = value }
  def self              : PlayerInfo          = WorldState.self
  def self_=(value      : PlayerInfo         ): Unit = { WorldState.self = value }
  def neutral           : PlayerInfo          = WorldState.neutral
  def neutral_=(value   : PlayerInfo         ): Unit = { WorldState.neutral = value }
  def enemies           : Vector[PlayerInfo]  = WorldState.enemies
  def enemies_=(value   : Vector[PlayerInfo] ): Unit = { WorldState.enemies = value }
  def friendlies        : Vector[PlayerInfo]  = WorldState.friendlies
  def friendlies_=(value: Vector[PlayerInfo] ): Unit = { WorldState.friendlies = value }
  def mapFileName       : String              = WorldState.mapFileName
  def mapFileName_=(value: String             ): Unit = { WorldState.mapFileName = value }
  def mapCleanName      : String              = WorldState.mapCleanName
  def mapCleanName_=(value: String            ): Unit = { WorldState.mapCleanName = value }
  def mapClock          : String              = WorldState.mapClock
  def mapClock_=(value  : String              ): Unit = { WorldState.mapClock = value }
  def umsMode           : Boolean             = WorldState.umsMode
  def umsMode_=(value   : Boolean             ): Unit = { WorldState.umsMode = value }
  def startNanoTime     : Long                = WorldState.startNanoTime
  def startNanoTime_=(value: Long             ): Unit = { WorldState.startNanoTime = value }
  def mapTileWidth      : Int                 = WorldState.mapTileWidth
  def mapTileWidth_=(value: Int               ): Unit = { WorldState.mapTileWidth = value }
  def mapTileHeight     : Int                 = WorldState.mapTileHeight
  def mapTileHeight_=(value: Int              ): Unit = { WorldState.mapTileHeight = value }
  def mapTileArea       : Int                 = WorldState.mapTileArea
  def mapTileArea_=(value: Int                ): Unit = { WorldState.mapTileArea = value }
  def mapPixelWidth     : Int                 = WorldState.mapPixelWidth
  def mapPixelWidth_=(value: Int              ): Unit = { WorldState.mapPixelWidth = value }
  def mapPixelHeight    : Int                 = WorldState.mapPixelHeight
  def mapPixelHeight_=(value: Int             ): Unit = { WorldState.mapPixelHeight = value }
  def mapPixelPerimeter : Int                 = WorldState.mapPixelPerimeter
  def mapPixelPerimeter_=(value: Int          ): Unit = { WorldState.mapPixelPerimeter = value }
  def mapWalkWidth      : Int                 = WorldState.mapWalkWidth
  def mapWalkWidth_=(value: Int               ): Unit = { WorldState.mapWalkWidth = value }
  def mapWalkHeight     : Int                 = WorldState.mapWalkHeight
  def mapWalkHeight_=(value: Int              ): Unit = { WorldState.mapWalkHeight = value }
  def mapWalkArea       : Int                 = WorldState.mapWalkArea
  def mapWalkArea_=(value: Int                ): Unit = { WorldState.mapWalkArea = value }
  def gameCount         : Int                 = WorldState.gameCount
  def gameCount_=(value : Int                 ): Unit = { WorldState.gameCount = value }

  var game              : bwapi.Game          = _
  var accounting        : Accounting          = _
  var agents            : Agency              = _
  var animations        : Animations          = _
  var architecture      : Architecture        = _
  var bank              : Bank                = _
  var blackboard        : Blackboard          = _
  var battles           : Battles             = _
  var productionHistory : ProductionHistory   = _
  var bullets           : Bullets             = _
  var bwapiData         : BwapiData           = _
  var camera            : Camera              = _
  var coordinator       : Coordinator         = _
  var configuration     : Configuration       = _
  var efficiency        : Efficiency          = _
  var fingerprints      : Fingerprints        = _
  var gathering         : Gathering           = _
  var geography         : Geography           = _
  var grids             : Grids               = _
  var groundskeeper     : Groundskeeper       = _
  var history           : History             = _
  var lambdas           : LambdaQueue         = _
  var latency           : Latency             = _
  var logger            : Logger              = _
  var macroCounts       : MacroCounts         = _
  var macroSim          : MacroSim            = _
  var manners           : Manners             = _
  var missionHistory      : MissionHistory      = _
  var matchups          : MatchupGraph        = _
  var microSlush        : MicroSlush          = _
  var paths             : Paths               = _
  var performance       : PerformanceMonitor  = _
  var placement         : Placement           = _
  var projections       : Projections         = _
  var proxy             : ProxyBWAPI          = _
  var proxies           : Proxies             = _
  var priorities        : Priorities          = _
  var reaction          : ReactionTimes       = _
  var recruiter         : Recruiter           = _
  var scheduler         : Scheduler           = _
  var scouting          : Scouting            = _
  var sense             : GameSensor          = _
  var simulation        : Simulation          = _
  var squads            : Squads              = _
  var strategy          : Strategist          = _
  var storyteller       : Storyteller         = _
  var tactics           : Tactician           = _
  var tasks             : TaskQueueParallel   = _
  var units             : UnitTracker         = _
  var unitsShown        : UnitsShown          = _
  var viewport          : Viewport            = _
  var visualization     : Visualization       = _
  var yolo              : Yolo                = _

  def enemy: PlayerInfo = enemies.head
  def framesSince(previousFrame: Int): Int = Math.max(0, frame - previousFrame)
  def framesUntil(futureFrame: Int): Int = Math.max(0, futureFrame - frame)

  def onFrame(): Unit = {
    frame = With.game.getFrameCount
  }

  def id: Long = startNanoTime

  def onStart(): Unit = {
    startNanoTime = System.nanoTime()
    game = EridanBWClient.getGame
    game.enableFlag(Flag.UserInput)
    game.setLatCom(false)
    game.setLocalSpeed(0)

    ////////////////
    // Basic data //
    ////////////////

    frame             = 0
    proxy             = new ProxyBWAPI
    self              = Players.get(game.self)
    neutral           = Players.get(game.neutral)
    enemies           = game.enemies.asScala.map(Players.get).toVector
    friendlies        = With.self +: game.allies.asScala.map(Players.get).toVector
    mapFileName       = game.mapFileName
    mapCleanName      = MapIdentifier(mapFileName)
    mapClock          = MapIdentifier.clock(new Tile(With.game.self.getUnits.asScala.maxBy(_.getHitPoints).getTilePosition).center)
    mapTileWidth      = game.mapWidth
    mapTileHeight     = game.mapHeight
    mapTileArea       = mapTileWidth * mapTileHeight
    mapPixelWidth     = mapTileWidth * 32
    mapPixelHeight    = mapTileHeight * 32
    mapPixelPerimeter = 2 * mapPixelWidth + 2 * mapPixelHeight
    mapWalkWidth      = mapTileWidth * 4
    mapWalkHeight     = mapTileHeight * 4
    mapWalkArea       = mapWalkWidth * mapWalkHeight

    ///////////////////
    // Configuration //
    ///////////////////

    bwapiData         = new BwapiData
    configuration     = new Configuration
    logger            = new Logger
    ConfigurationLoader.load()

    // Live debugging: prepare combat visualizer IO and launch
    try {
      if (configuration != null && configuration.debugging && configuration.simviz) {
        Debugging.CombatVisIO.ensureDirs()
        Debugging.CombatVisIO.exportMapData()
        Debugging.CombatVisIO.prepareEmptySimFile()
        Debugging.CombatVisIO.launchVisualizer()
      }
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }

    Bootstrap.init()

    With.logger.debug(f"${game.getGameType.toString.replace("_", " ")} on ${game.mapName} at ${game.mapFileName()} as ${self.fullDescription} with ${(self.allies ++ self.enemies).map(_.fullDescription).mkString(", ")}")
  }
}



