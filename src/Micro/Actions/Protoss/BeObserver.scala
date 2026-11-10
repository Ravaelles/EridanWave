package Micro.Actions.Protoss

import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Lifecycle.With

/**
 * Atlantis SpreadOutDetectors for our eyes: the newer Observer moves off
 * the older to nine tiles instead of stacking watches three apart.
 * Runs after BeFlier in Fight: the dodge sleeps first on conflict, so
 * Scourge-dodge keeps priority. Fliers skip destination chopping, so the
 * away point goes out as one order.
 */
object BeObserver extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Observer(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // Only older neighbors count: the newer eye moves, the older holds.
    // Engine edge distance is pixels; the threshold is tiles (rule 10).
    Maff.minBy(With.units.ours.view.filter(u => Protoss.Observer(u) && u.id < unit.id))(_.pixelDistanceEdge(unit))
      .foreach(older => {
        if (ObserverSpread.shouldSpread(unit.id, older.id, older.pixelDistanceEdge(unit) / 32.0)) {
          unit.agent.destinationNext.set(ObserverSpread.awayFrom(unit.pixel, older.pixel))
          Commander.move(unit)
        }
      })
  }
}
