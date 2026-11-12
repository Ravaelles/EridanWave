package Gameplans.Protoss.PvZ.reactive

import Lifecycle.With
import ProxyBwapi.Races.{Protoss, Zerg}
import Utilities.?
import Strategery.Strategies.Protoss.{PvZGoon, PvZReaver, PvZSpeedlot}
import Utilities.UnitFilters.IsHatchlike

/**
 * Composition selection: what the army should look like given what the
 * Zerg has shown and what we have already committed to. `go` appends a need,
 * `goo` appends it only while the list is short enough.
 */
trait PvZReactiveComposition extends PvZ1BaseReactiveUtilities {

  protected def composition: Seq[Composition]
  protected def composition_=(next: Seq[Composition]): Unit
  protected def safeVsAir: Boolean
  protected def committedSpeedlot: Boolean
  protected def committedReaver: Boolean
  protected def committedGoon: Boolean

  def on  (c: Composition*)                               : Boolean = c.exists(composition.contains)
  def go  (c: Composition,            p: Boolean = true)  : Unit    = if (p && ! composition.contains(c)) composition :+= c
  def goo (c: Composition, max: Int,  p: Boolean = true)  : Unit    = go(c, composition.length <= max && p)

  protected def chooseComposition(): Unit = {
    val static = enemies(Zerg.CreepColony, Zerg.SunkenColony)

    composition = Seq.empty

    // Disabling this because we get Stargate as part of going Speedlot!
    //go (Stargate,     have(Protoss.Stargate))
    go (Reaver,       safeVsAir && have(Protoss.Shuttle, Protoss.RoboticsSupportBay, Protoss.Reaver))
    go (Reaver,       enemyLurkersLikely && have(Protoss.RoboticsFacility))
    go (Speedlot,     have(Protoss.CitadelOfAdun) || (have(Protoss.Forge) && ! enemyLurkersLikely))
    go (Goon,         upgradeComplete(Protoss.DragoonRange) || units(Protoss.Dragoon) > 3)
    goo(Reaver,   0,  safeVsAir && (have(Protoss.RoboticsFacility) || enemyHydralisksLikely || enemyLurkersLikely))
    goo(Speedlot, 0,  anticipateSpeedlings)
    goo(Goon,     0,  enemyMutalisksLikely && units(Protoss.Gateway) >= 3)
    goo(Stargate, 0,  enemyMutalisksLikely)
    goo(Stargate, 0,  With.fingerprints.twoHatchGas()   && enemyHasShown(Zerg.Lair))
    goo(Speedlot, 0,  With.fingerprints.threeHatchGas() && enemies(IsHatchlike) > 2)
    goo(Reaver,   0,  static > 1)
    goo(Goon,     0,  PvZGoon     (committedGoon))
    goo(Reaver,   0,  PvZReaver   (committedReaver))
    goo(Speedlot, 0,  PvZSpeedlot (committedSpeedlot))
    goo(Stargate, 1,  enemyMutalisksLikely              && ! on(Goon))
    goo(Goon,     1,  enemyMutalisksLikely              && on(Stargate))
    goo(Reaver,   1,  enemyHydralisksLikely             && on(Stargate))
    goo(Speedlot, 1,  With.fingerprints.threeHatchGas() && on(Reaver))
    goo(Speedlot, 1,  miningBases > 1)
    goo(Reaver,   2,  static > 2)
    goo(Goon,     2,  enemyMutalisksLikely)
    goo(Reaver,   2,  units(Protoss.Zealot, Protoss.Dragoon) > 24)
    goo(Reaver,   3,  have(Protoss.RoboticsFacility))
  }

}
