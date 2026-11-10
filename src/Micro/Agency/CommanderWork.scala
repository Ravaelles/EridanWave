package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points, Tile}
import ProxyBwapi.Orders
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import ProxyBwapi.Upgrades.Upgrade
import Utilities.UnitFilters.IsLairlike

/** See Commander: role-grouped delegates behind the facade. */
object CommanderWork {

  private[Micro] def repair(unit: FriendlyUnitInfo, target: UnitInfo): Unit = {

    target.friendly.foreach(With.coordinator.healing.heal(unit, _))

    unit.agent.setRideGoal(target.pixel)

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    unit.bwapiUnit.repair(target.bwapiUnit)

    CommanderCore.sleep(unit, 24)

  }




  private[Micro] def returnCargo(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    if (unit.carrying) {

      unit.bwapiUnit.returnCargo()

      CommanderCore.sleepReturnCargo(unit)

    }

  }




  private[Micro] def gather(unit: FriendlyUnitInfo): Unit = unit.agent.toGather.foreach(gather(unit, _))

  private[Micro] def gather(unit: FriendlyUnitInfo, resource: UnitInfo): Unit = {

    unit.agent.setRideGoal(resource.pixel)

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    if (unit.carrying) {

      // Spamming return cargo can cause path wobbling

      if ( ! Vector(Orders.ResetCollision, Orders.ReturnMinerals, Orders.ReturnGas).contains(unit.order)) {

        lazy val nextClosestFrames = Maff.min(With.geography.ourBases.view.flatMap(_.townHall).filter(_.complete).map(h => unit.framesToTravelTo(h.exitTile)))

        val incompleteHall = unit.base.flatMap(_.townHall.filter(t => ! t.complete && ! IsLairlike(t)))

        if (incompleteHall.exists(hall => nextClosestFrames.forall(_ * 2 > hall.remainingCompletionFrames))) {

          CommanderMove.move(unit, incompleteHall.get.pixel)

        } else {

          unit.bwapiUnit.returnCargo()

        }

      }

    } else if ( ! resource.visible) {

      CommanderMove.move(unit, resource.pixel)

    } else {

      def doGather(): Unit = { unit.bwapiUnit.rightClick(resource.bwapiUnit) }

      if (resource.unitClass.isGas) {

        if ( ! unit.orderTarget.contains(resource)) {

          doGather()

        }

      } else {

        lazy val coworkers            = With.gathering.getWorkersByResource(resource)

        lazy val accelerantFrame      = 11 + With.latency.remainingFrames

        lazy val accelerantMineral    = With.gathering.getAccelerantMineral(resource)

        lazy val accelerantPixel      = With.gathering.getAccelerantPixelSteady(resource)

        lazy val distance             = unit.pixelDistanceEdge(resource)

        lazy val projectedFrames      = Maff.nanToZero(Math.max(0, distance - unit.unitClass.haltPixels) / unit.topSpeed + 2 * Math.min(distance, unit.unitClass.haltPixels) / unit.topSpeed)

        lazy val onAccelerantPixel    = With.gathering.onAccelerant(unit, resource)

        lazy val onTargetMineral      = unit.orderTarget.contains(resource)

        lazy val onAccelerantMineral  = accelerantMineral.exists(unit.orderTarget.contains)

        def doGatherFromAccelerant(): Unit = { unit.bwapiUnit.gather(accelerantMineral.get.bwapiUnit) }

        if (unit.order == Orders.MiningMinerals) {

          // Leave well alone!



        // Reduce delay after returning cargo. Cargo-culted from Stardust: https://github.com/bmnielsen/Stardust/commit/1f303c8735aac2de348a8528b7e42fb75de3d96b

        // If we microed units every frame we would only care about the frame after returning. Because we micro intermittently, allow a delay.

        // Require a minimum distance to avoid hurting ourselves on Fastest Possible Maps.

        } else if (With.framesSince(unit.lastFrameCarrying) < 8 && distance > 64) {

          doGather()

        } else if (onAccelerantPixel) {

          doGather()

        // See https://github.com/bmnielsen/Stardust/blob/b8a91e52f453e6fdc60798edac569826df98148a/src/Workers/Workers.cpp#L587

        } else if (coworkers.exists(w => w != unit && w.order == Orders.MiningMinerals && w.orderTarget.contains(resource) && w.bwapiUnit.getOrderTimer + 7 == 11 + With.latency.remainingFrames)) {

          doGather()

        } else if (projectedFrames == accelerantFrame) {

          doGather()

        } else if (accelerantMineral.isDefined && false) { // TODO: Need to filter accelerant minerals before relying on them; they need to pull the worker across their face relative to the town hall or at least be a straight horizontal shot

          if (onTargetMineral) {

            if (projectedFrames > accelerantFrame) {

              doGatherFromAccelerant()

            }

          } else if (onAccelerantMineral) {

            if (distance < 32 || unit.pixelDistanceEdge(accelerantMineral.get) < 32) {

              doGather()

            }

          } else if (projectedFrames > accelerantFrame) {

            doGatherFromAccelerant()

          }

        } else if ( ! onTargetMineral) {

          doGather()

        }

      }

    }



    CommanderCore.sleep(unit, 1)

  }




  private[Micro] def build(unit: FriendlyUnitInfo, unitClass: UnitClass): Unit = {

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    // Don't auto-unload! We have a separate process for building Scarabs in loaded Reavers

    unit.bwapiUnit.build(unitClass.bwapiType)

    CommanderCore.sleepBuild(unit)

  }



  private[Micro] def build(unit: FriendlyUnitInfo, unitClass: UnitClass, tile: Tile): Unit = {

    unit.agent.setRideGoal(tile.center)

    if (unit.unready)       return

    if (CommanderCore.autoUnburrow(unit)) return

    if (CommanderCore.autoUnload(unit))   return



    if (unit.pixelDistanceSquared(tile.center) > Math.pow(32.0 * 5.0, 2)) {

      CommanderMove.move(unit, tile.center)



      return

    }



    if (unitClass.isAddon) {

      unit.bwapiUnit.buildAddon(unitClass.bwapiType)

    } else {

      unit.bwapiUnit.build(unitClass.bwapiType, tile.bwapi)

    }

    CommanderCore.sleepBuild(unit)

  }




  private[Micro] def tech(unit: FriendlyUnitInfo, tech: Tech): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.research(tech.bwapiTech)

    CommanderCore.sleep(unit)

  }



  private[Micro] def upgrade(unit: FriendlyUnitInfo, upgrade: Upgrade): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.upgrade(upgrade.bwapiType)

    CommanderCore.sleep(unit)

  }




  private[Micro] def cancel(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready) return



    if (unit.teching) {

      unit.bwapiUnit.cancelResearch()

    } else if (unit.upgrading) {

      unit.bwapiUnit.cancelUpgrade()

    } else if (unit.training) {

      unit.bwapiUnit.cancelTrain()

    } else {

      unit.bwapiUnit.cancelConstruction()

    }

    CommanderCore.sleep(unit)

  }




  private[Micro] def rally(unit: FriendlyUnitInfo, pixel: Pixel): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.setRallyPoint(pixel.bwapi)

    unit.lastSetRally = With.frame

    CommanderCore.sleep(unit)

  }




  private[Micro] def addon(unit: FriendlyUnitInfo, unitClass: UnitClass): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.buildAddon(unitClass.bwapiType)

    CommanderCore.sleep(unit)

  }



}
