# Architectural challenges (living list, newest last)

Named patterns only: one line each (what, where, open or closed).
Rule 12 (conventions): every solved problem gets one paragraph here.
Retreats keep one verdict per unit per frame; latches stay the single
hysteresis home (rules 10-11).

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
  Contain/PushMain) is this seam disagreeing with itself. Measured
  2026-11 (`Motion` probe on 1DragWounded): the desired owner is
  stable for hundreds of frames at a time - the inputs are not the
  problem, the readers are.
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
  gameplan-level latch (open, watch only - rules 10/18). The ratchet
  has since moved into the policy as an explicit committed-play bypass
  (`haveReaver`/`haveBay` flow); the watch stands for pre-commitment
  request churn.
- Stateless sharp edges at the engage layer: PreyDrive 3.0, ZealotPack
  1.0/12/2.0 recompute symmetric thresholds every frame with no release
  band. No flap observed (duel/contact guards release them in practice);
  if a hunt/pose or approach/regroup dither ever traces to one, the band
  goes in with a unit-keyed Latch at the caller (open, watch only).
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

- Wrong reference frame (audit 2026-11, the TankHold class): a dozen
  kernels measured against the wrong side of the duel - our range
  where the enemy's belonged (HoldToShoot vs Vulture, TankHold seam),
  centers where edges belonged (ArmedCordon, ZealotPack feed, Scout
  hold point), the ground path where air belonged (KiteTo feed),
  cross-domain cooldowns (Reposition urgency), fogged positions where
  live ones belonged (kite/abuse/carrier/minedefuse/suicide picks),
  any-building/any-enemy where warriors belonged (SlotDiscipline
  seam), any ground attacker where hittable guns belonged
  (nearestArmed). Fixed kernel by kernel with suites, seams live;
  needs-a-game suspects parked in the queue. Pattern: every distance
  ships with whose-range stated, and the suite pins the frame, not
  just the numbers.
- Single motion verdict (`MotionVerdict` bus + `owner` kernel, 2026-11):
  upgrades preempt sharp (contact answers the frame the march sees it),
  downgrades wait out a 3-frame release, so a one-frame target flicker
  never walks the duel out of the map. The march ticks once per frame;
  the gates, the legs, the technique hunt and the cluster rejoin read
  the latched side instead of re-deriving it. Same shape as the retreat
  verdict bus, second instance of the pattern. Pinned in
  `MotionVerdictSuite` + `MarchGateOrderSuite` + `HuntBehaviorSuite` +
  `ClusterCohesionSuite` + `ArmyMarchSuite`, proven live (1DragWounded
  `held=Duel` stable across hundreds of frames, `Dress` blips absorbed).
- Duel-hold on ghosts (was `duelHoldsMarch` + `HoldingDuel` label, both
  since deleted): any `toAttack` (stale memory, unhittable watcher)
  pinned the march while the technique tugged home and out every few
  frames (Crona f5900 zealots vs nothing). Closed by
  `Target.holdsDuelTarget` (hittable-even-if-detected only), pinned in
  `TargetMemorySuite` and `ArmyMarchSuite` (ghost + watcher rows never
  hold).
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
- Single production policy sink (closed: rule #2 live, call-sites
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
- HoldingDuel removed outright (2026-11, owner call): the march never
  pins feet to the ground for a duel anymore - it always marches,
  duels own themselves through the technique. The detector showed the
  hold alternating against keep-off and technique holds, and years of
  flaps traced to readers disagreeing over one frame. LoneMarch/Abuse
  stay (load-bearing, suite-pinned); the verdict bus keeps ticking for
  the Hunt/rejoin/lone-march gates.
- Production parity in-engine (`starengine.production`, 2026-11):
  production gates proven without an economy - free gate, power and
  stockpile in, hulls out at table rate, vetoes as silence. First
  non-micro StarEngine module; the fight engine untouched (constraint
  seam), scenarios assert on emergence. Pattern: when live proof
  costs ladder games, pin the mechanism in-engine and keep ladder
  for the doctrine.
- Lone-march ownership (2026-11): the gate marched lone hulls at the
  goal past live guns (330 LoneMarch, zero HoldingDuel, zero technique
  acts in a 2506-frame 1DragWounded loss). Closed in two steps: the
  gate yields to the duel the legs would hold (`MarchUnit.hasDuel`,
  same predicate both sides), then to a visible-armed 20-tile cordon
  (`ArmedCordon`, latched 21 out / 19 in, fogged memories never pin).
  Proved in StarEngine (march + duel contracts) and live (HoldingDuel
  403 vs LoneMarch 1 in the re-run win).
- Dress outranks the duel while the far-wait holds (ZealDrag,
  2026-11): `MotionVerdict.desired` claimed Duel whenever a target
  existed, so the technique charged through the dressed line from ten
  tiles out while the march waited - the split read as a blob. Dress
  now owns the opening (the wait releases on contact, so the duel
  loses nothing) and `advance` pauses while Dress is held, so legs and
  technique never split the frame; ghost hunts and victim shots do not
  need advance and keep running. Validated live (Waiting-for-pack from
  f28, Dress held, first blood f203 vs f162, mirrors flip back to
  wins). Pattern: when the march owns a posture the technique must
  read it too, or the two phases fight over the same unit.
- Legacy brain starving the W1 pipeline (1DragWounded, 2026-11):
  `BeDragoonVsSlowlot` (pre-W1 container action, no trace credit) ran
  inside `Fight.perform` ahead of the W1 `Combat` dispatch and issued
  an order nearly every execute, so the unit was never `ready` at duel
  time and the kite/approach/hold legs never ran - Guard auto-acquire
  only, wounded duels donated for eight straight games while the probe
  showed a healthy Fight/advance/Duel verdict. Closed by yielding where
  the shipped kite kernel fires (`yieldsToKite`, same read as
  `KiteAwayBehavior.shouldKite`): the yield issues nothing, so the
  technique runs on a ready unit. Diagnosed live (unit-level probes:
  perma-unready at engage, phantom Retreat claims on unready frames,
  legacy action firing every execute). Pattern: a legacy action ahead
  of the new pipeline that issues without crediting starves the
  pipeline while staying invisible - port or gate it, never leave it
  ahead and silent. Validated on UMS (Close/KiteAway chains back,
  wins return, no regressions on 1Drag/16Drag).
- Silent legacy brains (P6, 2026-11): thirteen `Be*` brains ran ahead
  of the technique as credit-less containers, so their orders never
  showed in the decision chain while the technique credits did.
  Closed by a pure unit-context gate on `Action` (`appliesToUnit`,
  enforced in `act` beside `allowed`, game reads stay out) with one
  override per brain, flipping all thirteen to leaves so claimed frames
  credit the brain name beside the leg. Pinned in `BeUnitGatesSuite`
  (gate rows plus leaf rows for all thirteen). Pattern: a dispatch that
  controls units answers who it applies to and what it claimed, or it
  does not belong in the chain. Validated live (`BeDetector` in the Act
  chain beside the technique legs) and on UMS (same verdicts as the
  pre-P6 baseline on 1DragWounded/1Drag/4+3Zeal, Wounded flips to a win
  inside its known variance).
- Phase-rivalry tripwires (L1, 2026-11): the technique and the march
  voted separately with no witness when they fought over a unit (duel
  legs vs the verdict bus). First of three levels closed by counting
  the disagreement twice: starved-duel frames (engage voted duel, the
  unit was not ready) and sustained motion mismatches (duel-under-
  Withdraw at the engage choke, march-under-Duel at the march audit;
  twelve consecutive frames, so release noise never reports). Both
  print in the end-of-game box. Pattern: rivalry between two owners is
  detected as sustained disagreement, counted, reported once.
  Validated live (Motion mismatch none on Wounded/4+3Zeal; starvation
  named Dragoon #76 for 1084f inside a Wounded win - the next
  investigation).
- Kite discipline, plant-last and capped yield (todo #1, 2026-11): the
  kite entered its decision only when already cooling past the wound
  ladder with fixed wound-scaled steps, so loaded volleys died in the
  return trip and a plant inside the kite leg could never fire (gate
  needs cooldown past 10, plant window is 6 - dead on arrival).
  Closed by three kernels: a pre-gate plant (loaded gun plus hittable
  target plus no blades on top holds the volley instead of kiting),
  a return-capped yield (the step never exceeds the walk-back inside
  the remaining cooldown, so the trip stops eating the next volley),
  and a near-dead early-and-far flight gated on melee contact. Pinned
  in KiteHoldSuite/KiteYieldSuite/VolleyWasteSuite, with a plant-last
  guardrail in WoundedDuelSuite (survival parity-or-better in-engine;
  superiority judges live). Judged live by VolleyWaste in the
  end-of-game box. Pattern: the yield is a function of the cooldown,
  never of the wound alone. Deviation on record (rule 20): near-dead
  kites far only against melee - equal-speed ranged guns cannot be
  outrun, so against guns alone the hull holds its last volleys
  instead of donating them. Validated on UMS (16Drag waste 14-17f
  down to 4-5f with wins 16/12, 16/8, 16/10; 1Drag wins return;
  Wounded inside its variance; 4+3Zeal still loses - melee mirror,
  separate unit).
- Kite scoping: plant and cap are ranged-only (2026-11): the first
  ZealDrag/1Drag reps after the plant regressed zealot-duels (healthy
  hulls holding instead of approaching, capped steps donating blade
  contact), so both halves gate on melee - blades on top keep the full
  wound step and never plant, and only scratched hulls ever plant
  (whole hulls belong to the line). The yield is a function of the
  cooldown against guns, of the wound against blades. Pinned in
  KiteHoldSuite (wound and melee rows). Validated on UMS (16Drag holds
  16/9 with waste 4-8f; ZealDrag back to wins 9/2 with a 7/9 loss -
  the mirror is a coin flip, reps decide; 1Drag and Wounded inside
  their variance).
- Kite shared-margin read (2026-11): the mixed furball caught the next
  hole - capping a dragoon-pressured hull while zealots closed from
  outside its margin optimized the ranged sub-duel but donated blade
  contact. Plant and cap now share one squad-wide dismissed-melee read:
  any blade pressure on the pack keeps full wound steps and forbids the
  plant; the cap binds only against guns alone. Validated on UMS
  (ZealDrag 8/5 and 9/8 back-to-back; 16Drag 16/14 with waste ~1f).
- Natural defense outranks the shell yield (2026-11, owner order):
  sieged tanks shelling our natural held our dragoons at the 13-tile
  keep-off while the base died - and the wall mission held wounded
  hulls under the guns twenty tiles out against superior armies. Now
  shells at home (20 tiles) with three answering dragoons inside ten
  are fought, never yielded; holders stand only unpressured, shielded
  (20+) and within nine tiles of the wall; a held retreat verdict
  outranks the wall mission outright. Pinned in ArtilleryKeepOffSuite
  (shells-at-home and answer rows) and ProtossWallInSuite (reach and
  shields rows). Pattern: base defense is local superiority, not
  geometry - count the guns that answer before measuring the line.
  Live judgment pending (needs the ladder games that trigger it).
