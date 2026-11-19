# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- UMS round in flight: 2Zeal/1DragWounded/15Drag/16Drag on the
  melee-slowling jar. 14v16 competitive (wins f1355/f1685, losses),
  ZealDrag won twice 9/6 + 9/7, 15/16Drag flipped to VICTORY.
- Jar fresh, slot synced. Tree clean (27149e81c slowling veto).

## Needs a live game (code green, jar includes it)

- Wall-in doctrine. Needs a PvT wall-in (`details/wall-in.md`).
- Stalled-duel fetch. Needs Sunkens3 (`details/sunkens.md`).
- Retreat package + lonely gate + technique terminal. Validated:
  BetaStar VICTORY 261/75, f6900-7000 flap gone
  (`details/retreat.md`). Then Crona, Terminus.
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
