# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- Ladder validation in flight: BetaStar DEFEAT 48/137 (fair late
  grind loss, no flap), Crona DEFEAT 21/35 (greedy expand into
  9pool, known gameplan gap), Terminus running on the full jar.
- UMS: full batch 12W/11L; ZealDrag 4-3, 14v16 competitive,
  LingsHydra timeouts fixed twice, 1Drag/1DragWounded flip wins,
  2Zeal parked hard, Sunkens/DT walls stand.
- Jar verified content-complete (all five units + duel gate +
  creep window present by `strings` check).

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
