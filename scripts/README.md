# scripts/ — what runs games and reads them

Play: `run.sh --map=...` (Wine, THE way) · `wine-run.sh` (its backend)
· `list-maps.sh` (find maps) · `smoke.sh` / `verify-drop.sh` (gates)
· `ums-smoke.sh` (UMS duel gate: fought to a result, not seceded)
· `ums-batch.sh` (UMS survey: map subset headless one-by-one + results TSV;
  appends JSONL rows to `dashboard/data/history.jsonl`)
· `dashboard/` (results page: `index.html` + `app.js` + `style.css`, zero
  deps; serve with `python3 -m http.server`, melee rows reuse the schema)
· `run-batch.sh` / `run-tuning.sh` (batches) · `deploy-eridan-slot.sh` (scbw jar)
Read: `summarize-cherryvis.py` (trace) · `summarize-errors.py` (errors).
Probe (UMS micro has no trace): arm keys in `bwapi-data/write/probe.txt`
(one per line: `Act`, `Move`, `Sim`), then `probe.py <normal.log> [--unit ID] [--key KEY]`.
Legacy/Windows-only: `test_*.ps1` (ignore on Linux).
Details: `../.ai/tools.md`.
