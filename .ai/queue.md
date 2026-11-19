# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.

## Now

- BetaStar rematch RUNNING (GAME_8222603D, clean jar 16:39 with lonely
  gate): read `Retreating(...)` parens, confirm no rout with no foe
  inside 12 tiles. Then Crona, Terminus.
- Jar fresh 16:39, slot synced. Tree clean (d274575c6 mirror suites).

## Needs a live game (code green, jar includes it)

- Wall-in: depot-only, SCV-first, lifted-unblocks, ban below 5
  goons per sieged tank (release 195 supply / 1200 minerals, re-arm
  165), no dress near walls. Needs a PvT wall-in.
- Stalled-duel fetch (6s quiet, in-gun). Needs Sunkens3. Sunkens2 is
  an ammo wall (5 scarabs vs ~970 HP, no income): map fix, not code.
- Retreat package (overrule at >=1.5 skim, parens, floor 48f) plus
  lonely gate (no enemy warrior inside 12 tiles, both phases). Needs
  a strong-army grind.
- DT leash (no worker/loner hunt inside 60 of enemy main; duel
  inside 1.1 always fights). Needs a DT raid game.
- MineDefuse (mines inside 10, no threat inside 12). Needs PvT mines.
- Vulture cannons (4+ Vultures -> 2 per expo). Needs PvT vultures.
- Bank gates (minerals > 800 -> 13 gateways; 3+ bases forward of the
  natural choke) plus weak-PvP natural hold (ASU <= 140, < 10
  warriors). Needs a macro game.
- HT cast suppresses rejoin; artillery outranks duels (hug keeps its
  exception, 1-tile release latch). Needs a Templar / sieged-tank game.
- 3Zeal VERIFIED 3-0 f464 (regroup 28 -> 4). No re-run needed.

## Owner decisions (no code before the call)

- FocusFire minShooters = 4 shuts 3v3 focus off; StarEngine says
  focus and plant win crossed (d274575c6). Lower the gate or accept
  variance? Same question for ferrying progressing duels (Sunkens3
  grinds on foot, never ferries).
- Carrier vs sieged tank with no other AA: implemented as attack.
  Confirm or restate.
- Natural crescent hold: spec only (natural choke + N tiles, 40s,
  only if ASU <= 140 vs Protoss). Needs guard-machinery reading.

## Parked (do not touch)

- Contain carve-out, C6 profiling, krasi0, Sunkens2 mission fallback,
  refresh-hop timing. Goon_v_DT + DT_v_Protoss unwinnable without
  detection: leave alone.
