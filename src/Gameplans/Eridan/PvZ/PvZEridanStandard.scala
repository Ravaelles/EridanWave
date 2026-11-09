package Gameplans.Eridan.PvZ

import Gameplans.Protoss.PvZ.PvZ1BaseReactive
import Strategery.Strategies.Protoss.PvZ1BaseZealotIntoGoon

/**
 * Eridan's own PvZ gameplan (default; the PurpleWave modes listed in
 * ProtossVsZerg are disabled). Inherits all shared 1BaseReactive
 * machinery (scouting, defense, tech, tactics) and activates on our flag;
 * the Gates1012 opening, ZealotIntoGoon production/posture/expansion are
 * already routed by that flag (see PvZ1BaseReactive).
 */
class PvZEridanStandard extends PvZ1BaseReactive {
  override def activated: Boolean = employing(PvZ1BaseZealotIntoGoon)

  override def chooseOpening(): Unit = {
    // Zealot-into-Goon always opens Gates1012: two Gates into Core-first
    // pressure, with the 4-pool/speedling reactions in base executeBuild
    // still active.
    opening = Gates1012
  }

  override def buildArmy(): Unit = {
    if (PvZ1BaseZealotIntoGoon()) {
      ZealotIntoGoon.produce()

      return
    }

    super.buildArmy()
  }
}

