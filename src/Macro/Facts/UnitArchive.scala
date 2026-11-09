package Macro.Facts

import ProxyBwapi.UnitClasses.UnitClass

/**
 * Production history: lifetime unit counts, as opposed to the live army.
 * For sequencing rules ("four Dark Templars before High Templars") that
 * must not reset when units die.
 */
object UnitArchive {

  def producedTotal(unitClass: UnitClass): Int = MacroFacts.unitsEver(unitClass)
}
