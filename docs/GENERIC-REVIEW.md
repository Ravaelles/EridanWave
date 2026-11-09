# EridanWave review — structure, cooperation, and split candidates

Prioritized by payoff. Every P0/P1 item below bit us during real runs in this
session (UMS micro maps, Wine/JDK 17); file:line pointers are included.

## P0 — load-bearing issues (fix first)

### 1. `With.onStart` is all-or-nothing: one throw kills the whole bot
`Lifecycle/EridanWave.scala:34-56` wraps the entire frame in one try/catch, and
`With.onStart` has no internal compartmentalization. Measured: a single BWTA
failure on a baseless map left `units`/`geography`/`strategy` unassigned, and
every subsequent frame died in `With.units.onFrame` — a motionless bot that
only auto-retaliates. The same shape applies to `analyzeTerrain`
(`Lifecycle/With.scala:239`).
*Fix (concrete benefit: total-brick → degraded-but-alive):* compartmentalize
`With.onStart` into independently guarded phases (config → units → geography →
strategy), each logging and continuing with documented degraded state, instead
of one 100-line sequential block. The UMS fallback zone
(`GeographyBuilder.buildFallbackZone`) proves degraded operation works.

### 2. A per-frame exception anywhere kills the rest of the frame
`TaskQueue*` has no per-task exception handling; one throw aborts everything
after it in `EridanWave.onFrame` (Micro never runs → units freeze). Measured:
a single bad `ourMain` call site froze combat for 300+ frames while the log
flooded. UMS exposed this because half the codebase assumes bases exist.
*Fix:* wrap each top-level task body in try/catch that logs once per game
(not per frame — see item 6) and continues with the next task. Cost: ~10 lines
in `TimedTask.run`/`TaskQueueSerial.onRun`. Benefit: one bad subsystem can no
longer wedge combat, and the log points at the culprit instead of repeating it.

### 3. `ourMain/ourNatural/ourFoyer` throw on empty `bases`
`Information/Geography/GeographyCache.scala:22-35` (`Maff.maxBy`/`minBy` over
an empty seq). ~50 call sites assume a base game (Retreat, scouting, strategy
legality, placement, visualization, loggers). We guarded the ones that fire on
UMS (`Fingerprint`, `Tactician` defense, `Placement.initialize`,
`Storyteller`, `strategy.update`, `FindWall`, `WriteMapCSV`, `NoPlan` gate).
*Fix (structural, kills the class):* make the three accessors total. Options
in increasing order of invasiveness: (a) return a synthetic "nowhere" base
Cheap and local, but pollutes semantics; (b) change return type to
`Option[Base]` and fix call sites (~50, mechanical, compiler-guided — the
honest fix); (c) split `Geography` into `MeleeGeography` (bases required) vs a
baseless variant selected at startup. (b) is recommended: the compiler then
finds every assumption instead of the next UMS-shaped map finding it at
runtime.

### 4. `With` god-object + init-order fragility
`Lifecycle/With.scala:42-116` holds ~45 mutable subsystems; `onStart:127-234`
constructs them in a hand-maintained order with `null` defaults. Consequences
observed this session: `units`/`geography` null after a partial init;
`mapFileName` read before assignment risks; `strategy`/`tactics` usable only
after an invisible ordering contract.
*Fix (concrete benefit: impossible states become unrepresentable):* group
fields into phase-bundles (`PreGame`, `PerGame`, `PerFrame`) constructed
atomically, or at minimum assert-and-fail-fast with a clear message at each
phase boundary instead of NPE-ing 300 frames later. Do NOT keep adding
`!= null` guards per call site — that's how the current 8-spot patchwork
happened, and the next map shape will find spot #9.

## P1 — mega-classes worth splitting (concrete payoff each)

### 5. `Micro/Agency/Commander.scala` (727 lines) — the sole BWAPI boundary
Everything funnels through one object: attack/move/patrol/gather/build/tech/
sleep/push/escalation/anti-stick. It is the highest-risk file to touch and the
hardest to unit-test (live `bwapiUnit` calls inline with pure math).
*Split:* `CommanderOrders` (pure BWAPI calls), `CommanderMovement`
(adjustDestination/waypoints/push), `CommanderSleep` (AttackDelay/turn logic).
Benefit: the movement math becomes testable without a game; merge conflicts
on the hottest file drop to near zero.

### 6. `ProxyBwapi/UnitInfo/UnitInfo.scala` (627) + `UnitClass.scala` (802)
The unit snapshot mixes caching, geometry, combat math, targeting helpers,
simulation state and logging. `UnitClass` mixes raw JBWAPI proxies, derived
geometry, macro economics (`subjectiveValue/skimulationValue/budgets`) and
spell lists.
*Split:* `UnitInfo` → `UnitState` (cached BWAPI snapshot), `UnitCombat`
(range/cooldown/entanglement/fire solutions), `UnitIntel`
(visibility/presumed state). `UnitClass` → `UnitStats` (raw numbers),
`UnitEconomics` (costs/values/budgets), `UnitTags` (predicates). Benefit:
smaller recompilation surface (these files are included nearly everywhere)
and reviewable diffs.

### 7. `Micro/Actions/Combat/Decisionmaking/Combat.scala` (482)
A state machine (technique selection + transitions) sharing a file with ~10
mini-behaviors (`walk/aim/dodge/potshot/charge/chase/abuse/superkite/engage`).
*Split:* `CombatDecide` (technique selection only) vs `CombatBehaviors`
(one file or one small object per behavior, mirroring the existing
`Maneuvering/` and `Tactics/` split). Benefit: adding/changing one behavior
no longer risks the transition table, and each behavior becomes individually
testable against recorded micro scenarios.

### 8. `Placement/Walls/WallFinder.scala` (593)
Recursive search + scoring + filler placement + hallway validation + metrics
in one class, with magic budgets (45s/58s deadlines, 64/256/256 candidate
caps) baked in.
*Split:* `WallSearch` (candidate generation), `WallScoring`, `WallFillers`
(already partially separate), `WallValidator` (hallway/walkway checks).
Benefit: the filler-power rule change that just landed touched scoring,
validation and config in one diff — separated, each gets its own test
(`WallFillerPowerTest` pattern already exists to copy).

### 9. `Gameplans/Protoss/PvP/PvPOpening.scala` (968), `PvTOpeners.scala` (506)
Single files holding entire matchup playbooks (openers, transitions,
reactions, wall/filler calls). Hard to review, impossible to A/B cleanly.
*Split:* one file per opener family (`PvPGateCore.scala`, `PvP1012.scala`,
…) sharing a small `PvPConditions` helper for the fingerprint gates.
Benefit: playbook PRs stop being 968-line diffs; dead openers become deletable
files instead of commented branches.

### 10. `Tactic/Tactician.scala` + `runCoreTactics` (~230 lines, dense)
Squad assignment, defense-worker pulling, Terran SCV logic and mode switches
interleaved in one method with `Maff.orElse(...).getOrElse` chains that throw
on empty input (measured). *Split:* `DefenseAssigner`, `AttackAssigner`,
`WorkerPuller` behind the existing `Assignment` helpers. Benefit: the UMS
guards added this session (`bases.nonEmpty` ×3) collapse into one.

## P2 — cooperation between layers

### 11. Getters that can throw are used as if total
`Maff.minBy/maxBy(...).get(...)`, `.head`, `.reduce` on game state appear in
hot paths (`Retreat`, `Gathering`, `BattleProcessDivisions`, `Intrigue`,
`Placement`). Two this session threw every frame. *Fix:* a lint rule or a
review checklist — "no throwing accessors in per-frame code without an
emptiness proof" — plus preferring `Maff.minBy(...).getOrElse` with an explicit
fallback over `.get`.
*Counter-example already in tree:* `Maff.orElse(a, b)` is strict (both args
evaluated), so `orElse(ourBases, Seq(ourMain))` still throws. Either make it
by-name or stop using it as a safety net.

### 12. NoPlan / umsMode gating is scattered
UMS handling is now spread over `UmsMode`, `Strategist.gameplan`,
`Fingerprint.update`, `Tactician` (2 spots), `FindWall`,
`GeographyBuilder`, `WriteMapCSV`, `Storyteller`, `Placement.initialize`.
Each is a one-line `if (umsMode) ...`, which is fine today but will rot: the
next contributor won't know the list.
*Fix:* a single `GameMode` object (`Melee | UMS | ...`) consulted through one
predicate, e.g. `With.gameMode.requiresBases: Boolean`, with the full
call-site list in one place (or one test asserting the list). When the second
special mode arrives (FFA? Archon? tests?), it slots in without archaeology.

### 13. Config flows through stringly-typed JSON + reflection-by-convention
`ConfigurationLoader` matches files by substring (`"Eridan"`, bot name) and
newest-mtime wins; `getOrDefault` silently keeps defaults on type mismatch.
Measured cost this session: none (it worked), but near-miss: a missing
`bwapi-data/AI/` dir used to NPE the whole start (`listFiles` null — now
guarded). *Fix:* validate the loaded config once at startup (unknown keys →
warn; missing dir → info, not exception) and log the effective source file
(it does) plus a one-line effective-mode summary. Cheap, prevents a whole
class of "why is it playing the wrong strategy" mysteries.

### 14. Logging is buffered-to-file with no live tail contract
`With.logger` buffers everything and flushes one file at game end; the only
live channel is `System.out`/`stderr` tees. Debugging a live game means
waiting for it to end. The `System.err` one-liner added to `tryCatch` proved
its worth immediately (it located the BWTA abort in one run).
*Fix:* keep a small ring buffer of the last N errors/warnings and dump it on
request (chat command or hotkey), and always echo `ERROR` lines to stderr,
not just exceptions.

## P3 — hygiene (small, safe)

* **CRLF discipline:** repo standard is CRLF; `.sh` files must stay LF.
  Add a `.gitattributes` (`*.sh text eol=lf`, `*.scala text eol=crlf`) so
  editors don't silently break scripts. (Discovered the hard way: a CRLF
  `.sh` would have failed parsing.)
* **`--add-opens` is JDK-version-specific:** `run-wine.sh` passes it to a
  Java 8 Wine JVM that rejects unknown flags (measured: instant client death).
  `EridanWaveLocalMetal` + docs should pin "Java 8 under Wine, no JPMS flags;
  add-opens only for JDK 16+ hosts".
* **Tests are outside Maven** (`tests/` as `App` objects + `scripts/test_*.ps1`).
  Good coverage exists (placement reservation round-trips, wall filler power,
  micro spot checks) but nothing runs them in CI and several suites require a
  game env. *Fix:* wire the pure ones (config parsing, BwapiIni patching,
  marker lists, `NoPlan` gating) into `mvn test` via scala-test or plain JUnit —
  the UMS work this session would have been caught by 3 assertions without a
  single Wine boot.
* **Duplicated knowledge:** the UMS map-marker list now lives in both
  `BwapiIni.java` and `wine-run.sh` (with a "keep in sync" comment). Acceptable
  for two, but a third copy should trigger generating one from the other.
* **Dead weight to verify:** `JBWEBWrapper` is disabled (`enabled=false`) but
  still initialized and logged; `3rdparty/apache` jars duplicate Maven Central
  deps (offline-build reason — document it in one line or drop it).
* **`Main.jvmRuntimeArguments`/JMX flags** are logged but unaudited; fine.

## What NOT to touch (verified working, high blast radius)

* The `Commander` BWAPI call sites themselves (anti-stick timing, sleep math).
* `MacroSim` insertion math (`tryInsertAfter`/`updateStatesFrom`) — covered by
  `MacroMinInsertTest`.
* `Techniques` transition graph and `FightOrFlee` rule order — behavior is
  encoded in the order; reorder only with a recorded micro scenario.
* `WallFinder` budgets/caps — tuned against real ladder maps.
* `StrategySelectionTournament` opponent table — data, not logic.
