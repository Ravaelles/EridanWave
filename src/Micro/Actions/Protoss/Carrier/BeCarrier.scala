package Micro.Actions.Protoss.Carrier

import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Protoss.Carrier._
import Micro.Agency.Commander
import Micro.Targeting.FiltersSituational.{TargetFilterCombatants, TargetFilterVisibleInRange}
import Micro.Targeting.{CarrierBrawl, CarrierTankHuntSeam, Target}
import ProxyBwapi.Orders
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
import Utilities.LightYear
import Utilities.UnitFilters.IsTank


object BeCarrier extends Action {
  
  // Carriers are really finicky and demand their own micro.

  // TODO: Implement Stop-Hold (SH) micro:
  // https://tl.net/forum/brood-war/572188-new-carrier-micro-sh-micro
  // https://tl.net/forum/brood-war/572186-the-age-of-pusagi-new-carrier-micro-trick
  // https://www.youtube.com/watch?v=6W0B0jZEQrc
  // In short: Carrier DPS can be *doubled* if you're not picky about who they're targeting
  // Plan for implementation:
  // 1. Carrier micro test map
  // 2. Extensive visualizations on Carriers
  // Issue grouped commands using Game.issueCommand()
  //
  // Unfortunately BWAPI 4.4 does not support grouped commands for client bots.
  // So this may not be achievable without an API change.
  //
  // On the other hand, similar performance against small units could potentially be achieved by
  // having carriers target different enemies, which would reduce the frequency of carriers having to switch targets.
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Carrier(unit) && unit.matchups.enemies.nonEmpty

  protected final def isInterceptorActive(interceptor: UnitInfo): Boolean = Seq(Orders.InterceptorAttack, Orders.InterceptorReturn).contains(interceptor.order)

  private def carrierKite(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.shieldWounded) return false

    // Linear wound berth: fresh scratches hold close, a bleeding hull
    // keeps fifteen tiles from anything that can answer.
    val marginTiles = unit.woundPercentBonus(0.0, CarrierKite.maxMarginTiles)
    Maff.minBy(unit.matchups.threats.filter(t =>
      t.canAttack(unit) && CarrierKite.isRanged(t.pixelRangeAgainst(unit))))(
      unit.pixelDistanceEdge(_)).exists(threat => {
      CarrierKite.shouldKite(unit.shieldWounded, unit.pixelDistanceEdge(threat) * Maff.inv32, marginTiles) && {
        unit.agent.act("CarrierKite")
        unit.agent.toAttack = None
        val away = threat.pixel.project(unit.pixel, marginTiles * 32.0)
        unit.agent.decision.set(?(away.walkable, away, away.walkablePixel))
        Commander.move(unit)
        true
      }
    })
  }

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // Dying carriers kite: below 100 HP a ranged attacker inside fourteen
    // tiles pushes the hull straight out instead of trading the game away.
    if (carrierKite(unit)) return

    if (unit.agent.shouldFight) {
      val brawling = CarrierTankHuntSeam.brawlingFor(unit)
      // Brawl beat: hold the kill while it lasts inside the beat, else
      // take the nearest enemy inside the beat, so airborne interceptors
      // keep chewing the whole pack instead of chasing one far target.
      val brawlTarget: Option[UnitInfo] = Maff.minBy(
        unit.agent.toAttack.filter(t => t.alive && t.visible && t.isEnemy && unit.pixelDistanceEdge(t) <= 32 * CarrierBrawl.brawlRadiusTiles).toSeq ++
        unit.matchups.targets.filter(t => t.isEnemy && t.visible && unit.canAttack(t) && unit.pixelDistanceEdge(t) <= 32 * CarrierBrawl.brawlRadiusTiles))(
        _.pixelDistanceEdge(unit))
      // Avoid changing targets (causes interceptors to not attack)
      // Avoid targeting something leaving leash range
      unit.agent.toAttack = ?(brawling, brawlTarget, None)
        .orElse(unit
        .orderTarget
        .filter(t => t.alive && t.visible && t.isEnemy && unit.pixelDistanceEdge(t) <= 32.0 * CarrierBrawl.leashTiles)
        .orElse(Target.choose(unit, TargetFilterCombatants, TargetFilterVisibleInRange))
        .orElse(Target.choose(unit)))

      val target = unit.agent.toAttack
      val interceptorsActive      = unit.interceptors.count(isInterceptorActive) >= 1 + unit.interceptors.count(_.complete) / 2
      val interceptorSafetyMargin = Maff.min(unit.matchups.threats.filterNot(Protoss.Carrier).map(threat => threat.pixelsToGetInRange(unit) - 8 * 32)).getOrElse(LightYear().toDouble)
      val chasePixel              = target.map(_.projectFrames(48))
      val chasingAlready          = chasePixel.exists(p => unit.speedApproaching(p) > unit.topSpeed / 2)
      if (target.isDefined && interceptorsActive) {
        chasePixel.foreach(unit.agent.decision.set)
      }
      val order = CarrierEngagement.decide(CarrierEngagement.CarrierMargins(
        hasTarget           = target.isDefined,
        brawling            = brawling,
        tankHunt            = target.exists(t => IsTank(t) && CarrierTankHuntSeam.huntFor(unit)),
        interceptorsActive  = interceptorsActive,
        chasingAlready      = chasingAlready,
        safetyTiles         = interceptorSafetyMargin / 32.0,
        targetDistTiles     = target.map(t => unit.pixelDistanceEdge(t) / 32.0).getOrElse(0.0)))
      order match {
        // Brawl: a fifty-interceptor wing never plays leash games, it
        // closes and shoots while the beat holds targets.
        case CarrierEngagement.Brawl =>
          unit.agent.act("CarrierBrawl")
          Commander.attack(unit)
        case CarrierEngagement.TankHunt =>
          unit.agent.act("CarrierTankHunt")
          Commander.attack(unit)
        // Chase with impunity, or if target is leaving leash range
        case CarrierEngagement.Chase =>
          unit.agent.act("CarrierChase")
          Commander.move(unit)
        // Attack with impunity
        case CarrierEngagement.Attack =>
          unit.agent.act("CarrierAttack")
          Commander.attack(unit)
        // Open leash
        case CarrierEngagement.OpenLeash =>
          Retreat.delegate(unit)
          unit.agent.act("OpenLeash")
        // Hold leash
        case CarrierEngagement.HoldLeash =>
          unit.agent.decision.set(unit.agent.toAttack.map(_.pixel.project(unit.agent.defaultHome, CarrierBrawl.homeLeashTiles * 32)))
          Commander.move(unit)
        case CarrierEngagement.Launch =>
          unit.agent.act("Launch")
          Commander.attack(unit)
        case CarrierEngagement.WarmUp =>
          WarmUpInterceptors(unit)
          Commander.attackMove(unit)
      }
    } else {
      Retreat(unit)
    }
  }}
