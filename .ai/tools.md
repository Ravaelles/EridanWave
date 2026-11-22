# Tools (read me after context reset)

## Run games - only these two ways, never bare StarCraft

- `bash scripts/run.sh --map=<path> [--headless ...]`: THE way.
  Headless+Xvfb is default; builds the jar if missing. Map via
  `bash scripts/list-maps.sh [filter]`.
- scbw path: `/sc-ai/BOTS/__PlayAsEridan/_<Enemy>.sh` (all route
  through `__GENERIC-RUN-ERIDAN.sh`). No seeds on the Wine path.
- UMS micro maps self-start headless; UMS writes no CherryVis trace
  (`!umsMode`) - use `probe.py`, never traces.

## Read a game (cheapest first)

- `python3 scripts/summarize-cherryvis.py <trace.json|dir> [--unit ID] [--acts]`
- `python3 scripts/summarize-errors.py <errors.json> [--min-priority ...]`
- Dashboard: `dashboard/index.html` opens from disk (rows recorded
  per game; hide rows with the trash button, file untouched).
- `python3 scripts/probe.py <normal.log> [--unit ID] [--key KEY]`
  (per-unit chains on traceless UMS: `Act`, `Move`, `Sim`, `Form`,
  `Hold`, `Target`; `probe.txt` arm keys).
- Full behavior sequences (cycle hunting): arm
  `bwapi-data/write/behavior.txt` (`*` or unit ids, one per line),
  run the map, then `python3 scripts/detect-cycles.py
  bwapi-data/write/behavior.log [--unit ID] [--min N]` - ranks which
  behavior pairs actually alternate per unit (change-only PROBE Act
  rows hide repeats, this file keeps every frame).
- `bash scripts/run.sh --map=... --formation-audit` +
  `python3 scripts/formation-audit.py` (flag-gated pack/slot tally).
- After moving a class across packages: `mvn -o clean` (stale zinc
  kills the 2.12 backend).
- Traces: wine `bwapi-data/write/cherryvis_dir.rep.cvis/`; scbw
  `~/.scbw/games/GAME_*/write_0/...` (checker depth 3, not 2).
- Per-game logs: `out/wine/WINE_LOG`, `WINE_DEBUG`,
  `bwapi-data/write/*.normal.log.txt`, `errors.json` (absent = clean).

## Verify ritual (every change)

`python3 scripts/check-blank-lines.py <touched>` +
`scripts/tests.sh` (base; StarEngine excluded by tag) +
`./build-jar.sh` (auto-syncs the slot jar). Then commit per unit.
StarEngine alone: `scripts/starengine.sh [<Suite>...]`
(sources in `tests-starengine/`, pilots decide, engine advances).

## Gotchas

- Never edit `scripts/wine-run.sh` mid-run (bash reparses, kills teardown).
- New Scala = CRLF, scripts = LF; blank line after `return`.
- Human-only UMS maps idle headless: `list-maps.sh` hides them,
  `run.sh` refuses them - do not force-launch.
- Box stuck in NOATTACH: kill leftovers, `wineserver -k`, retry;
  still dead, prove game vs table side once by hand, then resume.
- Distances are tiles unless the name says pixels (rule 16).
- Full docs: `.ai/conventions.md`, `.ai/architecture.md`.
