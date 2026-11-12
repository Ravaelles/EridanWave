#!/usr/bin/env bash
# One finished game -> one dashboard row.
#
# Usage: bash scripts/dashboard-record.sh <run-dir> <map> [enemy-race]
#
# Called by scripts/wine-run.sh after it archives a run, so every game
# started through run.sh shows up in dashboard/data/history.jsonl and in the
# history.js snapshot that dashboard/index.html can load straight from disk
# (file:// cannot fetch, but it can load a same-folder script).
# Row schema matches scripts/ums-batch.sh; a run that never attached to a
# game records nothing. Re-recording the same run is a no-op.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="${1:-}"
MAP_NAME="${2:-?}"
ENEMY="${3:-?}"

if [ -z "$RUN_DIR" ] || [ ! -d "$RUN_DIR" ]; then
  echo "[dashboard] no run directory, nothing to record" >&2
  exit 0
fi

python3 - "$REPO" "$RUN_DIR" "$MAP_NAME" "$ENEMY" <<'PY'
import datetime
import json
import os
import re
import sys

repo, run_dir, map_name, enemy = sys.argv[1:5]


def read(name):
    path = os.path.join(run_dir, name)
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            return f.read()
    except OSError:
        return ""


log = read("WINE_LOG")
if "HELLO_ERIDANWAVE" not in log:
    print("[dashboard] run never attached, not recorded")
    sys.exit(0)
debug = read("WINE_DEBUG")

stamp = os.path.basename(run_dir.rstrip("/")).split("_")[:2]
try:
    date = datetime.datetime.strptime("_".join(stamp), "%Y%m%d_%H%M%S").replace(
        tzinfo=datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
except ValueError:
    date = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

if "VICTORY!" in log:
    result = "VICTORY"
elif "Defeat" in log:
    result = "DEFEAT"
else:
    result = "UNKNOWN"


def number(pattern, text, cast=int):
    found = re.search(pattern, text)
    return cast(found.group(1)) if found else None


kind = "ums" if re.search(r"(^|/)ums/", map_name.lower().replace("\\", "/")) else "melee"
if enemy in ("", "?"):
    enemy = "UMS" if kind == "ums" else "?"

row = {
    "date": date,
    "kind": kind,
    "map": map_name,
    "enemy": enemy,
    "result": result,
    "frames": number(r"Exiting after game end at frame (\d+)", debug),
    "killed": number(r"Killed:\s*(\d+)", log),
    "lost": number(r"Lost:\s*(\d+)", log),
    "times_hit": number(r"Times hit:\s*(\d+)", log),
    "duration_s": number(r"Total time:\s*(\d+) seconds", log),
    "errors": "?",
}

data_dir = os.path.join(repo, "dashboard", "data")
os.makedirs(data_dir, exist_ok=True)
history = os.path.join(data_dir, "history.jsonl")

rows = []
try:
    with open(history, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
except FileNotFoundError:
    pass

if any(r.get("date") == date and r.get("map") == map_name for r in rows):
    print(f"[dashboard] run {date} already recorded")
else:
    rows.append(row)
    with open(history, "a", encoding="utf-8") as f:
        f.write(json.dumps(row) + "\n")
    print(f"[dashboard] recorded {result} {row['killed']}/{row['lost']} on {map_name}")

with open(os.path.join(data_dir, "history.js"), "w", encoding="utf-8") as f:
    f.write("window.HISTORY_DATA = " + json.dumps(rows) + ";\n")
PY
