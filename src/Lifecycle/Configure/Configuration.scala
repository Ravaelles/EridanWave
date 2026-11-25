package Lifecycle.Configure

import Strategery.{DefaultPlaybook, Playbook}
import Utilities.Time.Seconds

case class Configuration(
  /////////////////////////
  // Tournament settings //
  /////////////////////////

  enableSurrenders: Boolean = false,
  enableChat: Boolean = false,
  frameTargetMs: Int = 20,
  frameLimitMs: Int = 35,

  ///////////////////
  // Mode settings //
  ///////////////////

  logstd: Boolean = false,
  humanMode: Boolean = false,
  visualizeDebug: Boolean = false,
  debugging: Boolean = false,
  simviz: Boolean = false,
  cherryVis: Boolean = true,
  auditFormationPaths: Boolean = false,
  logCombatForecasts: Boolean = false,
  formationAudit: Boolean = false,
  detectBreakpoints: Boolean = false,
  camera: Boolean = false,

  ////////////
  // Strategy //
  ////////////

  targetWinrate: Double = 0.75,
  historyHalfLife: Double = 3.0,
  recentFingerprints: Int = 2,
  fixedBuilds: String = "",
  forcedPlaybook: Option[Playbook] = None,

  /////////////
  // Battles //
  /////////////

  simulationAsynchronous: Boolean = true,
  simulationFrames: Int = Seconds(15)(),
  simulationProximityFrames: Int = Seconds(5)(),
  simulationResolution: Int = 8,
  simulationDamageValueRatio: Double = 0.1,
  simulationRecordingPeriod: Int = 240,

  /////////////////
  // Performance //
  /////////////////

  enablePerformancePauses: Boolean = true,
  maximumGamesHistoryPerOpponent: Int = 1000,

  ///////////////////
  // Visualization //
  ///////////////////

  visualizeScreen: Boolean = true,
  visualizeMap: Boolean = true,
  visualizationProbabilityHappyVision: Double = 0.0,
  visualizationProbabilityTextOnly: Double = 0.0,

  cameraDynamicSpeed: Boolean = false,
  cameraDynamicSpeedSlowest: Int = 30,
  cameraDynamicSpeedFastest: Int = 0,
  cameraViewportWidth: Int = 640,
  cameraViewportHeight: Int = 362,
  // Deliberately not derived from the viewport above: copy() with a new
  // viewport size does not follow, so keep both in sync by hand.
  conservativeViewportWidth: Int = 640 + 640,
  conservativeViewportHeight: Int = 480 + 362,

  ///////////////
  // Debugging //
  ///////////////

  trackUnit: Boolean = false) {

  def playbook: Playbook = forcedPlaybook.getOrElse(DefaultPlaybook)
}
