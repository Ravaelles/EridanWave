package Micro.Formation

import Information.Geography.Types.{Edge, Zone}

import Tactic.Squads.Group.FriendlyUnitGroup

object FormationZone {
  def apply(group: FriendlyUnitGroup, zone: Zone, edge: Edge): Formation = {
    new FormationStandard(group, FormationStyleGuard, edge.pixelCenter, Some(zone))
  }
}
