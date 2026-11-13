# Code review: `f/phase-b` vs `develop`

Scope: 140 commits, 336 files (11 189 insertions / 2 537 deletions), reviewed
2026-11-13 at branch tip `7d7b6fb80`. Focus, per the owner's brief:
(1) correctness, (2) placement - whether a change invents a concept that
already exists elsewhere and belongs in one class (or, if tiny, one method),
(3) SRP. Uncommitted work that appeared in the working tree during the review
(`ReaverBraveStand` + its suite, `ReaverThreats` fields) is *not* part of this
review; it is not in `develop...f/phase-b`. Note that it adds two more
decision fields (`foeInReach`, `tankCovered`) to the `ReaverThreats` snapshot,
which is the direction S4 argues against.

Verification run on the branch tip:

- `mvn -o -Ptests test-compile` - passes.
- `mvn -o -Ptests test` - BUILD SUCCESS, 622 tests, 132 suites, 0 failures.

Nothing below is a build/test failure; every finding is a design or
behavioural risk that the suite cannot see (game-coupled seams, mostly).

Legend: **C** = correctness, **D** = duplicated concept, **S** = SRP /
placement. Severity: high / medium / low.

---

## 1. Correctness

### C1 (high) Focus fire does not actually focus - the shared target is per-unit

`src/Micro/Targeting/FocusFire.scala:57-88`

The scaladoc promises a choice "identical for every shooter (so all of them
compute the same answer)". But `candidate` (line 84) builds the ranking key
with `attacker.damageOnNextHitAgainst(target)`, which is attacker-specific:

```scala
shotsToKill = shotsToKill(target.totalHealth, attacker.damageOnNextHitAgainst(target))
```

`choose` orders by `(-reach, shotsToKill, packDistanceTiles, id)`. Reach ties
are the normal case (a mass fight is exactly the situation where most
shooters can hit most targets), so the second key decides - and two shooters
with different damage (e.g. Dragoon vs Corsair vs Scout in one squad) get a
different `shotsToKill` per target and therefore a different "shared" target.
The feature then produces the spreading it exists to prevent.

Fix: rank on a squad-wide quantity, not a per-shooter one - e.g. remaining
health, wounds, or `totalHealth / squad.meanDamageAgainst(target)`. The
cheapest correct version is to keep `shotsToKill` out of the shared key and
use it only as a local tie-break after the shared target is chosen.

Same method, second problem: `sharedTarget` runs the whole O(shooters x
targets x shooters) scan for every attacker on every frame (`spawned` by
`Target.choose` -> `focusedTarget`). Compute it once per squad per frame
(`Squad` field or `Performance.Cache`), the way `Squad.targets` is already
published; publishing it also makes "one shared target" true by construction.

### C2 (cleared) Storm friendly-fire veto and the caster

`src/Micro/Actions/Combat/Spells/Protoss/PsionicStorm.scala:41`

Originally flagged as an off-by-one: `vetoBox` counts every friendly unit in
the box, and `SpellTargets.legal` does not exclude the caster, so a Templar
standing in its own box counts itself against `maxFriendlyHits = 2`.

Re-checked against the engine rule: Psionic Storm damages **every** unit in
the area, the caster included. A caster inside its own box is therefore a
third friendly unit that will be stormed, which is exactly what the rule says
should veto the cast. Counting it is correct; excluding it would let a storm
hit three of ours. No change made.

### C3 (medium) The attack-squad staffing guard is a tautology

`src/Tactic/Tactician.scala:374-377`

```scala
val freelancerValue = freelancers.view.map(_.subjectiveValue).sum
(With.blackboard.wantToAttack() || With.umsMode) && (With.blackboard.yoloing() || freelancerValue >= freelancerValue * .7)
```

`x >= 0.7x` is always true for a non-negative value, so the "enough
freelancers remain" half of the guard does nothing: the attack squad is
staffed even when the freelancers were already spent on defense. The original
code compared `freelancerValue` against `freelancerValueInitial`, but that was
read before the same `if`, so it was already tautological - the extraction
made it visible, which makes it the right moment to either drop the
comparison or capture the real "before `unitsPick`" value (the intent needs a
snapshot from earlier in `run()`, not from `fillAttackSquad`).

### C4 (medium) The trace label can disagree with the order it labels

`src/Micro/Actions/Basic/Build.scala:84` vs `:92`

The new builder-stage logging computes

```scala
lazy val affordable = With.self.minerals >= building.mineralPrice - 8 && With.self.gas >= building.gasPrice - 8
```

and five lines later the actual gate re-spells the same two comparisons
instead of using `affordable` (and `atSite`, `pushPixel`, `movePixel`). Any
future change to the affordability rule (e.g. a gas buffer) updates one and
not the other, and the log starts claiming `issueBuild` for orders that never
issue. Use the named values in the gate.

### C5 (medium) `LoneMarch` is a tested kernel that production bypasses

`src/Tactic/Squads/MarchOrders.scala:77-78`

```scala
val nearestWarrior = Maff.minBy(squad.units.view.filter(u => u != unit && u.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
if (nearestWarrior.forall(a => unit.pixelDistanceCenter(a.pixel) * Maff.inv32 > LoneMarch.aloneRadiusTiles)) return (squad.vicinity, "LoneMarch")
```

`LoneMarch.shouldMarchAlone` (`Decisions/LoneMarch.scala:15`) exists and is
tested by `LoneMarchSuite`, but has zero production callers. The predicate is
re-implemented by hand with `>`, `* Maff.inv32` and `forall`, so retuning the
kernel (or fixing it) silently changes nothing in the game. Call the kernel:

```scala
val alone = nearestWarrior.forall(a => LoneMarch.shouldMarchAlone(unit.pixelDistanceCenter(a.pixel) * Maff.inv32))
```

This is the exact failure mode the kernel-per-decision rule is meant to
prevent, and it is worth a grep for the same pattern in the other new gates
before closing the branch.

### C6 (medium) `MarchOrders` recomputes squad-wide aggregates per unit

`MarchOrders.scala:20-23, 77, 104-106, 115-125`

`sendUnits` computes the pack mean once (good), but the per-unit legs then
each scan the whole squad again: nearest warrior at line 77, nearest ally at
105, `warriorsCasters.map(_.subjectiveValue).sum` plus another nearest-ally
scan at 122-124. That is ~3 O(n) passes plus several `pixelDistanceCenter`
calls per unit, so O(n^2) per frame; at 40 units that is roughly 4-5k distance
computations per frame on the march path. The `warriorsCasters` sum is
unit-independent and should be hoisted into `sendUnits` with the mean; the
nearest-ally scan can be replaced by one pass that records the nearest
neighbour per unit.

### C7 (low) `defendVsBreach` reads `ourMain` unguarded

`src/Tactic/Tactician.scala:247-259`

`With.geography.ourMain` is dereferenced before any base check. The tree
already guards this exact hazard for baseless UMS maps (commit `75747c418`,
and the `MainMarch` guard in `MarchOrders`). It only fires when carriers
exist, so a UMS micro map is unlikely to reach it, but the guard should be
consistent: `if (With.geography.ourBases.isEmpty) return`.

### C8 (low) `ShuttleFleeGeometry.safetyMarginPixels` scales 10x, unbounded

`src/Micro/Actions/Protoss/Shuttle/ShuttleFleeGeometry.scala:25`

`(if (fasterFlyer) 64 else 160) * (1 + 10 * woundPercent)`: at full wounds the
margin grows to 704 px (faster flyer) or 1760 px (slower), and
`BeShuttle.unsafe` compares that against `pixelsEntangled`. There is no clamp, so an out-of-range
`woundPercent` (overkill damage, engine oddity) inflates the margin linearly.
The rest of the branch uses `woundPercentBonus`, which clamps. Either clamp
here or express it as a low/high band (see D2).

---

## 2. Duplicated concepts (new concept already exists elsewhere)

### D1 (high) "What counts as ranged" is defined three times

- `DarkTemplar/DarkTemplarEvade.scala:16` - `val rangedReachPixels = 32.0`, `isRanged`
- `Shuttle/ShuttleFleeGeometry.scala:38` - `val rangedReachPixels = 32.0`
- `Carrier/CarrierKite.scala:17` - `val rangedPixels = 32.0`, `isRanged`

Three new files, one concept, three names, two copies of the predicate. This
is a unit-class property, not a doctrine: put
`unitClass.rangedReachPixels` / `unitClass.isRangedAttacker` next to
`width`/`range` in `UnitClass`, or one helper in `CombatDistances`, and let the
doctrines compare against it. Otherwise the next doctrine (Reaver, Guardian)
adds a fourth copy.

### D2 (high) Wound-scaled margins hand-rolled next to the helper added for exactly that

`src/Micro/Actions/Combat/Maneuvering/AirFleeGeometry.scala:32-33`

```scala
def safeMarginTiles(woundPercent: Double): Double =
  baseMarginTiles + woundBonusTiles * woundPercent
```

`CombatUnit.woundPercentBonus(low, high)` was added **in this same branch**
(`ProxyBwapi/UnitInfo/CombatUnit.scala:61-65`, commit `dc0080024`) with the
comment "Replaces the hand-rolled `base + k * woundPercent()` scattered over
retreat margins". `AirFleeGeometry` is new code that hand-rolls it again, and
without the clamp. Replace with
`woundPercentBonus(baseMarginTiles, baseMarginTiles + woundBonusTiles)` and
the `woundBonusTiles` knob becomes the high bound, which is also what
`CarrierKite`/`BeCarrier` already do.

### D3 (high) Two identical temporal-confirmation filters

`Decisions/StanceVoteFilter.scala` and `Decisions/ConsensusVoteFilter.scala`

Same state machine - `confirmFrames = 24`, one streak counter, "confirm the
bad proposal over N frames, pass the good one through, reset on the good one"
- over `Stance` and `Boolean` respectively. `ConsensusVoteFilter`'s own header
introduces it as "StanceVoteFilter companion", i.e. the author noticed too.
This is one concept, so it should be one class:
`final class VoteConfirmation[A](good: A, var confirmFrames: Int = 24)` with
`confirm(adopted, proposed): A`. Two copies also means two places to retune
the 24 when one of them flickers in a game.

### D4 (high) The PvZ tech plan is implemented twice

`Gameplans/Protoss/PvZ/reactive/PvZReactiveTech.scala:55` (`ensureCompositionTech`)
vs `:119` (`buildTech` -> `techGoon`/`techSpeedlot`/`techReaver`/`techStargate`)

The same composition -> structure mapping exists in both:

| Composition | `ensureCompositionTech` | `buildTech` branch |
|---|---|---|
| Speedlot | Forge, Damage, Citadel, Speed, Archives (+ Corsair, DT) | `techSpeedlot` + `techSpeedlotCasters` |
| Reaver | RoboticsFacility, SupportBay | `techReaver` |
| Stargate | Stargate, Corsair, Cyber, Range / Citadel, Speed | `techStargate` |
| Goon | Cyber, Range | `techGoon` |

They will drift (one already has the DT/Corsair gating and the templar
priority, the other has the `cancelAlternative` calls). Since the entire point
of `ensureCompositionTech` is "the floor must not starve when `buildTech` is
pre-empted by `buildArmy`", the floor should be the same table read with a
different flag, not a parallel implementation.

### D5 (medium) Two choke-clearance rules for the same Pylon

`Placement/Pylons/PylonPlacement.scala:43-58` keeps every Pylon four tiles
from every metro exit; `Placement/Access/PlacementQuery.scala:118-121` keeps
**every** building (Pylons included) five tiles from the main choke. Both run
for a Pylon query, the comments acknowledge the overlap, and the numbers are
one tile apart with no shared constant. One requirement, one radius table
(`Map[PlaceLabel, Int]`), applied in `PlacementQuery` next to the other
defaults.

### D6 (medium) Two unload-safety vetoes in two packages

- `Micro/Actions/Transportation/DropSafety.scala` (new): no unload within 8.1
  tiles of an enemy anti-ground static, enforced in `CommanderTransport`.
- `Micro/Actions/Protoss/Reaver/ReaverUnloadSafety.scala` (existing): no
  Reaver drop inside a wound-scaled radius of any enemy, enforced from
  `ReaverMission`.

Both answer "is this landing spot safe", neither knows about the other, and a
third caller (`MissionDrop`) uses `withinSafetyMargin` for the same question.
Transportation is where this belongs; `ReaverUnloadSafety` should move there
(or `DropSafety` should take the Reaver radius as a parameter) so one place
owns the whole answer.

### D7 (medium) A Shuttle errand constant lives in the air-flee object

`Micro/Actions/Protoss/Shuttle/ShuttleQuestCost.scala:15`

```scala
val fightRadiusPixels: Int = (AirFleeGeometry.fightRadiusTiles * 32).toInt
```

`AirFleeGeometry` owns the *flee trigger* radius; `fightRadiusTiles` inside it
is really "a passenger this close still counts as fighting", a quest-cost
concept. The quest cost then reaches into the flee class to get it back.
Move the tile constant to `ShuttleQuestCost` (or give the two thresholds
their own homes); the current arrangement means changing the flee trigger
silently changes errand pricing.

### D8 (low) `MainAssault` restates `SuicideCharge`

`Decisions/MainAssault.scala` and `Decisions/SuicideCharge.scala` are the same
gate with different numbers and distance semantics (15 tiles Euclidean vs 30
tiles along the ground). `SuicideMission` already combines them, so the pair
is defensible, but if a third "never pose near the enemy main" condition
appears, it should become one object with a distance mode, not a third file.

### D9 (low) `SquadAutomation` is now a forwarding shell

`Tactic/Squads/SquadAutomation.scala` (75 lines) is one-line delegates to
`SquadTargeting`, `SquadForming` and `MarchOrders`, with 41 external call
sites. The file's own comment says the forwarders exist "so call sites do not
move". That is a legitimate temporary state, but it must be a planned one:
either migrate the 41 callers and delete the shell in this branch, or record
in `.ai/todo.md` that the shell is on the deletion list. As it stands there
are two names for one API, and the next feature will pick the wrong one.
Also `SquadAutomation.targetAndSend(squad, from, to)` ignores `to` (pre-existing
- the overload computes the target from consensus), which the split had the
chance to fix or remove.

---

## 3. SRP and placement

### S1 (high) The targeting ladder is a 35-call copy-paste chain

`src/Micro/Targeting/Target.scala` (366 lines, 35 `bestInStratum` calls)

- lines 101-153: snipe priority as 6 predicates x 2 sources (assigned,
  matchups) x 2 (`requireAcceleratesDemise` true, then the same six again with
  false) = 24 calls that differ only in the tier predicate and one flag;
- lines 156-185: DT doctrine, 6 calls with the same two-source pattern;
- lines 186-205: Carrier doctrine, 4 calls, same pattern.

Each new tier or unit doctrine is another 2-6 copies in the hottest file of
the tree, and the "assigned before matchups" convention is encoded by position
rather than by a helper. Extract

```scala
private def firstInStrata(attacker, filters, strata: Seq[UnitInfo => Boolean], requireAcceleratesDemise: Boolean): Option[UnitInfo]
```

that walks `(predicate, source)` pairs, and express each doctrine as data
(`Seq[(predicate, flags)]`). This also fixes the repetition of the
`requireAcceleratesDemise` pass (it could be one parameter over the list).

### S2 (high) `TargetFilterFocus.legal` is four doctrines in one OR chain

`src/Micro/Targeting/FiltersRequired/TargetFilterFocus.scala:13-60` (86-line
file)

Eight independent `output ||=` rules with inline world reads: commit, yolo,
targets-assigned lock, roadblocks, lone-hall cleanup (8-tile scan), the
in-range pick, the point-blank bust (`pointBlankLegal`, own kernel and
constant), and the wall bust (`wallBustLegal`, own kernel and constant). The
file is named "focus" but three of the rules are separate targeting
doctrines with their own thresholds, suites and comments, and the
required-filter chain (`Target.filters`) is the wrong place for a "these
override everything" list because it makes the filter's contract
unreadable.

Split the override rules into their own objects
(`TargetFilterHallCleanup`, `TargetFilterPointBlank`, `TargetFilterWallBust`),
each with its kernel, and let `Target` apply an explicit "always legal"
sequence before the required filters. That also makes S1's ladder shorter.

### S3 (medium) `marchTerminus` is a 45-line method with four early returns

`src/Tactic/Squads/MarchOrders.scala:44-100`

Architecture §7 asks for a named submethod past 15 lines. This method is the
first-claims chain (DT, main march, suicide mission, lone march) *plus* the
three march legs, and it computes `finalTravel` before the DT early return
(so Templars pay for a formation lookup they never use). The rules themselves
are fine and well commented; they should be an ordered
`Seq[MarchGate]`/list of `(applies, terminus, label)` and the legs their own
methods, so "order is the contract" reads as data instead of control flow.

### S4 (medium) `ReaverMission.braveDrop` is a controller inside a snapshot

`src/Micro/Actions/Protoss/Reaver/ReaverMission.scala:56-81`

The class doc says these are "one-frame reads" for `ReaverSituation`, but
`braveDrop` is a 25-line decision gathering five facts and calling
`ReaverDropBravery` three times. `AliveEnemies.get()` is called five times per
`read` (lines 33, 39, 61, 67, 70), three of those inside `braveDrop`. Hoist one
`val foes = AliveEnemies.get()`, and move the gathering into a
`ReaverDropContext.read(shuttle, unit)` seam so the case class stays a data
holder. Also `hikingFar` uses an inline `15 * 32` threshold (conventions rule
10 wants the tile name).

### S5 (medium) The new Reaver snapshots depend on the legacy brain

`ReaverOwn.scala:17` and `ReaverThreats.scala:30`

```scala
val cooldownReset = BeReaverLegacy.cooldownOnLanding
```

`ReaverOwn`/`ReaverThreats`/`ReaverMission` are the new Atlantis-side reads,
and they import the legacy action for a constant. `todo.md` has ReaverLegacy
on a removal criterion, so this dependency points the wrong way: move
`cooldownOnLanding` into `ReaverActions`/`ReaverOwn` and let the legacy brain
read it, otherwise deleting legacy breaks the new code (or keeps it alive).

### S6 (medium) The trace sink filters its callers by their message strings

`src/Debugging/CherryVis/CherryVis.scala:121`

```scala
if (reason.startsWith("Moving to ") || reason == "Holding move order" || reason.startsWith("#")) return
```

The branch removed `noteOrders` because engine echoes drowned the reasoning,
then re-implemented the filter inside the sink by matching the wording of
`CommanderMove`. Rename one string in `CommanderMove` and the noise comes
back; move the call out of `CommanderMove` instead (or give `decision` a
`kind` parameter). Debug code should not encode another class's literals.

### S7 (medium) `Probe.note` is not free, and its doc says it is

`src/Debugging/Probe.scala:30-40`, call sites
`Combat.scala:115`, `CombatEngagement.scala:94`, `SquadAttack.scala:266-328`

The gate is inside the callee, so every caller evaluates its arguments first:

```scala
Probe.note("Hold", unit.id, f"form=${...} flank=${...} me=${...} fired=${...} beyond=${...}%.1f hold=$hold")
```

That is a six-interpolation `f` string, plus `formationEngage.isDefined`,
`exists(_.flanking)`, `threatsInRange.exists(_.cooldownLeft > 0)` and a
distance, **per unit per frame** on the engagement path - for a probe that is
usually disarmed. Same shape in `Combat.scala:115` (per unit per frame) and
`SquadAttack.scala:328` (`units.count(...)` twice, inside a string).
The header's "Zero cost and zero output unless a key is armed" is false.

Fix once, for all 17 call sites: `def note(key: String, unitId: Int, message: => String)`
(by-name), or guard at the call sites with `if (Probe.on(key))`. The by-name
version keeps the one-line call-site style the doc advertises.

### S8 (medium) Wall scoring was split, filler scoring was left behind

`src/Placement/Walls/WallFinder.scala:109-128`

`WallScoring` was introduced with "The weights live here; WallFinder only
measures the geometry", and `scoreWall` now respects that. But
`scoreFillerGeometry` still carries `8 * ... + 30 * missing + 10 * ... + 6 * holes - 20 * ...`
inline, and `scoreFiller` adds `+ 5 * fillerPylons`. That is the same kind of
number in the same feature, still in the measuring class, so the next tuning
pass has to know which of the two files holds weights. Move the filler weights
into `WallScoring` (`scoreFiller(...)`) and keep `WallFinder` to measurements.

### S9 (medium) `PlacementQuery.resetDefaults` has two mechanisms for one idea

`src/Placement/Access/PlacementQuery.scala:40-110`

Pylon and Gateway defaults were extracted (`PylonPlacement`,
`GatewayPlacement`), but the per-building remainder is still inline:
`Seq(Terran.Barracks, Terran.Factory, Protoss.RoboticsFacility).contains(building)`,
then a 20-entry tech list, then a 5-entry defensive list. So "what a building
wants by default" is expressed both as a case class per building and as
`contains` lists in the middle of an ~80-line method (lines 40-118). Finish the extraction
(one `BuildingDefaults.apply(query, building)` map or one small class per
group) or, better, keep the lists but pull them out as named `val`s so the
method is a dispatch.

### S10 (low) The UMS formation audit lives in `Lifecycle`

`src/Lifecycle/UmsMode.scala:75, 82-97`

`auditFormation` writes probe rows (position, nearest enemy, order) every 48
frames. That is debugging/observation code inside the game-mode object, next
to `destinationFor`/terminus logic, and it duplicates the idea already
implemented by `Debugging.FormationAudit` + `scripts/formation-check.py`
(different payload, same question). It belongs in `Debugging` (Probe /
FormationAudit); then either merge it with the formation audit or name it as a
distinct `FormationCensus`.

### S11 (low) Dead field in a tested kernel

`PvZArmyWeights.Mix.scouts` is computed in `PvZArmyWeights.of` and never read;
`PvZReactiveArmy` pumps scouts unconditionally (`pump(Protoss.Scout)`). Either
use the weight or delete the field - dead data in a pure kernel invites
"why is this here" every time the mix is tuned.

### S12 (low) Unused imports left by the PvZ split

`Gameplans/Protoss/PvZ/reactive/PvZ1BaseReactive.scala` imports
`Macro.Facts.UnitArchive`, `Placement.Access.PlaceLabels`,
`ProxyBwapi.Buildable`, `Tactic.Missions.evaluation.DarkTemplarEffectiveness`,
`Utilities.UnitFilters.{IsHatchlike, IsWorker}` and (in the policy import)
`GasBank`, `ReaverGas` without using them. The orchestrator reads as if it
still owned the tech/army work it now delegates.

### S13 (low) Non-local `return` and cross-squad mutation in `staffRazeSquad`

`src/Tactic/Tactician.scala:340-365`

`donors.foreach(donor => { if (need <= 0) return ... })` exits the method from
inside a closure over a view of another squad's units. It works today, but it
is fragile against a future refactor of the collection or of `addUnit`.
`donors.take(need)` expresses the same intent without the non-local return
(the `need -= 1` loop then disappears too).

### S14 (low) `PvZ/__REVIEW.md` is stale and in Polish

`src/Gameplans/Protoss/PvZ/__REVIEW.md`

Three problems, one file: (a) `.ai/conventions.md` rule 1 requires English for
code docs, and this is Polish; (b) its own finding #3 says
"`PvZReactiveTech.buildTech` nadal ~70 linii, cztery gałęzie" - the branch
then split those branches into `techGoon`/`techSpeedlot`/`techReaver`/
`techStargate`, so the doc contradicts the code it describes; (c) as a
living review note it belongs in `.ai/`, not shipped under `src/`. Either
translate + refresh it, or move the remaining open items to `.ai/todo.md` and
delete the file.

---

## 4. What the branch does well

Worth recording so the next phase keeps it:

- **Policy objects with suites.** `EarlyPressure`, `RecoveryGas`,
  `DefenseWithoutDragoons`, `TemplarArchivesOrRoboticsFacility`,
  `GasBank`, `ReaverGas`, `StormResearch` - each tiny, pure, one gate, one
  suite in the mirrored test tree. This is exactly the shape the conventions
  ask for.
- **Wall finding split by responsibility.** `WallPlacement.blocker` (rules,
  facts in), `WallAdjacency` (geometry), `WallPlacementTry` (one permutation),
  `WallPlacementSearch`/`WallSearchBudget` (enumeration + budget),
  `WallScoring` (weights), `WallFinder` (game reads + filler). The extraction
  is behaviour-preserving against `develop` and the magic numbers moved with
  their loops. Only the filler weights were left behind (S8).
- **`MacroSim` -> `MacroStateAdvance`.** Small, documented, and the
  copy-on-write rule is stated next to the clones. (`techs` is an immutable
  `Set`, so its `+=` is a reassignment - no aliasing bug despite the missing
  clone.)
- **`SimulationSplash.splashDivisor`/`splashBounds`.** Splash maths pulled out
  of `SimulationGrid` with no behaviour change and a suite; the same for
  `HitCounter.countHits`, `FormationAuditStats`, `PsionicStorm` vetoes,
  `OurBaseUnderAttack`, `ShuttleQuestCost`, `FormationHold.fractionInSlot`.
- **`SquadForming` / `SquadTargeting`.** The two halves of `SquadAutomation`
  are genuinely different reasons to change and read well on their own; the
  forwarder (D9) is the only thing to settle.
- **Trace work is gated.** `FormationAudit` call sites are all behind
  `With.configuration.formationAudit` (one boolean read when off), and
  `CherryVis.decision` is change-only. `Probe` is the exception (S7).
- **Build is green**: 622 tests, and the new kernels are tested next to the
  code (`src/test/scala`, mirrored packages).

---

## 5. Suggested order

Non-game fixes first (all mechanical, suite-verifiable):

1. D1, D2, D3 - the three duplicated helpers (ranged threshold, wound bonus,
   vote filter). Pure extractions, suites already exist.
2. C5 - route `MarchOrders` through `LoneMarch.shouldMarchAlone`; then grep
   for other kernels with test-only callers.
3. S1, S2 - de-duplicate the targeting ladder and split the focus filter's
   overrides. Highest review value in the tree; no behaviour change intended,
   but the snipe order must be preserved exactly (it is the contract).
4. C4, C3, S13 - small seam cleanups (`Build.affordable`, the tautology,
   `take(need)`).
5. S7 - by-name `Probe.note` (perf on the engagement path, one signature).
6. D4, D5, D6, D7, S8, S9 - finish the splits that were started.

Needs a game (and a probe/trace), so schedule with the normal validation
round:

- C1 - focus fire: check in a mass fight whether two shooters with different
  damage pick the same target (`Probe.note("Target", ...)` rows already exist),
  then move the choice to the squad level.
- C2 - storm veto: storm-heavy game, count whether the caster was in the box.
- C8 - wounded Shuttle: a shuttle-vs-Hydra game to see the 704 px margin.
- C7 - only if a UMS map ever fields carriers.

Not urgent, but decide before the branch closes: D9 (keep or delete the
`SquadAutomation` shell), D8, S3, S4, S5, S6, S10, S11, S12, S14.

---

## 6. Fix status (2026-11-13)

Fixed in these passes; `mvn -o -Ptests test` green (646 tests), blank-line gate
clean:

| Finding | Change |
|---|---|
| C3 | Tautological freelancer guard replaced with `freelancers.nonEmpty` - same effect, and the intent is now stated honestly |
| C4 | `Build` uses the named `affordable` in the actual gate, so trace and order cannot disagree |
| C5 | `MarchOrders` calls `LoneMarch.shouldMarchAlone`; the kernel now has a production caller |
| C7 | `defendVsBreach` returns early when we have no bases (UMS) |
| D1 | One `RangedReach.isRanged` shared by `DarkTemplarEvade`, `CarrierKite` and `ShuttleFleeGeometry` |
| D2 | `Maff.woundBonus` is the single wound lerp; `CombatUnit.woundPercentBonus` and `AirFleeGeometry.safeMarginTiles` both use it (clamped) |
| D3 | `VoteConfirmation[A]` replaces `StanceVoteFilter` and `ConsensusVoteFilter`; both suites merged into one |
| S5 | `cooldownOnLanding` lives in `ReaverActions`; legacy and the Atlantis reads import it from there |
| S7 | `Probe.note` takes the message by name, so an unarmed key no longer builds its string; `Probe.reset()` added to the per-game resets |
| S8 | Filler weights and the filler formula moved into `WallScoring`; `WallFinder` only measures |
| S11 | Dead `Mix.scouts` removed |
| S12 | Unused imports removed from `PvZ1BaseReactive` |
| S13 | `donors.take(need)` replaces the non-local `return` and the manual counter |
| S14 | Stale Polish `PvZ/__REVIEW.md` deleted (its open items are covered by this document) |
| C6 (partial) | The Reaver mission read now does one registry scan instead of five; the `MarchOrders` per-unit aggregates remain |
| R1/R2/R12/R14/R17/R19 | See `.ai/review-reaver-shuttle.md` section E |

Second pass, behavior fixes carried by suites instead of games (`mvn -o -Ptests
test`: 646 tests, 0 failures):

| Finding | Change |
|---|---|
| C1 | `FocusFire` no longer builds its ranking key from the caller's damage: `volleysToKill` prices the squad's combined volley damage, so every shooter computes the same shared target by construction (`FocusFireSuite` covers the kernel) |
| C8/R20 | `ShuttleFleeGeometry.safetyMarginPixels` clamps the wound fraction at 1 (`ShuttleFleeGeometrySuite`) |
| R3 | `ContainStatus` moved out of the Reaver package into `Tactic.Squads` (the cluster and latch it owns are tactic state) and memoizes per frame; the six-plus scans per frame became one |
| R4/R5/R6 | One "shot ready" answer (`ReaverFireDiscipline.shotReady`) feeds both the boarding ban and the brave stand; the stand now also refuses when a foe is inside the close-pressure ring, and the snapshot's radii moved into `ReaverThreats`, which owns the facts it computes |
| R8 | `BeShuttle` owns every Shuttle move: the Scourge-rear flee moved out of `BeFlier`, which now skips transports entirely |
| R18 | The drop decision is a kernel: `ReaverDropBravery.decide(DropFacts)`; the read only gathers facts (`ReaverDropBraverySuite` covers both rules and every override) |
| R1/R2/R12/R14/R17/R19 | See `.ai/review-reaver-shuttle.md` section E |

Still open, and why:

- **C6** (`MarchOrders` scans) is a perf rewrite of the march path; do it with a
  profile, not by eye. The correctness half is done (the mission read scans the
  registry once).
- **D5** (the two Pylon choke radii) needs a placement tuning decision: which
  radius is the rule, and whether it is per building or per yard.
- **D4** (PvZ tech table twice), **S1/S2** (targeting ladder), **S3**
  (`marchTerminus`), **S6** (CherryVis string filter), **S9/S10** (placement
  defaults, UMS audit) are structural refactors. Each preserves behavior only
  if its order is kept byte for byte, so they belong in their own commit, not
  in a review sweep.
- **D8** (`MainAssault`/`SuicideCharge`) and **D9** (`SquadAutomation` shell)
  are decisions, not edits: merge the pair when a third condition appears;
  migrate the 41 callers or record the shell as a deletion target in
  `.ai/todo.md`.

The working tree also carried uncommitted work from another session during
this pass (`ReaverBraveStand`, `DragoonBrave`, `FightOrFlee`, steps); the owner
asked for it to be committed together with these fixes, so the commit carries
the whole tree.
