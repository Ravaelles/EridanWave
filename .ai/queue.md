# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- UMS failing-maps round done: ZealDrag (4-2), 3Drag, 16Drag,
  3DragHydra, Reav_v_Drag, 14Drag all VICTORY; 1DragWounded 2-3
  on long grinds; 2Zeal parked hard. Only structural walls left
  (Sunkens ammo, DT detection, mop-up surrounds decision,
  Sunkens3 chase rule).
- Jar fresh, slot synced. Tree clean except 4 line-ending M flags
  (verified byte-identical, normalize on next touch).

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
