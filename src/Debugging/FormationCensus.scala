package Debugging

import Lifecycle.With
import Mathematics.Maff

/**
 * Formation census for traceless UMS duels: every 48 frames each of our
 * units reports position, distance to the nearest live enemy (tiles) and
 * current order, so scripts/formation-check.py can tell whether the pack
 * formed up before engaging. Key-armed (Formation) like the other probes.
 * Distinct from FormationAudit (leaf-behavior tally plus slot-leave
 * ranges): this is a positions census, that is a behavior audit.
 */
object FormationCensus {

  def audit(): Unit = {
    if ( ! Probe.on("Formation")) return
    if (With.frame % 48 != 0) return

    With.units.ours.view.filter(_.alive).foreach(unit => {
      val foeTiles = Maff.minBy(With.units.enemy.view.filter(_.alive))(e => unit.tileDistanceCenter(e.pixel))
        .map(e => unit.tileDistanceCenter(e.pixel))
      foeTiles.foreach(tiles =>
        Probe.note("Formation", unit.id, f"${unit.pixel.x} ${unit.pixel.y} $tiles%.1f ${unit.order}"))
    })
  }
}
