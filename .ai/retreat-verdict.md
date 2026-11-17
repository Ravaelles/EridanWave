# Unified retreat verdict (CLOSED 2026-11-17)

Tracked in: `.ai/queue.md`. Status: migrated, validated, probes
removed. History lives in git log.

## Contract

One verdict object per unit per frame, computed ONCE, read everywhere.
`RetreatVerdict.shouldHold(...)` is the union of the old march and
technique retreat arms (warrior, reaver, canFlee, storm hold, fresh
verdict, dying HP, `RetreatAllowed` inputs) plus a 24-frame timed hold
(`RetreatHold`, sharp enter, release past the last fight edge), so the
verdict cannot flap the way raw cooldown/push inputs and phase skew do.

Readers (each ticks the shared bus on its own schedule, obeys the
held verdict): march retreat arm, `selectBaseTechnique` march arm
(above the fresh-fight arm), duel dispatch `advance`. Last writer wins
the raw side; the hold absorbs the skew. `RetreatAllowed` stays as a
verdict input. Rejected: a god-object decider (kills the squad/unit
split and OCP) and capability-typed permits (ceremony over readable
ifs). `MarchRetreat` deleted; `RetreatShadow`/`Technique` probes
removed after validation.

Evidence arc (Benzene PvP, mixed march/duel frames per game):
144 -> 8 -> 3 (per-site gates) -> 44 (canFlee split found) -> 3
-> 116 (arm-2 bypass found) -> 16, all technique-Flee smear
-> 21 (pipelines: 0 Abuse) -> 6-8 hard per game, every row a
flee verdict with a retreat tail. Zero same-tick contradictions
since the bus. Duplicates consistently 0 (single march writer).

## Design inputs (measured, not assumed)

- Task order is budget-driven (`TaskQueueParallel` sorts by due/budget,
  Planning skips up to 6 frames): squad phase and unit phase interleave
  arbitrarily frame to frame. No design may assume "march first, combat
  second, same frame".
- `shouldFight` has ~10 writers (`decideFight` callers), most silent.
  The verdict was already emergent; the bus removed the silent writers
  from the retreat question.
- Release handshake: the duel reader obeys the march flag, so the
  home-bound reader clears first on its own schedule and the duel
  resumes late, never early. Pinned in `RetreatVerdictHoldSuite`.
- Hysteresis answer (StarEngine): 24-frame unit hold (flips 89 -> 5,
  resume ~22f after the facts clear); 96f equally calm, slower.
  The 96-frame floor stays at the cluster posture layer.
- Residual audit rows are measurement smear (terminus re-notes +
  sleep-merged windows), not behavior - read them with
  `scripts/shadow-join.py`, and split hard `Retreating` from soft
  `Marching home` (`scripts/retreat-audit.py` reports both).

## Follow-up: dispatch order as pipeline (DONE)

Owner direction 2026-11-17, generalized from this migration: every
dispatch chain is one explicit list (`TechniquePipeline`,
`EngagePipeline`, `MarchGates.order`), each stage declaring its
`DispatchBand`, each list pinned by its order suite. A new behavior
means a registry entry plus a suite row, red until placed.
