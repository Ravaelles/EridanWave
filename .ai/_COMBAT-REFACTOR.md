# Combat refactor: cluster authority, unit roles (direction, non-binding)

Owner brief (2026-11-16): half the units flee while half fight; the verdict
must move to cluster scale with tasks delegated to individuals. Slot logic
(Returning to slot / Abuse, Retreat vs Marching to formation slot vs
Waiting for pack) flaps because three independent deciders share one
actuator with no arbitration. Long-term fix, time is not a factor.

## 1. Problem as observed

- `MarchOrders.sendUnit` forces the terminus to the Guard slot and the
  label to "Returning to slot" whenever `!fightConsensus`
  (`Tactic/Squads/MarchOrders.scala:112-125`). The label lies: it means
  "no consensus", not "formation decided to return".
- The unit phase then disagrees: `AbuseBehavior.charge()` walks to the
  *engage* formation slot (`CombatManeuvers.scala:105-113`), a different
  point than the march's Guard slot. One frame Abuse, one frame Returning
  to slot, the Dragoon mills in place ~15 tiles from the enemy.
- `Waiting for pack` (`marchLegs`, `holding=true` pins `unit.pixel`:
  STAND) alternates with `Retreat` (move to safety): directly opposed
  orders every ~9 frames.
- Root cause, not a threshold bug: the fight-or-flee verdict is computed
  N times on N different inputs (personal matchups, confidence, cooldown,
  per-unit latches), so identical neighbors get different answers. No
  tuning converges; the split is architectural.

Reference: `.ai/review-d.md` L1/L2/L3 (intent ownership, destination
writers, `decideFight` multi-writer) already flag the same disease.

## 2. Direction

The cluster decides WHETHER and WHERE (binding). The unit decides only
HOW. Per-unit sims survive as cluster inputs, never as verdicts.

Cluster posture (one per fighting cluster per frame), latched:

- `Fight` - close and kill; roles: spearhead / flank / support.
- `HoldLine` - keep formation, let the enemy come; ranged holds, melee
  still closes, flankers still circle (today's `FormationHold` semantics).
- `Withdraw` - the whole cluster leaves, at least four seconds, with a
  decaying advantage penalty so no flicker re-engages; roles: rearguard
  (healthy, fast) + main body. Below twenty warriors no rearguard
  detaches - the cluster withdraws whole (see `ClusterSplit.keepTogether`).
- `Regroup` - no battle yet or reforming; march dressed, no posing.

Slot/formation legs (dress, rally, return) may only fire when ALL hold:
no attack target, weapon cooling, no damage taken in the last ~60 frames,
no threat in range. A shot, a cooldown or an incoming round vetoes the
slot that frame, by definition.

Owner-pinned numbers (already landed as `RetreatDiscipline` + suite):

- Minimum withdrawal: 96 frames (4 s), no re-engage sooner.
- Advantage multiplier by age of withdrawal: <1 s x0.6, <2 s x0.7,
  <3 s x0.8, 4-6 s x0.85, older x1.0. Multiplies the sighed strength
  ratio, never raw counts.
- Future anchor (not started): withdraw toward `Chokes.middleChoke`
  instead of bare safety once the posture owns destinations.

## 3. Proposed classes and responsibilities

New kernels (pure, suited, no game access):

| Class | Home | Owns |
|---|---|---|
| `ClusterPosture` | `Tactic.Squads.Decisions` | Sealed `Fight \| HoldLine \| Withdraw \| Regroup` + rally pixel; `decide()` on the squad forecast snapshot; owns the posture latch (histereza) |
| `RetreatDiscipline` | `Tactic.Squads.Decisions` | DONE: 96-frame floor + decaying multiplier table + `shouldHoldWithdraw` |
| `ClusterRoles` | `Tactic.Squads.Decisions` | Members + posture -> role per unit (`Spearhead \| Flank \| Support \| Rearguard \| MainBody`); pure assignment from position/class/health |
| `SlotDiscipline` | `Tactic.Squads.Decisions` | The four slot gates (no target, cooling, 60-frame damage silence, no threat in range); one predicate the march legs and the unit behaviors share |

Changed seams (thin, game-coupled, reviewed by reading):

| Class | Change |
|---|---|
| `Squad.run` / `SquadAttack` | Publish `(posture, roles)` beside `targets`/`formations`/`vicinity`; `fightConsensus` stays as compat during migration, later derived from posture |
| `MarchOrders` | Legs read posture + `SlotDiscipline`; fix the lying `ReturningToSlot` label (separate "no fight, marching" from a real return) |
| `CombatEngagement.engage` + `Combat.dispatchByTechnique` | Consume `(posture, role)`; stop deciding. `syncConsensus` loses the `shouldFight` override; `decideFight` gets one writer |
| `Retreat` maneuver | Executes `Withdraw`: safety now, middle choke later; honors the 96-frame floor |
| `FormationHold` | Keeps today's line-hold semantics under `HoldLine`; the gradual-advance creep is QUEUED (section 5), not this refactor |
| `RegroupLatch` / `ReengageLatch` | Fold into the posture latch or keep as executor detail; decide at implementation time (likely the posture latch supersedes `RegroupLatch.stance`, per-unit `ReengageLatch` stays as the duel-level hold) |
| `FightOrFlee` / `FightRules` | Verdict becomes cluster input: per-unit sims aggregate (median/percentile) into the posture facts instead of answering per unit |

## 4. Migration phases (each step useful alone)

1. Disagreement audit (metric, no game change): on finished traces count
   frames where one cluster's units held different verdicts. Baseline and
   later proof.
2. Kernels first: `ClusterPosture.decide` + `ClusterRoles.assign` +
   `SlotDiscipline.maySlot`, each with a suite (TDD per convention 9).
3. Squad seam: publish posture + roles; old paths untouched, shadow mode
   (log disagreements between posture and what units actually did).
4. Behavior rewire: `engage()`/`dispatchByTechnique` consume the verdict;
   remove `syncConsensus` override; single `decideFight` writer.
5. Live validation: disagreement rate to zero first, then game results.

## 5. Queued follow-ups (not this refactor)

- HoldLine creep: while attacking, hold formation and close the distance
  gradually (~0.1 tile steps) instead of breaking or standing. Needs
  per-unit hold positions advancing with the enemy + suite first. Also in
  `.ai/queue.md` under march validation.
- Withdraw anchor: middle focus choke (`Chokes.middleChoke`) as the
  Withdraw destination once posture owns destinations.
- Interaction with the small-army rule: `keepTogether` (<20 warriors)
  forbids role splits (no rearguard, no raze, no backstab) - the whole
  cluster shares one posture and one destination.

## 6. Honest risks

- A cluster verdict from a worse forecast errs *wholesale* instead of
  singly. Mitigation: conservative withdraw thresholds, histereza, the
  decaying penalty, and the rearguard as a physical backstop.
- Mid-migration games may regress: each of today's ~19 behavior files
  "works" in isolation. Shadow mode (phase 3) exists to catch that before
  the rewire commits.
- Raw PurpleWave looks cleaner today because its behaviors never
  contradict (single chain, no squad/unit split). Our payoff must come
  from coordination only a cluster authority can deliver: shared targets,
  contains, surrounds. This refactor is that precondition, not a bonus.
