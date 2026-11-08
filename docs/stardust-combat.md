# How Stardust keeps units together (and what we should steal)

How Stardust (`/sc-ai/Stardust/src`) makes 8 Dragoons cross the map as one
ball and arrive in a firing line instead of a scattered walk. Read alongside
`docs/architecture.puml` (our pipeline); the point of this note is the delta,
not a rewrite.

All file paths below are relative to `/sc-ai/Stardust/src`.

## 1. The decision chain, end to end

```
Squad::execute / AttackBaseSquad::execute        (WHAT: move, attack or regroup?)
  -> UnitCluster::move | attack | regroup         (per-cluster tactic)
       -> MyUnit::moveTo / attackUnit             (per-unit micro, at most ONE BWAPI order per unit per frame)
```

* A **Squad** owns clusters, detectors and arbiters; it does not position
  individual units.
* A **UnitCluster** is a proximity group with a running center, a **vanguard**
  (lead unit), a physical footprint (`ballRadius = sqrt(area / PI)`,
  `lineRadius = 16 * unitCount`) and an activity:
  `Moving | Attacking | Regrouping` (+ sub-activities like `ContainChoke`,
  `StandGround`, `Flee`).
* Per-unit micro (`MyDragoon::attackUnit`, kiting boids, unstick, order
  issue) only runs for units that are **ready** — a unit mid-attack is never
  touched (`if (!unit->isReady()) return/continue`). This is the single
  biggest anti-thrash rule: no re-orders while a shot is in flight.

Ours, for comparison: `Strategery -> Planning -> Tactic -> Agent ->
Combat -> Commander`, with `Intention` as the per-unit chit and `Commander`
as the only BWAPI caller. Same shape, different glue (see below).

## 2. Who belongs together: clusters with hysteresis, not assignments

`General/Squad.cpp`:

* A unit joins the cluster whose **vanguard** is nearest, if
  `dist - ballRadius <= 480` (`ADD_THRESHOLD`).
* Two clusters combine when their balls overlap within 480, or when both fit
  an arc at the same progress toward the enemy main (`< 0.05` apart).
* A unit is kicked out only past **600** (`REMOVE_THRESHOLD`) *and* falling
  behind the vanguard's progress to the target.
* Add (480) vs remove (600) is deliberate hysteresis: membership never
  flickers on a boundary.

Every frame each cluster recomputes: prune dead units, recompute the center
as the mean position, and re-elect the vanguard = the unit closest to the
target (within a 32 px band; ties go to the unit closest to the center).

Contrast: our squads are built once by the recruiter (`LockUnits`,
`Assignment`), and an 8-Dragoon ball has no representation at all - no
center, no vanguard, no radius. Units join by value, not by position.

## 3. Moving as a ball: one combined order per unit

`UnitCluster::move` (`Tactics/Move.cpp`) + `Formations/Ball.cpp`:

* Only the **vanguard cluster** flocks, and only with 2+ ground units outside
  narrow chokes and leaf areas. Everyone else gets a plain `moveTo(target)`.
  Units separated from the vanguard by a narrow choke are also excluded -
  flocking across a choke would string the ball out single-file.
* Flocking units get **one** move order computed from four boids:
  * **Goal (128)**: toward the 3rd navigation-grid waypoint ahead (not the
    raw target - the path bends around unwalkable ground first).
  * **Collision (64)**: away from unwalkable tiles.
  * **Cohesion (64, scaled down as the ball grows)**: toward the running
    cluster **center**. This single vector is what keeps the ball a ball.
  * **Separation (96, or 160 with radius scaled to enemy AoE range)**:
    apart from each other, so the ball never collapses into a clump -
    and spreads out exactly when splash damage threatens.
* One order per unit per frame (`issuedOrderThisFrame` guard); waypoints
  come from the grid + choke path and are re-pathed as the unit advances.
* Unstick is **event-driven, not frame-counted**: the engine's own
  `isStuck()` flag, a dragoon-specific cancelled-attack case, and in both
  cases a grace window (`unstickUntil = now + latency`) during which the
  unit is left alone. Compare ours (`Unstick`: 24 motionless frames while
  ordered to move), which fires inside an ordinary traffic jam.

Net effect: the whole ball converges on the same waypoint cloud, pulled to
one center, never re-issued mid-step, and never frozen by its own neighbours.

## 4. Arriving as a line: the arc, formed *before* contact

`UnitCluster::attack` + `Formations/Arc.cpp`:

* If **nobody is in danger and nobody is in range yet** (and the vanguard
  has a clear walkable lane to its target), the cluster does **not** attack:
  it forms an **arc** around a pivot placed past the enemy, at a desired
  standoff distance.
* Each unit holds its arc slot (goal boid toward the pivot at the right
  radius + the same separation boid), stragglers more than 128 px behind the
  vanguard are pulled straight to it, and anyone who cannot compute a
  direction falls back to the vanguard position instead of a random tile.
* Once ~8 units sit within a tile of their slots, the arc tightens by one
  tile (`desiredDistance -= 32`). The line is *built*, then it *advances*.
* Only when something is actually in range/danger does per-unit micro
  (`attackUnit`, kiting boids: goal 64 toward target, separation 1.5/96)
  take over - and units mid-attack are still not touched.

This is the piece we are missing most: we have no "form up before contact"
phase. Our march formation only exists while `fightConsensus` holds, i.e.
*after* somebody already decided to fight, so the line (if any) assembles
mid-fight instead of before it.

## 5. Observers have an owner and a state machine

`General/UnitCluster/Detectors.cpp` - detectors are not squad members. A
dedicated manager assigns each observer one of `None | EscortingArmy |
ScoutingEnemyArmy | DetectingEnemy`:

* damaged or far behind  -> escort the vanguard (at a point *ahead* of it),
* a cloaked enemy is around -> go scout it,
* a cloaked unit is found -> close to 64 px / back off (the 64 px rule),
* otherwise -> sit at home.
* Every assignment is logged per unit (`CherryVis::log(id)`).

Compare ours: the observer shares the army's terminus/vicinity and the same
`Combat` brain as the shooters (only softened to Walk by our Pacifist rule).
There is no "who owns the observer right now" answer in our code.

## 6. The attack decision is simulated, not voted

`AttackBaseSquad::execute`: enemies within ~640 px of the center ->
`selectTargets` -> combat sim (with reinforcements, choke detection,
detector input) -> the cluster's *current activity* picks the verdict
function (`shouldStartAttack` / `shouldContinueAttack` /
`shouldStopRegrouping`). Regroup itself is a small menu (contain static /
hold choke / stand ground / flee / attack the blocking army), each gated by
its own sim run. Every decision is logged with its numbers.

Compare ours: `fightConsensus` is a majority vote of `shouldFight` flags, and
flapping the vote flips destinations, formations and targets for the whole
squad in one frame. Their equivalent state (`Moving | Attacking |
Regrouping`) only changes through a sim verdict.

## 7. What to adopt, in order of payoff

1. **One order per unit per frame, and never touch a busy unit.**
   Stardust's `isReady()` gate (mid-attack units are left alone) is the
   cheapest anti-thrash rule we lack. Ours re-issues every frame.
2. **Event-driven unstick.** Replace "24 motionless frames while ordered"
   with the engine's stuck flag plus a grace window. Ours freezes clumps.
3. **Form the arc before contact.** Gate our march/engage formation on
   "approaching a known or suspected enemy", not on `fightConsensus`.
   Close the loop with a formed-up check (their ~8-units-in-place rule)
   before advancing the line.
4. **Straggler rally.** Units >128 px behind the vanguard go to the
   vanguard, not to their own terminus. Ours have no such rule.
5. **Cohesion toward a running center** as a first-class force, with
   separation scaled by enemy AoE. Our pushes are threat- and wall-centric;
   nothing pulls the group into a shape.
6. **A detector owner.** Either a `TacticMonitor`-style dedicated holder
   with explicit states (escort / scout / detect), or at minimum a
   latched escort offset from the vanguard instead of the shared terminus.
7. **Per-unit decision logs** (`CherryVis::log(id) << ...` at every branch).
   We have the board + unit logs; we lack the "why this waypoint/order"
   line that makes their replays debuggable. Cheap, high value.
8. **Sim-gated regroup** instead of threshold voting. Larger project;
   do it last, after 1-6 stop the bleeding.
   (Implemented: `RegroupLatch` - an Engaged|Regrouping stance latched through
   the per-unit sim verdicts with asymmetric thresholds, 0.35 to break off
   and 0.65 to re-engage, so a borderline simulation cannot flap the squad.
   Latched regroup overrules the frame vote and HuntContact; yolo resets it.)

Explicitly *not* proposed: copying the cluster/membership machinery.
Our recruiter + squad + HuntContact latch already answers "who goes where";
what we lack is how they *move together* once assigned (items 1-6).

## 8. Addendum: holding the line while outranged (implemented)

`Combat.engage` used to break formation up to `128 * confidence` px *before*
reaching firing range, so each unit chased on its own. `FormationHold`
(`Micro/Actions/Combat/Decisionmaking`) now vetoes `breakFormationToAttack`
while a ranged target sits more than 1.1 tiles beyond our range: the unit keeps
its engage slot (`destinationNext` resolves to the station, which outranks the
terminus) and lets the enemy come. Inside the 1.1-tile band the fight starts -
the unit steps in and shoots. The veto spares melee (it must close), flankers, and an
explicit yolo. Sliding forward inside the formation additionally requires
`fractionInSlot == 1.0` - the line steps only once fully formed, with the
straggler rally feeding laggards back into their slots.
