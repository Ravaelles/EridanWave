# Replay reader plan (positions + enemy orders joined with trace.json)

## Why (the gap)
`trace.json` has OUR orders, never positions; `.rep` has COMMANDS, never
positions either. True movement analysis (scout path length, spread radii,
who idled where) needs positions, enemy build timing needs their commands.
Both live one parser away, in two tiers.

## Tier 1: command stream (cheap, do first)
- Parse `.rep` header (map, players, frame count) + command blocks for one
  pinned engine version (1.16.1 only): Select/Build/Train/Move/Attack.
- No dependency exists offline (no parser vendored, PyPI unreachable), so
  hand-roll against the community rep spec; validate on a known game by
  reproducing the enemy build order from the scouting log.
- Interface: `scripts/replay-orders.py <rep> [--player N]` prints a
  token-cheap timeline (frame, player, command, args), same style as
  `summarize-*.py`. Cross-checks OUR orders against `trace.json`.
- Risk: format quirks per map version; bound by supporting 1.16.1 reps only.

## Tier 2: positions via re-simulation (big, later)
- Positions emerge only from deterministic simulation: drive vendored
  `../OpenBW` headless over the rep, dump per-frame positions, join with
  `trace.json` on (frame, unit id). E2E precedent exists in this env
  (`../E2E OpenBW LOGS/`).
- Interface: `scripts/replay-positions.py <rep> [--unit ID]` prints path
  rollups (distance traveled, idle frames, max spread), never raw frames.
- Risks, in order: building OpenBW offline; engine-version determinism
  (rep must replay bit-identical); speed (full sim per query is minutes -
  cache the dump next to the rep).
- Acceptance: scout path length sane vs map distance; spread radii match
  the 9-tile rule on a spread-verified game.

## First spike (time-boxed, Tier 1 only)
Header + frame count + first 100 commands of `E_19:22:26 Steamhammer.rep`.
If the command framing parses clean, Tier 1 is a go; if not, stop and
reassess before touching OpenBW.
