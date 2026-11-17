# Unified retreat verdict (deferred rewrite)

Tracked in: `.ai/queue.md` (last section, "Unified retreat verdict (deferred)").
Status: NOT STARTED - deliberate decision, do not implement without
re-reading this file and confirming with the owner. Per-site fixes
continue in the meantime; each closes one pair of disagreeing readers
(see Evidence).

## Problem

Retreat is decided in several places from overlapping but different
inputs, on different frames, and the phases can claim opposite things
in the same frame: the march sends a unit home while combat duels it,
or combat retreats while the march marches it at the goal. Concrete
sightings (all per-unit, same-frame March label + Act chain):

- `Retreating` / `Marching home` + `Abuse` (poke dance while leaving)
- `Retreating` / `Marching home` + `Approach`, `Engage`, `Chase`
  (closing in / charging while leaving)
- `LoneMarch` + `Retreat*` (marching at the goal while fleeing)
- duplicate same-frame `March` notes (two writers, re-squadding)

History shows every one of these turned into a long hunt: each fix
closed one pair of readers and the next pair flapped instead, because
no single place owns the answer to "is this unit retreating".

## Evidence (live games, Benzene PvP, f/phase-f)

- Baseline with per-leg gates only: 144 mixed frames.
- After march-verdict-technique parity + Abuse-under-retreat ban +
  single march writer + RetreatAllowed gating: 8, then 7 mixed frames
  per game, duplicates consistently 0.
- Remaining mixes all share one shape: the march label and the combat
  act are computed from inputs that disagree across the squad/unit
  phase boundary (stale `shouldFight` vs fresh verdict, squad posture
  vs individual verdict, latched technique vs fresh march).

In other words the per-site fixes work, but the surface they patrol
is combinatorial: every new behavior re-opens the question.

## Proposal (not implemented)

One verdict object per unit per frame, computed ONCE, read everywhere:

- `RetreatVerdict.shouldHold(...)` - the existing pure inputs
  (warrior, retreat verdict, canFlee, storm hold, dying HP,
  RetreatAllowed inputs) plus a hysteresis latch, so the verdict
  cannot flap every other frame the way raw cooldown/push inputs do.
- Callers: march retreat arm, `selectBaseTechnique` march arm,
  `advance` gates, Abuse/dispatch gates - all read the verdict,
  none re-derives it.
- Migration: shadow mode first (compute alongside, probe
  disagreements, no behavior change - same pattern as the posture
  shadow), switch readers one by one with a game each, then delete
  the old predicates.

Explicitly rejected alternatives:

- A god-object deciding everything (march + duel + technique in one
  place): kills the squad-owns-WHERE / unit-owns-HOW split and turns
  every new behavior into a central edit. The per-chain first-claims
  lists plus order suites stay; only the verdict centralizes.
- Capability-typed permits (`RetreatPermit`/`DuelPermit` threaded
  through behaviors): correct on paper, wrong for this tree - it
  fights the existing `Action`/`Commander`/intent architecture and
  replaces readable `if`s with ceremony nobody here will maintain.

## Decision

Owner decision 2026-11-17: do NOT rewrite now. The incremental series
lands first with live validation per fix; this file keeps the design
from being lost and the evidence from being re-gathered.

## Agreed next step (owner confirmed 2026-11-17, work starts on context refresh)

The rewrite above is approved in the stepped form: shadow verdict +
per-reader switch behind shadow data (plan steps 1-2 in
`.ai/queue.md` under NEXT UP). Open question for the shadow data:
24-frame unit-level hold vs the 96-frame cluster floor.
