# Distance hygiene (owner direction: tiles everywhere)

Convert `.pixelDistance` uses to tiles where possible
(`tileDistanceEdge/Center`, `Pixel.tx/ty`, `Maff.div32`). Exceptions:
pixel construction, genuinely mixed range math, hot sim loops. Count
with `grep -rn "\.pixelDistance" src --include=*.scala | wc -l`; keep
suites green, no behavior change.

# Targeting parity with Atlantis — test roadmap

Pin pure kernels beside the caller with a `*Suite`
(`TargetScoring.damageForSize`, `creepColonyBonus` — done).
Game-coupled strata stay thin and delegate. `testkit.TestKit` covers
geometry/static math (DONE 2026-11-11); matchups/battles stay
game-only. Next seam on demand: per-stratum MatchupSnapshot views.
Atlantis ports (NEXT #184/#185, need a stub harness):
`TargetLadderTest` (ladder order), `AcquireTargetTest` (top of
ladder), `MeleeChewerTargetTest` (weakest-in-range vs keep-current),
`TargetPlacementNearTest`, `UndetectedTargetTest`, `ChaseVetoTest`,
`ChaseVetoFollowsDecidedPostureTest`, `FocusFireTest`.

## Creep Colony acceptance (the repeated miss, needs a game)

Morphing colony (future Sunken) over idle; fighters still outrank
colonies; Reavers still skip non-combat buildings (`TargetFilterReaver`).
Both combat states intended: out of combat the bonus dominates, in
combat fighters outrank it — neither may idle past a standing colony.

# SRP pass on the longest files

`docs/longest-files.md` lists the 20 longest sources. Done:
`UnitClass` tables, `PlacementCacheFormat.writeWall`, three
`Tactician` methods, `BeRushingZealot` split. Next cheapest first:
`WallFinder.canPlace/scoreWall`, `SimulationGrid.applySplash`,
`Debugging/*` painters; controllers (`MacroSim.simulate`,
`PvZ1BaseReactive.buildTech`, `SquadDefendBase.run`) need a game per cut.

# ReaverLegacy removal criterion

Two disciplines behind `With.blackboard.reaverAtlantisBehavior`.
Decide once: if Atlantis holds through the next games with no lost
Reaver shuttle, fold `ReaverLegacy` into `Reaver/Legacy`. Details:
`src/Micro/Actions/Protoss/__REVIEW.md`.

# SquadAutomation shell deletion target

`Tactic.Squads.SquadAutomation` is a forwarder shell over
`SquadTargeting`, `SquadForming` and `MarchOrders` (41 call sites).
Migrate the callers to the halves and delete the shell; until then
new code must call the halves, never the shell.
