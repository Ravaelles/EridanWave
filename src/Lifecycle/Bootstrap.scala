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


/**
 * Game-start construction (the Systems half of With.onStart): every service
 * in dependency order - PerformanceMonitor first (tasks sample it while
 * constructing), TaskQueue last (it references other systems). With stays
 * the registry; this object only states the build order in one place.
 */
object Bootstrap {

  def init(): Unit = {
    ////////////////////
    // Normal systems //
    ////////////////////

    TerrainAnalysis.analyze(With.game, With.mapFileName)

    // Order-dependent initialization:
    // PerformanceMonitor must exist when creating any task
    With.performance       = new PerformanceMonitor
    With.agents            = new Agency
    With.animations        = new Animations
    With.architecture      = new Architecture
    With.bank              = new Bank
    With.battles           = new Battles
    With.blackboard        = new Blackboard
    With.productionHistory = new ProductionHistory
    With.bullets           = new Bullets
    With.camera            = new Camera
    With.coordinator       = new Coordinator
    With.accounting        = new Accounting
    With.efficiency        = new Efficiency
    With.fingerprints      = new Fingerprints
    With.groundskeeper     = new Groundskeeper
    With.gathering         = new Gathering
    With.geography         = new Geography
    With.grids             = new Grids
    With.history           = new History
    With.lambdas           = new LambdaQueue
    With.latency           = new Latency
    With.macroCounts       = new MacroCounts
    With.macroSim          = new MacroSim
    With.manners           = new Manners
    With.missionHistory      = new MissionHistory
    With.matchups          = new MatchupGraph
    With.microSlush        = new MicroSlush
    With.paths             = new Paths
    With.placement         = new Placement
    With.priorities        = new Priorities
    With.projections       = new Projections
    With.proxies           = new Proxies
    With.reaction          = new ReactionTimes
    With.recruiter         = new Recruiter
    With.sense             = new GameSensor
    With.scheduler         = new Scheduler
    With.scouting          = new Scouting
    With.simulation        = new Simulation
    With.squads            = new Squads
    With.strategy          = new Strategist
    With.storyteller       = new Storyteller
    With.tactics           = new Tactician
    With.units             = new UnitTracker
    With.unitsShown        = new UnitsShown
    With.viewport          = new Viewport
    With.visualization     = new Visualization
    With.yolo              = new Yolo

    // Order-dependent initialization:
    // TaskQueue comes last because it references other systems
    With.tasks             = new TaskQueueGlobal
  }
}
