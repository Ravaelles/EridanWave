# Tactic module refactor plan

Scope: `src/Tactic/` (78 files). Goals: SRP per unit, OCP extension points,
less coupling, test seams for complex rules. Folders group by functionality;
no folder keeps stray files unless it is the namespace entry itself.
Process: one step at a time, green build plus tests plus gate, commit each.

## Conventions for this refactor

- Moves are `git mv` plus package plus import updates; no logic changes
  inside a move commit (logic changes get their own commits).
- New pure kernels go next to callers with `*Suite` next to code convention
  (`src/test/scala` mirrors `src/`).
- Kill dead code found on the way (dead hooks, commented experiments) in
  the same commit, listed explicitly.

## Step 0 — Root (Tactic/) [DONE, commit 43c403533]

- `Tactician.scala` (238): registration plus defense assignment plus attack
  fill plus workers plus SCVs. Split `runCoreTactics` into named phases
  (defense divisions, special defenses, attack fill, fallback, workers, SCVs).
- `Assignment.scala` (67): two assignment algorithms; cohesive, stays.
  Maybe move to a new `Tactic/Assignment/`? No — single file, stays loose.
- `DefenseDivision.scala` (47) + `GetDefenseBase.scala` (9): defense
  helpers used only by Tactician. Move both to new `Tactic/Defense/`.
- Loose leftovers: `Tactician.scala`, `Assignment.scala` (2, entry + helper).

## Step 1 — Missions/ (11 files) [DONE, commit 2423101e8]

- Deleted dead `Mission.onRun` hook, unused `nextUnits` helper and the
  all-TODO `MissionRecall` stub (zero references).
- New `Missions/core/` (`Mission`, `MissionDrop`), `routing/`
  (`DropManifest`, `DropRoute`), `drops/` (DT/Speedlot/Storm),
  `reaver/` (ReaverDrop + AirSafety), `ground/` (KillExpansion).
- No new suites: the pure helpers need game-free unit stubs first
  (`Tile` needs map dims, `FriendlyUnitInfo` has no test harness).
- New `Missions/core/`: `Mission.scala`, `MissionDrop.scala`.
- New `Missions/routing/`: `DropManifest.scala`, `DropRoute.scala`.
- New `Missions/drops/`: `MissionDTDrop`, `MissionSpeedlotDrop`,
  `MissionStormDrop` (triplicated recruit/raid-release shape documented;
  shared intermediate only if trivial).
- New `Missions/reaver/`: `MissionReaverDrop.scala`,
  `ReaverDropAirSafety.scala`.
- New `Missions/ground/`: `MissionKillExpansion.scala`
  (+ `MissionRecall.scala` if kept, else delete).
- Test seams (new suites): `DropManifest` matcher (pure, stub units),
  `DropRoute.flybys` geometry, `MissionReaverDrop` companion targeting
  helpers, `MissionKillExpansion` eligibility/sizing.

## Step 2 — Squads/ support (15 files) [DONE, commit 704d89811]

- New `Squads/Decisions/`: the 5 pure files plus `SquadMarch`/`SquadRally`
  (+ mirrored suites). `SquadAutomation` stays loose (single shared engine).
- New `Squads/Group/`: the `UnitGroup` lattice (splitting the trait itself
  would risk caching semantics; the folder move is structural only).
- `Qualities/` stays as-is; `QualityCounterSuite` skipped (needs unit stubs).
- `Squads.scala` (scheduler) stays loose.

## Step 3 — Squad* behavior classes (13 files) [RESEARCH PENDING]

## Step 4 — Production/ (9 files) [RESEARCH PENDING]

## Step 5 — Tactics/ (done earlier, verify nothing stray)

`Tactic.scala` + `TacticMeldArchons` + `TacticMeldDarchons` loose (3, entry
plus meld pair); Cheese/Defense/Economy/Scouting/Monitor/ done.

## Verification per step

`python3 scripts/check-blank-lines.py` on touched trees, `mvn -o -Ptests
test` (MAVEN_OPTS add-opens from build-jar.sh:30), `./build-jar.sh`,
commit. Never push.
