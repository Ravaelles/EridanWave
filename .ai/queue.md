# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- Ladder validation: BetaStar DEFEAT 48/137 (fair late grind,
  no flap), Crona DEFEAT 21/35 (greedy expand into 9pool, known
  gameplan gap), Terminus DEFEAT 108/225 (17 min grind, no
  mechanism), Crona REMATCH VICTORY 703/329 (36 min, clean -
  retreat votes ordinary No/OutOfContact, no flap), Terminus
  REMATCH VICTORY 461/330 (29 min, 286 retreat votes, clean),
  BetaStar REMATCH DEFEAT 57/173 (16 min PvP: our PvPDT/DTRush
  + GatewayFE into observers - 2 DTs managed 1 kill, enemy
  massed dragoons, we peaked 26 warriors and traded 85 for 31
  goons; micro clean throughout - duels, no flap, sane votes;
  same known gameplan-selection gap as Crona 21/35, no mechanism
  found), BetaStar SINK DEFEAT 70/167 (18 min PvP: Obs+goon answer
  to DTs, Robo never built - policy correctly silent with zero
  vetoes and zero crashes; peaked 22 warriors, killed their Nexus
  on Timing1, then lost the grind on even votes - out-massed macro
  loss, no mechanism), BetaStar SINK2 DEFEAT 7/73 (9 min PvP vs
  4GateGoon rush: dead min 6-8 with 12 warriors, 486 ordinary
  votes, no flap - failed rush defense, macro loss; policy again
  correctly silent, zero vetoes), BetaStar SINK3 DEFEAT 24/129
  (14:30 PvP: toggles steered to Citadel/Archives, zero Robotics in
  the log - selection and production obeyed; wiped min 11-13 in
  4Gate+DT chaos, 443 ordinary votes, no mechanism), Hao Pan
  DEFEAT 180/612 (34 min PvT carriers: 1:3.4 grind, 603 ordinary
  votes dominated by OutOfContact/No at advantage, no flap -
  out-comped late loss, toggles N/A outside PvP).
- S2 jar ladder validation: BetaStar DEFEAT 20/118 (13 min PvPDT
  vs 4GateGoon on TauCross: wiped min 7, same known
  gameplan-selection gap as SINK2/REMATCH; unit timeline ordinary -
  duels/approach/abuse, single-digit transitions, no flap).
  Crona DEFEAT 23/51 (7 min PvZSpeedlot vs ZerglingOnly on TauCross:
  overrun min 4-5 by 3HatchSpeedling; zealot timeline ordinary -
  Holding duel/Brawl/ChokeHold/PackRegroup, retreat reasons
  No/OutOfContact/CantFlee, single-digit transitions, no flap -
  out-massed composition loss, no mechanism).
  Terminus DEFEAT 38/132 (13 min PvTDoubleRobo vs FD on TauCross:
  grind loss with doctrine rows firing (ArtilleryKeepOff, Abuse,
  ZealotLeash); retreats technique-local only, bus never held (sim
  kept voting fight while behind - sim tuning, pre-existing, out
  of S2 scope); no S2 mechanism).
  Audit-jar Hao Pan DEFEAT 142/277 (20 min PvTDoubleRobo vs
  2FacVultures on TauCross: fair grind, better ratio than the
  180/612 baseline; ArtilleryKeepOff dominates sanely all game,
  Holding duel/Retreat/Approach/Abuse/KiteAway/TankHold all firing,
  bus-held retreats present with ordinary reasons, single-digit
  transitions - no mechanism).
- UMS on the toggle jar: full batch 9W/13L/3NA, then re-run wins
  on 1DragWounded + 1DragLing (flippers flip back); 3DragHydra
  timeouts sit inside its all-time flip distribution (8W/9L);
  Reav_v_Drag double NOATTACH is box flake (won before). Structural
  walls unchanged (Sunkens ammo, DT detection, mop-up timeouts).
  No UMS regression from toggles, standoff bonus or halves.
- Jar: sink + toggles + standoff bonus + halves + lone-march
  duel gate (all `strings`-verified, slot md5 matches target).

## Needs a live game (code green, jar includes it)

- Wall-in doctrine. Needs a PvT wall-in (`details/wall-in.md`).
- Stalled-duel fetch. Needs Sunkens3 (`details/sunkens.md`).
- Retreat package + lonely + technique terminal + duel-hold gate.
  BetaStar VICTORY 261/75 (flap gone); BetaStar DEFEAT 48/137
  (13:53 grind loss, outmassed late, no flap/freeze found -
  fair loss, not a mechanism bug) (`details/retreat.md`).
  Then Crona, Terminus.
- DT leash, MineDefuse, vulture cannons, bank gates, weak-PvP hold,
  HT-vs-rejoin, artillery-vs-duels. Needs the triggering games
  (`details/pending-specs.md`).
- 3Zeal VERIFIED 3-0 f464 (regroup 28 -> 4). No re-run needed.
- UMS mirrors (Fight force + focus 4->3 + kite61 + far-wait):
  2Zeal parked hard (2v6 upgraded, kills 3-5; needs melee shared
  pick - unbuilt); 1Drag diagnosed (Move/Attack dither ~10f,
  0 kills x4; gun-discipline or hill check open); 1DragWounded
  still losing (`details/kite.md`, `details/mirror-focus.md`).

## Owner decisions (no code before the call)

- Mirror focus gate (taken: 4->3 everywhere, `details/mirror-focus.md`)
  + progressing-duel ferry question (`details/sunkens.md`).
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
  filter, ArmedCordon edge feed, nearestArmed canAttack filter,
  Scout hold edge, ZealotPack edge feed, Reposition domain urgency,
  KiteTo air frame, Suicide ghost charge): KiteAway squad-mate margin
  gating (pack coherence vs back-line kite), dogfight 0.25 band,
  carrier-hunt and minedefuse release bands, escaping-toggle
  smoothing, SuicideMission path/euclidean + squadInZone latch,
  FormationHold Reaver hold, MeleeKiteAway dragoon-as-mate counting.
