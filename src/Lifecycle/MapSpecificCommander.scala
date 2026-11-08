package Lifecycle

import Lifecycle.Keyboard.GameCommands

/**
 * Per-map development tweaks, ported from Atlantis' MapSpecificCommander.
 *
 * Only runs on local dev runs (global keyboard hook active - never on ladder),
 * so tournament games are untouched. Branches Atlasitque used only for camera
 * work and advanced painting are kept as commented code, marked
 * TODO(EridanWave painter), until the AAdvancedPainter equivalent exists.
 * Speed/frame-skip branches are live through [[GameCommands]].
 */
object MapSpecificCommander {

  private var firstSettings = true

  def reset(): Unit = {
    firstSettings = true
  }

  /** UMS maps that play like normal games (same list as Atlantis). */
  def shouldTreatAsNormalMap(): Boolean = {
    val umsMapsTreatedAsNormal = Seq(
      "ums/vsGosuComputer.scx",
      "ums/rav/vsGosuRav.scx"
    )
    // Atlantis compared for equality against its ActiveMap.name(); our
    // With.mapFileName carries the maps/ prefix the game reports, so
    // containment matches exactly the same games.
    val name = With.mapFileName
    name != null && umsMapsTreatedAsNormal.exists(name.contains)
  }

  private def isMap(substrings: String*): Boolean = {
    val name = With.mapFileName
    name != null && substrings.exists(name.contains)
  }

  private def ourCombatUnits: Int = With.units.ours.count(_.unitClass.isWarrior)
  private def enemyCombatUnitsVisible: Int =
    With.units.enemy.count(e => e.unitClass.isWarrior && e.visible)

  def onFrame(): Unit = {
    if (With.game == null || With.units == null) return
    if ( ! Lifecycle.Keyboard.Keyboard.isActive()) return

    // =========================================================
    // Marines & Medics v. Zealots

    // if (isMap("minimaps/")) {
    //   if (With.frame <= 1) {
    //     GameCommands.setSpeed(30, 0)
    //     // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
    //   }
    // }

    // =========================================================

    if (isMap("Bullet")) {
      // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      if (With.frame <= 1) {
        GameCommands.setPainting(true)
      }

      // if (ourCombatUnit.lastUnderAttackLessThanAgo(2)) {
      //   GameCommands.setSpeedOnly(GameSpeed.NORMAL_SPEED)
      // }

      // TODO(EridanWave painter): needs last-under-attack tracking per unit
      // if (underAttackRecently) {
      //   GameCommands.setSpeedOnly(20)
      // }
    }

    // =========================================================

    else if (
      isMap("M_v_", "Tanks_v_", "inCorner")
    ) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("ZealDrag_v_", "Drag_run", "Probe_run", "Reav_v_")) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("v_Sunkens")) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("1DragWounded_v_1Zeal")) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("DragWounded_v_Zeal")) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (
      isMap("1Drag_v_1Zeal.", "2Drag_v_3Zeal", "Drag_v_3Drag", "Drag_v_4Drag",
        "Drag_v_8Drag", "Goon_v_", "Dragoons_v_")
    ) {
      if (With.frame <= 1) {
        GameCommands.setPainting(true)
        GameCommands.setSpeedOnly(20)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("1Drag_v_1Drag")) {
      if (With.frame <= 1) {
        GameCommands.setPainting(true)
        GameCommands.setSpeedOnly(20)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (
      isMap("Drag_v_", "DragUpg_v_", "Zeal_v_")
    ) {
      if (With.frame <= 1) {
        GameCommands.setPainting(true)
        GameCommands.setSpeedOnly(20)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================

    else if (isMap("Dragoons_v_Zealots")) {
      if (With.frame <= 1) GameCommands.setSpeedOnly(20)
    }

    // =========================================================

    else if (isMap("Wraith_v_Wraith")) {
      if (With.frame <= 1) GameCommands.setSpeedOnly(20)
      // TODO(EridanWave painter): camera on first combat unit for the first 3 seconds
      // if (With.frame <= 3 * 24) CameraCommander.centerCameraOn(ourCombatUnits.first)
    }

    // =========================================================

    else if (isMap("Lings_v_Zealots")) {
      if (With.frame <= 1) GameCommands.setSpeed(30, 0)
      // TODO(EridanWave painter): camera on our unit nearest to the enemy for the first 3 seconds
    }

    // =========================================================

    else if (
      isMap("rav/Dragoon_v_Zealot", "rav/Dragoons_v_Zealots")
    ) {
      if (With.frame <= 1) {
        GameCommands.setSpeed(50, 0)
      }
    }

    // =========================================================
    // v. ZERG / PROTOSS / Gosu maps

    else if (
      isMap("vsGosuComputer", "vsGosuRav", "exp_skilltest", "7th")
    ) {
      if (firstSettings && ourCombatUnits == 1 && enemyCombatUnitsVisible > 0) {
        // TODO(EridanWave painter): AAdvancedPainter.enablePainting()
        GameCommands.setFrameSkipOnly(0)
        GameCommands.setSpeedOnly(0)
        // TODO(EridanWave painter): CenterCamera.onFirstCombatUnit()
        firstSettings = false
      }
      if (firstSettings && With.frame <= 0) {
        GameCommands.setPainting(false)
        // TODO(EridanWave painter): AAdvancedPainter.enablePainting()
        GameCommands.setSpeedOnly(0)
        GameCommands.setFrameSkipOnly(1000)

        // TODO(EridanWave painter): vsGosuRav centers on our main base here
        // if (isMap("vsGosuRav")) {
        //   CameraCommander.centerCameraOn(mainOrAnyBuilding)
        // }
      }

      // if (With.frame == 120 * 30) {
      //   GameCommands.setFrameSkipOnly(0)
      //   GameCommands.setSpeedOnly(20)
      // }

      // if (isMap("exp_skilltest") && !Have.controlTower()) {
      //   GameCommands.setFrameSkipOnly(2000)
      // }
    }

    // =========================================================
    // Marines v. ZERGLINGS

    else if (isMap("marines_v_zerglings")) {
      if (With.frame <= 1) {
        GameCommands.setSpeed(20, 0)
      }
      // TODO(EridanWave painter): Atlantis ended the game at 32 kills here
      // if (kills >= 32) { With.game.leaveGame() }
    }

    // =========================================================
    // Marines v. LURKER

    else if (isMap("Jim_v_Lurker")) {
      if (With.frame <= 1) {
        GameCommands.setSpeed(1, 0)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // =========================================================
    // Marines v. ZEALOTS

    else if (isMap("mar_v_zea")) {
      if (enemyCombatUnitsVisible == 0) {
        GameCommands.setSpeed(0, 70)
      }

      if (
        With.frame > 60
          && GameCommands.frameSkip >= 30
          && ourCombatUnits >= 5
          // && Select.ourOfType(Terran_Science_Vessel).atLeast(1)
          && enemyCombatUnitsVisible >= 2
      ) {
        // GameSpeed.pauseGame()
        GameCommands.setPainting(false)
        GameCommands.setSpeed(30, 0)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
        // GameSpeed.unpauseGame()
      }
    }

    // =========================================================
    // Rebasing

    else if (isMap("T_rebasing")) {
      if (With.frame <= 3) {
        // TODO(EridanWave painter): CameraCommander.centerCameraOn(ourBases.first)
        GameCommands.setSpeed(0, 0)
      }
    }

    else if (isMap("TBall_v_HydraLings")) {
      if (With.frame <= 3) {
        // TODO(EridanWave painter): CameraCommander.centerCameraOn(ourBases.first)
        GameCommands.setSpeed(1, 0)
      }
    }

    // =========================================================
    // Ladder TauCross: the pre-planned layout is only visible in full painting.

    else if (isMap("TauCross", "Tau Cross")) {
      if (With.frame <= 1) {
        GameCommands.setSpeedOnly(20)
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.centerCameraOn(ourBases.first)
      }
    }

    // =========================================================

    else if (isMap("M&M_v_Zealots")) {
      if (With.frame <= 1) {
        GameCommands.setPainting(true)
        // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
      }
    }

    // Explicit --speed= wins over the per-map defaults above (and only there:
    // keyboard presets pressed later still take effect).
    if (With.frame <= 1) {
      GameCommands.initialSpeed.foreach(GameCommands.setSpeedOnly)
    }
  }
}
