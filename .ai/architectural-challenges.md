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
  (needs a ladder game that reaches lone statics first).
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
