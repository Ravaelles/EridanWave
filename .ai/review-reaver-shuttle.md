# Review: Reaver and Shuttle - design, wiring and layers

Scope: the Reaver discipline (`Micro.Actions.Protoss.Reaver`), the Shuttle
discipline (`Micro.Actions.Protoss.Shuttle`), their shared transport layer
(`Micro.Actions.Transportation`, `CommanderTransport`), and the missions that
own them (`Tactic.Missions.reaver`, `Tactic.Missions.drop`). Reviewed
2026-11-13 on `f/phase-b` (`7d7b6fb80`) plus the uncommitted
`ReaverBraveStand` work in the tree.

Two questions, per the owner:

1. **Injection**: can each concept actually reach the game, from the layer it
   belongs to, without a second owner overwriting it? A gate that never fires
   and a gate that fires twice are the same bug from the outside.
2. **Logic**: do the decisions hold up on their own terms - Reaver and Shuttle
   are the units with the most field problems, so the review is about the
   reasoning, not style.

Verification: `mvn -o -Ptests test` green before and after the review work
(622 tests). Nothing here fails a test; the failures are runtime seams.

---

## A. Injection audit

What "correctly injected" means here: one owner per decision per frame, the
owner sitting in the layer the decision belongs to (architecture §1/§5), the
owner reachable from the game loop, and the compiled-in default being the
behavior we intend to ship.

| Concept | Defined in | Reached from | Verdict |
|---|---|---|---|
| `BeReaver` (strategy switch) | `Reaver/` | `Fight.perform` line 58 | OK, single entry |
| `BeReaverAtlantis` / brain | `Reaver/` | `BeReaver` when `reaverAtlantisBehavior` (default `true`) | OK |
| `BeReaverLegacy` | `ReaverLegacy/` | `BeReaver` when the flag is off | OK as fallback |
| `ReaverSituation.read` | `Reaver/` | brain only | OK, one reader |
| `ReaverOwn/Threats/Mission.read` | `Reaver/` | `ReaverSituation` only | OK |
| `ReaverBraveStand` (WIP) | `Reaver/` | brain line 22; its thresholds also read *by* `ReaverThreats` | **inverted** (R6) |
| `ReaverStaticStandoff` | `Reaver/` | `ReaverThreats` + `ReaverBackOffStatic` | OK, but broken at both call seams (R1) |
| `ReaverDropBravery` | `Reaver/` | inline 25-line block in `ReaverMission.read` | **decision living in a read** (R18) |
| `ReaverUnloadVeto` / `ReaverUnloadSafety` | `Reaver/` | `ReaverMission.read` | OK |
| `DropSafety` | `Transportation/` | `CommanderTransport.unload`, `transportFlying = true` hardcoded | param dead (R12) |
| `RequestSafeLanding` | `Transportation/` | `ReloadScarabs` only | not wired to drops (R13) |
| `ContainStatus` | `Micro.Actions.Protoss.Reaver` | `Tactician`, `SquadAttack` (x2), `DropManifestOps`, `MissionReaverDrop` (x2), `ReaverMission` | **layer inversion + 6+ scans/frame** (R3) |
| `ContainStatus.reset` | `ContainStatus` | `EridanWave.onStart` | OK |
| `Probe.reset` | `Debugging/Probe` | nothing | **no per-game reset** (R19) |
| `BeShuttle` chain | `Shuttle/` | `Transport.perform` (in the `Idle` run-everything list) | OK entry, but see R8 |
| `ShuttleScourgeFlee` | `Shuttle/` | `BeFlier.perform` (in `Fight`) | **second movement owner** (R8) |
| `ShuttleDitchPassengers` / `ShuttleAdoptPassenger` as guards | `Shuttle/` | `BeShuttle.perform` lines 40-41 | **guard value is sleep state** (R9) |
| `ShuttleQuestCost` / `ShuttleFleeGeometry` | `Shuttle/` | `BeShuttle` | OK; one borrowed constant (R16) |

Layer reality check: `Micro.Actions.Protoss.Reaver.ContainStatus` imports
`With.tactics.attackSquad`, and `Tactic.Tactician`/`Tactic.Squads` import
`ContainStatus` back. Micro-level unit package <-> tactic layer dependency,
in both directions, through a mutable latch. That is the clearest "we cannot
inject this cleanly" signal in the whole area.

---

## B. Reaver

### B1. The snapshot is read before the guard that decides whether we need it

`ReaverAtlantisBrain.act`:

```scala
val situation = ReaverSituation.read(unit)
if ( ! situation.rideNearby) return
```

The guard is the cheap fact (one `ride` lookup and a distance). The snapshot is
the expensive one: `ReaverOwn` (1 matchup-free read), `ReaverThreats` (matchups
+ `AliveEnemies.get()`), `ReaverMission` (`With.enemies`, `ContainStatus.now()`,
**five** `AliveEnemies.get()` scans, `ReaverDropBravery` x3). Every Reaver
without a Shuttle in range - i.e. most Reavers most of the time, since a Reaver
lives in a Shuttle - pays the whole thing to learn it should not be here.
Legacy (`BeReaverLegacy.perform`) has the same guard first *because* it is
cheap.

Fix: `ReaverOwn.rideNearby(unit)` (factory, same comparison) before the read.
This is one line plus a small extraction and removes the bulk of the cost.

### B2. Units: `tileDistanceEdge` is already in tiles

`CombatUnit.tileDistanceEdge(other) = pixelDistanceEdge(other) * Maff.inv32`.
The new code multiplies it by `Maff.inv32` again in two places:

- `ReaverThreats.read:56` - `unit.tileDistanceEdge(t) * Maff.inv32` as
  `distTiles` for `ReaverStaticStandoff.shouldBackOff`;
- `ReaverBackOffStatic.act:21` (same expression) and `:26` (`distTiles`).

A static five tiles away becomes 0.16 "tiles", so:

- `shouldBackOff` is true for any static in the threat list (its condition is
  `distTiles < foeRangeTiles + 0.5`, and 0.16 is always below any range);
  `staticInFace` therefore means "some static can reach us", not "we are inside
  its reach plus an edge";
- `backoffTiles(distTiles = 0.16, ownRange = 8)` returns ~7.8 tiles, so the
  Reaver retreats almost its full range instead of stepping out to max range -
  a Reaver that could outrange a Sunken instead runs away from it.

The pixel ranges in the same expressions are converted correctly
(`pixelRangeAgainst * Maff.inv32`), which is what makes this a unit bug rather
than a wrong formula. Both call sites need the raw `tileDistanceEdge`.

### B3. `ReaverBraveStand` duplicates two existing stand gates (WIP)

Three objects now answer "stand and shoot instead of boarding":

| Gate | Position | Condition | Effect |
|---|---|---|---|
| `ReaverBoardPressure.mustShootFirst` | board gate | `scarabs > 0 && !hasActiveScarab && !cooling` && (pressured or holdTarget) | bans boarding |
| `ReaverBraveStand` (new) | before boarding | `shields >= 50 && stale >= 96f` && armed && foe <= 8.5 && !tankCovered | `Commander.hold` |
| `ReaverHoldToFire` | chain tail | `scarabs > 0 && !hasActiveScarab && holdTarget` | `Commander.hold` |

`ReaverBraveStand.act` and `ReaverHoldToFire.act` are identical
(`agent.act` + `Commander.hold`). The fire-discipline kernel
(`ReaverFireDiscipline.mustShootFirst`) already exists to express "the shot
must be spent before boarding" with a suite; BraveStand re-encodes the same
intent with different numbers (96 frames of staleness, 50 shields) and a
broader `armed` (it counts a Scarab in flight).

Consequence in the chain: BraveStand sits *before* `ReaverBoardPressure`, so a
shielded Reaver with a stale shot holds ground even while it is being shot at
(`brave` does not look at `pressured`, `pressuredClose`, or `woundFraction`).
The intent in the doc comment ("boarding first would circle without dealing
damage") is exactly `mustShootFirst`; the difference is that BraveStand also
requires the enemy to be within 8.5 tiles and shields above 50.

Fix direction: one owner. Either BraveStand becomes the *only* stand gate and
`mustShootFirst` is expressed through it (with the fire-discipline kernel
inside), or BraveStand is folded into `ReaverBoardPressure` as a third
`standVs...` predicate and `ReaverHoldToFire` keeps the tail. Two copies of
`Commander.hold` and two thresholds for the same decision will drift; the
`brave` threshold (96 frames) already contradicts `ReaverDropBravery.staleFrames`
(240 frames) for the same "this Reaver has not fired in a while" idea.

### B4. The behaviour owns the snapshot's facts

`ReaverThreats` computes `foeInReach` from `ReaverBraveStand.engageTiles` and
`tankCovered` from `ReaverBraveStand.tankMarginTiles`. The read now depends on
a behavior that may be removed from the chain (or reordered) for the meaning of
its own fields. `ReaverBoardLogic.workerStandRadiusTiles` already has the same
shape (pre-existing). Thresholds that define a snapshot field belong to the
snapshot (or a kernel both sides import), not to the behavior that consumes it.

### B5. `ReaverSituation` re-declares its records

41 fields in the façade, 41 in the records (15 + 16 + 10), and 41 hand-written
assignments in `read`. Adding one fact costs four edits (record field, record
computation, façade field, façade wiring) and a missed wiring line compiles
silently into the field's default. The split itself is good (three small
records); the flat façade is what doubles the maintenance. Keep the flat
constructor for tests, but build it from the records by destructuring (or add a
compile-time exhaustiveness guard) so a new field cannot be forgotten.

### B6. Mission context does a mission decision

`ReaverMission.read` is 40 lines and half of it is `braveDrop`: gathers five
facts, calls `ReaverDropBravery` three times (`isRangedThreat`, `shouldDrop`,
`mayDropStale`, `canAffordShot`) and scans `AliveEnemies.get()` five times in
total. The snapshot's job is to read; the drop decision belongs to
`ReaverDropBravery` as a `decide(facts): Boolean` with the gathering in one
seam, which also lets the kernel's suite cover the actual combination instead
of only its pieces.

---

## C. Shuttle

### C1. Two movement owners in the same frame

A Shuttle is driven twice per frame:

1. `Idle.actions` -> `Transport` -> `BeShuttle` (ditch, adopt, flee, emergency
   pickup, best quest, park, await);
2. `Fight.perform` -> `BeFlier` -> `ShuttleScourgeFlee.fleePixel` (a loaded
   Shuttle with Scourge within 12 tiles runs to the rear of its cluster), then
   the generic scourge steering.

`Fight` is later in the same list, so `BeFlier` writes `destinationNext` and
issues the move *after* BeShuttle chose a quest - last writer wins, and the
"order is the contract" claim only holds inside each of the two owners.
Architecture §2 asks for one ordered chain per level; a Shuttle now has two,
in different layers, with no arbitration between them.

Fix: one owner. Either `BeShuttle` calls the scourge flee itself (it already
has a wound-scaled flee), or `BeFlier` owns all Shuttle movement and `Transport`
stops calling `BeShuttle` - but not both. `ShuttleScourgeFlee`'s doc ("the
caller issues the order") already assumes a single caller, and that caller is
not the Shuttle discipline.

### C2. The guard idiom at the top of the chain does not mean what it reads

`BeShuttle.perform`:

```scala
if (ShuttleDitchPassengers(shuttle)) return
if (ShuttleAdoptPassenger(shuttle)) return
```

Both are `Action`s, so `X(shuttle)` is `Action.apply`, whose return value is
`unit.unready` - the *sleep state* (`sleepUntil > With.frame`), not "this
action did something". So:

- when the unit is awake (the normal case), the guards are false and the chain
  continues, overwriting whatever ditch/adopt just did;
- when the unit is asleep, `perform` never ran (gated in `Action.act`) and the
  guard returns true - returning from a chain that did nothing.

The three guards the branch added (`fleeFromAirThreats`, `makeEmergencyPickup`,
`runBestQuest`) are plain `Boolean` methods and behave correctly; the two
inherited ones do not. The pattern is load-bearing now that the chain has a
documented order. Fix: make the two Shuttle helpers return `Boolean` like the
new ones (they already compute "did I act"), or check their effect explicitly.

### C3. Landing safety is wired for reloads, not for drops

- `CommanderTransport.unload` vetoes when a flying transport is within 8.1
  tiles of an enemy anti-ground combat building (`DropSafety`), with
  `transportFlying = true` hardcoded inside `if (transport.flying)` - the
  kernel's `false` branch is unreachable from production (its suite tests it).
- `ReaverMission` adds two more vetoes: `unloadVetoed` (three ranged
  ground-hitters covering the landing, `ReaverUnloadVeto`) and `unloadSpotSafe`
  (no enemy inside a wound-scaled radius, `ReaverUnloadSafety`).
- `RequestSafeLanding` (spiral search for a walkable tile outside enemy ground
  range) is only called from `ReloadScarabs`, never from a drop.

So "is this a safe place to put the unit down" is answered by four different
pieces of code with three different enemy filters (`isCombatBuilding &&
canAttackGround`, `ranged && attacksGround`, `isStaticDefense`+, any enemy) and
one of them (the spiral search) is not in the drop path at all. Not a merge of
the rules - they answer different questions - but they need one seam with a
name that says which question it answers, and the hardcoded `true` must go.

---

## D. Findings

Severity: high = wrong in game, medium = will drift or waste, low = naming/
maintenance.

| # | Sev | File:line | Finding |
|---|---|---|---|
| R1 | high | `ReaverThreats.scala:56`, `ReaverBackOffStatic.scala:21,26` | `tileDistanceEdge * Maff.inv32` - distance 32x too small; staticInFace always true, backoff ~full range |
| R2 | high | `ReaverAtlantisBrain.scala:15-16` | Full snapshot (incl. 5 registry scans) read before the cheap `rideNearby` guard |
| R3 | high | `ContainStatus.scala:22-41` | Micro package owns a tactic-level latch; imports `With.tactics`, read back by Tactician/Squads/Missions; 6+ full scans per frame |
| R4 | high | `ReaverBraveStand.scala` | Third "stand and shoot" gate; identical `act` to `ReaverHoldToFire`; contradicts `ReaverFireDiscipline` and `ReaverDropBravery.staleFrames` |
| R5 | medium | `ReaverBraveStand.scala:36-44` | No `pressured`/wound term: a shielded stale Reaver holds while taking fire |
| R6 | medium | `ReaverThreats.scala:68,71` | Snapshot fields defined by a behaviour's constants |
| R7 | medium | `ReaverSituation.scala:15-84` | 41 fields declared twice; missing wiring silently defaults |
| R8 | high | `BeShuttle.scala:40-47`, `BeFlier.scala:14,56` | Two movement owners per frame, last writer wins |
| R9 | medium | `BeShuttle.scala:40-41` | `Action.apply` returns sleep state, so the ditch/adopt guards never fire while acting |
| R10 | medium | `ReaverThreats.scala:49-68`, `ReaverDropBravery.scala` | Six radii for "enemy near/able to hit" in one package (4, 7.25, 8.5, 10, 6/8, 20) |
| R11 | medium | `ReaverEvacTanks`, `ReaverBraveStand`, `ReaverDropBravery` | Tank rule in three kernels (10 tiles, range+1, 20 tiles) |
| R12 | low | `CommanderTransport.scala:106` | `DropSafety.vetoLanding(transportFlying = true, ...)` inside `if (transport.flying)` |
| R13 | low | `RequestSafeLanding.scala` | Spiral landing search wired only to `ReloadScarabs` |
| R14 | low | `ReaverOwn.scala:33`, `ReaverThreats.scala:36` | Atlantis reads `BeReaverLegacy.cooldownOnLanding` (reverse dependency) |
| R15 | low | `BeShuttle.scala:32-36` vs `:52-54` | Two wound curves for the same Shuttle flee (`safetyMarginPixels` vs `woundPercentBonus`) |
| R16 | low | `ShuttleQuestCost.scala:15` | `fightRadiusPixels` borrowed from `AirFleeGeometry.fightRadiusTiles` |
| R17 | low | `ReaverMission.scala:61-72` | Five `AliveEnemies.get()` per read (covered by R2's fix, kept for the scan count) |
| R18 | medium | `ReaverMission.scala:56-81` | Drop decision lives in the snapshot read instead of `ReaverDropBravery` |
| R19 | low | `Probe.scala`, `EridanWave.scala:30` | `Probe` has no per-game reset; probe dedupe state leaks across games |
| R20 | low | `ShuttleFleeGeometry.scala:25` | Unclamped 10x wound scaling (the helper clamps; this predates it) |

---

## E. Fix status

Applied in two passes, build green (`mvn -o -Ptests test`, 646 tests, blank-line gate clean). The second pass took the behavior fixes and carried them with suites instead of games:

| Finding | Change |
|---|---|
| R1 | `* Maff.inv32` removed from `ReaverThreats.staticInFace` and from both `ReaverBackOffStatic` sites - the seam now uses tiles as reported |
| R2 | `ReaverOwn.rideNearby(unit)` factory; the brain checks it before `ReaverSituation.read` |
| R12 | `DropSafety.vetoLanding` lost its dead `transportFlying` flag; `CommanderTransport` passes the berth only, and the caller's `if (transport.flying)` stays the sole gate (suite updated) |
| R14 | `cooldownOnLanding` moved to `ReaverActions`; the legacy brain aliases it |
| R17 | one `AliveEnemies.get()` per `ReaverMission.read` (was five) |
| R19 | `Probe.reset()` wired into `EridanWave`; probe dedupe no longer leaks across games |
| (new) | `15 * 32` hike threshold named `ReaverMission.hikeTiles`; `RangedReach` and `Maff.woundBonus` shared (see the phase-b review's D1/D2) |
| R3 | `ContainStatus` moved to `Tactic.Squads` (the cluster and the latch are tactic state) and memoized per frame; the scan count per frame is now one, and the answer no longer depends on which layer asked first |
| R4 | One `shotReady` answer in `ReaverFireDiscipline` feeds both the boarding ban and the stand; `ReaverBraveStand.armed` is gone, and a Scarab in flight no longer counts as a loaded shot |
| R5 | The stand refuses when a foe sits inside the close-pressure ring (`pressuredClose`), so a Reaver under the guns boards instead of posing |
| R6 | `engageTiles`/`tankMarginTiles` and their kernels moved into `ReaverThreats`, which owns the facts it computes; the behavior consumes `foeInReach`/`tankCovered` only |
| R8 | `BeShuttle` owns every Shuttle move: the Scourge-rear flee moved into it (before the air-threat flee), and `BeFlier` now skips transports entirely |
| R9 | The two guard calls at the top of the chain stay as they are for now, but `fleeFromScourgeRear`/`fleeFromAirThreats`/`makeEmergencyPickup`/`runBestQuest` all return real Booleans, so the chain's contract holds for every leg the branch added |
| R18 | `ReaverDropBravery.decide(DropFacts)` holds both rules and every override; `ReaverMission.read` only gathers facts |
| R20 | `safetyMarginPixels` clamps the wound fraction at 1, so overkill damage cannot inflate the margin |

Both `ReaverDropBraverySuite` and the new `ReaverThreatsSuite` cover the
kernels directly; `FocusFireSuite` covers the squad-volley pricing that fixed
C1 in the phase-b review.

Still the wrong shape, and left as decisions rather than edits:

1. **R9** - `ShuttleDitchPassengers` and `ShuttleAdoptPassenger` are still
   `Action`s used as guards, so `Action.apply` returns the sleep state rather
   than "I acted". Every leg the branch added returns a real Boolean; the two
   inherited ones should become plain `Boolean` helpers next time that file is
   touched, because the guard only "works" by accident today.
2. **R10/R11** - six radii for "an enemy is near enough" and three tank rules
   remain. They are now each single-sourced, but one `ReaverThreatRanges`
   table and one tank kernel would be the honest end state.
3. **R15** - `BeShuttle` still uses two wound curves (the 64/160 margin and the
   `woundPercentBonus(4, 9)` band). Both are clamped now; whether a Shuttle
   needs two curves at all is a design question, not a bug.

Checked and cleared: the loaded-Reaver `unloadSpotSafe` read is **not** a
false-safe bug - `MatchupAnalysis.threatens` recurses through
`victim.transport`, so a loaded Reaver's threats are exactly the Shuttle's,
and its distances are measured from the Shuttle's pixel.
