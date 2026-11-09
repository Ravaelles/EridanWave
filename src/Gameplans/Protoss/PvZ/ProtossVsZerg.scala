package Gameplans.Protoss.PvZ

import Gameplans.All.{Modal, ModalGameplan}
import Gameplans.Eridan.PvZ.PvZEridanStandard
import Gameplans.Protoss.PvR.PvR2Gate4Gate

object ProtossVsZerg {
  // Eridan gameplans (default):
  val eridanModes: Seq[Modal] = Seq(
    new PvZEridanStandard)

  // PurpleWave gameplans (disabled - append to the ModalGameplan call below to restore):
  val purpleWaveModes: Seq[Modal] = Seq(
    new PvZ1GateCoreExpand,
    new PvZ1GateCoreYoloSpeedlot,
    new PvZ1GateCoreYoloDragoon,
    new PvZFE,
    new PvZ1BaseReactive,
    new PvR2Gate4Gate)
}

class ProtossVsZerg extends ModalGameplan(
  (ProtossVsZerg.eridanModes /* ++ ProtossVsZerg.purpleWaveModes */): _*)