package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Tile}
import Micro.Actions.{Action, Idle}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import ProxyBwapi.UnitTracking.IndexedSet
import ProxyBwapi.Upgrades.Upgrade

class SquadIntention {
  val frameCreated      : Int                           = With.frame
  var terminus          : Option[Pixel]                 = None
  var station           : Option[Pixel]                 = None
  var redoubt           : Option[Pixel]                 = None
  var toScan            : Option[Pixel]                 = None
  var toNuke            : Option[Pixel]                 = None
  var toAttack          : Option[UnitInfo]              = None
  var toGather          : Option[UnitInfo]              = None
  var toHeal            : Option[UnitInfo]              = None
  var toFinish          : Option[UnitInfo]              = None
  var toBoard           : Option[FriendlyUnitInfo]      = None
  var toTrain           : Option[UnitClass]             = None
  var toUpgrade         : Option[Upgrade]               = None
  var toTech            : Option[Tech]                  = None
  var toLand            : Option[Tile]                  = None
  var toBuild           : Seq[BuildIntent]              = Seq.empty
  var toScoutTiles      : Seq[Tile]                     = Seq.empty
  var action            : Action                        = Idle
  var canFight          : Boolean                       = true
  var canFlee           : Boolean                       = true
  var canSneak          : Boolean                       = false
  var shouldMeld        : Boolean                       = false
  var shouldLiftoff: Boolean = false
  var shouldAdjustXY    : Boolean                       = true
  var targets           : Option[IndexedSet[UnitInfo]]  = None
  var marchHome         : Boolean                       = false

  def setTerminus         (value: Pixel)                    : SquadIntention = { terminus          = Some(value); this }
  def setStation          (value: Pixel)                    : SquadIntention = { station           = Some(value); this }
  def setRedoubt          (value: Pixel)                    : SquadIntention = { redoubt           = Some(value); this }
  def setScan             (value: Pixel)                    : SquadIntention = { toScan            = Some(value); this }
  def setNuke             (value: Pixel)                    : SquadIntention = { toNuke            = Some(value); this }
  def setAttack           (value: UnitInfo)                 : SquadIntention = { toAttack          = Some(value); this }
  def setGather           (value: UnitInfo)                 : SquadIntention = { toGather          = Some(value); this }
  def setHeal             (value: UnitInfo)                 : SquadIntention = { toHeal            = Some(value); this }
  def setFinish           (value: UnitInfo)                 : SquadIntention = { toFinish          = Some(value); this }
  def setBoard            (value: FriendlyUnitInfo)         : SquadIntention = { toBoard           = Some(value); this }
  def setTrain            (value: UnitClass)                : SquadIntention = { toTrain           = Some(value); this }
  def setUpgrade          (value: Upgrade)                  : SquadIntention = { toUpgrade         = Some(value); this }
  def setTech             (value: Tech)                     : SquadIntention = { toTech            = Some(value); this }
  def setLand             (value: Tile)                     : SquadIntention = { toLand            = Some(value); this }
  def setTerminus         (value: Option[Pixel])            : SquadIntention = { terminus          = value; this }
  def setStation          (value: Option[Pixel])            : SquadIntention = { station           = value; this }
  def setRedoubt          (value: Option[Pixel])            : SquadIntention = { redoubt           = value; this }
  def setScan             (value: Option[Pixel])            : SquadIntention = { toScan            = value; this }
  def setNuke             (value: Option[Pixel])            : SquadIntention = { toNuke            = value; this }
  def setAttack           (value: Option[UnitInfo])         : SquadIntention = { toAttack          = value; this }
  def setGather           (value: Option[UnitInfo])         : SquadIntention = { toGather          = value; this }
  def setHeal             (value: Option[UnitInfo])         : SquadIntention = { toHeal            = value; this }
  def setFinish           (value: Option[UnitInfo])         : SquadIntention = { toFinish          = value; this }
  def setBoard            (value: Option[FriendlyUnitInfo]) : SquadIntention = { toBoard           = value; this }
  def setTrain            (value: Option[UnitClass])        : SquadIntention = { toTrain           = value; this }
  def setUpgrade          (value: Option[Upgrade])          : SquadIntention = { toUpgrade         = value; this }
  def setTech             (value: Option[Tech])             : SquadIntention = { toTech            = value; this }
  def addBuild            (value: BuildIntent)              : SquadIntention = { toBuild           :+= value; this }
  def setScout            (value: Seq[Tile])                : SquadIntention = { toScoutTiles      = value; this }
  def setAction           (value: Action)                   : SquadIntention = { action            = value; this }
  def setCanFight         (value: Boolean = true)           : SquadIntention = { canFight          = value; this }
  def setCanFlee          (value: Boolean = true)           : SquadIntention = { canFlee           = value; this }
  def setCanSneak         (value: Boolean = true)           : SquadIntention = { canSneak          = value; this }
  def setMarchHome        (value: Boolean = true)           : SquadIntention = { marchHome         = value; this }
  def setShouldMeld       (value: Boolean = true)           : SquadIntention = { shouldMeld        = value; this }
  def setShouldLiftoff    (value: Boolean = true)           : SquadIntention = { shouldLiftoff     = value; this }
  def setShouldCorrectXY  (value: Boolean = true)           : SquadIntention = { shouldAdjustXY   = value; this }
  def setTargets          (value: Iterable[UnitInfo])       : SquadIntention = { targets           = Some(new IndexedSet[UnitInfo](value)); this }
  def setTargets          (value: UnitInfo*)                : SquadIntention = setTargets(value)

  def toBuildActive: Option[BuildIntent] = Maff.maxBy(toBuild)(_.startNow)
}
