package Micro.Agency
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points, Tile}
import Micro.Coordination.Pushing._
import ProxyBwapi.Orders
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
/** See Commander: role-grouped delegates behind the facade. */
object CommanderMove {
  private[Micro] def doNothing(unit: FriendlyUnitInfo): Unit = {
    if (unit.unready) return

    CommanderCore.sleep(unit)
  }
  private[Micro] def stop(unit: FriendlyUnitInfo): Unit = {
    unit.agent.setRideGoal(unit.pixel)
    CommanderCore.leadFollower(unit, stop)
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderMove.stop")
    unit.bwapiUnit.stop()
    Debugging.FlapLedger.note(unit.id, unit.unitClass.toString, Debugging.FlapLedger.HoldKind, With.frame)
    CommanderCore.sleep(unit)
    unit.resetSticking()
  }
  private[Micro] def hold(unit: FriendlyUnitInfo): Unit = {
    unit.agent.setRideGoal(unit.pixel)
    CommanderCore.leadFollower(unit, hold)
    if (unit.unready) return
    if ( ! Zerg.Lurker(unit) && CommanderCore.autoUnburrow(unit)) return

    if (unit.velocity.lengthSquared > 0 && unit.order != Orders.HoldPosition) {
      CommanderCore.claimOrder(unit, "CommanderMove.hold")
      unit.bwapiUnit.holdPosition()
      Debugging.FlapLedger.note(unit.id, unit.unitClass.toString, Debugging.FlapLedger.HoldKind, With.frame)
    }
    if (unit.matchups.targetsInRange.nonEmpty) return CommanderCore.sleepAttack(unit)

    CommanderCore.sleep(unit)
  }
  // We used to refuse to issue commands to Photon Cannons due to inadvertent attack cancelling.
  // It looks like we've fixed that by using a 6-frame stopFrame
  //
  // Via Ankmairdor, the iscript for Photon Cannon attacks:
  // PhotonCannonGndAttkInit:
  //   playfram           2
  //   wait               2
  //   playfram           1
  //   wait               2
  //   playfram           3
  //   wait               2
  // PhotonCannonGndAttkRpt:
  //   wait               1
  //   attack
  //   gotorepeatattk
  //   ignorerest
  //
  // on first frame of attack, the animation runs twice
  // so there is 6 frame delay for first attack and 0 frame delay for continued attacks, also no non-interruptible section so additional commands can interupt an attack
  // though the biggest optimization would be issuing an attack command rather than waiting for them to autotarget
  // though I suppose with latency, this can backfire a small percentage(about 1/15 or 2/15) of the time and result in fewer attacks
  //
  // on average issuing an attack command that arrives on the first frame that a target is available will save 7 frames
  // if you wait until a target is in range to issue attack, you save an average of 3 frames (LF3)
  // if you waited to issue attack and the tower autotargets before your order arrives, you lose at least 22 frames(cooldown)
  private[Micro] def attackMove(unit: FriendlyUnitInfo): Unit = attackMove(unit, unit.agent.destinationNext())
  private[Micro] def attackMove(unit: FriendlyUnitInfo, destination: Pixel): Unit = {
    unit.agent.setRideGoal(destination)
    CommanderCore.leadFollower(unit, attackMove(_, destination))
    unit.agent.tryingToMove = unit.pixelDistanceCenter(destination) > CommanderCore.tryingToMoveThreshold
    if (unit.unready) return

    val to = CommanderOrder.adjustDestination(unit, destination, remember = true)
    if (CommanderCore.autoUnburrowUnlessLurkerInRangeOf(unit, to)) return
    // Auto-acquiring a target changes AttackMove to AttackUnit; retain the original movement goal.
    val alreadyAttackMovingThere = (unit.order == Orders.AttackMove && unit.orderTargetPixel.exists(_.pixelDistance(to) < 128)) || (
      unit.order == Orders.AttackUnit && Option(unit.bwapiUnit.getLastCommand).exists(command =>
        command.getType == bwapi.UnitCommandType.Attack_Move && ! command.isQueued && new Pixel(command.getTargetPosition).pixelDistance(to) < 128))
    if ( ! alreadyAttackMovingThere || unit.seeminglyStuck) {
      CommanderCore.claimOrder(unit, "CommanderMove.attackMove")
      unit.bwapiUnit.attack(to.bwapi)
      Debugging.FlapLedger.note(unit.id, unit.unitClass.toString, Debugging.FlapLedger.AttackMoveKind, With.frame)
    }
    CommanderCore.sleepAttack(unit)
  }
  private[Micro] def patrol(unit: FriendlyUnitInfo): Unit = patrol(unit, unit.agent.destinationNext())
  private[Micro] def patrol(unit: FriendlyUnitInfo, destination: Pixel): Unit = {
    unit.agent.setRideGoal(destination)
    CommanderCore.leadFollower(unit, patrol(_, destination))
    unit.agent.tryingToMove = unit.pixelDistanceCenter(destination) > CommanderCore.tryingToMoveThreshold
    if (unit.unready) return

    val to = CommanderOrder.adjustDestination(unit, destination, remember = true)
    if (CommanderCore.autoUnburrowUnlessLurkerInRangeOf(unit, to)) return

    CommanderCore.claimOrder(unit, "CommanderMove.patrol")
    unit.bwapiUnit.patrol(to.bwapi)
    Debugging.FlapLedger.note(unit.id, unit.unitClass.toString, Debugging.FlapLedger.MoveKind, With.frame)
    CommanderCore.sleepAttack(unit)
  }
  private[Micro] def move(unit: FriendlyUnitInfo): Unit = move(unit, unit.agent.destinationNext())
  private[Micro] def move(unit: FriendlyUnitInfo, tolerancePixels: Double): Unit = move(unit, unit.agent.destinationNext(), tolerancePixels)
  /**
   * Explicit interrupt for gate-proven withdrawals (contact kites): a
   * standing firing solution never vetoes these - the gate already
   * proved moving beats the volley, and the gun is cooling anyway.
   */
  private[Micro] def moveOverriding(unit: FriendlyUnitInfo): Unit =
    move(unit, unit.agent.destinationNext(), 32.0, true)
  private[Micro] def move(unit: FriendlyUnitInfo, destination: Pixel, tolerancePixels: Double = 128, overrideFiringSolution: Boolean = false): Unit = {
    // Adjust even while unready: transport coordination below still needs the destination.
    val to = CommanderOrder.adjustDestination(unit, destination, remember = unit.ready)
    unit.agent.setRideGoal(to)
    CommanderCore.leadFollower(unit, move(_, to))
    unit.agent.tryingToMove = unit.pixelDistanceCenter(to) > CommanderCore.tryingToMoveThreshold
    Debugging.SlowAlone.noteIfSlowAlone(unit, to)
    if (unit.unready) {
      if (unit.pixelDistanceCenter(to) > 3 * 32) {
        Debugging.Probe.note("Move", unit.id,
          f"unready gap ${unit.pixelDistanceCenter(to).toInt} final ${unit.pixelDistanceCenter(unit.agent.destinationFinal()).toInt} order ${unit.order}")
      }
      return
    }

    // Never cancel an imminent volley: an attack order on a live target
    // in range owns the frame while the shot is about to leave -
    // re-issuing a move over it trades the volley for a step. A cooling
    // gun owns nothing: the shot already fired, and standing through the
    // whole cooldown under it pins kiting hulls in place while the
    // presser walks in (1DragWounded: the dragoon stood every cooldown
    // behind its own order and died standing). Kiting, retreats and
    // dodges run after the march and interrupt explicitly when they must.
    if ( ! overrideFiringSolution &&
      (unit.order == Orders.AttackUnit || unit.order == Orders.AttackTile) &&
      unit.orderTarget.exists(t => t.alive && t.visible && unit.inRangeToAttack(t) &&
        unit.framesToBeReadyForAttackOrder(t) <= With.latency.latencyFrames + 2)) return

    if (CommanderCore.autoUnburrow(unit)) return

    if (unit.pixelDistanceCenter(to) <= 8) {
      CommanderTransport.requestUnload(unit)
    }
    if (Terran.Medic(unit) && unit.agent.shouldFight) {
      attackMove(unit, to)
    } else if (unit.pixelDistanceCenter(to) > 3) {
      // Line movement is always an explicit move, never an attack-move
      // (owner order): attack-move auto-acquire breaks formation slots
      // and the volley cadence instead of saving them. Shooting stays
      // with the technique's explicit attacks.
      // Skip re-issuing an identical Move: re-orders
      // reset acceleration and break weapon cooldown alignment (Stardust: one order per unit per frame).
      // Path followers pass a tight tolerance: waypoints advance in small
      // hops, and a wide deadband lets the unit arrive and idle between hops.
      // Arrival flow (PurpleWave): inside 1.8 tiles of the move target,
      // while the final destination lies beyond along the same heading,
      // aim past the waypoint so the unit flows through instead of braking
      // into every hop. Backsteps and true arrivals keep braking.
      val finalPx: Pixel = unit.agent.destinationFinal()
      val aligned = Math.abs(Maff.normalizePiToPi(unit.pixel.radiansTo(finalPx) - unit.pixel.radiansTo(to))) <= Math.PI / 4
      val flowPush = ArrivalFlow.pushPast(unit.tileDistanceCenter(to), unit.tileDistanceCenter(finalPx), aligned)
      val goal = if (flowPush > 0 && unit.order == Orders.Move) unit.pixel.project(to, unit.pixelDistanceCenter(to) + flowPush * 32).walkablePixel else to
      // Order continuation, not just motion: keep the old order while it
      // still takes us there - same target, or a far goal on our heading.
      // A far goal that drifted keeps cruising instead of resetting
      // acceleration every frame; near goals keep the exact point check,
      // so kiting and dodges stay responsive. The heading math runs only
      // past the point check, never on the hot path.
      val orderTarget = unit.orderTargetPixel
      lazy val distTiles = unit.pixelDistanceCenter(goal) * Maff.inv32
      lazy val onCourse = Math.abs(Maff.normalizePiToPi(unit.angleRadians - unit.pixel.radiansTo(goal))) <= OrderContinue.headingConeRadians
      val continuing = unit.order == Orders.Move && (
        orderTarget.exists(_.pixelDistance(goal) < tolerancePixels) ||
          OrderContinue.holdsCourse(distTiles, onCourse))
      if ( ! continuing || unit.seeminglyStuck) {
        CommanderCore.claimOrder(unit, "CommanderMove.move")
        unit.bwapiUnit.move(goal.bwapi)
        Debugging.FlapLedger.note(unit.id, unit.unitClass.toString, Debugging.FlapLedger.MoveKind, With.frame)
      } else {
        Debugging.Probe.note("Move", unit.id,
          f"hold gap ${unit.pixelDistanceCenter(to).toInt} final ${unit.pixelDistanceCenter(unit.agent.destinationFinal()).toInt}")
      }
      if ( ! unit.flying) {
        CommanderCore.pushThrough(unit, to, TrafficPriorities.Freedom)
      }
    } else if (unit.pixelDistanceCenter(unit.agent.destinationFinal()) > 3 * 32) {
      Debugging.Probe.note("Move", unit.id,
        f"arrived gap ${unit.pixelDistanceCenter(to).toInt} final ${unit.pixelDistanceCenter(unit.agent.destinationFinal()).toInt}")
    }
    CommanderCore.sleep(unit)
  }
}

