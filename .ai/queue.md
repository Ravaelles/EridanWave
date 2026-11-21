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
- 1DragWounded REGRESSION (open): flipped ~50/50 through Nov 21 12:07,
  then ~2W/10L with k0 every loss. Window: S2 MotionVerdict + audit
  round (12:36-13:56). Narrowed: the technique duel legs never run on
  the map - the unit is never `ready` at duel time (sleep refreshed
  every 2-3f), no stage claims after the f0 Retreat, no attack orders;
  Guard auto-acquire only (2-13 hits), Move/Guard flap to death. Healthy
  1Drag still wins on stats. Detector on the losses: zero CROSS, zero
  HoldingDuel, Retreating+duel mixes absent, Motion held=Duel rock
  stable. Next: StarEngine repro driving the real dispatch, or bisect
  the 12:36-13:56 window.
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
