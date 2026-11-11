# scripts/ — what runs games and reads them

Play: `run.sh --map=...` (Wine, THE way) · `wine-run.sh` (its backend)
· `list-maps.sh` (find maps) · `smoke.sh` / `verify-drop.sh` (gates)
· `ums-smoke.sh` (UMS duel gate: fought to a result, not seceded)
· `run-batch.sh` / `run-tuning.sh` (batches) · `deploy-eridan-slot.sh` (scbw jar)
Read: `summarize-cherryvis.py` (trace) · `summarize-errors.py` (errors).
Probe (UMS micro has no trace): arm keys in `bwapi-data/write/probe.txt`
(one per line: `Act`, `Move`, `Sim`), then `probe.py <normal.log> [--unit ID] [--key KEY]`.
Legacy/Windows-only: `test_*.ps1` (ignore on Linux).
Details: `../.ai/tools.md`.
