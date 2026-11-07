package Debugging.Visualizations.Views.Fun

import Debugging.Visualizations.Rendering.DrawScreen
import Debugging.Visualizations.Views.DebugView
import Information.Battles.KillLedger
import Lifecycle.With

/**
 * Aggregate kill/loss ledger, top-right corner like Atlantis
 * AAdvancedPainter.paintKilledAndLost. Plain text - DrawScreen has no color
 * API, so "+" marks a winning resource trade instead of green/red.
 * Screen space (x ~505, rows 1-4) is free of the default views.
 */
object ShowKillLedger extends DebugView {

  override def renderScreen(): Unit = {
    val h       = With.visualization.lineHeightSmall
    val balance = KillLedger.balance
    DrawScreen.text(505, 1 * h, f"Killed: ${KillLedger.killed}")
    DrawScreen.text(505, 2 * h, f"Lost: ${KillLedger.lost}")
    DrawScreen.text(505, 3 * h, "-----------")
    DrawScreen.text(505, 4 * h, f"Price: ${if (balance >= 0) "+" else ""}$balance")
  }
}
