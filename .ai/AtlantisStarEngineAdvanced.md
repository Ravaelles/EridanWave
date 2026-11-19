# Atlantis StarEngine: what to borrow (menu, not mandate)

Ours stays the FAST micro-only harness. Anything below needs a test
that cannot pass without it.

## What Atlantis built

- Static map snapshot (tiles, regions, chokes, minerals, starts,
  preplaced units) + `SimMap` loader.
- `SimWorld` fight stepper (movement, attack cycles, damage).
  No economy.
- `SimConfig` FAST vs FAITHFUL (one codebase, three divergence
  points); per-verb order validators; bot seams calling real code;
  numbers from BWAPI tables + Wine logs, never memory; no second
  OpenBW, no fidelity trap.

## What we already have

- Real engine numbers (`Body` reads `UnitClass` tables).
- Seams over forks: pilots call shipped kernels, engine owns the
  loop only.
- Scenario = setup + scripted foe + assertions (`tests-starengine/`);
  ad hoc runner, default suite never pays.
- No tests-that-test-nothing: repros assert the behavior change.

## What we deliberately lack (and why)

No economy/production, no accel/braking/projectiles/boarding
(instant stops/damage; timing via `reissues` + cast-delay), no
FAITHFUL mode, no fog of war, no build-order validation.

## Worth borrowing later, with triggers

1. Static map snapshot - TRIGGER: a test needing walls/chokes
   (open plane covers 90% of micro questions today). Statics only;
   never store gameplay.
2. Fog-biased flee destinations - needs a game proving it helps.
