# Atlantis StarEngine: what it could do, and what we borrow

Comparison of the Atlantis test engine
(`/sc-ai/Atlantis/_AI/redesign/06_STARENGINE.md`, a plan, plus the
partially built `src/tests/unit/sim/` tree) against ours
(`tests-starengine/`). Read this before growing our engine: the
Atlantis list is a menu, not a mandate. Our engine stays the FAST,
micro-only harness; anything below marked LATER needs a test that
cannot pass without it.

## What Atlantis built or planned

| Atlantis piece | What it does |
|---|---|
| Static map snapshot (`taucross-snapshot.json`, dumped from a live game via `MAP_SNAPSHOT=1`) | 128x128 walkability tiles, 21 regions + neighbours, 21 chokes (center, width, ends, regions), 93 minerals + 9 geysers with amounts, 3 starts, preplaced units. Same class reads and writes (no schema drift). |
| `SimMap` loader | Installs regions + walkable/buildable/explored tiles, spawns minerals/geysers/preplaced units. |
| `SimWorld` fight stepper | Movement with acceleration/braking/turning, attack cycles from engine data, damage. Combat only, zero economy. |
| `SimConfig` FAST vs FAITHFUL | FAST: straight-line movement, flat mining income, accept-all-or-check placement, no combat detail. FAITHFUL: full mover, trips, full placement check. One codebase, three divergence points. |
| Order validators per verb | `SimOrder` value with ACCEPTED/REJECTED + reason (placement rules 1-8 from OpenBW: footprint, builder reachability, refinery/psi/creep/tile flags, occupancy, affordability). New verb = new validator, driver untouched. |
| Bot seams | Calls real bot code (`ProductionEngine`, `ConstructionRequests`) with no `if (isSim)` in production. |
| Calibration rule | Numbers from BWAPI tables + Wine logs (banks, warp frames); never from memory. Sim-vs-Wine parity within tolerance, or the sim is wrong. |
| Scope discipline | Explicit bans: no second OpenBW, no fidelity trap, no tests-that-test-nothing, sim state cleanup between tests. |

## What we already have (equivalents)

| Atlantis idea | Our version |
|---|---|
| Real engine numbers, never from memory | `Body` reads `UnitClass` tables (speeds, reaches, damage, cooldowns); no hardcoded movement numbers. Storm cost/damage/box/radius and cast frames cite their BWAPI/iScript sources. |
| Seams over forks, pilots decide | Pilots call the shipped kernels (`PsionicStorm.veto*`, `DarkTemplarTargets.pick`, `AdvantageBackoff` standoff) instead of copying policy; engine owns the loop only. |
| Scenario = setup + scripted enemy + assertions | Every `*Suite` under `tests-starengine/`: fixed geometry, deterministic foe scripts, DIAG lines with real numbers. |
| Batch runner | `scripts/starengine.sh [Suite...]` ad hoc; default suite excludes engine tests by tag. |
| No tests-that-test-nothing | Repro suites assert the behavior change (latch calms the flap, lead lands), not just "runs". |

## What we deliberately do NOT have (and why)

- **Economy/mining/production/construction**: our domain is micro UMS validation. Nothing we test builds anything.
- **Acceleration/braking/turning, projectiles, boarding**: instant stops, instant damage, no scarab flight, no loading. The order-change cost (`reissues`) and the cast-delay model the timing effects that matter; the rest is speed we keep.
- **FAITHFUL mode**: one engine, one (fast) fidelity. A second mode is a second engine to maintain.
- **Fog of war**: UMS tests see everything; no hidden-information tests exist.
- **Real BWAPI order validation** (placement rules, reachability): no build orders in our tests.

## Worth borrowing later, with triggers

1. **Static map snapshot (positions + walkability) — TRIGGER: a test that needs walls/chokes/regions.** Today our scenarios run on an open plane; 90% of micro questions do not need terrain. If a test ever needs it (corner routing, choke holding, unwalkable blocking a flee), dump that map's tiles + unit placement once and load it per-test. Do NOT preload the whole ladder "just in case".
2. **Snapshot = map, never gameplay.** If we snapshot UMS maps, store only statics (tiles, starts, preplaced units). Queues, HP, cooldowns, orders stay runtime state created by playing — otherwise we test old replays, not the bot.
3. **Order validator per verb.** We have no build/gather/train verbs in-engine and no plans for any. Revisit only if economy tests ever exist.
4. **Fog-biased flee destinations** (their `goElsewhere` picks invisible spots). Our corner guard covers the trap case; fog preference needs a game proving it helps.

## Non-negotiables (shared with Atlantis)

- Engine numbers come from the game/BWAPI tables, never from memory.
- New mechanics arrive as new pilots/constraints/directives behind stable seams (OCP); the loop stays shut.
- Scenarios run ad hoc and singly; the default suite never pays for them.
- Keep it fast enough to run while thinking, or it stops being used.
