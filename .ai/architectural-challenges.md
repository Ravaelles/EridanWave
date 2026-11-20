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
  Contain/PushMain) is this seam disagreeing with itself. Specimen
  2026-11: an early lone Dragoon alternating LoneMarch/Approach+Engage/
  Dress every 1-2 frames - march, technique and formation each owning
  a frame. Measured 2026-11 (`Motion` probe on 1DragWounded): the
  desired owner is stable for hundreds of frames at a time - the
  inputs are not the problem, the readers are. Specimen
  2026-11: an early lone Dragoon alternating LoneMarch/Approach+Engage/
  Dress every 1-2 frames - march, technique and formation each owning
  a frame.
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
  gameplan-level latch (open, watch only - rules 14/17). The ratchet
  has since moved into the policy as an explicit committed-play bypass
  (`haveReaver`/`haveBay` flow); the watch stands for pre-commitment
  request churn. The ratchet
  has since moved into the policy as an explicit committed-play bypass
  (`haveReaver`/`haveBay` flow); the watch stands for pre-commitment
  request churn.
- Stateless sharp edges at the engage layer: PreyDrive 3.0, ZealotPack
  1.0/12/2.0 recompute symmetric thresholds every frame with no release
  band. No flap observed (duel/contact guards release them in practice);
  if a hunt/pose or approach/regroup dither ever traces to one, the band
  goes in with a unit-keyed Latch at the caller (open, watch only).
- Lone-march ownership (2026-11): the gate marched lone hulls at the
  goal past live guns (330 LoneMarch, zero HoldingDuel, zero technique
  acts in a 2506-frame 1DragWounded loss). Closed in two steps: the
  gate yields to the duel the legs would hold (`MarchUnit.hasDuel`,
  same predicate both sides), then to a visible-armed 20-tile cordon
  (`ArmedCordon`, latched 21 out / 19 in, fogged memories never pin).
  Proved in StarEngine (march + duel contracts) and live (HoldingDuel
  403 vs LoneMarch 1 in the re-run win).
- Temporary testing switches (`Strategery.StrategyToggles`, 2026-11):
  PvP Robo family and Reaver track default OFF for the test series;
  revert after. The file tracks them so a temporary kill cannot
  fossilize into doctrine - flip back is a revert of two vars.
- Mode selection as if/else (`SquadAttack.chooseMode`): the squad's
  destination ladder is the only first-claims chain not held as data
  (`MarchGates`, `EngagePipeline`, `HandleEnemyCombatBuildings` all pin
  their order beside the rows). Reordering a mode today is silent;
  reordering a gate is a diff to one place. Extract the modes into an
  order table when a reorder flaps provenly - not before (open).
- Temporary testing switches (`Strategery.StrategyToggles`, 2026-11):
  PvP Robo family and Reaver track default OFF for the test series;
  revert after. The file tracks them so a temporary kill cannot
  fossilize into doctrine - flip back is a revert of two vars.

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
- Single production policy sink (closed: rule #1 live, call-sites
  dissolved): the reaver bypass proved a gate in one production path
  is not a gate. Veto-by-what-not-by-who - one policy object
  (`Macro.Actions.ProductionPolicy`) consulted inside the mechanical
  sink (`once`/`pump`/`get`), keyed on the produced class, rules
  registered in one file, veto reasons on the `ProductionVeto` probe.
  `getReavers` survives as a strategy flag for expand and hold.
  Pattern: same shape as the retreat verdict bus, for production.
  Second rule candidate: the Shuttle veto.
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
- LoneMarch/Abuse/HoldingDuel keep-or-delete (2026-11): kept. The
  flap log showed reader disagreement, not concept guilt: HoldingDuel
  pins mop-up (timeouts without it), Abuse owns outranged pack duels
  (pinned in mirror suites), LoneMarch already is a 17/13 Latch (the
  template, marching past instead of milling alone). Deleting any of
  the three regresses proven wins; the pain is owned by the
  single-verdict item above.
- Production parity in-engine (`starengine.production`, 2026-11):
  production gates proven without an economy - free gate, power and
  stockpile in, hulls out at table rate, vetoes as silence. First
  non-micro StarEngine module; the fight engine untouched (constraint
  seam), scenarios assert on emergence. Pattern: when live proof
  costs ladder games, pin the mechanism in-engine and keep ladder
  for the doctrine.
- LoneMarch/Abuse/HoldingDuel keep-or-delete (2026-11): kept. The
  flap log showed reader disagreement, not concept guilt: HoldingDuel
  pins mop-up (timeouts without it), Abuse owns outranged pack duels
  (pinned in mirror suites), LoneMarch already is a 17/13 Latch (the
  template, marching past instead of milling alone). Deleting any of
  the three regresses proven wins; the pain is owned by the
  single-verdict item above.
- Production parity in-engine (`starengine.production`, 2026-11):
  production gates proven without an economy - free gate, power and
  stockpile in, hulls out at table rate, vetoes as silence. First
  non-micro StarEngine module; the fight engine untouched (constraint
  seam), scenarios assert on emergence. Pattern: when live proof
  costs ladder games, pin the mechanism in-engine and keep ladder
  for the doctrine.
