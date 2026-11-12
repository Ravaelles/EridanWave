# Distance hygiene (owner direction: tiles everywhere)

Review every `.pixelDistance` use in `src/` and convert to tiles where
possible (`tileDistanceEdge/Center` on units, `tileDistance` on pixels,
`Pixel.tx/ty`, `Maff.div32` for the odd division). Wherever a raw `32`
appears, prefer the tile form - it reads better and kills a whole class
of factor-32 mixups (rule 10 in `.ai/conventions.md`). Exceptions stay:
pixel construction (projections, order radii, grid indexing), mixed
range math that genuinely mixes pixels and tiles, and hot sim loops
where the conversion itself costs. Count before/after with
`grep -rn "\.pixelDistance" src --include=*.scala | wc -l`; convert,
keep suites green, no behavior change by construction.

# Targeting parity with Atlantis — test roadmap

Our picker is `Micro/Targeting/Target.scala` (strata + `TargetScoring`);
Atlantis picks through the ordered ladder in
`Atlantis/src/atlantis/combat/targeting/TargetLadder.java`
(chewer, keepCurrent, weakestInRange, sticky, shared, cover, nearest,
breach) with the tiers in `src-old/.../battle/targeting/generic/
ATargetingImportant.java`. `testkit.TestKit` covers geometry, static
class math and player-free fields; the strata themselves still need
per-stratum MatchupSnapshot seams. What we pin instead, per rule:

- Pure kernels beside the caller with a `*Suite` next to code
  (`TargetScoring.damageForSize`, `creepColonyBonus` — done).
- Game-coupled strata stay thin and delegate to the kernels.

## TestKit (DONE 2026-11-11: Mockito test-scope dep + testkit.TestKit
seedWorld/mockUnit/friendly; TileSuite + CombatUnitSuite green). Boundary:
geometry, static class math and player-free field math are testable;
live fields via readProxy need grids->performance->geography (map data),
and matchups/battles need a world - those stay game-only. Next seam on
demand: per-stratum MatchupSnapshot data views, never a big rewrite.

## Atlantis tests to port (owner direction NEXT #184/#185)

- `src/tests/unit/TargetLadderTest.java` — ladder order is explicit and
  stable; chewer beats keep-current beats weakest; no silent reordering.
  Ours: pin any reorder of `Target.best`/`defaultTargets` strata the
  same way once a stub harness allows it.
- `src/tests/unit/AcquireTargetTest.java` — top-of-ladder behavior.
- `src/tests/unit/MeleeChewerTargetTest.java` — weakest-in-range over
  the committed target when the shot is ready; keep-current while
  cooling. Ours: `acceleratesDemise` strata in `Target.best`.
- `src/tests/unit/TargetPlacementNearTest.java`,
  `UndetectedTargetTest.java` — geometry and cloaked handling.
- `ChaseVetoTest.java`, `ChaseVetoFollowsDecidedPostureTest.java`,
  `FocusFireTest.java` — chase discipline and focus.

## Creep Colony acceptance (the repeated miss)

Atlantis ranks Creep Colonies as their own tier (`Targ_CB_CreepC`:
in 8 radius, `C5d`: buildings in 7) between combat buildings and
ordinary buildings. Ours lifts them in `TargetScoring.apply` via
`creepColonyBonus` (morphing 14, idle 8, below fighters at combat
12). Verify in a game:

- Army finishing other buildings then engages a standing Creep Colony
  instead of idling past it.
- A morphing colony (future Sunken) is picked over an idle one.
- Fighters still outrank colonies; Reavers still skip non-combat
  buildings entirely (see `TargetFilterReaver`).
- Check both combat states: out of combat the colony bonus dominates,
  in combat fighters outrank it (slow scoring flips +-12 with action
  match) - both are intended, neither may idle past a standing colony.

# SRP pass on the longest files

`docs/longest-files.md` lists the 20 longest sources with every method over
20 lines and what is done. Done so far: `UnitClass` build tables per race,
`PlacementCacheFormat.writeWall`, three of `Tactician`'s methods, and
`BeRushingZealot` (split into `Zealot/ZealotRush*`). Next, cheapest first:
`WallFinder.canPlace/scoreWall`, `SimulationGrid.applySplash`, then the
`Debugging/*` painters; the game-state controllers (`MacroSim.simulate`,
`PvZ1BaseReactive.buildTech`, `SquadDefendBase.run`) need a game per cut.

# ReaverLegacy removal criterion

Two Reaver disciplines run side by side behind
`With.blackboard.reaverAtlantisBehavior`. Decide once: if Atlantis drops hold
through the next batch of games without a lost Reaver shuttle, fold
`ReaverLegacy` into `Reaver/Legacy` and stop maintaining the switch.
`src/Micro/Actions/Protoss/__REVIEW.md` has the details.
