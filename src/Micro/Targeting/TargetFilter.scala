package Micro.Targeting

import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

trait TargetFilter {
  def appliesTo(actor: FriendlyUnitInfo): Boolean = true
  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean
}

/**
 * Override vote: a filter in the always-legal family (focus and its
 * doctrines). Target ORs these votes before ANDing the required chain,
 * so splitting the old focus OR-chain across objects keeps its meaning.
 */
trait TargetOverrideFilter extends TargetFilter
