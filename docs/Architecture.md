# PurpleWave (EridanWave) — Architecture

Tournament StarCraft: Brood War bot in Scala 2.12.18 + JBWAPI. Plays all three
races; strategy is data-driven (strategy tree + fingerprints), micro is a
per-unit state machine fed by battle simulation. This document describes the
code as it is, with `file:line` pointers.

## 1. Runtime entry point and the frame loop

```
Main.main(args)                                   Lifecycle/Main.scala:29
  → GameLauncherFactory.forConfig(...).launch()   Lifecycle/Launch/
  → PurpleBWClient.startGame(config)              Lifecycle/PurpleBWClient.scala:5
      (JBWAPI BWClient: async, frame buffer sized from free RAM, max 30 ms/frame)
  → PurpleEventListener.onStart/onFrame           Lifecycle/PurpleEventListener.scala:13
      → new PurpleWave().onStart/onFrame          Lifecycle/PurpleWave.scala:19,34
```

`PurpleWave.onStart` order matters: `With.onStart()` (builds ~45 subsystems,
`With.scala:127-234`) → history → units → `UmsMode.detectAndApply()` →
geography. `PurpleWave.onFrame` pumps: `performance → With/latency/units →
MapSpecificCommander → UmsMode → gathering → lambdas → tasks.run() →
efficiency → storyteller → WriteMapCSV → camera/visualization`.

All bot logic lives inside `With.tasks.run()` (`Performance/TaskQueue/TaskQueueGlobal.scala:13`).
It is a weighted parallel queue: `Micro=55, Battles=35, Planning=6, Gather=2,
Placement=2` (`TaskQueueGlobalWeights`). Order inside one frame:
`geography → grids → Intel(scouting+sense) → Fingerprinting → yolo → battles
→ Planning serial (accounting … strategy.update → gameplan.update → macroSim …)
→ Micro serial (matchups → agents) → manners`. `TaskQueueSerial` resumes
across frames when the budget runs out; `Planning` may skip up to 6 frames,
`Micro` at most 1.

Two locally added steps run before the queue: `MapSpecificCommander.onFrame`
(per-map dev tweaks, active only with the global keyboard hook) and
`UmsMode.onFrame` (UMS micro maps, see §8).

## 2. `With`: the global state object

`Lifecycle/With.scala:42-116` — one mutable singleton holding the game
(`game/self/enemies`), the frame, and every subsystem
(`strategy, scheduler, macroSim, bank, recruiter, groundskeeper, priorities,
tactics, squads, agents, blackboard, yolo, placement, ...`). Everything reads
everything through `With`, which makes data flow easy to follow but creates
hidden coupling and init-order fragility: if `With.onStart` aborts partway
(e.g. terrain analysis on a baseless map), downstream fields stay `null` and
every frame throws until something guards them (see `.ai/REVIEW.md`).

## 3. Decision layers, top to bottom

```
Strategery (WHAT strategy?)          Strategery/Strategist.scala
  → Planning (WHAT build order?)     Planning/Plans/*, Gameplans/*/*
    → Macro (WHAT do we want?)       Macro/Actions/*, Macro/Scheduling/*, Macro/Allocation/*
      → Tactic (WHO does it?)        Tactic/* (Tactician, squads, missions)
        → Micro (HOW, per unit?)     Micro/Agency/*, Micro/Actions/*
            → Commander → bwapiUnit.* (the ONLY place that issues BWAPI orders)
```

### 3.1 Strategery — strategy selection
`Strategist` (`Strategery/Strategist.scala:13`) mixes in `StrategyDatabase`
(legality graph), `RecentFingerprints`, `GameFeatures` (map features:
`isRamped/Flat/Inverted`, rush distances, island/FFA/money), `Rolling`
(one-off RNG per game). `update()` picks `strategiesSelected` from the
playbook policy (`Playbook`: Normal→Greedy, Tournament→per-opponent,
Human/Pretraining→Dynamic, Testing→Random). A `Strategy`
(`Strategery/Strategies/Strategy.scala`) is declarative: allowed races/maps/
entrances/rush distances, white/blacklists vs fingerprints and maps,
`activationRequirements`, and a graph of `StrategyChoice` children expanded
to full root-to-leaf `StrategyBranch`es (`Selection/ExpandStrategy.scala`).
`StrategyLegality` gates legality; `StrategyEvaluation` scores win probability
from game history. A strategy may carry its own `gameplan: Option[Plan];
otherwise the default `StandardGameplan` runs.

### 3.2 Planning — build orders as plans
`Planning/Plans/Plan.scala`: `update() = prioritize + onUpdate`. A gameplan
(`Gameplans/All/StandardGameplan` → per-race `*StandardGameplan` →
`ModalGameplan` of enemy-race branches) only *declares demand* through
`MacroActions` (`Macro/Actions/MacroActions.scala`): `get(Unit/Upgrade/Tech)`,
`pump/pumpRatio/pumpWorkers`, `expandNTimes/requireBases`, `buildCannonsAt*`,
`attack()/harass()/allIn()/aggression()`, `scout()`, gas ratios,
`upgradeContinuously`, `cancel`. No direct unit orders.

### 3.3 Macro — demand → scheduled, funded, staffed production
* `Scheduler` (`Macro/Scheduling/Scheduler.scala`): dumb buffer of
  `RequestBuildable` (what/quantity/placement/min-start-frame).
* `Bank` (`Macro/Allocation/Bank.scala`): who gets minerals/gas/supply, by
  `prioritize()` order (gameplan vs tactics).
* `MacroSim` (`Macro/Scheduling/MacroSim.scala:51-186`): simulates the queue
  (income, producers, larva, supply, addons, tech) and emits the feasible
  `queue: Seq[(Request, dFrames)]`, injecting autosupply. Filters fantasy
  builds before they waste resources.
* `Recruiter` (`Macro/Allocation/Recruiter.scala`): assigns units to
  `LockUnits` (who may use which unit); `available` = free + interruptible.
* `Groundskeeper` (`Macro/Allocation/Groundskeeper.scala`): tile reservations
  for buildings (`LockTiles`).
* `Tactic/Production/Produce*.scala`: turns the simulated queue into real
  `Commander.build/train/tech/upgrade` calls.

### 3.4 Tactic — squads, missions, intentions
`Tactician.onRun` (`Tactic/Tactician.scala:24`): launches `missions`
(KillExpansion, drop/reaver/storm/speedlot missions), ~20 priority tactics
(worker rush, pylon block, cannon rush, scouting, anti-proxy, meld…), then
`runCoreTactics` assigns freelancers to `SquadDefendBase`s / one
`SquadAttack`, then background tactics (mineral-block removal, overlords…).
`Squad` (`Tactic/Squads/Squad.scala`) groups units, picks modes
(`SquadAttack.chooseMode`: YOLO/RazeBase/PushMain/Contain…), and
`SquadAutomation` converts that into per-unit orders.
`Mission` (`Tactic/Missions/Mission.scala`) is a squad with a lifecycle
(`shouldForm/recruit/terminate`), e.g. `MissionDrop` (603 lines: manifests,
routes, pickup discipline).
Cross-layer contract: a tactic calls `unit.intend(tactic)` to get the frame's
single `Intention` (`Micro/Agency/Intention.scala:13-80`: terminus/station/
redoubt, `toAttack/toGather/...`, `action` defaulting to `Idle`,
`canFight/canFlee/canSneak`, `targets`). `Agent.execute`
(`Micro/Agency/Agent.scala:64`) copies `intent.*` into per-frame state and
runs `intent.action(unit)`. `Commander` (`Micro/Agency/Commander.scala:17-727`)
is the ONLY place that touches `bwapiUnit.*` (move/attack/patrol/gather/build/
tech/sleep), with anti-stick logic, destination adjustment and push integration.

### 3.5 Micro — per-unit combat
`Combat` (`Micro/Actions/Combat/Decisionmaking/Combat.scala:25-481`) is a
state machine over `Techniques` (`Fight/Walk/Flee/Aim/Dodge/Abuse/SuperKite/
Scavenge/Fallback`, `Techniques.scala`). Each frame: `Target.choose` (if no
target), `choosePerch`, `defaultEscalation`, technique selection + transitions
(can-move/can-fight/safe-passage/pushes/matchups), then exactly one of
`walk/aim/dodge/potshot/Retreat/Brawl/abuse/superkite/engage`. Movement blends
potential-field `Force`s (`Micro/Heuristics/Potential.scala`,
`Mathematics/Physics/*`: travel/target/threat/spacing/pushing/regrouping)
with `MicroPathing` waypoints; pushing between units uses `TrafficPriorities`
(`Freedom < Nudge < Bump < Shove < Dodge < Dive`).
`FightOrFlee` (`.../Decisionmaking/FightOrFlee.scala:20`) decides
`shouldFight` from ~20 named rules (`CantFight/CantFlee/Committed/YOLO/
Cloaked/...`) or the battle forecast. `Target.choose`
(`Micro/Targeting/Target.scala`) filters (`Enemy/CanAttack/Missing/Type/Focus/
StayCloaked/...`) then scores (`TargetScoring`: value + injury − pixel cost +
bonuses − threat).
Races add bespoke micro (`Micro/Actions/Protoss|Terran|Zerg/Be*.scala`).

## 4. World model: `ProxyBwapi`, `Information`, `Mathematics`

* `ProxyBwapi/UnitInfo/UnitInfo.scala` (627 lines): per-frame snapshot with
  `canMove/canAttack/cooldown/pixelsOfEntanglement/pixelToFireAt/
  presumptiveTarget/matchups/simulacrum/confidence/visibility`. Subtypes:
  `FriendlyUnitInfo` (+`agent/hysteresis/intent/squad/formations`),
  `ForeignUnitInfo` (last-seen state), `HistoricalUnitInfo` (dead).
  `UnitClass.scala` (802 lines): the unit dictionary (geometry, range,
  damage, cooldowns, costs, subjective/skimulation value, spell lists…).
  Source of truth is JBWAPI (`3rdparty/jbwapi`); `UnitTracker` births/kills
  units and updates them under a mutex each frame.
* `Information/Battles`: battle pipeline
  (`Initial→Cluster→Divisions→MatchupAnalysis→Predict→Judge→Complete→Swap`,
  `ProcessingStates/BattleProcess*.scala`). `Simulation` runs a real
  step-based battle sim (`Simulacrum` per unit, `behavior` per role) async;
  `Skimulator` is the cheap analytic fallback; `BattleJudgment.shouldFight`
  blends both. `MatchupAnalysis` answers per-unit questions
  (threats/targets/deepest/framesToLive/pixelsEntangled).
* `Information/Grids`: tile grids (`Walkable/Mobility/Vision/Floody enemy
  range/FormationSlots/PsionicStorm/...`), refreshed on schedules.
* `Information/Geography`: `Zone/Base/Edge/Metro` over BWTA regions;
  `GeographyBuilder` assigns every tile, finds bases, merges mains;
  `Expansions` tracks ours/enemy/neutral bases; `Scouting` mixes
  debuts/tech/base-inference/intrigue/timings.
* `Information/Scouting/Fingerprints`: enemy build-order detectors feeding
  strategy selection and tactics.
* `Mathematics`: `Maff` (trait mixin: Angles/Approximate/Bitshift/Convert/
  Polygons/Rectangles/Reduce/Sample/Sort), `Points` (Pixel/Tile + distances),
  `Physics` (forces). Hot paths use whitebox macros from `src-macros`
  (`?.apply` ternaries, `minBy/maxBy` unrolled loops); the forwarders live in
  `src-macros` on purpose so zinc doesn't recompile half the codebase on
  unrelated edits (see `.ai/REVIEW.md`).

## 5. Cross-cutting infrastructure

* **Configuration** (`Lifecycle/Configure/*`): `BwapiData` locates
  `bwapi-data/{ai,read,write}` upwards from the CWD; `ConfigurationLoader`
  picks `bwapi-data/AI/*Purple*.config.json` (bot-name match, else newest) and
  maps flags onto `Configuration` (frame targets, mode switches, playbook
  overrides). `configs/*.json` are ladder/dev presets; `FileFlag` reads
  toggle files (`fixedbuilds-by-opponent.json`).
* **Performance** (`Performance/*`): every system is a `TimedTask` with a
  millisecond budget; `TaskQueueParallel` schedules by `recentMs/weight`,
  `TaskQueueSerial` resumes across frames; `PerformanceMonitor` tracks
  overruns and disqualification risk; `ReactionTimes`/`Latency` measure
  sluggishness; `With.game.setLocalSpeed(0)` runs uncapped locally.
* **Debugging** (`Debugging/*`): `Visualization` renders ~50 toggleable map/
  screen views (`DrawMap`/`DrawScreen` over BWAPI draws, viewport-culled);
  `Storyteller` logs only-on-change game stories + end-of-game perf report;
  `Camera` auto-follows interesting fights until the user grabs the mouse;
  `Logger` buffers to `bwapi-data/write/<enemies>.normal|error.log.txt`
  (logstd mirrors to stderr); `KeyboardCommands` are chat commands from the
  bot (`1-4` speed, `c/v/vm/vs/vh/vt`, `rewall`, `perform`…).
* **Placement** (`Placement/*`): `Template` DSL (`H/G/P/W/R/T/C/B/F` tiles +
  label modifiers), `Fitter` (rectangle/spiral/proximity search honoring
  occupancy/creep/psi/gap rules), `PlacementCache` (versioned gzip cache keyed
  by map+matchup+revision, atomic write), `WallFinder` (recursive wall search
  with filler 2×2 pylons/cannons + hallway validation; Protoss/Terran only,
  Zerg returns None).
* **Build & run** (`pom.xml`, `build-jar.sh`, `scripts/*`, `tests/*`):
  Maven is source of truth (`scala-maven-plugin 4.9.2`: `compile-macros` →
  `macros.jar` → `compile-main` with `-opt:inline` for hot paths → shaded fat
  jar, `Main-Class: Lifecycle.Main`). `build-jar.sh` = incremental offline dev
  build (`-Pdev` drops `-opt`); `--release/--exe/--clean` variants. `run.sh`
  (entry: requires `--map`, rebuilds, supervisor-safe jar copy, execs the
  Java supervisor) + `wine-run.sh` (Wine game orchestration: lock, bg build,
  client-first JVM start, `bwapi.ini` patching, ChaosLauncher, HELLO gate,
  watch/cleanup/archive to `out/wine_runs/`) + `list-maps.sh`.
  `tests/` are standalone `App` objects run via `scripts/test_*.ps1|.sh`
  (not Surefire). `3rdparty/{jbwapi,javajps,jbweb,mjson,apache}` is compiled
  into the fat jar via `build-helper add-source`.

## 6. Concepts map (one-liners)

* **Intent** — one per-unit order-chit per frame (`Intention`), written by
  tactics, read by `Agent.execute`.
* **Technique** — combat state (`Fight/Walk/Flee/Aim/...`) with allowed
  transitions; separate from *intent*.
* **Squad / Mission / Tactic** — `Tactic` recruits (`Recruiter`) and assigns;
  `Squad` steers a group for a fight; `Mission` is a squad with a lifecycle.
* **Blackboard** — per-frame shared flags (`wantToAttack`, aggression,
  gas ratios); `Yolo` can override them.
* **MacroSim queue** — the only legal shopping list for production.
* **Matchup / Battle / Judgment** — per-unit threats, per-group fight state,
  fight/no-fight verdict (sim + skim blend).
* **Fingerprint** — named enemy-build detector (macro openings, cheese).
* **StrategyBranch** — one full root-to-leaf strategy path with a win
  probability; the playbook policy picks one per game.
* **NoPlan / umsMode** — UMS micro maps: no bases → no macro gameplan, no
  strategy evaluation, units march on the enemy (see `UmsMode`).

## 7. Local extensions in this fork (beyond upstream)

`Lifecycle.Launch` (CLI launcher: `none|chaos|wine`, bwapi.ini patching,
Wine supervisor/client split), `Lifecycle.Keyboard` + `actions/*`
(global hotkeys, one class per key, supervisor↔bot relay file),
`Lifecycle.UmsMode` (baseless-map detection, NoPlan gate, terminus forcing),
`Lifecycle.MapSpecificCommander` (per-map dev tweaks, active only with the
hook), `Lifecycle/Launch/BwapiIni` (ini patching shared by JVM + script),
`scripts/{run,run-wine,wine-run,list-maps}.sh`, `build-jar.sh` profiles,
`Strategist.update`/`Fingerprint.update`/`FindWall`/`GeographyBuilder`
UMS guards, a `Terrain analysis unavailable` non-fatal path, and the
`--[no-]headless` Wine session handling.
