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
| `Squad.run` / `SquadAttack` | DONE: publishes `(posture, roles)` + `battlePosture` query; `fightConsensus` follows `advances(posture)` (yolo/suicide keep explicit outs) |
| `MarchOrders` | DONE: legs read posture + `SlotDiscipline`; Retreat outranks formation unconditionally (march-home bypass); honest `Marching home` vs `Returning to slot` split |
| `CombatEngagement.engage` + `Combat.dispatchByTechnique` | DONE: consume `(posture, advances)`; `syncConsensus` lost the vote; `decideFight` has one writer |
| `Retreat` maneuver | OPEN: honor the 96-frame floor + rearguard role on Withdraw |
| `FormationHold` | Keeps today's line-hold semantics under `HoldLine`; the gradual-advance creep is QUEUED (section 5), not this refactor |
| `RegroupLatch` / `ReengageLatch` | DONE: `RegroupLatch.stance` deleted with its suite (VoteConfirmationSuite votes postures now); per-unit `ReengageLatch` stays as the duel-level hold |
| `FightOrFlee` / `FightRules` | DONE (validating): fallback obeys the posture; `committed` still fires first, so explicit orders bypass the vote by construction |

## 4. Migration: status and remaining work

Phases 1-3 DONE. Phase 4 in progress, one validated game per edit
(Benzene PvP every time for comparability: 71/74 -> 83/3 -> 80/9 ->
79/9 -> 84/11; losses collapsed, wins held):

- DONE #1 `syncConsensus`: technique lost the `shouldFight` vote; the
  skirmish verdict is the single writer. Readers (`Root`, `KiteAway`
  standoff, `IdleEngage`, formations, squad consensus) follow one writer.
- DONE #2 `walk()`: safe passage no longer re-arms fighting; a unit
  walking home keeps its disengage formation.
- DONE #4 `engage()` gate: `battlePosture` squad query (L4-clean, no
  type test; `None` keeps the old gate). Attack squad duels on
  Fight/HoldLine, walks the march otherwise - a stale regroup consensus
  no longer pins posture Fight windows to the march.
- DONE #5 `dispatchByTechnique`: `ClusterPosture.advances` bounds Brawl,
  Abuse, DragoonStand, SuperKite and `engage()`; Withdraw/Regroup walk.
- IMPL #7 `applyEstimation`: no-rule fallback obeys the squad posture
  (`Posture`/`PostureHold` reasons) instead of re-asking each sim.
  Game #6 validating now. Commit on green.
- OPEN #6 `MarchOrders` legs + label (last rewire piece, see below).
- OPEN stance retirement: `fightConsensus` derived from posture, then
  `RegroupLatch.stance` removed (see findings).

### Findings that reshaped the endgame

- Verdict audit (`scripts/verdict-audit.py`, PROBE Verdict + trace tasks):
  game 4 split 79.6% = ~4/5 `+Committed vs -OutOfContact/-Sim` (the PvP
  zealot-rush commit, BY DESIGN with its own off-ramps) + ~1/8 pure
  `+Sim vs -Sim`. The posture subsumes the sim part; commit stays an
  explicit order - `FightRules.committed` fires before the fallback, so
  committed units never reach the posture vote (no code needed, by
  construction).
- Stuck `RegroupLatch`: stance latched Regrouping ~frame 4300 in games
  2-4 and never released (release needs fighting > 0.65 while split
  sims hover ~0.5). The posture voted Fight 7x through it. This is the
  old latch's asymmetry, exposed - not caused - by the rewire, and the
  reason stance must retire into the posture (floor + agreement +
  parity) rather than be tuned.
- The act-level audit stays noisy by nature (cooldowns/positions differ):
  the verdict audit is the ruler from here on. Per-act mixing during a
  shared Fight verdict is execution, not dissent.

### Remaining: #6 march legs + label, then stance retirement

6. `MarchOrders.sendUnit` (`MarchOrders.scala:112-125`): route legs
   through `SlotDiscipline.maySlot` (no target, cold weapon, 60 silent
   frames, no threat in range); split the lying `ReturningToSlot` label
   into "marching" (no fight) vs a real return. Needs its own game:
   legs move the terminus, the most visible change of the set.
7. Derive `fightConsensus` from posture (`advances(posture)`), keep the
   `HuntContact` OR and the UMS/suicide overrides; validate that the
   march follows posture windows 1:1 in the trace boards. Then delete
   `RegroupLatch.stance` + `stanceFilter` - the posture latch
   (floor + sim agreement + parity + deadband) is strictly more
   informative, and two latches would flap against each other.
8. `Retreat` maneuver: honor the 96-frame floor + rearguard role when
   posture says Withdraw (middle choke anchor stays queued - it needs
   posture-owned destinations first, section 5).

Validate each edit with a live game: verdict split first (sim-driven
part must fall toward zero; committed-vs-rest stays by design and is
reported separately), results second. Revert any single edit that
regresses - they are independent by construction.

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

## 7. Session log 2026-11-16/17 (what landed, what proved it)

All commits on `f/phase-f`; every one green on the full base suite
(919 tests) before commit; jar synced every time.

- Disagreement audit (`scripts/disagreement-audit.py`): SquadAttack
  split 47-88% across TauCross traces; Benzene PvP baselines 82.9%.
- Kernels: RetreatDiscipline (96f floor, 40/30/20/15 decay),
  ClusterPosture (Fight/HoldLine/Withdraw/Regroup + deadband),
  ClusterRoles, SlotDiscipline - all suite-pinned.
- Shadow posture in SquadAttack + AttackPosture board: 20 flips/19k
  frames, 21.1% disagreement vs stance (526x Engaged/Withdraw).
- Phase 4 rewire, one validated game each (Benzene PvP):
  #1 syncConsensus loses the vote (83/3), #2 walk stops re-arming
  (80/9), #4 engage gate on battlePosture (79/9, kills spread
  mid-game), #5 dispatch bounded by advances (84/11), #7 fallback
  obeys posture (80/25). Split fell 82.9% -> 74.8%; verdict audit
  (`scripts/verdict-audit.py`) shows ~4/5 of the rest is
  +Committed-vs-contact BY DESIGN (PvP zealot rush), ~1/8 pure sim.
- Small armies never split (<20 warriors consolidate, Raze needs a real
  expansion - naturals excluded, no backstab).
- Pair glue (FleeToMate, 15-tile band, 2-tile hover cap) + raid-status
  deadband (0.5 out / 0.65 back) for the PairRide/RaidFerry flap.
  Engine pair suites 24/24 green.
- Targeting: WorkerSpread (id-dealt draw of 3), TargetChaff (Larva/Egg
  out of AliveEnemies, the type filter and stale release), Scout
  discipline (PvP: Carriers only, wingmate in 5, LoneScout verdict),
  AlwaysAttackInRange (rename + Observer/Reaver/DT/HT ladder),
  Dragoon carrier hunt (20+ shields, in-range shot, 11-tile pathed
  close), Scout static keep-off, ForceGlobalAttack (first DT + no
  detection forces the attack; covered statics wait for 140),
  CanProduceShuttle veto (observers eat the gas; <7 dragoons or <12
  without 250 score hold the ferry in PvP), DragoonPvP retreat +
  early-mirror 20% pressure penalty below 7 hulls vs Dragoons.
- March: DragoonRoam (unseen mirror roams, closes on first recognition),
  ZealotLeash (5.5 tiles to Dragoons), ClusterCohesion (14-tile drift
  band, battle-gated, UMS-exempt).
- Trace: EnemyStrategy + EnemyIntel boards (change-only), Posture +
  ShuttleVeto + reason-tagged Verdict probes.
- UMS batch 0/4 exposed the baseless hole (Withdraw on maps with
  nowhere to regroup to); fixed via canWithdraw=false, needs re-run.

OPEN, in order: HoldLine creep (queued); Withdraw-to-middle-choke
anchor (queued); rearguard covers validated in one more grind;
live validation of every unproven rule above on raid/PvP/PvT/PvZ games.
DONE since: march legs gated by SlotDiscipline + honest Marching-home
vs Returning-to-slot split (validated: Waiting x1, Rallying dozens,
no more mill-flap); roam suspends Fight/Abuse in the unit phase (same
facts as the march gate); defense triggers only on anti-ground weapons
(Observer false alarm fixed) + DefenseWhy board (reason, triggering
enemies, counts); muta/drop proactive guards answer at any army size.

## 8. Live validation log (2026-11-16/17, all on f/phase-f)
- Benzene PvP x5 (71/74 -> 83/3 -> 80/9 -> 79/9 -> 84/11, then 80/25):
  verdict split 82.9% -> 74.8%, losses collapsed and stayed down.
  Shadow stable (9-20 flips/game); stance latched Regrouping ~f4300 in
  games 2-4 (0.65 release never reached) - posture voted Fight through
  it 7x, which is why #4 exists.
- Benzene PvT VICTORY 96/27 (shadow validation, split 88.9%).
- Benzene PvP 80/25 (#7 game): Posture/PostureHold reasons flow;
  +Sim:Posture vs -Sim:PostureHold pairs are cross-frame smear
  (change-only probe + forward fill), not same-frame dissent.
- Benzene PvP 132/158 (all new rules live): ZealotLeash 918,
  DragoonRoam 609, AlwaysAttack 674, SpreadWorkers 65, EnemyStrategy
  EarlyForge->GatewayFE, EnemyIntel dt/bases transitions, ShuttleVeto
  holding (3-5 dragoons, score ~50). No carriers/scouts built - those
  rules await their matchup.
- Benzene PvZ 35/3: TargetChaff 0 picks with 3 larvae on the map.
- scbw BetaStar VICTORY 226/129 (18 min grind): leash 1050, spread 773,
  roam 588, EnemyStrategy evolving, posture all three states. No probes
  on the release log - trace-only audit.
- UMS batch (Sunkens3/4, Reaver_v_Sunkens, Reav&Shut x2): pair fights,
  rejoins (15-18x), unloads and shoots; no await dither, no abandonment.
  Sunkens3 16/10 -> 17/14 after the baseless fix (Fight on UMS).
  LoneMarch flap is hunt-destination alternation, not a march leg.
- StarEngine full pass: 255 suites / 1014 tests green (march, pairs,
  mirrors, focus fire, hold lines, pursuit horizons).
- Unvalidated live (need their matchup/game): DragoonCarrier (needs
  carriers), ScoutStandoff (needs our scouts + enemy AA), FleeToMate
  (needs a pressured pair), ForceGlobalAttack (needs our DT without
  enemy detection), ClusterRejoin (0 acts so far - healthy cohesion or
  narrow conditions, watch the next grinds), CanProduceShuttle veto
  held correctly (0-shuttle early PvP proven by absence).

## 9. Priority bands: retreat is never outvoted by positioning

Atlantis lesson, applied here: managers run at high/medium/low, and a
higher band with a live claim ends the frame - lower bands are not even
asked. Suppression is by band membership (a static property of the
behavior), never by comparing thresholds across bands, so no tuning
inside one band can ever overrule another. Every arbitration chain in
the tree carries an explicit order-is-load-bearing header naming its
bands; move whole arms, never retune one leg against another.

Bands, top to bottom:

- HIGH owns survival and unconditional kills: unseen-killer dodges,
  siege keep-off, the always-attack ladder (Scourge/Carrier/capitals
  first - even while retreating), static keep-off (hold outside AA
  reach). A live HIGH claim ends the dispatch; positioning never runs.
- MEDIUM owns the duel: stand-fight overrides (DragoonStand,
  DragoonCarrier), rearguard cover (holds instead of turning), brawl,
  abuse, superkite, engage. Techniques pick the how inside the posture.
- LOW is whatever the squad phase decided before us: march legs,
  formation slots, rally, dress waits, leashes, rejoins, static
  positioning holds (hold-instead-of-walk near statics outranks other
  formation inside LOW).

Consequences, all pinned by existing suites plus the StarEngine
scenarios that mirror each shipped kernel:

- Retreat (HIGH verdict + Flee/Fallback technique) can never be
  stopped, dressed, rallied or slotted: the march sends the hull home
  without asking a single gate or leg (MarchRetreat bypass), and the
  dispatch reaches Retreat with every formation claim already skipped.
  The f16368 Retreat-vs-Waiting/Marching flap is structurally impossible:
  the two labels cannot share a frame because only one band speaks.
- Roaming Dragoons skip the duel entirely (Fight base and Abuse gated
  on the same facts as the march gate): scouting the unseen mirror is
  MEDIUM business, dueling can wait for the first enemy Dragoon.
- Static keep-off is HIGH (threat avoidance); static positioning holds
  (hold-instead-of-walk, FormationHold, brave stands) are LOW with
  priority over the rest of formation - documented at each site.
- The pair arbiter keeps its own first-rule-wins contract (one decider,
  sealed orders); BeShuttle executes arms most-to-least urgent per
  order. No second manager may claim a bonded hull - see the Shuttle
  assignment rule (PairGlue + status latch instead of a registry: the
  bond already maps shuttle to mate).
