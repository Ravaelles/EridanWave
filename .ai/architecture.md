# Architecture consensus

New code follows this file; it outranks local habits.

## 1. Squad WHERE, unit HOW

- Squad: destination, continuity, roles - vicinity, latch, centroid,
  scouts, bombs (`SquadAcePilots.chooseActivity`, missions).
- Unit: execution in contact - duels, cooldowns, holds, boarding.
- Cluster concepts (centroid, latch, shared targets) live only at
  squad level.

## 2. Arbitration: first-claims chain

Ordered behaviors, first `applies` wins (one `*Behavior` object per
decision, orchestrated by `CombatEngagement.engage`). Order is the
contract and reads as documentation.

## 3. Override contract (unit over squad)

The unit brain may redirect iff it takes targets (preferring the
squad's own), makes a bounded local adjustment (holds, dances,
offsets), or runs an explicit time-boxed scout task with a staleness
gate. Never widen the search to a farther/staler target. Best case:
the unit never touches destination at all.

## 4. Testability

Branch with 2+ thresholds -> pure function with a suite (`KiteAway`,
`ReaverBoardLogic`, ...). Game-coupled reads go through a snapshot
(`ReaverSituation.read`). Squad choosers and missions stay untested
but thin; kernels go pure. `testkit.TestKit` seeds a game-free
world; matchups/battles stay game-only - next seam on demand.

## 5. Layer guide

Gameplan (tech/army) -> squad activity (where) -> unit behavior
(how) -> missions (multi-unit jobs with locks, drops only).
Shared combat vocabulary lives in `Combat` + `Decisioning/Combat/`.
`Be*` = dispatched Actions, `*Behavior` = engage helpers,
`*Brain` = first-claims chain.

## 6. From Atlantis: decisions, not plumbing

Port the decision (unload safety, hold-to-fire, tank evac), keep our
plumbing. Risky rewrites hide behind a Strategy switch with the old
code intact and the new behavior default.

## 7. New behavior = new object + dispatch line + contract comment

One `*Behavior` object per decision (when it fires, what it yields
to). Split methods past 15 lines. No new run-everything lists.
