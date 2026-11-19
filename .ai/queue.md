# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- BetaStar rematch RUNNING (GAME_8222603D, clean jar 16:39 with lonely
  gate): read `Retreating(...)` parens, confirm no rout with no foe
  inside 12 tiles (see `details/retreat.md`). Then Crona, Terminus.
- Jar fresh 16:39, slot synced. Tree clean (d274575c6 mirror suites).

## Needs a live game (code green, jar includes it)

- Wall-in doctrine. Needs a PvT wall-in (`details/wall-in.md`).
- Stalled-duel fetch. Needs Sunkens3 (`details/sunkens.md`).
- Retreat package + lonely gate. Needs a strong-army grind
  (`details/retreat.md`).
- DT leash, MineDefuse, vulture cannons, bank gates, weak-PvP hold,
  HT-vs-rejoin, artillery-vs-duels. Needs the triggering games
  (`details/pending-specs.md`).
- 3Zeal VERIFIED 3-0 f464 (regroup 28 -> 4). No re-run needed.

## Owner decisions (no code before the call)

- Mirror focus gate + progressing-duel ferry (`details/mirror-focus.md`,
  `details/sunkens.md`).
- Carrier vs sieged tank, natural crescent hold
  (`details/pending-specs.md`).

## Parked (do not touch)

- Contain carve-out, C6 profiling, krasi0, Sunkens2 mission fallback,
  refresh-hop timing. Goon_v_DT + DT_v_Protoss unwinnable without
  detection: leave alone.
