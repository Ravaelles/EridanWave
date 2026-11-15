# Code review: `src/Micro`

Scope: the whole folder, 250 sources / 16 693 lines, reviewed 2026-11-15 on
`develop`. Brief from the owner: layer communication, SRP, duplication,
concepts to extract or merge, readability, OCP, short classes and methods.

Method: every file in the folder was read (the seven `Agency/Commander*`
files, `Formation/WallGaps`, `Targeting/Target` and `Formation/FormationStandard`
only in part by size, the rest whole). Counts below come from `grep` over
`src/Micro` on the reviewed tree. No build or suite run: this review changes
no code.

Legend: **L** = layer seam, **S** = SRP / size, **D** = duplication or a
concept that belongs somewhere else, **O** = OCP / extension point,
**R** = readability and hygiene. Severity: high / medium / low.

Baseline worth stating up front, because it is unusual for a folder this size:
the "pure kernel + game seam" discipline is real and consistently applied, and
it is the single biggest asset here (`ThreatPriority`, `FocusFire`,
`FormationHold`, `FormationApproach`, `KiteAway`, `HoldToShoot`, `ArrivalFlow`,
`OrderContinue`, `FerryFetch`, `DropSafety`, `WallBreach`, ...). Almost every
finding below is about the seams *between* those kernels, not about the
kernels themselves.

---

## 1. Layer communication

### L1 (medium) `intend()` is last-writer-wins and nothing says so

`src/ProxyBwapi/UnitInfo/FriendlyUnitInfo.scala:90`

```scala
def intend(intendingClient: Any): Intention = {
  val newIntent = ?(intent.frameCreated >= With.frame, intent, new Intention)
  _client = intendingClient
  _intent = newIntent
  ...
}
```

Whoever calls `intend` last in a frame owns the unit's whole intention set
(destination, station, targets, `canFight`, action). There are 87 call sites
across the tree: 24 in `Tactic/Missions`, 23 in `Tactic/Tactics`, 19 in
`Tactic/Squads`, 10 in `Micro/Corsair`, 7 in `Tactic/Production`, 3 in
`Macro/Gathering`. The behaviour of a unit therefore depends on the *order* in
which squad, mission, production and micro run - an ordering contract that
exists only implicitly in the `Tactician` call sequence. Two owners in one
frame do not conflict loudly; the later one silently wins.

Two concrete consequences already visible in `Micro`:

- `Micro/Corsair/*` is the only package in `Micro` that authors intentions
  (`unit.intend(squad).setTerminus(...)` in 8 behaviours + `CorsairBrain`).
  Corsair doctrine is therefore a *Tactic* concept living in `Micro`, and it
  competes with the squad's own activity on the same field.
- `Agent.execute` reads `unit.intent.toAttack` / `toGather` / `toBoard` /
  `toNuke` before dispatching, so an intention written by a squad *after* the
  agent cycle is one frame stale - and the frame on which that changes hands is
  invisible.

Proposal: make ownership explicit. `intend` should either accept a second
owner by recording it (a conflict is then visible in the probe log per unit,
which the `CherryVis.noteOwner` call already half does) or refuse it
(`FrameOwnership`-style check with an explicit `seize` for the mission layer).
The cheapest useful step is a documented phase model: squads/missions author
intentions, micro may only *narrow* them (never widen a destination), and
`Micro/Corsair` moves under `Tactic` so "`Micro` never authors intent" becomes
a checkable invariant rather than a habit.

### L2 (medium) Two competing ways to override a destination

`DestinationStack` models precedence with named levels (`Home` -2, `Redoubt`
-1, `Terminus` 0, `Perch` 1, `Station` 2, `Forced` 3, `Decision` 4) and
`destinationFinal` / `destinationNext` pick by level. On top of that, 61
direct writes across 24 files bypass the model entirely:

```scala
unit.agent.decision.set(...)        // 24 files
unit.agent.destinationNext.set(...) // overrides the squad's terminus outright
unit.agent.forced.set(...)
```

Examples of the second kind, where a unit doctrine simply replaces the level-0
destination: `BeReaver.backOffOutrangedStatic/closeStatic/duelWalk`,
`BeShuttle` (nine sites), `CorsairHunt`, `CorsairHuntMutas`,
`CorsairHuntKnownOverlords`, `StaticDefenseRepel`, `BeRushingZealot`,
`BeDragoonVsSlowlot`. Meanwhile `CommanderMove.move` reads
`destinationNext`, `Destination.pixel` prefers `Decision`/`Forced`, and
`Agent.execute` rebuilds `terminus` from the intention every frame. So a
doctrine's write survives only until the next `resetDestinations`, and the
reader has to know which accessor wins.

Proposal: `DestinationStack.destinationNext` becomes read-only outside
`Agency`; doctrines write `Forced` (they are *forced* by local facts) via a
named method (`forceTo(p)`, `forceAway(threat, tiles)`), and `Decision` stays
the single "final say" level that `Commander` reads. That turns the level
numbers back into the contract they were designed to be, and it makes "who
moved this unit where" answerable from the toString that already exists.

### L3 (medium) `Agent` is a mutable blackboard shared by five layers

`src/Micro/Agency/Agent.scala`

81 direct field writes from 24+ files land on `Agent`: `decision`, `forces`,
`shouldFight`, `fightReason`, `commit`, `rideGoal`, `priority`, `spellIntent`,
`toAttack`, `blackboard`. `shouldFight` alone has 18 writers (FightRules,
`CombatTechniques.syncConsensus`, `CombatManeuvers.walk/dodge`,
`BeZealotVsZealot`, `Cast`, `Fight`, `Gather`, ...). `Agent.execute` resets
half of them at the top of the frame and lets the chain fill the rest; the
precedence between two writers on one frame is again "last one wins".

This is also where the class size comes from (S2): history, decisions,
diagnostics, pushing caches, ridesharing, blackboard, perch, plus the sorting
order trait.

Proposal, in two cuts:

1. Split by *direction*. `AgentInputs` (written once per frame by squad /
   mission: terminus, station, targets, board, canFight) / `AgentOutputs`
   (written only by the acting chain: destination, action, orders) /
   read-only queries (the `Cache`/`KeyedCache` push fields, leader, perch).
   Phase 2 of `execute` then becomes an explicit "seal inputs" step.
2. Make `shouldFight` a single method that also records the reason
   (`decideFight(value, reason)`); `fightReason` already exists for exactly
   that, so the field pair just needs one writer instead of eighteen.

### L4 (low-medium) Unit code branches on squad *classes*

21 references to `Tactic.*` types in 13 files under `Micro`. Most are benign
(`UnitGroup` reads), but three are type tests that will rot with every new
squad:

- `BeShuttle.adopt`: `shuttle.squad.exists(s => s.isInstanceOf[SquadAttack] || s.isInstanceOf[SquadDefendBase])` decides whether to fetch newborn Reavers.
- `CommanderTransport.rightClick`: pattern-matches `MissionDrop` to call `abandonUnsafePickups`.
- `Transport.perform`: `collect { case mission: MissionDrop => mission }`.

Architecture §5 puts unit behaviour *below* squad activity. A unit asking
"what kind of squad is above me" inverts that. Proposal: publish the facts as
squad queries (`squad.wantsNewbornReavers`, `squad.allowsPickup(transport,
passenger)`), so a new squad type satisfies a contract instead of adding an
`isInstanceOf` case at the bottom of the stack.

---

## 2. SRP: classes and methods that do more than one thing

### S1 (high) `Formation/WallGaps.scala`, 644 lines, one object

Six nested classes (`Body`, `Box`, `Gap`, `Navigation`, `Slot`, `Planned`), a
BFS grid with resize/route caching, a slot-candidate search, a reinforcement
flood fill, a min-defender fixed point (`countMinimumDefenders` loops until the
whole wall seals) and the public `assign` seam - all in `DefenseWallGaps`
object. Nothing here can be exercised except through the whole allocator.

Proposal: four files, mirroring the four questions the code already asks:

- `WallGapGeometry` - extents, boxes, `fits`, `forbidden` (the pure kernel).
- `WallGapNavigation` - grid BFS, `reachable`, `route`, `waypoint`.
- `WallGapSlots` - candidates + exposure + reinforcement candidates.
- `WallGapAllocator` - `allocate`, `assign`, `minimumDefenders`.

`WallGapGeometry` and `WallGapNavigation` are pure and would get suites
immediately (the repo's `testkit.TestKit` seeds mock units and map dims, which
is exactly what they need).

### S2 (medium-high) `Agent.scala`, 232 lines, seven responsibilities

Named collaborators hiding inside the class, each with its own reason to
change:

- ridesharing (`passengers`, `hailers`, `ride`, `addPassenger`, `removePassenger`, `_passengers.synchronized`),
- traffic (`receivedPushForces`/`receivedPushForce`/`receivedPushPriority` + `escalatePriority`),
- leading (`leader`, `leadFollower`, `isLeader`),
- diagnostics (`actions`, `lastAction`, `chainActions`, `flushDecisionChain`),
- blackboard (`put`/`get`/`contains`/`remove`).

`Ridesharing` and `Trafficing` are self-contained and worth extracting on their
own; doing so also removes the only `synchronized` block from the god object
and shrinks `Agent` to what it is: per-frame scratch for one unit.

### S3 (medium) `Combat.scala` mixes situation, lifecycle and doctrine table

174 lines: ~30 one-line decision accessors, the `Action` lifecycle
(`perform`/`innerPerform`), target acquisition, wall-breach targeting, the
technique dispatch ladder, and the flight tracking.

- The ~30 accessors are a legitimate "situation" object - but they read the
  world (squad, matchups, formation, yolo), so they are a *seam*, not a view.
  Extracting `CombatSituation` would let the behaviours take facts instead of
  `combat: Combat` (architecture §4, second bullet), which is the pattern
  `ReaverSituation` already uses.
- `acquireTarget`'s wall-breach block (lines 91-105) is a claims-style
  behaviour with a pure kernel (`WallBreach`) and no business inside the
  generic pipeline; it belongs next to `Target` as `WallBreachTarget`.
- `CombatTechniques.applyTransitions` is a 10-row table of *unit* facts
  (Wraith/Mutalisk → SuperKite, Dragoon/Goliath/Reaver → Fallback,
  Archon/Zealot vs Speedling → Fallback) sitting in the shared pipeline.
  Architecture §5: "vocabulary and dispatch, never unit specifics". Each row
  belongs in the unit's own brain as a technique claim.

### S4 (medium) `CommanderEngage.attack`, ~70 lines, eight sequential special cases

`src/Micro/Agency/CommanderEngage.scala:31-196` (~80 logic lines). Reaver push, airlifted
unload-with-scarab-path, Lurker burrow/move, Interceptor target, Carrier
target, closing-in attack-move, the `useAttackCommand` OR-chain, then the
`shouldOrder` cluster. Every new unit quirk lands here, which makes it the
most-edited method in `Agency` and the least readable.

`BeReaver` already demonstrates the fix in this same folder:
`groundClaims: Seq[GroundContext => Boolean]`, first claim wins, each claim
one small named method with a contract comment. Apply that shape here
(`airliftUnload`, `lurkerSiege`, `carrierChase`, `interceptorTarget`,
`closeIn`, `issueAttack`) and the method becomes a list.

### S5 (medium) `Matchups/MatchupAnalysis.scala` - snapshot, derived scalars and long predicates in one case class

131 lines, ~20 `Cache` fields, read by every layer. The shape is right for a
snapshot; the problem is that three members are 10-line inline predicates with
player-relative branches:

- `_threateningMiners` (metro + worker + resource + base + two distances),
- `_wantsToVolley` (four-way flatMap with three comment blocks justifying the DPS algebra),
- `targetsInRange` (visibility + range + ground minimum range).

And `threatens(shooter, victim)` recurses through transports and switches on
`?(shooter == me, groupOf, groupVs)` - it means different things depending on
who asks, so no suite can pin it.

Proposal: keep the snapshot, move each predicate to a named kernel object
(`VolleyDiscipline.wants(...)`, `MinerThreat.threatened(...)`) and pass the
side in as a flag instead of `shooter == me`. Those three are the only members
whose correctness is argued in comments rather than by structure.

### S6 (low) `Actions/Action.act` owns tracing, timing and dispatch

`Action.act` gates readiness, asks `allowed`, copies `lastAction` for credit,
appends to the trace, calls `perform`, restores credit, times the call and
returns `unit.unready`. Timing is unrelated to deciding an action and already
has a home (`Agency.actionPerformance`); the pre/post credit dance is
tracing. A small `ActionTiming` collaborator (or moving the nanos to `Agency`,
which owns the map) would leave `Action` with one reason to change.

---

## 3. Duplication and merges

### D1 (high) "Shoot the nearest hittable unless already on it", copied four times

Identical shape in:

- `Decisioning/Combat/Behaviors/ContainFireBehavior.scala:20-32`
- `Decisioning/Combat/Behaviors/IdleEngageBehavior.scala:24-35`
- `Decisioning/Combat/Behaviors/CrucialAirSnipeBehavior.scala:40-52`
- `Decisioning/Combat/Behaviors/SuicideEngageBehavior.scala:19-30`

```scala
val target = Maff.minBy(hittable)(unit.pixelDistanceEdge(_)).get
if ( ! unit.orderTarget.contains(target)) {
  unit.agent.act("<label>")
  Commander.attack(unit, target)
}
true
```

Extract `NearestShot.take(unit, label, candidates, rankBy)` next to the other
`*Behavior` shared pieces (`AttackIfReadyBehavior` is already that kind of
helper). Four files lose their boilerplate, and a fifth behaviour that shoots
the nearest thing costs one line.

### D2 (high) Spell value maths duplicated across five spells

`PsionicStorm.valueTarget` and `Maelstrom.valueTarget` build the same five
multipliers (`confidence`, `player`, `unitClass.stormValue/maelstromValue`,
`danger` from health ratio, `rich` from team battery count) with the same
comment-worthy algebra. `Stasis` and `DisruptionWeb` re-derive the same
`min(1.0, targets.size / 3.0)` and player-sign factors. `Stasis`,
`Maelstrom`, `PsionicStorm` and `DisruptionWeb` also repeat the victim veto
ladder (`stasised` / `invincible` / `underStorm` / `Larva` / `Egg` /
`Interceptor`), part of which overlaps `SpellTargets.legal`.

Proposal, two pure kernels both suite-able:

- `SpellValue.of(target, caster, unitValue, playerSign, dangerScale, batteryScale)` - one home for the multiplier chain, with per-spell knobs.
- `SpellVetoes.invalidVictim(target, caster)` - the shared rejection ladder, merged *into* `SpellTargets.legal` where it already belongs.

`.ai/queue.md` already lists "Yamato/Lockdown-style value sharing for
Maelstrom/Storm/Disruption/Stasis" as the next merge; this is the same work
from the other end.

### D3 (medium-high) Cloak policy is four files and one silent bug

`Spells/Terran/GhostCloak`, `GhostUncloak`, `WraithCloak`, `WraithUncloak` are
the same `Action`: `allowed` = unit class + cloak state + energy reserve +
`enemyDetection`/frame-since-last-cloak gate, `perform` = `Commander.cloak` or
`decloak`. Four files, four copies of the gate, differing only in constants.

Worse, `WraithUncloak.perform` passes the wrong tech:

```scala
Commander.decloak(unit, Terran.GhostCloak)   // WraithUncloak.scala:29
```

`CommanderAbilities.decloak(unit, tech)` ignores `tech` entirely (it calls
`unit.bwapiUnit.decloak()`), so the bug is invisible today and becomes real
the moment the parameter is used. `Commander.cloak` *does* use its tech, so
the asymmetry is also a trap for the next reader.

Proposal: one `CloakPolicy` table (unit class → tech, energy reserve, cloak
and uncloak predicates) plus one `Action`, and either drop the unused `tech`
parameter from `Commander.decloak` or make it select the tech. `.ai/queue.md`
had this merge parked as "needs a game"; the four-way duplication itself does
not - only the tuning does.

### D4 (medium) Readiness/unburrow/unload ladder, 22 times

`grep -c "unit.unready) return"` in `Agency`: `CommanderAbilities` 8,
`CommanderMove` 5, `CommanderWork` 5, `CommanderCore` 2, `CommanderEngage` 1,
`CommanderTransport` 1. Each command repeats the same three-step prologue in
slightly different order, and the differences are load-bearing: `CommanderWork.build`
deliberately does *not* call `autoUnload` ("We have a separate process for
building Scarabs in loaded Reavers"), `CommanderMove.move` adjusts the
destination before checking readiness, `useTechOnUnit`/`useTechOnPixel` set a
ride goal first.

Proposal: `CommanderCore.ready(unit, unload = true)(body)` - readiness,
`autoUnburrow`, optional `autoUnload`, in the canonical order, with the
`build` case passing `unload = false` and a comment. That is ~60 lines less
and, more importantly, one place where the intended order can be argued.

### D5 (medium) Fleet flee is split three ways

`Maneuvering/AirFlee` documents itself as "one home for the flee rule so air
doctrines cannot drift apart", and then:

- `Micro/Corsair/CorsairRangedFlee` calls `AirFlee.fleePixel` with its own trigger and corner width (fine - parameterised),
- `Micro/Actions/Protoss/ReaverShuttle/BeShuttle.flee` re-implements the *scoring* half: per-threat penetration, `RangedReach.isRanged`, latch on worst penetration, focus pixel, corner guard - 40 lines next to the rule it duplicates,
- `ReaverShuttleFlight` owns the margins/latch, so the numbers live in two files for the same hull.

Proposal: `AirFlee` takes a `threats: Seq[(Pixel, Double)]` (pixel + score)
and the latch, so the Shuttle passes its penetration scoring and the Corsair
passes plain distance. The scoring kernel stays in `ReaverShuttleFlight`
(pure, already tested); only the *flee mechanism* collapses to one place.

### D6 (low-medium) Waypoint triple, twice

`Retreat.getRetreat` and `BeDragoonVsSlowlot.retreat` both write:

```scala
waypointSimple.orElse(waypointPath).orElse(waypointForces)
```

with the same `getWaypointInDirection` / `getWaypointAlongTilePath` /
`requireSafety` calls. `Micro.coordination.Pathing.MicroPathing` is the right
home: `bestWaypoint(unit, force, preferHome, requireSafety)`.

### D7 (low) Wound-scaled margin, open-coded nine times

`unit.woundPercentBonus(lowTiles, highTiles) * 32` (or
`Maff.woundBonus(...) * Maff.inv32`) appears in `BeCarrier`, `BeArbiter`,
`BeShuttle` (twice), `KiteAwayBehavior`, `Spot`, `CorsairSafety`,
`ReaverShuttleStaticAttack`, `DropSafety`-adjacent code. A `WoundMargin(low,
high)` value object with `tiles(wound01)` / `pixels(wound01)` makes each site
one line and puts the tile/pixel conversion at one seam (conventions rule 12).

---

## 4. OCP: extension points that require editing code elsewhere

### O1 (medium-high) Adding an action means editing four lists, one of them by display name

- `Actions/Idle.scala:33` - `private val actions = Array[Action](...)`, 26 entries, order is the priority.
- `Actions/Combat/Fight.scala` - 23 sequential calls (grandfathered by architecture §7, still the main offender).
- `Actions/Combat/Decisioning/Actions/Cast.scala:27` - 16 spells.
- `Agency/Agent.scala:64` - `chainActions: Set[String]`, 64 **display-name** strings, used only to keep container names out of the decision trace.

The first three are documented as run-everything lists. The fourth is a new
kind of problem: it is keyed by `Action.name` (derived from the class name), so
renaming an action silently changes trace output, and adding a container means
editing a file in a different package. `Action.act` already knows the fact
(`delegate` vs `apply` is exactly "container vs leaf"). Proposal: put it on
`Action` (`def isContainer: Boolean = false`) or derive it from the chain
depth the tracer already sees, and delete `chainActions`.

### O2 (medium) New spell = new file + two edits that duplicate its own declarations

`Cast` needs the spell in `spells`, and `perform` re-states the caster set:

```scala
if (unit.ready && unit.isAny(Terran.ScienceVessel, Protoss.Arbiter, ...)) {
  unit.agent.shouldFight &&= unit.unitClass.spells.exists(s => s() && unit.energy >= s.energyCost - 2)
}
```

Both duplicate knowledge `TargetedSpell` already has (`casterClass`, `canCast`,
`hasEnoughEnergy`). Proposal: build `Cast.spells` from the spell objects and
replace the caster gate with `spells.exists(_.canCast(unit))`; then a new spell
needs zero edits outside its own file, which is the OCP bar this folder
otherwise meets.

### O3 (low) `TrafficPriorities` numbers come from a mutable counter

`Coordination/Pushing/TrafficPriorities.scala:7`:

```scala
private var nextValue: Int = 0
private def next: Int = { nextValue += 1; nextValue - 1 }
```

`TrafficPriority.compare` is by that value, and ladders all over the folder
compare with `>=` / `<` (`Combat`, `CombatTechniques`, `CommanderCore`,
`MicroPathing`, `DragoonSlowlotPolicy`). Inserting a priority anywhere but the
end silently renumbers the whole ladder. Make the values explicit literals -
the declaration order is already the documentation.

### O4 (low) `Destinations.All` order is load-bearing and unasserted

`DestinationStack.at` indexes `destinations(level - destinations.head.level)`,
so `All` must stay sorted by level with no gaps. A new level inserted in the
middle silently shifts every lookup. Build a `Map[DestinationLevel, Destination]`
from `All` (or sort and assert the levels are contiguous at construction).

---

## 5. Readability and hygiene

### R1 (high, mechanical) Seven `Agency` files are double-spaced and LF

Every code line is followed by a blank line, and the files are LF while the
rest of the folder is CRLF:

| file | blank / total |
|---|---|
| `Agency/CommanderWork.scala` | 198 / 358 |
| `Agency/CommanderMove.scala` | 155 / 312 |
| `Agency/CommanderEngage.scala` | 153 / 274 |
| `Agency/CommanderOrder.scala` | 142 / 261 |
| `Agency/CommanderCore.scala` | 112 / 197 |
| `Agency/CommanderAbilities.scala` | 87 / 145 |
| `Agency/CommanderTransport.scala` | 59 / 115 |

This looks like an automated edit that inserted a blank line after every line.
It roughly doubles the apparent length of the most-read files in the folder
(`CommanderEngage.attack` *reads* as 140 lines), and it violates AGENTS.md
("preserve line endings", "no whitespace-only edits"). Fix it as one
mechanical commit, nothing else in it.

### R2 (medium) Dead code parked as `&& false`

Six sites where a feature was disabled by making the condition unreachable,
which forces every reader to re-derive liveness:

- `Combat.scala:47` - `assembling = false && ...` (and `CombatEngagement.dispatchEngage` still tests it).
- `Decisioning/Actions/Root.scala:61` - `duckForCover = false && ...` (still listed in `wantsToRoot`).
- `Actions/Basic/Cancel.scala:45` - `isDecoy = false && ...`.
- `Agency/CommanderWork.scala:166` - `accelerantMineral.isDefined && false`, plus a 12-line unreachable branch.
- `Targeting/FiltersRequired/TargetFilterReaver.scala:26` - `return true` followed by ~18 unreachable lines (the tank-snipe safety rule and `safeToAttack`), which reads as live policy.
- `Coordination/Pushing/Explosions/ExplosionSpiderMineBlast.scala` - `else if (false)` with a TODO.

Either delete or move to a named `Disabled` with the reason and the date, so
the branch shows up in one grep instead of six shapes. `Tickle.repair`
(`Potshots/Tickle.scala:141`, ~20 lines, never called) is the same class of
problem.

### R3 (medium) `AttackDelay.scala` is 189 lines, 10 of them code

The decision is `framesToWaitAfterIssuingAttackOrder` (6 lines) and
`safeToIssueOrder` (6 lines); the rest is Dragoon iScript dumps, a jaj22 chat
log and a spreadsheet link. Valuable archaeology, wrong file: `.ai/` already
has precedent (`reaver-shuttle-baseline.md`). Move the history out, keep the
constants and the rule with a pointer.

15 `TODO`s remain in the folder; three are load-bearing honesty about the
design (`Spot`: "This logic is sloppy and doesn't handle our various use cases
well"; `AttackDelay`: "Need a better answer than this" for Carriers;
`BeCarrier`: the stop-hold plan). Those three deserve entries in
`.ai/queue.md` rather than a `TODO` that never surfaces in the process.

### R4 (low) Per-tech cooldowns live inside the command facade

`CommanderEngage.useTech` special-cases `Terran.Stim` (24-frame guard),
`useTechOnUnit` special-cases the two melds (48 frames), `useTechOnPixel`
special-cases `SpiderMinePlant` (12 frames). That is a `tech → frames` table
expressed as three `if` branches in a 121-logic-line object - new techs edit the
facade. `TechCooldowns.after(tech, unit)` (pure, suite-able) collapses it.

### R5 (low) Two vocabularies for "behaviour"

`Decisioning/Combat/Behaviors/*Behavior` and `Actions/{Protoss,Terran,Zerg}/Be*`
both name unit-level decision units, and `Micro/Corsair/*` uses `*Brain` for
the third variant. The difference is real (`Be*` are `Action`s dispatched from
`Fight.perform`; `*Behavior` are pure-ish helpers called by
`CombatEngagement`; `CorsairBrain` is a first-claim chain over a whole squad),
but nothing in the names or doc comments says so. One line in
`architecture.md` §5, or renaming `Be*` → `*Doctrine`, would stop the next
contributor from picking the wrong one.

---

## 6. What the folder does well (do not regress)

- The pure-kernel + seam split is consistent and documented per file, with the numbers and the reason next to them. It is why this folder can be reviewed at all.
- `BeReaver.groundClaims: Seq[GroundContext => Boolean]` is the OCP shape the rest of the folder should copy: a new claim is a new `private def`, zero edits to the chain.
- `Target` is the success story of the "merge duplicated branches into one atom" direction: `firstInSources` + `bestInStratum` + `TargetFilter`/`TargetOverrideFilter` turned eight near-identical blocks into data, and the doctrine comments now read as the ladder's documentation.
- `Skirmish/FightFacts` + `FightRules` is the cleanest testability seam in the folder: facts gathered once, priority pinned by a suite.
- `NoRetreat` states its own SRP/OCP contract in the header and keeps it.
- The `Commander` facade + role delegates + `CommanderCore` lifecycle is the right decomposition; the problems above are inside the delegates, not with the shape.

---

## 7. Suggested order

1. **R1** - restore single spacing and CRLF in the seven `Agency` files. Mechanical, no behaviour; it unblocks reading everything else.
2. **D1 + D4** - the two pure extractions with the best lines-removed-per-risk ratio (`NearestShot`, `CommanderCore.ready`).
3. **L2/L3 seams** - read-only `destinationNext` + `forceTo`/`forceAway`, and one `shouldFight` writer. Then **L1** explicit intent ownership (needs a decision, not just code).
4. **O1/O2** - `isContainer` on `Action`, spell discovery in `Cast`. Both remove "edit elsewhere to add behaviour".
5. **S1 + S4** - `WallGaps` split and `CommanderEngage.attack` claims. Largest single readability win; suite-covered by construction.
6. **D2/D3/D5** - spell value / cloak policy / flee mechanism. These change behaviour subtly at the edges, so they need a suite per merge or a game to prove; `WraithUncloak`'s wrong-tech call should be fixed regardless.
7. **R2** dead-code sweep last, so the deletions do not collide with the merges above.

---

## 8. Not findings (deliberate, leave alone)

- `Fight.perform`'s run-everything list and `Idle.actions` - grandfathered by architecture §7; the sequenced actions rely on it.
- `Target.choose` writing `attacker.agent.toAttack` - documented as the ladder's contract (conventions rule 15).
- Public `var` knobs (`KiteAway`, `HoldToShoot`, `ArrivalFlow`, `FormationApproach`) - deliberate tuning surface, Atlantis style; the doc comments name the strategy that assigns them.
- `Formation/FormationStandard.scala` (350 lines) - it is the geometry engine for every formation and reads as one thing; splitting it would separate the shape math from the shape it makes.
- Duplicated `if (unit.ready)` guards inside behaviours - cheap, local and clearer than a decorator in a chain that must return readiness.
