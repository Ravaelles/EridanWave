package Lifecycle

import Lifecycle.Keyboard.GameCommands
import Lifecycle.Launch.GameLauncherFactory

/**
 * Per-map development tweaks, ported from Atlantis' MapSpecificCommander.
 *
 * Only runs on local dev runs (global keyboard hook active - never on ladder),
 * so tournament games are untouched. Speed/frame-skip branches are live through
 * [[GameCommands]] and applied once from EridanWave.onStart (never inside the
 * frame budget); the two dynamic pacings below keep running per frame.
 *
 * Painter backlog (one list instead of a TODO per family; git has the
 * commented bodies): focus the camera on interesting combat units (most
 * families), center on our bases first (T_rebasing, TBall, TauCross),
 * camera on the first combat unit for the first 3 seconds (Wraith),
 * camera on our unit nearest to the enemy for the first 3 seconds
 * (Lings), leave the game at 32 kills (marines_v_zerglings),
 * CenterCamera.onFirstCombatUnit (vsGosu), vsGosuRav centers on our main.
 */
object MapSpecificCommander extends PerGame {

  private var firstSettings = true

  override def reset(): Unit = {
    firstSettings = true
  }

  /** UMS maps that play like normal games (same list as Atlantis). */
  def shouldTreatAsNormalMap(): Boolean =
    MapCatalog.isNormalMapException(With.mapFileName)

  private def isMap(substrings: String*): Boolean = {
    val name = With.mapFileName
    name != null && substrings.exists(name.contains)
  }

  /** One row of static per-map tuning: the first matching family wins. */
  private case class MapTuning(
    markers: Seq[String],
    speedMs: Option[Int] = None,
    frameSkip: Option[Int] = None,
    painting: Option[Boolean] = None,
    maxFrame: Int = 1)

  private val tunings: Seq[MapTuning] = Seq(
    MapTuning(Seq("Bullet"), painting = Some(true)),
    MapTuning(Seq("M_v_", "Tanks_v_", "inCorner"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("ZealDrag_v_", "Drag_run", "Probe_run", "Reav_v_"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("v_Sunkens"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("1DragWounded_v_1Zeal"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("DragWounded_v_Zeal"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("1Drag_v_1Zeal.", "2Drag_v_3Zeal", "Drag_v_3Drag", "Drag_v_4Drag", "Drag_v_8Drag", "Goon_v_", "Dragoons_v_"), painting = Some(true), speedMs = Some(20)),
    MapTuning(Seq("1Drag_v_1Drag"), painting = Some(true), speedMs = Some(20)),
    MapTuning(Seq("Drag_v_", "DragUpg_v_", "Zeal_v_"), painting = Some(true), speedMs = Some(20)),
    MapTuning(Seq("Dragoons_v_Zealots"), speedMs = Some(20)),
    MapTuning(Seq("Wraith_v_Wraith"), speedMs = Some(20)),
    MapTuning(Seq("Lings_v_Zealots"), speedMs = Some(30), frameSkip = Some(0)),
    MapTuning(Seq("rav/Dragoon_v_Zealot", "rav/Dragoons_v_Zealots"), speedMs = Some(50), frameSkip = Some(0)),
    MapTuning(Seq("marines_v_zerglings"), speedMs = Some(20), frameSkip = Some(0)),
    MapTuning(Seq("Jim_v_Lurker"), speedMs = Some(1), frameSkip = Some(0)),
    MapTuning(Seq("T_rebasing"), speedMs = Some(0), frameSkip = Some(0), maxFrame = 3),
    MapTuning(Seq("TBall_v_HydraLings"), speedMs = Some(1), frameSkip = Some(0), maxFrame = 3),
    MapTuning(Seq("TauCross", "Tau Cross"), speedMs = Some(20), painting = Some(true)),
    MapTuning(Seq("M&M_v_Zealots"), painting = Some(true)))

  private def ourCombatUnits: Int = With.units.ours.count(_.unitClass.isWarrior)
  private def enemyCombatUnitsVisible: Int =
    With.units.enemy.count(e => e.unitClass.isWarrior && e.visible)

  /** Static per-map tuning, applied once from onStart (never in the frame budget). */
  def applyStaticTuning(): Unit = {
    // Never in headless runs: validation games play exclusively at
    // speed 0 with frameskip 2000 - per-map pacing would just burn
    // wall-clock on watchable speeds nobody watches.
    if (GameLauncherFactory.isHeadlessRun()) return

    tunings.view.filter(t => With.frame <= t.maxFrame && isMap(t.markers: _*)).headOption.foreach(tuning => {
      (tuning.speedMs, tuning.frameSkip) match {
        case (Some(speed), Some(skip)) => GameCommands.setSpeed(speed, skip)
        case (Some(speed), None)        => GameCommands.setSpeedOnly(speed)
        case (None, Some(skip))         => GameCommands.setFrameSkipOnly(skip)
        case _                          =>
      }
      tuning.painting.foreach(GameCommands.setPainting)
    })
  }

  def onFrame(): Unit = {
    if (GameLauncherFactory.isHeadlessRun()) return
    if (With.game == null || With.units == null) return
    if ( ! Lifecycle.Keyboard.Keyboard.isActive()) return

    if (isMap("vsGosuComputer", "vsGosuRav", "exp_skilltest", "7th")) paceGosu()
    else if (isMap("mar_v_zea")) paceMarVZea()
  }

  // v. ZERG / PROTOSS / Gosu maps: pace the first combat unit, then run free.
  private def paceGosu(): Unit = {
    if (firstSettings && ourCombatUnits == 1 && enemyCombatUnitsVisible > 0) {
      GameCommands.setFrameSkipOnly(0)
      GameCommands.setSpeedOnly(0)
      // TODO(EridanWave painter): CenterCamera.onFirstCombatUnit()
      firstSettings = false
    }
    if (firstSettings && With.frame <= 0) {
      GameCommands.setPainting(false)
      GameCommands.setSpeedOnly(0)
      GameCommands.setFrameSkipOnly(1000)
      // TODO(EridanWave painter): vsGosuRav centers on our main base here
    }
  }

  // Marines v. ZEALOTS: run fast until the armies meet, then slow down.
  private def paceMarVZea(): Unit = {
    if (enemyCombatUnitsVisible == 0) {
      GameCommands.setSpeed(0, 70)
    }

    if (With.frame > 60
      && GameCommands.frameSkip >= 30
      && ourCombatUnits >= 5
      && enemyCombatUnitsVisible >= 2) {
      GameCommands.setPainting(false)
      GameCommands.setSpeed(30, 0)
    }
  }
}
