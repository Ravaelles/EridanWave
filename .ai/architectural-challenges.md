# Architectural challenges (living list, newest last)

Named patterns only: one line each (what, where, open or closed).
Rule 19 (conventions): every solved problem gets one paragraph here.
Retreats keep one verdict per unit per frame; latches stay the single
hysteresis home (rule 14).

## Open

- Single retreat verdict: `FightOrFlee`, `RetreatVerdict` (+bus),
  `ReengageLatch`, `ClusterPosture` floor, `NoRetreat`, technique
  ladder, march legs all vote retreat separately today and must be
  kept in agreement by hand (lonely gate, terminal gate, overrule).
  Target: one pure `RetreatVote` per unit per frame, both phases read
  it. Guarded by `ArmyMarchSuite` + UMS duels before any move.
  Known leftover divergence: the technique terminal gates on
  `nearestArmedTiles` while the march bus additionally gates on
  `nearestEnemyWarriorTiles` (lonely rule) - unify on one predicate
  (needs a ladder game that reaches lone statics first). Coupling
  measured 2026-11: 10 of 29 `MarchOrders` commits co-change the
  technique or verdict counterparts, and all 6 `RetreatVerdict`
  commits move `MarchOrders`/`CombatTechniques` with them - the trio
  already moves as one, maintained by hand; the sink only makes it
  official.
- March-vs-technique movement ownership: both write movement (march
  terminus/intent, technique decision/move) and only convention
  separates them; every flap (HoldingDuel jitter, Marching/Retreat,
  Contain/PushMain) is this seam disagreeing with itself.
- Wall-in doctrine duplication: Terran (`TerranWallIn`,
  `EnemyWallInDetected`, `BreakTerranWallIn`, `WallInBan`) and Protoss
  (`ProtossWallIn`) grew as parallel implementations; the next wall
  rule (Zerg?) must generalize, not copy.
- Target-memory horizon is spatial only (`TargetRecall.shouldRecall`):
  a stale-but-near memory pins a duel forever unless something
  hittable walks by; needs a temporal bound that does not break the
  Goon_v_DT hunt.
- Sunkens3 chase-into-cover: the reaver duel chases a picked Lurker
  inside static cover and dies there (8/7 timeout). Fix is a hold-at-
  range rule for picked lurkers, or a map-side answer (owner call).
- Gameplan gate stickiness rides have()-ratchets: DragoonsBeforeReavers
  is symmetric with no release band; post-Bay the have() ratchet latches
  it permanently, so the only live edge is pre-Bay request churn while
  dragoons hover at the boundary. If a Bay request start/stop saw ever
  shows in trace, the release band goes in the kernel with a
  gameplan-level latch (open, watch only - rules 14/17).
- Stateless sharp edges at the engage layer: PreyDrive 3.0, ZealotPack
  1.0/12/2.0 recompute symmetric thresholds every frame with no release
  band. No flap observed (duel/contact guards release them in practice);
  if a hunt/pose or approach/regroup dither ever traces to one, the band
  goes in with a unit-keyed Latch at the caller (open, watch only).
- Single production policy sink (owner proposal, evaluated 2026-11):
  the reaver bypass proved a gate in one production path is not a gate.
  The fix is veto-by-what-not-by-who: one policy object consulted
  inside the existing mechanical sink (`pump`/`once`/`get`), keyed on
  the produced class, with per-tech predicates registered in one file
  and veto reasons logged centrally (the Probe/status reason channel
  already exists - the enum is its taxonomy, not a second request path).
  `DragoonsBeforeReavers` becomes rule #1 and its four call-site reads
  dissolve back into dumb pumps. Same shape as the retreat verdict bus,
  for production (open, design agreed, awaiting implementation).

## Closed

- Duel-hold on ghosts (`duelHoldsMarch` + `HoldingDuel` label):
  any `toAttack` (stale memory, unhittable watcher) pinned the march
  while the technique tugged home and out every few frames (Crona
  f5900 zealots vs nothing). Closed by `Target.holdsDuelTarget`
  (hittable-even-if-detected only), pinned in `TargetMemorySuite`
  and `ArmyMarchSuite` (ghost + watcher rows never hold).
- Far-wait creep eaten by reissue suppression (found BY
  `ArmyMarchSuite` before it ever ran live): a single creep frame
  after Holds never moves (order-change skips movement), so the
  dress waited forever. Closed by a 2-frame creep window
  (`MarchDress.creepNow`), pinned in `MarchDressSuite`.
- Melee kite vs faster chasers (2026-11): the pack kited apart and
  donated backs (2Zeal). Closed by the pack gate
  (`MeleeKiteAway.packIntact`): kite balled, stand split.
- Ranged kite stutter (2026-11): the formation slot anchored the
  kite, so wounded hulls braked every stride and died standing
  (1DragWounded losses). Closed by anchoring on own pixel
  (b3f112560), validated live (flawless 1/0 wins).
- Gameplan tech-gate bypass (reaver first hull, `c8c3c026c`): the
  fifteen-dragoon gate lived only in `decideTechOptions` while
  `trainRoboUnits` pumped, `PvPOpeningVsFE` built Bay+Reaver freely,
  and `PvPLateGame` pumped around it. Closed by `DragoonsBeforeReavers`
  (one pure kernel, all four PvP production sites read it, suite
  pinned). Pattern: a gate in one production path with ungated sibling
  paths is not a gate - new tech gates start as a kernel, callers follow.
- March gate that was a fight vote (`NoTankMarch` -> `NoTankAttack`
  posture vote, `0bd8919aa`). Pattern: locomotion-shaped gates that are
  really intent votes belong in posture, not in the legs.
- Stance/posture unification (`RegroupLatch` deleted into the
  `ClusterPosture` latch, `e7fa90814`): one verdict pass feeding the
  posture latch, the Sim probe and the board. Pattern: parallel
  vote-keepers merge into one latch, never two agreeing by hand.
- Static-defense scatter (`HandleEnemyCombatBuildings`, one home with
  ordered claims, legs keep kernels and suites). Pattern: when N
  behaviors answer the same question from different chain spots, the
  group owns the order, the legs own the math.
- Shell callers (`MissionDarkTemplarFerry` -> `SquadTargeting`,
  2026-11): branch-new code called the slated-for-deletion
  `SquadAutomation` shell. Pattern: the todo already bans shell calls
  from new code - review greps added files for `SquadAutomation`.
- Retreat/move flap at no foe (BetaStar f6955): the technique ladder
  fell through to a bare `Flee` with no enemy check while the march
  (lonely-gated) voted advance. Closed by gating the terminal on
  `RetreatAllowed.forUnit`, so both phases read one permission.
- Retreat reason invisibility: retreats carried no rule/odds, so
  strong-army routs were unreadable in trace. Closed by
  `RetreatVerdict.retreatLabel` parens on march labels and
  RetreatPlan acts.
- No-foe retreats (Crona min-5 rout): the old `forUnit` let weak
  clusters retreat from empty fields. Closed by requiring an armed
  enemy within 12 tiles on both arms (`01166c5de`).
- Dead-code drift: `KiteAway` carried a mild-wound ladder,
  fresh/median gates and four knobs nobody called after the 61-HP
  rewrite - pure deletion, no behavior change (`ff18e7ab7`).
