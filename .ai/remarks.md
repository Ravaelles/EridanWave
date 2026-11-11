# Owner remarks (living list, newest last)
#
# History lives in git log; this file keeps only open items.
# Cleaned 2026-11-11: all DONE lines retired (contain hold, 55/45 split,
# SquadBaseGuards, Reaver latch/gas, ferry/shuttle/dragoon gates, muta
# cannons, FewZealots, hold-to-shoot, behavior split, shuttle run-home,
# scbw pairing race, DT audit, burrowed expo).

## Open (code-ready, no owner decision needed)
- Evacuate transports share the travel hold pattern (Retreat + hold while
  waiting for pickup rescue). Travel was fixed to run home under fire;
  evacuate left alone on purpose (rescue intent). Revisit on game evidence.

## Game-gated watch (needs a game, not code)
- Retreat 160 deadband: kept with rationale; revisit on game evidence.
- Long regroup verdicts: 5-minute Regrouping with 74-84 strength observed
  (Monster 28-minute game); latch is sane, sims need observation, no blind
  tuning. Log sim verdicts next time before touching.
- Creep Colony acceptance (see todo.md): needs game verification.
- 4th/5th base progression: expected solved by shifted base, watch.
- Single unexplained suite flake before three 290/290 greens: watch.

## Parked / blocked
- Sim-minus-statics: no contain code in tree, parked.
- Atlantis suite ports (see todo.md): blocked on a UnitInfo stub harness.
- Replay reader Tier 1 (see replay-reader.md): planned, unbuilt.
- Merge review f/new-drop vs develop: DONE (`08ee6ae2e`, two review
  passes, equivalence re-proved, jar rebuilt). Branch is merge-ready.
