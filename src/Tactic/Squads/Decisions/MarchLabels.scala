package Tactic.Squads.Decisions

/**
 * March labels, one per decision (gates, legs, trip home): a sealed
 * family instead of string literals, so a typo cannot silently fork
 * the trace. The rendered strings stay byte-for-byte identical.
 */
sealed abstract class MarchLabel(val label: String) {
  override def toString: String = label
}

object MarchLabel {

  case object DarkTemplarHunt extends MarchLabel("DarkTemplarHunt")

  case object CorsairHunt extends MarchLabel("CorsairHunt")

  case object MainMarch extends MarchLabel("MainMarch")

  case object SuicideMission extends MarchLabel("SuicideMission")

  case object PairRide extends MarchLabel("PairRide")

  case object StrongMarch extends MarchLabel("StrongMarch")

  case object NoTankMarch extends MarchLabel("NoTankMarch")

  case object ToothlessMarch extends MarchLabel("ToothlessMarch")

  case object DragoonRoam extends MarchLabel("DragoonRoam")

  case object Retreating extends MarchLabel("Retreating")

  case object MarchingHome extends MarchLabel("Marching home")

  case object LoneMarch extends MarchLabel("LoneMarch")

  case object StaticPush extends MarchLabel("StaticPush")

  case object WaitingForPack extends MarchLabel("Waiting for pack")

  case object RallyingToPack extends MarchLabel("Rallying to pack centroid")

  case object MarchingToSlot extends MarchLabel("Marching to formation slot")

  case object ReturningToSlot extends MarchLabel("Returning to slot")
}
