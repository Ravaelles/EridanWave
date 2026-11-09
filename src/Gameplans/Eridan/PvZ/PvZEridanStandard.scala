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
}
