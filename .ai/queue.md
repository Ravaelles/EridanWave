# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- Ladder validation (S2 + audit + W1 jars, ~15 games: BetaStar, Crona,
  Terminus, Hao Pan series): every loss diagnosed as gameplan or
  composition gap (greedy expand into 9pool, flood vs 1-base, PvP
  mass-goon, carrier out-comp) or unwinnable forts - no mechanism
  found. Post-fix Crona shows zero ChokeHold and zero HoldingDuel
  rows: the lone holder no longer commits, the line marches, kites
  and retreats instead of donating.
- UMS: toggle-jar full batch 9W/13L/3NA (structural walls unchanged:
  Sunkens ammo, DT detection, mop-up timeouts); post-W1 duel maps
  green (1Drag/16Drag/ZealDrag/Reav_v_Drag wins, TanksVult kills
  rising).
- ZealDrag line doctrine (owner order, 2026-11-21): dress early,
  wait, inch forward - implemented via the existing dress machinery
  (far-wait + FormationApproach 0.1 steps): Dress outranks Duel while
  the far-wait holds, advance pauses while Dress is held, and healthy
  hulls with nothing inside two tiles never kite
  (`KiteAwayBehavior.shouldKite` gate: shieldWound==0 + no live threat
  within 2 tiles). Validated on ZealDrag_v_ZealDrag (line dresses from
  f28, first blood f203 vs f162, 3W/1L k9/l8 k9/l7 k9/l8 k4/l9 vs
  0W/2L before); no regression on 1DragWounded (V k1/l0) and 16Drag
  (V 16/12). Ladder validation pending.
- 1DragWounded MECHANISM FOUND + FIXED (2026-11-21): the losses were
  not a kite-tuning gap - the W1 duel legs never ran (legacy
  `BeDragoonVsSlowlot` issued every execute, unit never `ready`, Guard
  auto-acquire only). Fix: the legacy dance yields where the shipped
  kite kernel fires; chains show Close/KiteAway again, wins return
  (~1/3 in 15 post-fix games vs ~1/10 before; healthy 1Drag and 16Drag
  still win, ZealDrag mirror flips as before). Residual: fast early
  deaths + k0/l0 timeouts (kill too slow) - next is hold/approach
  tuning or accepting the map's variance.
- Jar: W1 + shouldRun unification (`strings`-verified, slot md5
  matches target).

## Needs a live game (code green, jar includes it)

- Wall-in doctrine. Needs a PvT wall-in (`details/wall-in.md`).
- Stalled-duel fetch. Needs Sunkens3 (`details/sunkens.md`).
- DT leash, MineDefuse, vulture cannons, bank gates, weak-PvP hold,
  HT-vs-rejoin, artillery-vs-duels. Needs the triggering games
  (`details/pending-specs.md`).
- 2Zeal parked hard: needs a melee shared pick (unbuilt).

## Owner decisions (no code before the call)

- Mirror focus gate taken (4->3 everywhere, `FocusFire.minShooters`):
  validated on drag mirrors. If small lines still spread, the next
  suspect is reach (wings out of shared-target range), not the count.
- Progressing-duel ferry question (`details/sunkens.md`).
- Carrier vs sieged tank, natural crescent hold
  (`details/pending-specs.md`).

## Parked (do not touch)

- Contain carve-out, C6 profiling, krasi0, Sunkens2 mission fallback,
  refresh-hop timing. Goon_v_DT + DT_v_Protoss unwinnable without
  detection: leave alone.
- Audit 2026-11 follow-ups needing a game (reference-frame sweep;
  fixed: HoldToShoot own-range + loaded-hold, TankHold tank-range
  + reply-range leg, ArtilleryKeepOff 13.1, TankHug proactive leave,
  kite/abuse/carrier/minedefuse ghost gates, SlotDiscipline warrior
  feed, ArmedCordon edge feed, nearestArmed canAttack filter,
  Scout hold edge, ZealotPack edge feed, Reposition urgency,
  KiteTo air frame, Suicide ghost charge): KiteAway squad-mate margin
  gating (pack coherence vs back-line kite), dogfight 0.25 band,
  carrier-hunt and minedefuse release bands, escaping-toggle
  smoothing, SuicideMission path/euclidean + squadInZone latch,
  FormationHold Reaver hold, MeleeKiteAway dragoon-as-mate counting.
