# Architecture consensus

How we structure unit control, what we promote, and the contract between
levels. Decisions here outrank local habits; new code follows this file.

## 1. Primary decomposition: squad WHERE, unit HOW

- **Squad** owns destination, continuity and roles: vicinity, latch, centroid,
  who scouts, who bombs (`SquadAcePilots.chooseActivity`, missions).
- **Unit** owns execution in contact: duels, cooldowns, holds, boarding
  (`CorsairBrain`, `ReaverAtlantisBrain`).
- Neither level alone works: squad-only wastes flight time on stale info,
  unit-only commutes map-wide (ghost chases). Cluster concepts (centroid,
  latch, shared targets) live only at squad level.

## 2. Arbitration inside each level: first-claims chain

Ordered behaviors, first `applies` wins (`CorsairBrain`, `ReaverAtlantisBrain`,
activity early-returns in `chooseActivity`). Order is the contract and reads
as documentation. This complements §1, it does not compete with it.

## 3. Override contract (unit over squad)

The unit brain runs after the squad activity and may redirect, iff:

1. **It takes targets** (real or remembered contact): hunt, dance, duel.
   Prefer the squad's own targets first (`CorsairDanceToOverlord` reads
   `squad.targets` before picking its own).
2. **Or bounded local adjustment**: holds, dances, relocate offsets.
3. **Or an explicit time-boxed scouting task** with a staleness gate
   (`CorsairExploreEnemyMain`: 9 s per 30 s, unseen-main gate).
4. **Never widen the search**: no override to a farther or staler target
   than the squad's. `CorsairHuntKnownOverlords` (sorted by our main,
   yanks across the map) is the anti-example to fix, not to copy.

Best case: the unit never touches destination at all (Reaver chain only
holds/loads/unloads via `Commander`).

## 4. Testability rule

- Numeric decisions become pure functions with suites (`HuntLogic`,
  `AceExploreLogic`, `KiteAway`, `ReaverBoardLogic`, `ReaverUnloadSafety`,
  `ReaverDropAirSafety`). Rule of thumb: a branch with 2+ thresholds
  deserves a pure kernel.
- Game-coupled chains read the world once into a snapshot
  (`ReaverSituation.read`) so behaviors take facts, not the game.
- Squad choosers and missions stay untested (game-coupled) but thin;
  their kernels go pure (§4, first bullet).

## 5. Layer guide (which architecture for what)

- **Gameplan/strategy**: what tech and army (`Gameplans`, `Strategery`).
- **Squad activity**: where the cluster goes (`Tactic.Squads`).
- **Unit behavior**: how in contact (`Micro.Corsair`, Reaver chain).
- **Mission**: coordinated multi-unit jobs with locks/itineraries
  (`Tactic.Missions`, drops only). No new state machines elsewhere.
- **Shared combat pipeline** (`Combat` facade + grouped delegates in
  `Decisioning/Combat/`): vocabulary and dispatch, never unit specifics.

## 6. From Atlantis: port decisions, not plumbing

Take the decision (unload safety, hold-to-fire, tank evac), keep our
plumbing (targeting, pipeline, missions). Do not import the Manager base,
`Alpha` globals, or magic numbers inline. Risky rewrites hide behind a
Strategy switch with the old code intact and the new behavior default
(`BeReaver` dispatcher, blackboard flag).

## 7. Adding a behavior

New object + one line in the chain + pure kernel and suite where numbers
decide. No new entries in run-everything lists (`Fight.perform` is
grandfathered, not a pattern).
