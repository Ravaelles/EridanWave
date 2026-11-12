package Gameplans.Protoss.PvZ.reactive

import Gameplans.Protoss.PvZ.policy.{GasBank, ReaverGas}
import Lifecycle.With
import Macro.Actions.Compositor
import Macro.Facts.UnitArchive
import ProxyBwapi.Races.{Protoss, Zerg}
import Utilities.?

/**
 * Army production: translate the army weights into compositor needs and
 * goals, then pump the filler units.
 */
trait PvZReactiveArmy extends PvZ1BaseReactiveUtilities {

  protected def composition: Seq[Composition]
  protected def compositor: Compositor

  def buildArmy(): Unit = {
    val mix = PvZArmyWeights.of(PvZArmyWeights.Seen(
      zerglings             = enemies(Zerg.Zergling),
      hydralisks            = enemies(Zerg.Hydralisk),
      mutalisks             = enemies(Zerg.Mutalisk),
      sunkenColonies        = enemies(Zerg.SunkenColony),
      confidenceDefending01 = confidenceDefending01,
      highTemplars          = units(Protoss.HighTemplar),
      gas                   = gas,
      hydralisksLikely      = enemyHydralisksLikely,
      mutalisksLikely       = enemyMutalisksLikely))

    compositor.reset()
    compositor.setNeed(Protoss.Zealot, mix.zealots)
    compositor.setGoal(Protoss.Zealot, 0.35 * mix.zerglings)
    if (GasBank.allowsDragoon(units(Protoss.Dragoon), gas, With.units.ours.count(_.unitClass.isWarrior))) {
      if (composition.contains(Goon)) {
        compositor.setNeed(Protoss.Dragoon, mix.dragoons)
        compositor.setGoal(Protoss.Dragoon, 6 + enemies(Zerg.Mutalisk) + enemies(Zerg.Hydralisk))
      } else {
        compositor.setNeed(Protoss.Dragoon, 5.0)
        compositor.capGoal(Protoss.Dragoon, ?(composition.contains(Stargate) && enemyHasShown(Zerg.Scourge), 2, 1))
      }
    }

    if (composition.contains(Reaver) && ReaverGas.allowsReaver(units(Protoss.Reaver), gas)) {
      compositor.setNeed(Protoss.Reaver,  mix.reavers)
      compositor.setGoal(Protoss.Reaver,  6)
    }
    // A Shuttle for the ferry always queues; one only for Reavers waits on gas.
    if (composition.contains(Reaver) && (With.blackboard.ferry() || ReaverGas.allowsReaver(units(Protoss.Reaver), gas))) {
      compositor.setNeed(Protoss.Shuttle, mix.reavers * 4)
      compositor.capGoal(Protoss.Shuttle,  units(Protoss.Reaver) / 2)
    }

    if (have(Protoss.TemplarArchives) && supplyUsed200 >= 150) {
      compositor.setNeed(Protoss.HighTemplar, mix.archons)
      compositor.setGoal(Protoss.HighTemplar, 4)
    }

    if (have(Protoss.Stargate)) {
      compositor.setNeed(Protoss.Corsair, mix.corsairs)
      compositor.capGoal(Protoss.Corsair, Math.max(mix.mutalisks, ?(enemyHydralisksLikely, 1, 3)))
    }

    compositor.produceAndPump()

    if (supplyUsed200 >= 150) {
      pump(Protoss.HighTemplar)
    }
    if ( ! enemyHydralisksLikely) {
      pump(Protoss.Scout)
    }
    pump(Protoss.Zealot)
  }}
