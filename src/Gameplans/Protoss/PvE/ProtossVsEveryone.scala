package Gameplans.Protoss.PvE

import Gameplans.All.{Modal, ModalGameplan}

object ProtossVsEveryone {
  // Eridan gameplans (default): none - race-agnostic responses live in the
  // per-matchup gameplans. An empty Modal is complete and runs nothing.
  val eridanModes: Seq[Modal] = Seq.empty

  // PurpleWave gameplans (disabled - append to the ModalGameplan call below
  // to restore, e.g. cannon-rush defense vs Terran/Protoss):
  val purpleWaveModes: Seq[Modal] = Seq(
    new PvECannonRushMaximum,
    new PvECannonRushFE,
    new PvEProxy2Gate)
}

class ProtossVsEveryone extends ModalGameplan(
  (ProtossVsEveryone.eridanModes /* ++ ProtossVsEveryone.purpleWaveModes */): _*)
