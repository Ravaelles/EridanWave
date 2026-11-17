# Unified retreat verdict (deferred rewrite)

Tracked in: `.ai/queue.md` (last section, "Unified retreat verdict (deferred)").
Status: STEP 1 LANDED (c44df2aa8) - shadow kernel + suite + probe live,
validating in game; no reader switched yet, no behavior change. Do not
start step 2 without reading the shadow data with the owner.

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
  shadow), switch readers one by one with a StarEngine scenario each
  (temporal guarantees: no mixed frames under jitter, clean release)
  plus base suites, then delete the old predicates. One validation
  game at the very end, not per reader.

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

## Step 2 design inputs (measured, not assumed)

- Task order is budget-driven (`TaskQueueParallel` sorts by due/budget,
  Planning skips up to 6 frames): squad phase and unit phase interleave
  arbitrarily frame to frame. No design may assume "march first, combat
  second, same frame" - the shadow game caught march running after the
  unit phase (Retreating label with a stale-false marchHome flag).
- `shouldFight` has ~10 writers (`decideFight` callers: FightOrFlee,
  CombatManeuvers.move, Cast, BeRushingZealot, BeZealotVsZealot,
  GetRepairedMobile, BeMutalisk, ...), most of them silent (no Verdict
  probe row). The verdict is already emergent; centralizing it removes
  the silent writers too.
- Release handshake: the duel reader obeys the march flag (the
  engage-gate shape), so the home-bound reader clears first on its own
  schedule and the duel resumes late, never early. Late re-engage
  (<= one march tick) is the safe direction; early retreat entry stays
  sharp. Pinned in `RetreatVerdictHoldSuite` (mixed == 0 on every leg).

## Hysteresis answer (StarEngine, 2026-11-17)

`RetreatVerdictHoldSuite` (tags retreat+march): one body playing both
readers on skewed schedules (march every 6th frame, duel every 2nd)
against one shared hold bus fed by the shipped `RetreatVerdict`
kernel on jittered facts. hold=0: mixed 0 (the march flag rules it
out structurally) but 89 decision flips (the dither disease);
hold=24: mixed 0, 5 flips, resume 22 frames after the facts clear;
hold=96: mixed 0, 5 flips, resume 94 frames after. Decision: the
24-frame unit hold; the 96-frame floor stays at the cluster posture
layer where it belongs.
