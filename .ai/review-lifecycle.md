# Code review: `src/Lifecycle`

Scope: the whole folder, 46 sources / 1 737 lines (Scala + Java), reviewed
2026-11-15 on `develop`. Brief from the owner: layer communication, SRP,
duplication, concepts to extract or merge, readability, OCP, short classes and
methods.

Method: every file in the folder was read in full (`With.scala` 275 lines and
`MapSpecificCommander` 316 lines included). Counts below come from `grep` over
`src/` on the reviewed tree. No build or suite run: this review changes no code.

Legend: **L** = layer seam, **S** = SRP / size, **D** = duplication or a
concept that belongs somewhere else, **O** = OCP / extension point,
**R** = readability and hygiene, **B** = concrete bug found while reading.
Severity: high / medium / low.

Baseline worth stating up front: this folder is the one place in the tree where
the "one decision point per backend" rule is actually implemented, and it works.
`GameLauncher` + `GameLauncherFactory` + four backends is textbook OCP, the
`Keyboard`/`GameCommands` split puts every shortcut behaviour in one file, and
`GameSummary`/`HitCounter.countHits` are pure kernels with suites next to them.
Most findings below are about the *seams* around that core - chiefly `With`,
which is where the folder's discipline stops.

---

## 1. Layer communication

### L1 (high) `With` is a 73-field global that Lifecycle publishes and everyone writes

`src/Lifecycle/With.scala:46-119`

73 mutable `var`s (20 world-data at lines 46-65, 53 service refs at 67-119), 52 of them constructed by hand in `onStart` (line 131). The
read side is the whole bot: 3 900 `With.` references across 617 files. That is a
deliberate service locator and undoing it is a rewrite, so the useful question
is not "remove `With`" but "who may write it".

Today: any layer. `grep` for assignments outside this folder finds game code
writing configuration and world state as if it owned it:

```
Micro/Agency/CommanderCore.scala:179   With.configuration.trackUnit = false
Debugging/KeyboardCommands.scala:46    With.configuration.trackUnit = ! ...
Debugging/KeyboardCommands.scala:26    With.configuration.camera = ! ...
Micro/..(keyboard)                     With.configuration.visualizeDebug = on
```

`With` mixes two very different things: **inputs** owned by the engine (frame,
map dimensions, players, map name) and **services** owned by subsystems
(`units`, `geography`, `battles`, ...). Only the first group is truly global
state; the second group is a dependency injection registry.

Fix, in two steps that do not require touching call sites:

1. Extract the 20 "basic data" vars (lines 46-65: `frame`, `startNanoTime`,
   `self`/`neutral`/`enemies`/`friendlies`, the seven `map*` dimensions,
   `mapFileName`/`mapCleanName`/`mapClock`, `umsMode`) into a `WorldState`
   value that only `Lifecycle` writes. `With.frame` etc. stay as forwarders, so
   3 900 call sites keep compiling.
2. Make the ~50 service refs a single `Systems` group (see L4), initialized once,
   read-only afterwards. Then "who may mutate what" is a type, not a convention.

Testability: today `TestKit` writes `With.frame`, `With.mapTileWidth`,
`With.game`, `With.proxy`, `With.units` by hand (`src/test/scala/TestKit.scala:20-28`).
A `WorldState` value makes that a constructor call and removes five reflective
assignments from every suite.

### L2 (high) `Configuration` is a second global, with two sources of truth for every mode

`src/Lifecycle/Configure/Configuration.scala`, `ConfigurationLoader.scala:41-140`

`Configuration` holds 43 public `var`s spanning tournament settings, modes,
strategy, battle simulation, performance, visualization and debugging. It is
written from three layers (the loader, `Debugging/KeyboardCommands`, the
`Keyboard` actions) and read from everywhere (`configuration.camera` 15 sites,
`configuration.debugging` 14, `configuration.playbook` 15).

Inside the loader the duplication is visible: each JSON flag is parsed into a
**local** boolean, then applied by mutating `With.configuration`, then the
**local** is logged - two variables for one setting:

```scala
val cherryVis = getOrDefault(config, "cherryvis", true)   // line 84
With.configuration.cherryVis = cherryVis                  // line 93
With.configuration.cherryVis = cherryVisFlagOverride().getOrElse(With.configuration.cherryVis) // line 94
```

Line 94 reads back what line 93 just wrote, which is how the first assignment
became redundant without anyone noticing.

There are also four config fields with no reader anywhere: `forcedStrategy`
(line 42), `logTaskDuration` (line 63), `visualizeFun` (line 23, only ever set),
and `simviz`'s boolean twin pattern in the launcher. Dead settings in a config
class are worse than dead code: they look like knobs.

Fix: `Configuration` becomes an immutable `case class` built once by the loader
(`flags.copy(camera = true)`), with `With.configuration` a `var` assigned once in
`onStart`. Mode application (`setHumanMode`, `setLadderMode`, ...) becomes a
`Seq[Mode]` fold over the case class instead of 9 methods mutating shared state.
The four dead fields go.

### L3 (medium) A game-logic package reads a listener's mutable counter

`src/Lifecycle/EridanEventListener.scala:11` - `var gameCount: Int`

`src/ProxyBwapi/UnitTracking/GhostUnit.scala:12,20` reads
`EridanEventListener.gameCount > 1` to decide whether a ghost unit is a
carry-over from a previous game.

Two problems. First the direction: `ProxyBwapi` (a data layer) depends on a
`Lifecycle` adapter object - a leaf-to-root dependency that exists only because
that is where the counter happened to live. Second the shape: a public mutable
`var` on a BWAPI callback object.

Fix: the counter belongs in the session state from L1 (`With.gameCount` or
`WorldSnapshot.gameIndex`), written once in `onStart`. The listener keeps its
two genuinely adapter-shaped fields (see B2) private.

### L4 (medium) `With.onStart` is the construction hub: 52 `new` in one method

`src/Lifecycle/With.scala:131-241`

```scala
// Order-dependent initialization:
// PerformanceMonitor must exist when creating any task
performance       = new PerformanceMonitor
agents            = new Agency
...
// Order-dependent initialization:
// TaskQueue comes last because it references other systems
tasks             = new TaskQueueGlobal
```

Two of those ~50 lines are genuinely order-sensitive; the other ~48 are an
alphabetical list that only *looks* ordered because someone sorted it. Adding a
system means editing a 110-line method in the middle, and the two real
constraints are enforced by a comment.

Fix: a `Systems` class with an explicit `init()` that states the dependency
graph once (`performance` first, `tasks` last) and constructs the middle group
as a data list, or - cheaper - keep `With` as the registry but move the body to
`Bootstrap.start()` returning an immutable `Systems`, with `With` holding one
reference. Second-order benefit: `onStart` drops under 40 lines and terrain
analysis (S1) leaves the god object with it.

### L5 (medium) The per-game reset protocol is half automatic and half a dozen hand-written lines

`src/Lifecycle/EridanWave.scala:33-55`

```scala
Debugging.ErrorLedger.reset()
Debugging.MapSnapshot.reset()
Debugging.SlowAlone.reset()
Utilities.Latches.resetAll()          // <- the good one: self-registering
Micro.Actions.Combat.Maneuvering.FleeRelocation.reset()
Lifecycle.HitCounter.reset()
Debugging.FormationAudit.reset()
Debugging.Probe.reset()
Debugging.DeathWatch.reset()
Tactic.Squads.ContainStatus.reset()
```

`Utilities.Latches.resetAll()` is exactly the right pattern (`conventions.md`
§14: latches register themselves because unit ids restart every game). Every
other subsystem on that list was remembered by hand, and the file itself
documents the failure mode in a comment three lines above: *"no per-latch line
to forget"*.

It has already been forgotten once: `EridanWave.enginePauseNoticed` (line 109)
is per-game state that `onStart` never clears. After a game that ended while
paused from outside (`enginePauseNoticed == true`), the next game ignores the
first foreign pause instead of resuming - the exact bug the flag exists to
prevent (see B3).

Fix: one registry, `trait PerGame { def reset(): Unit }`, with instances
collected once; `onStart` calls `PerGame.resetAll()`. Until then, add
`enginePauseNoticed = false` to `onStart` - it is a one-line fix for a real,
if rare, stuck-paused game.

---

## 2. SRP and size

### S1 (high) `With` has three responsibilities: world state, bootstrap, terrain policy

`src/Lifecycle/With.scala` - 275 lines, of which:
- lines 46-119: the field list (world state + service registry),
- lines 131-241: bootstrap,
- lines 243-273: `analyzeTerrain`, a three-level BWTA retry policy with its own
  stdout+logger reporting.

The terrain block is the clearest violation: it decides *policy* (skip UMS maps,
retry with assertions off, give up and play without terrain) inside the object
that is supposed to only hold state.

Fix: `TerrainAnalysis.analyze(game): Unit` owns the skip and both retries;
`With.analyzeTerrain` collapses to one call. This also makes the policy testable
in principle (it takes a `Game` and a map name, nothing else).

### S2 (high) `MapSpecificCommander` is a 19-branch `if/else` chain of near-identical bodies

`src/Lifecycle/MapSpecificCommander.scala:44-316`

19 map branches; `setSpeedOnly(20)` appears 13 times, `setPainting(true)` 11
times, and the majority of branches are literally:

```scala
else if (isMap("ZealDrag_v_", "Drag_run", "Probe_run", "Reav_v_")) {
  if (With.frame <= 1) {
    GameCommands.setSpeedOnly(20)
    GameCommands.setPainting(true)
    // TODO(EridanWave painter): CameraCommander.focusCameraOnInterestingCombatUnit()
  }
}
```

83 of the 316 lines are commented-out code (26%), which `conventions.md` §6
allows as porting material but which here makes the file hard to scan for the
handful of branches that differ.

This is data, not code. Fix: a table.

```scala
case class MapTuning(markers: Seq[String], speedMs: Int, frameSkip: Int, painting: Boolean)
val tuning: Seq[MapTuning] = Seq(...)   // one line per map family
```

`onFrame` becomes: find the first row whose markers match, apply it once
(`firstSettings`), then apply `initialSpeed`. The three genuinely special
branches (vsGosu first-combat-unit pacing, `mar_v_zea` unskip, `Bullet`) stay as
named functions the table can point at.

### S3 (medium) `ConfigurationLoader.load()` does five jobs in 100 lines

`src/Lifecycle/Configure/ConfigurationLoader.scala:41-141`

In one method: find the `bwapi-data/AI` directory, filter candidate files, pick
the newest per opponent name, parse 19 keys, run 8 "both modes enabled"
warnings, apply 9 mode setters, and print a 16-row summary. Each of those is a
different reason to change (a new config key touches four of them - see O3).

Fix: `findConfigFile(): Option[File]`, `flags(config): Flags` (a case class of
the 19 parsed values), `validate(flags): Seq[String]` (pure, testable - the eight
warnings are a truth table), `applyModes(flags)`, `logSummary(flags)`.

### S4 (medium) `Configuration` groups unrelated settings

`src/Lifecycle/Configure/Configuration.scala`

Tournament settings, mode switches, strategy tuning, battle-simulation
parameters, performance, visualization and debugging all share one class because
they all come from one JSON file. The JSON file is one file; the settings are
not one concept. `playbook` (a derived getter, line 43) sitting between two
`var`s shows the mixture.

Fix, if L2 is done: split the case class along the file's existing `/////////`
sections. A `Flags` case class per concern, composed into `Configuration`.

### S5 (medium) `EridanWave.onEnd` - 40 statements of teardown plus the exit policy

`src/Lifecycle/EridanWave.scala:130-177`

Teardown for 15 subsystems, two `System.out.println` blocks, a
`CombatVisIO.terminateVisualizer()` guarded inline, and the "should this process
die now?" decision (`isWineClientJvm() || isScbwMatchContainer()`) with its
`With.frame > 120` guard - all in one method, mixed with the answer to a
different question (which launcher are we running under?).

Fix: `EndOfGame.report(isWinner)` (the summary + audit teardown) and
`SessionExit.afterGame(): Boolean` (the exit policy, with the frame guard as its
own named rule). `onEnd` becomes 8 lines of delegation.

### S6 (low) `ProcessHelper` mixes process control with path utilities

`src/Lifecycle/Launch/ProcessHelper.java`

`killStarcraft`/`killChaosLauncher`/`killClientJvm`/`startChaosLauncherWindows`
are OS process control; `isProcessRunning` (unused), `readAll` (stream helper),
`defaultFile` and `userHome` (path utilities) are not. `defaultFile` and
`userHome` are used only by `ChaosGameLauncher` and `BwapiIni`.

Fix: move the two path helpers into `LaunchPaths` (which D1 creates anyway);
`ProcessHelper` keeps the four kill/start methods and `sleep`.

### S7 (low) `Keyboard` bundles the keymap, the relay dedup and a background thread

`src/Lifecycle/Keyboard/Keyboard.java`

The keymap (`static {}`, 25 entries, lines 81-115) is a table; the dedup
(`recordDirectPress`/`consumeDirectTwin`, lines 175-206, with a 2000 ms window
and 16-entry ring per key) is a self-contained algorithm with no game knowledge;
the drain daemon (lines 154-175) is a thread lifecycle. Three reasons to change.

Fix: `PressDeduper` (pure: `record(int code, long ms)`,
`boolean consumeTwin(int code, long ms)`) - and it becomes the folder's second
unit-testable kernel after `HitCounter.countHits`.

---

## 3. Duplication and concepts that belong together

### D1 (high) "Where is the live game?" is derived twice, by two different algorithms

`src/Lifecycle/Launch/BwapiIni.java:32-70`

```java
private static String explicitIniPath()   // -Deridanwave.bwapiIni -> -Dpurplewave.bwapiIni
public static String defaultGameRoot(...) // gameRoot -> winePrefix + "/drive_c/sc"
private static String defaultWinePrefix() // WINEPREFIX -> ~/.wine
```

`src/Lifecycle/Keyboard/KeyRelay.java:37-90`

```java
static File file()                  // -Deridanwave.keyRelay -> -Dpurplewave.keyRelay
                                    // -> env ERIDANWAVE_KEYRELAY -> PURPLEWAVE_KEYRELAY
static String gameRootDir(...)      // ERIDANWAVE_GAME_ROOT -> PURPLEWAVE_GAME_ROOT
                                    // -> WINEPREFIX + "/drive_c/sc" -> user.home + "/.wine"
```

Same concept, two implementations, and they already differ: `BwapiIni`'s chain
has no `PURPLEWAVE_GAME_ROOT`, `KeyRelay`'s has no `-Deridanwave.gameRoot`, and
the `~/.wine` default is spelled two ways (`ProcessHelper.userHome() + ".wine"`
vs `System.getProperty("user.home", ".") + "/.wine"`). Both class headers
explain the Wine path problem at length - twice.

There are 13 `purplewave`/`PURPLEWAVE` occurrences across 6 files, all
implementing the same legacy-name fallback by hand.

Fix: one `LaunchPaths` object: `gameRoot`, `bwapiIni`, `keyRelay`, plus
`property(String name): Option[String]` that encapsulates the
`eridanwave.*` -> `purplewave.*` fallback. `BwapiIni` and `KeyRelay` then take a
`File` and stop knowing where it came from - which is also what makes them
testable with a temp file.

### D2 (high) "Is this a scenario/UMS map?" has four answers, one of them in bash

1. `src/Lifecycle/Launch/BwapiIni.java:85-98` - `UMS_MARKERS`, 31 substrings,
   with the comment *"Keep in sync with the marker list in scripts/wine-run.sh"*.
2. `src/Lifecycle/MapSpecificCommander.scala:35` - `isMap(...)`, 19 branches of
   map-name matching, some of which are the same names.
3. `src/Lifecycle/MapSpecificCommander.scala:23` - `shouldTreatAsNormalMap`,
   two UMS maps that are exceptions to the rule.
4. `src/Lifecycle/UmsMode.scala:38-40` - the runtime heuristic
   (`workers > 3 && bases > 0`) that decides the same question from the game.

A comment that says "keep in sync with a shell script" is a duplication bug
waiting for its first drift. Fix: a `MapCatalog` (markers + exceptions) in Scala
used by all three Scala sites and by `BwapiIni`'s game-type default; the script
either consumes the same list (it can `grep` it from the source, as some repos
do) or the Scala side stops claiming the script is authoritative.

`MapSpecificCommander`'s per-map *speed* settings are then a table over the same
catalog (S2), so the answer to "what do we know about this map?" lives once.

### D3 (medium) The speed ladder exists twice, in two languages

`src/Lifecycle/Keyboard/GameCommands.scala:11-12`

```scala
val speedLadder: Vector[(Int, Int)] = Vector(
  (20, 0), (0, 0), (0, 10), (0, 20), (0, 40), (0, 80), (0, 150), (0, 250), (0, 500))
```

`src/Lifecycle/Keyboard/actions/Speed/Speed1.java` .. `Speed9.java` - nine 9-line
files whose only content is `super(20, 0)`, `super(0, 0)`, ... - the same nine
pairs, in the same order.

Fix: `Keyboard`'s static block builds the nine entries from
`GameCommands.speedLadder` (or `GameCommands.speedPresets`), and
`SetGameSpeed`+`Speed1..Speed9` (10 files) collapse into the map. See O6.

### D4 (medium) Shortcut behaviour has two homes: `Keyboard` and the chat commands

`src/Debugging/KeyboardCommands.scala:31-46`

```scala
case "c"  => With.configuration.camera  = ! With.configuration.camera
case "v"  => With.visualization.enabled = ! With.visualization.enabled
case "1"  => With.game.setLocalSpeed(24 * 32) ; With.configuration.camera = false
...
case "pause" => Lifecycle.Keyboard.GameCommands.togglePause()
```

`GameCommands` is documented as *"exactly one place where shortcut behavior is
defined"* and the pause command follows that rule. The rest do not: "c"/"v"
re-implement `toggleCamera`/`toggleVisualization`, and "1".."4" call
`With.game.setLocalSpeed` directly - bypassing the ladder, so `speedIndex` stays
stale and the next `+`/`-` jumps from wherever the keyboard left it (see B4).

Fix: every chat command delegates to `GameCommands`, and "1".."4" become
`speedLadder` presets like the numeric keys. This is a five-line change in
`Debugging`, and it makes `GameCommands`'s header true.

### D5 (medium) `LaunchConfig` pays five edits per flag, and the help text twice

`src/Lifecycle/Launch/LaunchConfig.java` - 307 lines: 22 fields, 21 getters,
a 26-branch `if/else` parse chain, a 41-line javadoc listing every flag, and
`printHelp()` (line 196) listing them again in different wording.

Adding `--foo` means: field, parse branch, getter, javadoc, `printHelp`. The
javadoc and `printHelp` already disagree in formatting (javadoc uses
`--map=&lt;name&gt;`, help uses `[--map=<name>]`), and neither is checked.

Fix: no framework needed - a `Seq<Flag>` table (`name`, help text, setter), with
`parse` looping it and `printHelp`/the javadoc generated from it. Alternatively
accept the boilerplate but delete the javadoc list (one home for the text).

### D6 (medium) `UnixChaosGameLauncher` contains a second copy of the Wine-client launcher

`src/Lifecycle/Launch/UnixChaosGameLauncher.java:24-40`

```java
if (GameLauncherFactory.runningUnderWine()) {
    // banner, keyboard.listen unless headless, BwapiIni.patch(defaultWineIni(...), ..., true)
    return;
}
```

`WineClientGameLauncher` (73 lines, own file, own header) does exactly this, and
`GameLauncherFactory.forConfig` already routes every Wine JVM to it *first*
(line 33). So this branch is either dead or - worse - reachable only in the case
where the factory's routing is wrong (B1), in which case two implementations of
"client only" disagree about the banner and the keyboard mode.

Fix: delete the branch; `UnixChaosGameLauncher` is then purely the supervisor
(fork `wine-run.sh`, print its exit code). One backend, one file, no sync burden.

### D7 (low) Two output channels, chosen ad hoc, sometimes both for one event

50 `System.out.println`/`System.err.println` calls across 12 files
(`UnixChaosGameLauncher` 10, `LaunchConfig` 10, `BwapiIni` 6,
`WineClientGameLauncher` 6, `Keyboard` 5, ...) against 39 `With.logger` calls in
this folder.

Two of them print the *same* message twice:

```scala
With.logger.debug("Skipping terrain analysis (UMS map).")        // With.scala:220
System.out.println("[launcher] Skipping terrain analysis (UMS map).")  // With.scala:221
```

This is defensible - stdout is the launcher's channel (it must work before
`Logger` exists, and `wine-run.sh` tees it), the logger is the game's - but it
is nowhere written down, so each new line picks a channel by imitation. Fix:
a one-line doc comment on each channel's owner in `conventions.md`, plus a
`Launcher.say(...)` for the `[launcher]` prefix (repeated 30+ times by hand).

### D8 (good - do not "fix") the pure-kernel pair

`src/Lifecycle/GameSummary.scala` (box + details, exact strings, pinned by
`GameSummarySuite`) and `HitCounter.countHits` (pure, pinned by
`HitCounterSuite`) are the model for the rest of this folder: the impurity is
one `units.view.filter(_.alive)` in `onFrame`, everything that could be wrong is
a pure function with a suite.

---

## 4. OCP and extension points

### O1 (high) `GameLauncherFactory` is the pattern to copy - with one heuristic crack

`src/Lifecycle/Launch/GameLauncherFactory.java:33-51`

One decision point, four backends, no backend `if` anywhere in game logic. New
backend = new `GameLauncher` + one `case`. This is the best-structured code in
the folder and should be the reference when `Micro`'s dispatch chains get
reworked (see `.ai/review-d.md` S4/D1).

The crack is `runningUnderWine()` (line 93): a backend decision made from
environment sniffing.

```java
boolean wineMarkers = System.getenv("WINEDEBUG") != null
    || System.getenv("WINEDLLOVERRIDES") != null
    || System.getenv("WINEPREFIX") != null;
```

`WINEPREFIX` is a normal shell variable in any Wine user's profile. A Linux
supervisor JVM started from such a shell (IDE, terminal) is routed to
`WineClientGameLauncher` - the backend that deliberately starts nothing - and
the bot then attaches from a Linux JVM, which is the permanent
"Game table mapping not found" loop the class header warns about. The client JVM
already carries `-Deridanwave.wineClient=true` (`scripts/wine-run.sh:310`) and
`-Dos.name=Windows 10` (line 307), so the robust discriminator is already in
hand: `ProcessHelper.isWindows()`. Fix: `isWineClientJvm() || isWindows()`, and
drop the env heuristic.

Testability: a factory taking `(boolean underWine, Launcher flag)` as parameters
is a three-line unit test; today it takes no arguments and reads the process
environment, so it is untestable by construction.

### O2 (medium) `EridanWave`'s three lifecycle methods are fixed sequences

`src/Lifecycle/EridanWave.scala:20-55, 60-100, 129-177`

A new subsystem participates in start (reset + initialize), frame (update) and
end (teardown) by editing three methods in this file. `Utilities.Latches` is the
one mechanism that solved this (register once, sweep all); everything else is a
manual line in `onStart`, `onFrame` and `onEnd`.

Fix: the `PerGame` registry from L5 for the reset half; for the frame half, the
`TaskQueue` already exists and most per-frame work *is* a task - the items that
run inline (`MapSpecificCommander`, `UmsMode`, `With.lambdas`, `HitCounter`,
`Debugging.DeathWatch`, `CherryVis`) are inline because they were easy, not
because they must be ordered before `tasks.run`.

### O3 (medium) `ConfigurationLoader`: a new config key touches four places

Per key: the parse block (line 66-84), the warnings block (lines 96-102), the
apply `if` chain (lines 104-118), and the summary `Seq` (lines 120-136). Plus the
field in `Configuration`.

Fix: `case class Setting[T](jsonKey: String, default: T, apply: T => Unit)`
and a `Seq[Setting[_]]`; the summary is generated from the table. Warnings stay
a separate explicit `Seq[(Boolean, String)]` because they are cross-field rules.

### O4 (medium) `With.onStart`: a new system is two edits in two files

See L4. The cost is not the two edits, it is that the edit has a *position*
requirement held only by a comment.

### O5 (low) `MapSpecificCommander`'s chain: a new map edits the middle of a method

Same fix as S2: the map list becomes data.

### O6 (low) 18 one-line `KeyAction` classes add files, not concepts

`src/Lifecycle/Keyboard/actions/` - 18 Java files, 230 lines, of which roughly a
third is code. `ToggleCamera.execute()` is `GameCommands.toggleCamera();`,
`PauseGame.execute()` is `GameCommands.togglePause();`, and nine classes differ
only in two integers.

The stated rationale ("one class per button") is real OCP when a button owns
behaviour, but here every behaviour is in `GameCommands`, so a new key adds a
class that cannot be wrong and cannot be reused. `KeyAction` is worth keeping
(it makes the map readable and gives `ExitGame` - which does own logic - a home),
but the delegates can be lambdas:

```java
actions.put(NativeKeyEvent.VC_C, GameCommands::toggleCamera);
actions.put(NativeKeyEvent.VC_P, GameCommands::togglePainting);
```

That is a 16-file reduction with no behaviour change.

---

## 5. Readability and hygiene

### R1 (medium, mechanical) Line endings are split 9 CRLF / 37 LF, and three files
are mixed inside themselves

`AGENTS.md` says default to CRLF unless a file clearly already uses something
else. Here the split is not by subdirectory but by age - the nine CRLF files are
the older ones, the 37 LF files are everything added later:

| CRLF (9) | LF (37) |
|---|---|
| `With.scala`, `Main.scala`, `Manners.scala`, `Configure/{BwapiData,ConfigurationLoader,Configuration,FileFlag}.scala`, `Launch/ChaosGameLauncher.java`, `Keyboard/KeyAction.java` | every other file: `EridanWave`, `UmsMode`, `MapSpecificCommander`, `HitCounter`, `GameSummary`, `EridanEventListener`, `Enemy`, `We`, all of `Keyboard/` except `KeyAction`, all of `Launch/` except `ChaosGameLauncher` |

The pattern is consistent enough to be a convention ("new files are LF"), but
three files are genuinely mixed, which is the part that actually hurts:

```
With.scala:18-19                    two LF import lines inside a CRLF file
Configure/BwapiData.scala:76-77     LF-only trailing blank lines
Configure/ConfigurationLoader.scala:230-231   same
```

So a `git diff` on `With.scala` shows whole-line changes for a two-line edit, and
`AGENTS.md`'s "preserve existing line endings" rule has no single answer for
this folder.

Fix: one mechanical commit - normalize the three mixed files to their dominant
ending and drop the trailing blank lines. Decide the folder-wide convention
separately (CRLF per `AGENTS.md` means touching 37 files, which belongs in its
own commit or not at all); nothing else in the same commit, so review stays
readable.

### R2 (medium) `With.scala`'s field list has drifted out of its own alignment

`src/Lifecycle/With.scala:94`

```scala
  var productionHistory : ProductionHistory   = _   // 75 - breaks alphabetical order
  var missionHistory      : MissionHistory      = _ // 94 - extra space, wrong column
  var matchups          : MatchupGraph        = _
  var microSlush        : MicroSlush          = _
```

Three separate drifts: an off-by-two space on `missionHistory`, a
`productionHistory` at line 75 sitting between `battles` and `bullets` instead of
next to `missionHistory`, and `mapClock` among the map dimensions. In a 73-line
aligned block, one wrong column makes the whole list look untended (and the next
editor copies the wrong pattern).

Fix: re-align the block and put the `productionHistory` line in its alphabetical
place. Pure whitespace, so it belongs in its own commit under `AGENTS.md`'s rule.

### R3 (medium) `EridanWave.onFrame` is 30 statements with one rule written three times

`src/Lifecycle/EridanWave.scala:70-78`

```scala
if ( ! Keyboard.GameCommands.isBotPaused) MapSpecificCommander.onFrame()
if ( ! Keyboard.GameCommands.isBotPaused) UmsMode.onFrame()
...
if ( ! Keyboard.GameCommands.isBotPaused) With.tasks.run(With.performance.msBeforeTarget)
```

The comment above (lines 65-69) explains the parked/not-parked split, then the
guard is repeated per call. A reader has to diff three lines to learn what is
parked.

Fix: one `if (!isBotPaused) { parkable() }` block containing all three, with the
comment above the block. Same behaviour, and the rule is stated once.

### R4 (medium) `getOrDefault` swallows type mismatches

`src/Lifecycle/Configure/ConfigurationLoader.scala:22-39`

```scala
val attemptedOutput: T = default match {
  case _: Boolean => if (value.isBoolean) value.asBoolean().asInstanceOf[T] else default
  ...
}
if (attemptedOutput == null) { return default }
```

A JSON file with `"framemstarget": "25"` (a string) silently keeps 20, with no
warning, which is the kind of typo a config file invites. The `null` check after
`asInstanceOf[T]` over a primitive default can never fire for `Boolean`/`Int`/
`Double` and hides that the code is really checking mjson's own null.

Fix: return `Option[T]` from the match and warn on a present-but-wrong-type
value (`"Ignoring framemstarget (want a number)"`), matching the existing
`boolFlagOverride` warning style two methods below - that method is the good
version of this idea.

### R5 (medium) Dead API and dead state in the folder

| What | Where | Status |
|---|---|---|
| `We` (whole object, 13 lines) | `Lifecycle/We.scala` | 0 call sites |
| `Enemy` (whole object, 19 lines) | `Lifecycle/Enemy.scala` | 0 call sites |
| `Configuration.forcedStrategy` | `Configure/Configuration.scala:42` | never read |
| `Configuration.logTaskDuration` | `Configure/Configuration.scala:63` | never read |
| `Configuration.visualizeFun` | `Configure/Configuration.scala:23` | only written |
| `EridanEventListener.lastException` / `lastStackTrace` | `EridanEventListener.scala:9-10` | write-only (see B2) |
| `ProcessHelper.isProcessRunning` | `Launch/ProcessHelper.java:48` | 0 callers |
| `Manners.debugChat` | `Manners.scala:33` | 0 callers |

`We` and `Enemy` are Atlantis-parity helpers that nothing adopted (the codebase
spells it `With.self.isTerran` / `With.enemies.exists(_.isTerran)` instead, e.g.
`Micro/Agency/Commander*.scala`). Keeping them costs a reader a detour into a
file that promises a convention the rest of the tree does not follow - either
adopt them or delete them; leaving both is the worst option.

### R6 (low) `Main.main` does a Java null-check idiom in Scala

`src/Lifecycle/Main.scala:47`

```scala
if (launch.getTuningStrategy != null && launch.getTuningStrategy.nonEmpty) {
```

`getTuningStrategy` returns `null` or a Java `String`; `.nonEmpty` on the Scala
side already handles `null` (`StringOps.nonEmpty` is a null-safe call on a
nullable).

Fix: give `LaunchConfig` an `Optional<String>`-shaped accessor, or wrap once at
the top of `main`: `Option(launch.getTuningStrategy).filter(_.nonEmpty)`. The
same shape appears three lines below for `getTuneParams`.

### R7 (low) `MapSpecificCommander`'s comment wall

83 of 316 lines are `//` - mostly ported Atlantis camera code
(`TODO(EridanWave painter)` repeated 25 times). The TODO markers are useful; the
dead camera calls they surround are noise. Fix: keep one TODO per map family
(or a single list at the top) and delete the commented bodies; git has them.

---

## 6. Concrete bugs found while reading

Ordered by likelihood of showing up in a game.

### B1 (medium) A Linux supervisor JVM with `WINEPREFIX` in its environment never starts the game

`src/Lifecycle/Launch/GameLauncherFactory.java:33,93` - see O1 for the analysis.
A user who exports `WINEPREFIX` in their shell (standard practice for Wine) and
launches from the IDE gets `WineClientGameLauncher`, which prints
*"the game must already run under Wine"*, patches `bwapi.ini` and does nothing
else. Fix: `isWineClientJvm() || ProcessHelper.isWindows()`.

### B2 (medium) A failed `onStart` is completely silent

`src/Lifecycle/EridanEventListener.scala:13-26`

```scala
catch { case exception: Exception =>
  lastException = Some(exception)
  lastStackTrace = Some(exception.getStackTrace.mkString(System.lineSeparator()))
}
```

No print, no logger. `lastException`/`lastStackTrace` have no reader anywhere in
the tree, so an exception during `With.onStart` (a map with no units to
`maxBy` on line 150, a missing `bwapi-data`, BWTA failure outside the retry) is
stored in two write-only fields and the process then sits in a game that never
does anything. From the outside that is a hang, not a crash - the hardest
failure to debug, and the folder spent care elsewhere to make exactly this
visible (`GameSummary` on stdout, `ErrorLedger`, `"[launcher]"` prefixes).

Fix: print it like every other launcher failure
(`System.err.println("[launcher] Startup failed: " + exception)`, stack to
stderr) and/or `Debugging.ErrorLedger`. Then either keep the fields private or
delete them.

### B3 (low) `enginePauseNoticed` is never reset between games

`src/Lifecycle/EridanWave.scala:109,126` - see L5. A game that ends while paused
from outside leaves the flag `true`; the next game ignores its first foreign
pause. One line in `onStart`.

### B4 (low) Chat speed presets desync the ladder index

`src/Debugging/KeyboardCommands.scala:38-41` - `With.game.setLocalSpeed(24 * 32)`
(and `24*8`, `24`, `0`) bypass `GameCommands`, so `speedIndex` still points at
whatever the last key press selected. The next `+`/`-` (or `setFrameSkipOnly`,
which reads `speedLadder(speedIndex)`) then applies a rung nobody chose. The
`"speed <ms>"` command right below it does the right thing. Fix: route the four
presets through `GameCommands.setSpeedOnly`.

### B5 (low) `GameCommands.guiEnabled` is a cache that diverges from the engine

`src/Lifecycle/Keyboard/GameCommands.scala:15,119`

`guiEnabled` starts `true`, but the headless path calls
`With.game.setGUI(false)` directly (`EridanWave.scala:30`). The first "0" press
computes `guiEnabled = false` and calls `setGUI(false)` - a no-op that looks
like a broken key. `botPaused` got the correct treatment (it reads
`With.game.isPaused` instead of caching, line 108); `guiEnabled` did not.

Fix: derive it from the engine (`With.game.getGUI` is not available in all
BWAPI builds, so track it in one place: give `setPainting`/headless the same
setter and never write the field from outside `GameCommands`).

### B6 (low) `MapSpecificCommander` can sleep 50 ms inside the frame budget

`src/Lifecycle/MapSpecificCommander.scala:313` ->
`GameCommands.setSpeedOnly` -> `applySpeedAndFrameSkip` ->
`Thread.sleep(SpeedDelayMs)` twice (`GameCommands.scala:37-45`).

The 25 ms bracket per call is a measured StarCraft requirement, and it is fine
from a keyboard thread or `onStart`. From `onFrame` (frame 0-1 of every map with
a tuning row) it spends 50 ms of a 20 ms frame target
(`Configuration.frameTargetMs`), which is exactly when the first decisions are
being made. Fix: apply `initialSpeed` from `EridanWave.onStart` (where the other
start-time pacing already lives) instead of the first `onFrame`, or make the
sleep conditional on not being on the game thread.

### B7 (low) `HitCounter` cannot see the frame a unit dies

`src/Lifecycle/HitCounter.scala:27`

`units.view.filter(_.alive)` drops a unit the moment it dies, so the final hit
points never appear in `current` and the killing blow is never counted - a
one-shot from full HP records zero hits, which is closer to the old
`isUnderAttack` behaviour than the docstring ("damage ticks count every frame
the shot lands") claims. The pure kernel is right; the input snapshots are the
problem. Fix: keep dead units' last known hp in the map and count the drop to
zero when a unit disappears, or document the exclusion.

---

## 7. What the folder does well (do not regress)

- **`GameLauncher` + `GameLauncherFactory` + 4 backends**: one decision point,
  no backend branching in game logic, and the routing rules documented on each
  class. Copy this shape for other dispatch chains (`.ai/review-d.md` S4).
- **`EridanEventListener` -> `EridanWave` -> `With`**: the adapter is 47 lines
  and has no game knowledge; it catches per callback so a bot exception cannot
  kill the BWAPI thread (`tryCatch`, line 181). `onEnd` even guards the stale
  score-screen case (`With.frame > 120`).
- **`Keyboard` / `GameCommands` split**: Java classes are one-line delegates,
  all game access and `With.game == null` guards live in Scala. The stated
  intent (one place for shortcut behaviour) is right; D4 is the drift from it.
- **`GameSummary` and `HitCounter.countHits`**: exact strings and a pure
  transition function, each with a suite. The `CountHits`/`box` shape is what
  the rest of this folder should imitate when it grows logic.
- **`UmsMode`'s design**: detect once at start, persist the correction for the
  next game (patching `bwapi.ini`), then a two-line per-frame nudge with a
  cached enemy start. The three responsibilities are separated by method and
  each has a comment saying why.
- **Comments that carry *why*, not *what***: the Wine path-rendezvous note in
  `KeyRelay`, the `-x` vs `-f` note in `ProcessHelper:32-34`, the duplicate-press
  window in `Keyboard`. These are the parts a future reader cannot reconstruct
  from the code, and they are all written down.

---

## 8. Suggested order

1. **R1 line endings** - mechanical, one commit, no review risk. Do it first so
   the later diffs are readable.
2. **B2 (silent startup failure)** and **B1 (Wine routing)** - two small fixes
   that turn the folder's worst failure modes into visible ones. Add
   **B3** (`enginePauseNoticed`) to the same commit: all three are one-liners.
3. **D1 `LaunchPaths`** - removes the duplicated path derivation and 13 legacy
   env spellings, and gives `BwapiIni`/`KeyRelay` testable seams. Unblocks D2.
4. **L5 `PerGame` reset registry** - stops the manual reset list from growing;
   `EridanWave.onStart` drops by 10 lines.
5. **S2 + D2 map catalog / tuning table** - the single biggest readability win
   in the folder (316 -> ~120 lines), and it merges four UMS answers into one.
6. **L2 + S3 + S4 + O3 configuration** - immutable `Configuration` + table-driven
   loader. Do this in one go; the parts are only cheap together.
7. **L1 `WorldState`** - the invasive one. Worth it because it makes `TestKit`
   honest and gives "who may write `With`" an answer, but nothing above depends
   on it.
8. **L4 `Bootstrap`/`Systems`**, **S1 `TerrainAnalysis`**, **S5/D6/S6/S7 splits**,
   **R2/R3/R7 tidying**, **O6 lambda keymap** - steady-state cleanup, no
   ordering constraints.
9. **R5 dead API** (`We`, `Enemy`, three config fields, `isProcessRunning`,
   `debugChat`) - trivial, but should ride along with the commit that touches
   its area rather than stand alone as a delete-only change.

---

## 9. Not findings

Things that look like problems and are not, so nobody "fixes" them:

- **`System.out.println` in the launcher classes.** Deliberate: the launcher runs
  before `With.logger` exists and `scripts/wine-run.sh` tees client stdout. Only
  D7's *ad hoc* mix is worth improving, not the channel itself.
- **`LaunchConfig`'s 43 getters.** Java, no records on the target JVM; the
  boilerplate is the price of a parsed-args object. D5 targets the duplicated
  help text, not the getters.
- **`With` being a global.** A 3 900-site service locator is a loaded gun, but
  replacing it is a tree-wide rewrite with no behavioural payoff; L1 only asks
  for the writable half to be separated from the readable half.
- **Commented-out Atlantis camera code in `MapSpecificCommander`.** Porting
  practice per `conventions.md` §6 ("port decisions, not plumbing"); R7 asks for
  compression, not deletion of the TODO markers.
- **`ProcessHelper`'s empty `catch (Exception ignored)` blocks.** Intentional:
  kill/start calls must never break the launcher.
- **`BwapiData.getWorkingDirectoryDeJure` walking up to `bwapi-data`.** Ugly but
  correct for the Wine layout, and guarded by a catch-all that falls back to
  `user.dir`. Leave it.
- **`Main`'s static `memoryFree`/`framesToBuffer` math.** Runs once at class load and
  encodes the JBWAPI frame-buffer sizing rule; not a candidate for extraction.
