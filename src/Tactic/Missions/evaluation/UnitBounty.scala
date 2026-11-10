package Tactic.Missions.evaluation

import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitClasses.UnitClass

/**
 * Pure per-unit bounty (no game access): what a kill is worth and what a
 * loss costs. BWAPI prices Zerglings per pair (50) and Scourge per egg
 * (25+75 per two); both halve to per-unit bounties, floored. The split is
 * pinned by suite; the class adapter below just feeds it.
 */
object UnitBounty {

  /** Pair/egg price to per-unit bounty, floored. */
  def split(price: Int): Int = price / 2

  def total(minerals: Int, gas: Int): Int = minerals + gas

  def of(unitClass: UnitClass): Int = {
    if (unitClass == Zerg.Zergling) split(unitClass.mineralPrice) + unitClass.gasPrice
    else if (unitClass == Zerg.Scourge) split(unitClass.mineralPrice) + split(unitClass.gasPrice)
    else unitClass.mineralPrice + unitClass.gasPrice
  }
}
