# Code review: `f/phase-c` vs `develop`

Scope: the phase-c delta itself - 120 commits, 151 files (5 622 insertions /
426 deletions) on top of `f/phase-b`, which is already an ancestor of `HEAD`.
`f/phase-b` was reviewed separately (`.ai/review-phase-b.md`); findings there
are only referenced where phase-c repeats the same mistake. Reviewed
2026-11-14 at branch tip `17f0bffaa`, working tree clean.

Brief for this review: SRP/OCP, but above all - **can the new concepts be
grouped with what already exists**, and does the branch introduce new edge
cases? A skeptical pass over the branch's own abstractions, not a bug hunt.

Verification actually run:

- `scripts/tests.sh` - BUILD SUCCESS, 673 tests, 169 suites, 0 failures.
- `scripts/starengine.sh SunkensScenarioSuite` - BUILD SUCCESS, 1 test. The
  rest of the StarEngine scenarios were **not** run (they are `starengine.Slow`
  and deliberately out of the routine gate).
- `python3 scripts/check-blank-lines.py <110 changed src files>` - clean.

Nothing below is a red build. Every finding is a design, duplication or
hidden-behaviour risk the suite cannot see.

Legend: **G** = grouping / placement (a new concept that already has a home -
this is where the SRP and OCP findings are filed), **D** = duplicate or new
edge case, **C** = correctness or silent behaviour change, **T** = tests.
Severity: high / medium / low.

---

## 1. What the branch does well (keep this shape)

- **The march gates.** `MarchOrders.MarchGate` + `marchGates: Seq[...]` is the
  best structural idea in the phase: first claim wins, order is data, labels
  are a sealed family instead of string literals, and each rule reads as one
  sentence. G5 and D2 use it as the yardstick against the branch's own weaker
  spots.
- **Pure decision objects, one gate each.** `StrongMarch`, `NoTankMarch`,
  `ToothlessMarch`, `KiteTo`, `SuicideEngage`, `IdleAttack`, `ContainFire`,
  `TankHug`, `CrucialAirSnipe`, `FerryIdleDraft`, `OrderContinue`,
  `WorkerThreatFlee.fleeDarkTemplarStraight`: small, no `With`, named
  thresholds, suite next to the code. This is the house style and it is
  consistent.
- **The StarEngine harness.** `World` (loop), `Body` (state), `Pilot`
  (decision), `Constraint` (map rule) with a documented per-frame order and
  documented simplifications is genuinely SRP/OCP: new behaviours arrive as
  pilots, new maps as constraints, the loop stays shut. Tagged out of the
  default suite, summoned with `scripts/starengine.sh`. It already paid for
  itself (1181 vs 0 band crossings).
- **`TargetedSpell.reachValue`** finally puts Yamato/Lockdown pricing in one
  place, and the storm veto/threshold helpers keep their numbers beside the
  rule.
- **`OrderContinue`** is a nice, minimal seam: the expensive heading math runs
  only past the cheap point check.

---

## 2. Grouping: the same new concept built several times

This is the core of the review. Each of these is a concept the branch *named*
in comments ("Latched LoneMarch shape", "exactly the Observer rule", "same ray
geometry") and then implemented again anyway.

### G1 (high) The hysteresis latch now exists six times, with no home

- `Tactic/Squads/Decisions/LoneMarch.scala:38` `marchAlone`
- `Tactic/Squads/Decisions/MarchDress.scala:41` `holdDress`
- `Tactic/Squads/Decisions/MarchDress.scala:97` `pushPastStatics`
- `Micro/Actions/Protoss/Detector/DetectorSpread.scala:55` `standOffLatched`
- `Micro/Actions/Protoss/DarkTemplar/DarkTemplarEvade.scala:66` `evadeLatched`
- `Micro/Actions/Protoss/ReaverShuttle/ReaverShuttleFlight.scala:64` `fleeLatched`

Six bodies, one algorithm:

```scala
val next =
  if (fires) true
  else if (releases) false
  else state.getOrElse(key, fires)
state.put(key, next)
next
```

they differ only in how `fires`/`releases` are computed from a distance,
plus a `private val <something> = new mutable.HashMap[Int, Boolean]()` and a
`def reset()`. Four of the six have a comment saying they are "the LoneMarch
shape". Two more latches were already extracted earlier for exactly this
reason (`RegroupLatch`, `VoteConfirmation`), so the concept is understood -
it just has no type.

The cost is not the 6 x 8 lines, it is the **reset ladder**: `EridanWave.scala`
now carries nine hand-written `reset()` calls (lines 39-49). Every new latch is
one more line that can be forgotten, and the failure is silent and nasty: a
stale id->Boolean map crossing a game boundary (ids restart every game) flips
behaviour on frame 0 with no trace. The queue already asks for a seventh latch
("Adopt-vs-AirFlee flap on pressured pickups wants a flee latch next"), which
is the honest signal that this will keep multiplying.

Fix (one small class, ~15 lines, behaviour identical):

```scala
final class Latch {
  private val held = mutable.HashMap.empty[Int, Boolean]
  def apply(key: Int, fires: Boolean, releases: Boolean): Boolean = {
    val next = if (fires) true else if (releases) false else held.getOrElse(key, fires)
    held.put(key, next)
    next
  }
  def reset(): Unit = held.clear()
}
```

Each caller keeps its own named thresholds and calls
`latch(unit.id, dist < enterTiles, dist > exitTiles)`; `EridanWave` resets one
registry (`Latches.resetAll()`) instead of nine lines. Then `LoneMarch`,
`DetectorSpread` etc. keep only knobs + geometry, which is what their headers
already claim.

### G2 (high) Same-type separation is implemented twice, and a third caller borrows the wrong one

`Micro/Corsair/SpreadOutAirUnits.scala` (new) vs
`Micro/Actions/Protoss/Detector/DetectorSpread.scala:12-29`:

| member | DetectorSpread | SpreadOutAirUnits |
|---|---|---|
| `minDistTiles` | 9.0 | 5.0 |
| `shouldSpread(newerId, olderId, distTiles)` | identical body | identical body |
| `OlderEye(id, pixel, distTiles)` | identical shape | identical shape (`OlderWing`) |
| `partner(usId, others)` | identical body | identical body |
| `awayFrom(us, them)` | identical body | identical body |

The only difference is the radius. And `BeArbiter.scala:42-51` calls
`DetectorSpread.partner/shouldSpread/awayFrom` while constructing
`DetectorSpread.OlderEye` values **for Arbiters** - the type name is now a lie
at its third call site, and its own comment says "exactly the Observer rule".

One concept, one home:

```scala
final case class Neighbor(id: Int, pixel: Pixel, distTiles: Double)
object Separation {
  def partner(usId: Int, others: Seq[Neighbor]): Option[Neighbor]
  def shouldSpread(usId: Int, olderId: Int, distTiles: Double, minTiles: Double): Boolean
  def awayFrom(us: Pixel, them: Pixel, minTiles: Double): Pixel
}
```

`DetectorSpread` keeps its detector-specific half (`enemyKeepOffTiles`,
`standOff`, the latch), Corsair passes 5, Observer and Arbiter pass 9, and
`SpreadOutAirUnits` disappears. Note this is the third instance of the *same*
copy-forward (D1/D2/D3 in the phase-b review were ranged threshold, wound
bonus and vote filter); the pattern is "copy, then name the copy after the
first caller".

### G3 (high) Three air-flee geometries, one of them dead

- `Maneuvering/AsAirAvoidLandUnits.scala` (new, used by Corsair, Shuttle,
  Observer and the StarEngine pilot): trigger 12, margin 3->10, corner guard.
- `Maneuvering/AirFleeGeometry.scala` (pre-existing): trigger 12, margin
  2.5->10.5, no corner guard, `fleePixel(own, focus, wound)`.
- `ReaverShuttleFlight` (new): `scourgeDistTiles 12`, `baseFleeMarginTiles 4`
  + `woundFleeBonusTiles 9`, i.e. 4->13, plus its own `threatensPreemptively`.

`AirFleeGeometry` now has **zero production callers** - only
`AirFleeGeometrySuite` keeps it alive (`grep -rn "AirFleeGeometry\." src`
returns nothing outside the suite). That is dead production code with a green
test: the worst kind, because the test says "this is covered" while the class
is unreachable. Meanwhile `AsAirAvoidLandUnits` is misnamed: its callers feed
it *any* enemy that can attack (`CorsairRangedFlee.scala:22`,
`BeDetector.fleeAttackers`), air units included, and its own header hedges with
`Air_{(and ground)}`.

Fix: delete `AirFleeGeometry` + suite; rename `AsAirAvoidLandUnits` to
`AirFlee` (or `AirAvoidance`) and let the three callers pass their bands
through the existing `fleePixel(..., marginLowTiles, marginHighTiles)` overload
- Observer already does exactly that (6->12). `ReaverShuttleFlight` then keeps
only the pair-specific margins and calls the same object.

### G4 (medium) "Penetration" is a vocabulary, but only one place spells it

`ReaverShuttleFlight.threatensPreemptively(distPixels, rangePixels, marginPixels)`
exists and has **no caller**. `BeShuttle.flee` (`BeShuttle.scala:139-180`)
re-spells the same idea inline:

```scala
Some((reach + preemptPixels - t.pixelDistanceEdge(shuttle), t.pixel))
...
if (RangedReach.isRanged(reach)) ...
```

and `worstPenetration` is used to reduce it. So the branch has the concept
named (`threatensPreemptively`, `worstPenetration`, `fleeExitMarginTiles`) but
the shipping path computes it by hand - the exact `LoneMarch`-has-no-caller
failure the phase-b review flagged as C5. Either call the kernel or delete it;
do not keep both.

### G5 (medium) The march gates recompute world facts per unit, and one rule is evaluated twice

`MarchOrders.sendUnit` computes

```scala
val toothless = ToothlessMarch.shouldMarchDirectly(...)   // line 73
```

and the gate list computes the *same* predicate again at line 139 with the
same three world reads. Both run for every unit every frame. `NoTankMarch`
(line 132) and `ToothlessMarch` additionally each scan
`With.units.enemy.exists(t => Terran.SiegeTankSieged(t) || ...)`, and
`With.enemies.exists(_.isTerran)` is evaluated in three separate places (two
gates plus the pre-gate `toothless` val above). With
~40 squad units and 10 gates that is a few hundred full enemy-list scans per
frame on the march path - the same shape as phase-b C6, on the branch's own
new hot path.

Fix, in the spirit of `MarchGate`: compute a per-frame `MarchFacts` once in
`sendUnits` and pass it into the gates:

```scala
private final case class MarchFacts(
  enemyIsTerran: Boolean,
  enemyHasTanks: Boolean,
  enemyHasVultures: Boolean,
  bankedMinerals: Int,
  armyStrength: Double,
  supplyUsed: Int,
  homeBases: Int)
```

`MarchGate(label, (facts, squad, unit) => Boolean)` keeps the data-driven
shape and removes the duplicate evaluation and the scan storm in one move.

### G6 (medium) A third targeting ladder, outside the targeting pipeline

Phase-b review S1 says `Target.scala` is already a 35-call copy-paste ladder and
should be reduced to one walker. Phase-c adds two more ranking ladders beside
it:

- `Micro/Targeting/DarkTemplarTargets.tier/pick` - "weakest first, then tank,
  warrior, worker, nearest inside tier", applied directly in
  `BeDarkTemplar.visibleHittable` (which also sets `unit.agent.toAttack`
  itself, bypassing `Target.choose` and therefore the squad's
  `targetsAssigned` and the required-filter chain);
- `CrucialAirSnipe.pick/kindOf` - a fixed five-kind priority that outranks
  everything in `CombatEngagement.engage`, including retreat;
- `TargetFilterFocus.allowsBuilding` - a new doctrine vote inside a required
  filter (phase-b S2 asked for the opposite direction).

Individually each is defensible, but the effect on OCP is that "what to shoot
first" now has three vocabularies (Target strata, DT tiers, crucial-air kinds)
and a fourth caller can reasonably pick any of them. Either
(a) express DT and crucial-air as `TargetFilter`s/score modifiers so there is
one ladder, or (b) make the doctrine layer explicit and documented - a
`Micro/Targeting/Doctrine/` bundle that `Combat` consults *before* `Target`,
with the rule "doctrine picks are single-target and never override the
squad's assignment unless ..." written down. What should not stay is an
undocumented second ladder that silently ignores squad assignment.

### G7 (medium) Kernel signatures carry parameters the only caller hard-codes

- `IdleAttack.shouldForceAttack(framesSinceAttack, hasHittableEnemy, shouldFight, locallyAhead)`
  - `IdleEngageBehavior.scala:31` only reads `IdleAttack.idleFrames`; the
    function has **no production caller**, and the caller never computes
    `locallyAhead` at all (it substitutes `unit.matchups.threatsInRange.isEmpty`).
- `ContainFire.shouldShoot(containing, cooldownLeft, hasHittableTarget)` -
  `ContainFireBehavior.scala:24` calls it with `containing = true` and
  `hasHittableTarget = true` after already checking both. Two of three
  parameters are constants at the only call site.
- `SuicideEngage.shouldForceAttack(framesSinceAttack, hasEnemy, isSuicideMission)`
  - `hasEnemy = true` and the suicide guard is already in the caller.
- `ReaverShuttleStatus.decide` grew a fourth boolean `enemyIsZerg`
  (`ReaverShuttleStatus.scala:23`) that short-circuits the other three rules,
  and both call sites (`ReaverShuttlePair.scala:78,88`) read
  `With.enemies.exists(_.isZerg)` on every frame.

This is the "fake seam" smell: the signature advertises a general rule, the
call site uses a degenerate case of it, and the suite then pins the general
rule (see T1). Either shrink the signature to what ships, or move the
lazy caller checks into the kernel so the kernel is the rule. A `PairFacts`
case class would let `decide` grow a new doctrine flag without a fifth
positional boolean, which is the direction `ReaverThreats` was already pushed
in during phase-b.

---

## 3. Duplicates and edge cases introduced by the phase

### D1 (medium) Two identical flee bodies in `BeDarkTemplar`

`BeDarkTemplar.scala:65-74` (`fleeFrom`) and `:76-92` (`revealedHide`) are the
same five statements - clear `toAttack`, `act`, project 4 tiles away, walkable
fallback, `Commander.move`, `true` - differing only in the act label and the
inner predicate. One `fleeAway(unit, threatPixel, label)` with the two
predicates as callers.

### D2 (medium) `BeReaver.perform` is now an eleven-claim if-chain

`BeReaver.scala:22-140` has eleven first-claim branches (five of them new or
reordered in this phase): `BoardRescue`, `BackOffStatic`, `BoardShotless`,
`StandShoot`, two static ranges, `BoardRide`, `BoardIdle`, `DuelWalk`. Several
re-scan `With.units.ours` /
`With.units.enemy` per frame (`duelWalk` repeats
`ReaverShuttlePair.duelTarget`'s scan; `:137` scans every friendly unit for a
Shuttle). The branch already has the better pattern for exactly this problem
(`marchGates`): a `Seq[Claim]` where each claim has a label and an
`apply(unit): Boolean`. It would also make the queue's "reboard/board/park/await
chains" audit tractable, since the claim order would be one readable list.

### D3 (low) Slot release duplicated in `Combat`

`Combat.scala:88-91` now has two consecutive `unit.agent.toAttack.exists(...) => None`
checks (dead, lifted-off) with the same shape. One
`releaseStaleTarget(unit)` reads better and is the natural home for the next
release rule. Also the `shouldDropLifted` kernel (5 lines of doc for a 1-line
`&&`) sits in `Target.scala` while its only caller is `Combat` - fine as a
kernel, but it should be `Combat`-adjacent or a named `TargetFilter`.

### D4 (low) Small formatting/consistency drift on new code

- `BeDetector.scala:123` - `private def keepOffDetectors(...): Boolean = {    if (`
  (statement on the brace line; the only place in the branch that does this).
- `WorkerThreatFlee.scala:24-27` - the new `def fleeDarkTemplarStraight` is
  sandwiched between two doc blocks with no blank line, so the following
  `/** A fighter this close ... */` reads as if it documents the new def.
- `unit.woundPercent` (no parens, `BeDetector:103`) next to
  `unit.woundPercent()` (`:112`) in the same file.
- `AsAirAvoidLandUnits.marginTiles(wound01)` (one-arg) is dead: every caller
  uses the four-arg overload with its own band (Observer 6->12). Delete the
  forwarder; keep `fleePixel`'s five-arg form, which Corsair/Shuttle do use.

### D5 (low) Two hysteresis idioms coexist

`RegroupLatch` and `VoteConfirmation` carry the state in the caller (pure,
stateless). The six G1 latches carry it in a HashMap. Both are legitimate, but
the split is undocumented: the next author will pick whichever file they read
first. Decide one rule - "pure kernel returns the threshold pair; per-unit
state goes through `Latch`" - and write it in `.ai/conventions.md` next to the
kernel rule.

---

## 4. Correctness and silent behaviour changes

### C1 (high) The contain-leave confirmation does not measure consecutive frames

`SquadAttack.scala:75`:

```scala
val keepContain = containFilter.confirm(mode == ContainArmy, newMode == ContainArmy)
if (mode == ContainArmy && ! keepContain) return
```

`containFilter` is `VoteConfirmation[Boolean](fastPath = true, slowPath = false)`
with `confirmFrames = 24`. Two problems:

1. `setMode` is only called from the *conditional* branches of `chooseMode`
   (`:87`-`:208`), unlike `updateStance` which runs every frame. So the streak
   only advances on frames that produce a proposal; a leave proposal repeated
   every other frame accumulates 24 entries over 48 frames and confirms - the
   filter no longer means "consecutive". The doc in
   `VoteConfirmation.scala` ("adopts the slow proposal only after it holds for
   consecutive frames") becomes false for this consumer.
2. The name reads backwards: `keepContain` is `true` when the proposal is
   *adopted*. `val adoptMode = ...` (or `APPROVED`) keeps the reader out of the
   inverted boolean.

Fix: call the filter on the *state* once per frame in `run()` (like
`updateStance`), or reset `streak` when no proposal was made - a
`confirmNothing()` on the filter. The second option also protects the stance
filter from the same drift if its call site ever becomes conditional.

### C2 (high) Arbiters silently lost the senior-arbiter attraction

`BeArbiter.scala` diff removes:

```scala
if (seniorArbiters.nonEmpty) {
  arbiter.agent.forces(Forces.threat) = ForceMath.sum(seniorArbiters.map(...)).clipAtMost(1.0)
}
```

That force is what used to bunch arbiters under the oldest one's cloak. It was
replaced by `spread` (same-type separation at 9 tiles) inside a commit about
spread, with no note in `queue.md` and no game behind it. Either it is a
deliberate doctrine change (arbiters cloak separately rather than stack) - then
it needs a queue line and a game - or it is collateral and should come back.
Removing a force vector changes every Arbiter game subtly; that is not a
refactor.

### C3 (medium) "Never raid into Zerg" hides inside a pure decision

`ReaverShuttleStatus.decide(..., enemyIsZerg)` returns `SupportArmy` before
looking at `isOldestReaver` or either health fraction. The intent is stated in
the class doc, but the *policy* ("no Reaver raids vs Zerg") is now an argument
to a status mapper, and both call sites pay a `With.enemies` scan per frame.
This is a doctrine, not a fact about the pair: it belongs where the pair
policy is read (`statusOfShuttle` / a `PairDoctrine`), leaving `decide` a pure
health/seniority mapper. Otherwise the next doctrine ("no raids vs Terran
mech") adds a fifth boolean.

### C4 (medium) `PsionicStorm` world reads inside a threshold property

`PsionicStorm.scala:50-51`: `maxApproachTiles = Some(maxApproachVsTerran(With.enemies.exists(_.isTerran)))`.
A `protected def` that is a getter now depends on the live world, and it is
re-evaluated on every `perform` (i.e. per caster per frame), whereas every
sibling threshold is a constant. The same race classification is spelled
`_.isTerran` here and `_.isZerg` in `ReaverShuttleStatus` - pick one
(`MacroFacts`/`With.enemies` helper: `enemyRaceIs(...)`) so "what race are we
facing" has one answer in the tree.

Also note the ladder renumbering (195/180/110-base-1 -> 180/100/75) is still
documented with the old numbers in `.ai/queue.md` ("energy-scaled pack
(195:1, 180:2, 110:base-1)"). Doc drift on a tuned rule; fix the queue line.

### C5 (low) `TacticWorkerScout.cachedThird` is cached forever

`TacticWorkerScout.scala:139-150`: `cachedThird` memoises "nearest base past the
natural" the first time `enemyMainFullyScouted` is true and never invalidates.
The layout is static, so that is mostly right, but `enemyMain` itself is not:
if the scouted main turns out to be a natural (or the enemy is re-identified),
the cached third is the wrong base for the rest of the game, and the minute
check walks there forever. Cheap fix: recompute when the cached base equals
`enemyMain`, or key the cache on `enemyMain.map(_.heart.center)`.

### C6 (low) `ferry`-side scans per frame

`ReaverShuttlePair.isBaitRide` scans every friendly unit for a Reaver/HT and is
called twice per Shuttle per frame (`BeShuttle.flee`, `BeShuttle.support`).
One `Performance.Cache` per frame (the tree uses it for exactly this) removes
the scan storm; same for `mateOnFoot` if the escort claim runs hot.

---

## 5. Tests

The branch's tests are not the problem in volume - 673 green - but in *what*
they pin. This is the "ten tests for one trivial thing, none for the complex
thing" split the owner asked to avoid.

### T1 (high) Three suites pin rules production does not run

- `IdleAttackSuite` covers `IdleAttack.shouldForceAttack` - a function with
  **no production caller** (only `IdleAttack.idleFrames` is used). The suite
  asserts four parameters that `IdleEngageBehavior` never supplies, including
  `locallyAhead`, which the behavior does not compute. The queue lists this
  feature as proven "(`IdleAttackSuite`)" - it is not pinned end to end.
- `MarchDressSuite` covers `shouldWait` (no production caller; production uses
  the latched `holdDress`, which uses `waitAheadTiles` directly).
- `TankHugSuite` covers `TankHug.closeEnough` (no caller:
  `TankHugBehavior` compares against `TankHug.hugGapTiles` inline).

This is phase-b C5 recurring in the same phase that recorded the lesson. The
fix is mechanical: make the behavior call the kernel (`IdleAttack.shouldForceAttack(...)`
with the facts it actually has, `TankHug.closeEnough(...)`) or delete the
kernel together with its suite (`MarchDress.shouldWait`, whose latched twin
owns the rule now). A green test on unreachable code is worse than no test,
because it closes the question.

### T2 (medium) Suites that restate the source line

- `ContainFireSuite`: four asserts on `containing && hasHittableTarget && cooldownLeft <= 0`.
- `TankHugSuite`: three tests on a four-term conjunction, several at the
  literal boundary of a constant (`siegedTankTiles = 7.01`, `6.9`).
- `MarchDressSuite` (11 tests): `staticNear(8.0)` / `! staticNear(8.01)` is the
  constant read back; the same for `shouldPushPastStatics(10.0)`.
- `SlowAloneSuite`: five single-assert cases of one conjunction.

None of these can fail for a reason a reader could not see in the one line of
production code. Meanwhile the genuinely tricky parts have **no** unit test:
`MarchOrders.marchGates` order and the `marchLegs` combination (the "order is
the contract" claim), `BeShuttle.flee` penetration reduction, and the latch
reset lifecycle. Those are where a suite earns its keep, and they are exactly
what StarEngine cannot see either (no march legs, no registry resets).

Prefer one table-driven suite per kernel with the interesting rows only, and
spend the saved budget on: gate order (a `Seq` of facts -> expected label),
`MarchDress.holdDress` latch through `Latch` (already has tests, keep), and a
"no latch survives a reset" test once G1 lands.

### T3 (medium) `AsAirAvoidLandUnitsSuite` assertions are disjunctive

`assert(out.x >= 192 || out.y >= 192)` (`:39`, `:41`, `:43`) passes for several
wrong corner implementations (e.g. always sliding vertically, or not clamping
at all in one axis). The function is deterministic pixel math - assert the
exact expected `Pixel` for one left-top, one right-top, one left-bottom case.
The rest of the suite (centroid, wound ordering, trigger boundary) is good.

### T4 (positive) StarEngine usage

Scenarios are written as behaviour claims ("closers run the worker down, kiters
watch it escape"), each suite is tagged, and the deleted `StarTest` app was
replaced by the shared `World`/`Pilot` harness rather than duplicated. That
consolidation is the model for G1-G3: many scenarios, one engine.

---

## 6. Suggested order

Mechanical and behaviour-preserving first (all suite-verifiable):

1. **G1** - `Latch` + `Latches.resetAll()`; convert the six copies; delete the
   nine reset lines in `EridanWave`. Do it before the seventh latch (queue)
   is written.
2. **G2** - `Separation` + `Neighbor`; Arbiter and Corsair stop borrowing
   `DetectorSpread.OlderEye`; delete `SpreadOutAirUnits`.
3. **G3** - delete `AirFleeGeometry` + its suite; rename
   `AsAirAvoidLandUnits`; pass bands.
4. **T1** - wire or delete `IdleAttack.shouldForceAttack`,
   `MarchDress.shouldWait`, `TankHug.closeEnough`; then G4
   (`threatensPreemptively` either called or deleted).
5. **D1, D3, D4** - small extractions/formatting (`fleeAway`,
   `releaseStaleTarget`, brace line, blank line, parens).

Structural, one commit each, order kept byte-for-byte:

6. **G5** - `MarchFacts` through the gates; removes the duplicated
   `toothless` evaluation.
7. **D2** - `BeReaver.perform` as a claim list (same shape as `marchGates`).
8. **G7/C3** - `PairFacts` for `ReaverShuttleStatus`, and move the
   "no raids vs Zerg" doctrine to the caller.

Needs a game (behaviour, not refactor):

- **C2** arbiters without the senior attraction: one Arbiter game, watch cloak
  coverage and clumping.
- **C1** contain-leave confirmation: sunken map, count Contain/Deprox
  alternation (the queue already has this number: 160 -> 39).
- **C4** storm leash 10/12 and the new pack ladder: the Templar game that has
  been pending for three phases.
- **C5** worker-scout third: only if a game shows the wrong base being walked.

Decisions to record rather than code:

- **G6** - either fold DT/crucial-air into `Target`, or write down the doctrine
  layer. Silence is what makes the third ladder a fourth later.
- **D5** - one hysteresis idiom, written into `.ai/conventions.md`.

---

## 7. Status table

| Finding | Area | Verdict |
|---|---|---|
| G1 | latch x6 | extract `Latch`, one reset registry - highest value, zero behaviour change |
| G2 | separation x2 + Arbiter | extract `Separation`; `SpreadOutAirUnits` disappears |
| G3 | air flee x3 | delete dead `AirFleeGeometry`; one named home |
| G4 | penetration | kernel exists, no caller - call it or delete it |
| G5 | march gates | `MarchFacts` per frame; drop duplicate `toothless` |
| G6 | targeting ladders | decide: fold into `Target` or document the doctrine layer |
| G7 | kernel params | shrink or move caller checks in; `PairFacts` |
| D1 | `BeDarkTemplar` | one `fleeAway` |
| D2 | `BeReaver` | claim list, like `marchGates` |
| D3 | `Combat` | one `releaseStaleTarget` |
| D4 | formatting | brace line, blank line, parens |
| D5 | idioms | document the rule |
| C1 | contain filter | per-frame confirm (or reset on no-proposal); rename `keepContain` |
| C2 | Arbiter force | restore or record; needs a game either way |
| C3 | pair doctrine | move the Zerg rule out of `decide` |
| C4 | storm thresholds | hoist the race read; fix the queue's stale ladder |
| C5 | scout third | invalidate on `enemyMain` change |
| C6 | ferry scans | per-frame `Cache` |
| T1 | dead kernels | wire or delete |
| T2 | trivial suites | consolidate; spend the budget on gate order + latches |
| T3 | corner math | exact pixels |
| T4 | StarEngine | keep as is - the model for G1-G3 |

---

## 8. Applied (2026-11-14)

Every finding was applied on the same branch; `scripts/tests.sh` green,
blank-line gate clean, jar rebuilt.

| Finding | Change |
|---|---|
| G1 | `Utilities.Latch` (per-key enter/release/initial) + `Latches.register`/`resetAll`; the six copies now delegate (`LoneMarch`, both `MarchDress` latches, `DetectorSpread`, `DarkTemplarEvade`, `ReaverShuttleFlight`); `EridanWave` keeps a single `Latches.resetAll()` instead of six lines; `LatchSuite` covers fire/hold/release, the initial side, per-key isolation and the game reset |
| G2 | `Mathematics.Separation` + `Neighbor` (`shouldSpread`/`partner`/`awayFrom`, radius passed in); Observer, Arbiter and Corsair use it; `SpreadOutAirUnits` deleted with its suite; `SeparationSuite` covers the rule |
| G3 | `AirFleeGeometry` and its suite deleted (zero production callers); `AsAirAvoidLandUnits` renamed `AirFlee` with the band in the header, dead one-arg `marginTiles` removed, corner asserts now exact pixels |
| G4 | the redundant `threatensPreemptively` predicate deleted; `penetrationPixels` is the one vocabulary and `BeShuttle.flee` computes both branches (Scourge trigger, ranged trigger) through it |
| G5 | `MarchFacts` (enemy race, tanks, vultures, bank, strength, supply, home) read once per `sendUnits` and threaded through `MarchGate`, `sendUnit` and `marchTerminus`; the duplicated `toothless` evaluation and the per-unit enemy scans are gone |
| G6 | doctrine layer documented (conventions rule 15 + headers of `DarkTemplarTargets`, `CrucialAirSnipe`) instead of a fourth ladder: `Target` owns the squad ladder, doctrine picks are pre-`Target` overrides |
| G7 | `IdleAttack.shouldForceAttack` wired into `IdleEngageBehavior` (the behavior no longer owns a private copy), `ContainFire.shouldShoot` lost the two constant parameters, `SuicideEngage.shouldForceAttack` lost the caller-owned guards |
| D1 | `BeDarkTemplar.fleeAway` replaces the duplicated evade/hide bodies |
| D2 | `BeReaver.perform` is a `groundClaims` list over one `GroundContext` (ride + nearest foe measured once), loaded handling extracted; order unchanged |
| D3 | `Combat.releaseStaleTarget` folds the three slot-release rules |
| D4 | brace line, blank line and `woundPercent()` fixed; one-arg `marginTiles` deleted |
| D5 | conventions rule 14 fixes one hysteresis idiom |
| C1 | `VoteConfirmation.confirmNothing`; `SquadAttack` breaks the contain streak on proposal-free frames and renames `keepContain` to `adoptProposal` |
| C2 | Arbiter force removal recorded in `queue.md` as a doctrine change needing one game (restoring the old pull would fight the new spread) |
| C3 | `ReaverShuttleStatus.decide` is a pure health/seniority mapper again; "never raid into Zerg" lives in `ReaverShuttlePair` with one per-frame race read |
| C4 | storm leash reads the matchup through a per-frame `Cache`; `maxApproachVsTerran` renamed `approachTilesFor`; the queue's ladder numbers refreshed |
| C5 | the scouted-third cache is keyed on `enemyMain`, so a re-identified main cannot leave the old base walking |
| C6 | `unloadedReavers` and `anyPairCargo` are per-frame caches, so bait/mate reads stop rescanning the army per Shuttle |
| T1 | the three dead kernels are now live (or gone: `MarchDress.shouldWait` deleted with its rows) |
| T2 | suites trimmed to non-obvious rows; `LatchSuite`/`SeparationSuite` added; the gate-order table test still needs per-unit facts extracted (proposed next, not done) |
| T3 | exact corner pixels |
| T4 | unchanged |

Still open on purpose: the gate-order table test (needs a `MarchUnit` facts
extraction - `MarchFacts` covers the world half only), and the Arbiter game
(C2). Both are recorded in `.ai/queue.md`.
