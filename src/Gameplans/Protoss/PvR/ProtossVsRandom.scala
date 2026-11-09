package Gameplans.Protoss.PvR

import Gameplans.All.{Modal, ModalGameplan}

object ProtossVsRandom {
  // Eridan gameplans (default): none - unscouted-random play is out of
  // scope while testing our PvZ framework. An empty Modal is complete
  // and runs nothing.
  val eridanModes: Seq[Modal] = Seq.empty

  // PurpleWave gameplans (disabled - append to the ModalGameplan call below to restore):
  val purpleWaveModes: Seq[Modal] = Seq(
    new PvRDT,
    new PvR2Gate1012,
    new PvRZZCoreZ,
    new PvRFFE,
    new PvRGFE,
    new PvR2Gate4Gate)
}

class ProtossVsRandom extends ModalGameplan(
  (ProtossVsRandom.eridanModes /* ++ ProtossVsRandom.purpleWaveModes */): _*)