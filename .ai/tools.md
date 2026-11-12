# Tools (read me after context reset)

## Run games - only these two ways, never bare StarCraft
- `bash scripts/run.sh --map=<path> [--race= --enemy-race= --headless --speed=0 --cherryvis]`:
  THE way. Headless+Xvfb is default; builds the jar if missing. Map via
  `bash scripts/list-maps.sh [filter]`.
- scbw path: `/sc-ai/BOTS/__PlayAsEridan/_<Enemy>.sh` (all route through
  `__GENERIC-RUN-ERIDAN.sh`; new launchers via `__CREATE-BOT-LAUNCHER.sh`).
  No seeds on the Wine path (host sets the seed; scbw has `seed_override`).
- UMS micro maps self-start headless now (briefing dismissed by keypress);
  UMS writes NO CherryVis trace (`!umsMode` gate) - use `probe.py` on the
  debug log instead, never traces.

## Read a game (cheapest first)
- `python3 scripts/summarize-cherryvis.py <trace.json|dir> [--unit ID] [--acts]`:
  move/hold counts per unit (scout smoothness), acts, kills with balances.
- `python3 scripts/summarize-errors.py <errors.json> [--min-priority ...]`:
  one line per key, worst first.
- Dashboard: `dashboard/index.html` opens straight from disk. Every finished
  `run.sh` game is recorded by `scripts/dashboard-record.sh` (called from
  `wine-run.sh`) into `dashboard/data/history.jsonl` plus the
  `history.js` snapshot the page can load under `file://`; `ums-batch.sh`
  opts out and writes its own richer rows. Rows can be hidden per browser
  with the row's trash button (localStorage, the file is untouched).
- `python3 scripts/probe.py <normal.log> [--unit ID] [--key KEY]`:
  per-unit decision chains on traceless UMS games (arm keys in
  `bwapi-data/write/probe.txt`: `Act` decision chains, `Move` ordered travel,
  `Sim` stance votes, `Formation` pack audit for `formation-check.py`, `Form`
  formation census, `Hold` line-hold decisions, `Target` chosen enemy id).
- Traces: wine `bwapi-data/write/cherryvis_dir.rep.cvis/`; scbw
  `~/.scbw/games/GAME_*/write_0/...` auto-paired to `E_*.rep.cvis`
  (needs default-on cherryvis; checker depth is 3, not 2).
- Per-game logs: `out/wine/WINE_LOG` (score box), `WINE_DEBUG` (frames,
  exceptions), `bwapi-data/write/*.normal.log.txt` (births/tech),
  `errors.json` (absent = clean), replays in `/sc-ai/REPLAYS/`.

## Verify ritual (every change, one command chain)
`python3 scripts/check-blank-lines.py <touched>` +
`mvn -o -Ptests test` (MAVEN_OPTS add-opens from `build-jar.sh:30`) +
`./build-jar.sh` (auto-syncs the scbw slot jar). Then commit per unit.

## Gotchas that already burned us
- Never edit `scripts/wine-run.sh` mid-run (bash reparses, kills teardown).
- New Scala = CRLF, scripts = LF; blank line after `return`.
- Distances are tiles unless the name says pixels (rule 10).
- `scripts/verify-drop.sh` (micro gate), `scripts/smoke.sh` (melee gate).
- Full docs: `.ai/conventions.md` (rules), `.ai/architecture.md` (levels),
  `.ai/knowledge.md` (vendor/build).
- TestKit: `testkit.TestKit` seeds a game-free world (Mockito dep,
  test scope); boundary in `todo.md`.

## Planned tooling (not built)
- Replay reader: full plan in `.ai/replay-reader.md` (commands first,
  OpenBW re-sim later for positions).
- Replay reader: trace has orders, never positions - per-frame positions
  from `.rep` would unlock true movement analysis (scout speed, spread
  radii). Needs a replay parser; nothing in-repo reads `.rep` today.
