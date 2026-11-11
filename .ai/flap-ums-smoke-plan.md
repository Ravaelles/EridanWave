# Plan: melee sim-vote flap + UMS micro smoke

Two workstreams, ordered: smoke first (tooling, also serves flap validation),
then the flap fix. No Scala changes for smoke; flap changes are melee-gated.

## A. UMS micro smoke (`scripts/ums-smoke.sh`)

### Research
- Template: `scripts/smoke.sh` (background `run.sh`, timeout kill, PASS/FAIL
  assertions, INCONCLUSIVE on expiry). UMS self-start works headless since
  `4ca8799c5` (verified 3x on the 15Drag map).
- UMS assertions differ from melee smoke:
  - No mapinfo (`Skipping terrain analysis (UMS map)`), so drop that check.
  - Births == starting force only (`New unit:` per initial unit); assert a
    floor, not growth.
  - Result comes from `out/wine/WINE_LOG` (`VICTORY!` / `Defeat`), not from
    CherryVis (disabled in UMS: `CherryVis.scala:34`) and not from probes
    (smoke must not depend on `probe.txt` contents).
  - Frames played + no high-priority `errors.json`, same as melee smoke.
- Candidate maps (fast first, all under `ums/rav/protoss/`):
  - `1Drag_v_1Drag.scm` — symmetric 1v1, seconds.
  - `15Drag_v_16Drag.scm` — the reported case, ~2-7 min real time.
- Box lock (`/tmp/eridanwave-wine-game.lock`) forces serial runs; run maps
  sequentially with per-map timeout (600 s) and INCONCLUSIVE on expiry.

### What's needed
1. New `scripts/ums-smoke.sh` (LF line endings), modeled on `smoke.sh`:
   arg `--timeout`, per-map run + assertions, `RESULT: PASS/FAIL/INCONCLUSIVE`,
   nonzero exit on FAIL.
2. One line in `scripts/README.md` next to the smoke entry.
3. Proof: one green run over both maps (no code changes, so no test suite
   or jar rebuild beyond what `run.sh` already does per run).

## B. Melee sim-vote flap (stance oscillation in contact)

### Research
- Vote chain: `SquadAttack.updateStance` (`src/Tactic/Squads/attack/SquadAttack.scala`,
  updateStance) recomputes fighting/coverage fractions from
  `u.combatJudgment.map(_.unitShouldFight(u))` every frame and feeds
  `RegroupLatch.nextStance` (flip below 0.35 / above 0.65, coverage floor 0.5;
  pinned by `RegroupLatchSuite`).
- Judgement source: `BattleProcessJudge` (`src/Information/Battles/ProcessingStates/`)
  stamps a fresh `BattleJudgment` per battle per estimation cycle; battles are
  continuously re-clustered and re-simulated from live positions
  (`BattleProcessCluster/Predict/Swap`). Near-even scores hover at the target,
  so the binary verdict flips frame to frame. Observed: stance 0/1 flap at
  f49-108 in the duel; same mechanism drives the known melee contact flap.
- Weight schedule (`src/Information/Battles/Types/Battle.scala:88-96`):
  `skimWeight` 0 early (pure sim: noisy) → 1 late (pure skim: stable);
  `sluggishness > 0` (`ReactionTimes.scala:25-27`) forces skim. Flap is worst
  early/mid game, exactly when sims are noisiest.
- UMS precedent (`49b598b21`): stance held `Engaged`, votes skipped. Not
  portable to melee — retreat is sometimes correct there.
- Second consumer of the same judgement: micro `FightOrFlee` sets
  `shouldFight` per frame (`Skirmish/FightOrFlee.scala:26-34`). Whether units
  visibly jitter Fight/Flee in melee contact is an open check during
  implementation (look at `restrainedFrames` / hysteresis damping first).

### Proposal
Temporal confirmation at the squad layer, not a threshold change:
- Pure kernel (e.g. `StanceVoteFilter` beside `RegroupLatch`): flip stance
  only after K consecutive frames of contravening supermajority
  (K ~ 24-48, i.e. ~1-2 s). Genuine routs still flip fast; sim noise does not.
- `*Suite` pinning: noise alternating 0/1 never flips; steady 0.0/1.0 flips
  after K; keeps the existing 0.35/0.65 semantics (filter feeds the latch,
  does not replace it).
- Wire into `updateStance` only (melee path; UMS/YOLO branches untouched).

### What's needed
1. Kernel + suite (TDD), blank-line gate, full suite green, jar rebuild.
2. Validation games (Monster and/or Benzene melee): count `PROBE Sim`
   stance transitions per game before/after from the normal log —
   the metric is directly observable, no new instrumentation.
3. Accepted cost: genuine retreats delay ~1-2 s. Micro-level jitter stays
   out of scope unless the games show it.
4. Risks: none to UMS (separate branch); melee doctrine thresholds unchanged.
