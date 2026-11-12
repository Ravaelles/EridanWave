package Gameplans.Protoss.PvZ.reactive

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Utilities.Time.GameTime

import scala.collection.mutable

/**
 * Opening selection for the one-base reactive line: economy stance first,
 * then the pool timing we have seen bans the openers that cannot survive it.
 * The pick is recorded in `opening`; the build orders live in `openings/`.
 */
trait PvZReactiveOpenings extends PvZ1BaseReactiveUtilities {

  protected def opening: Opening
  protected def opening_=(next: Opening): Unit
  protected def economy: Economy

  protected def chooseOpening(): Unit = {
    if (frame > GameTime(3, 30)())      return
    if (units(Protoss.Gateway)    > 1)  return
    if (units(Protoss.Nexus)      > 1)  return
    if (unitsEver(Protoss.Zealot) > 1)  return
    if (have(Protoss.CyberneticsCore))  return

    val allowed = new mutable.ArrayBuffer[Opening]()

    def ban(openings: Opening*): Unit = allowed --= openings
    def consider(candidate: Opening, predicate: => Boolean): Unit = if (allowed.contains(candidate) && predicate) opening = candidate

    allowed ++= (economy match {
      case Muscle =>  Seq(Gates1012,  Gates910,   CoreZ,    ZCoreZ,     ZZCoreZ,    GateNexus,  NexusFirst)
      case Expand =>  Seq(NexusFirst, GateNexus,  CoreZ,    ZCoreZ,     ZZCoreZ,    Gates1012,  Gates910)
      case Tech   =>  Seq(CoreZ,      ZCoreZ,     ZZCoreZ,  NexusFirst, GateNexus,  Gates1012,  Gates910) })

    // Per Khala: Vs. 9 Pool, ZZCoreZ; Vs. Hatch-first, CoreZ; otherwise, ZCoreZ.

    if (haveComplete(Protoss.Gateway))      ban(NexusFirst)
    if (haveEver(Protoss.Zealot))           ban(NexusFirst, CoreZ)
    if (haveComplete(Protoss.Assimilator))  ban(NexusFirst, GateNexus,  Gates1012,  Gates910)
    if (With.fingerprints.twoHatchMain())   ban(NexusFirst, GateNexus)
    if (anticipateSpeedlings)               ban(NexusFirst, GateNexus,  CoreZ,      ZCoreZ, ZZCoreZ)
    if (poolMin         <= timing4p)        ban(NexusFirst, GateNexus,  CoreZ,      ZCoreZ, ZZCoreZ)
    if (poolMin         <= timing9p)        ban(NexusFirst, GateNexus,  CoreZ,      ZCoreZ)
    if (poolMin         <= timingOp)        ban(NexusFirst,             CoreZ)
    if (poolMin         <= timing12p)       ban(NexusFirst,             CoreZ)
    if (pool.exists(_   >= timing10h))      ban(ZCoreZ,     ZZCoreZ,    Gates1012,  Gates910)
    if (With.fingerprints.gasSteal())       ban(ZZCoreZ,    ZCoreZ,     CoreZ)

    if (allowed.isEmpty) return

    opening = allowed.head

    consider(GateNexus, poolMax         >= timingOp && economy == Expand)
    consider(Gates1012, poolMinRecently <= timing4p)
    consider(Gates910,  poolMaxRecently <= timing4p)
  }

  def opened(o: Opening*): Boolean = o.contains(opening)
}
