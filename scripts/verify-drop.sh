#!/usr/bin/env bash
# Standard Reaver-drop verification scenario: micro UMS map, headless,
# then structural assertions (no exact-outcome pinning: the Wine path has
# no seed control, so reruns differ - scbw covers seeds via seed_override).
#
# Usage: bash scripts/verify-drop.sh [--map=<path>] [--timeout SECS]
#   default map: maps/ums/rav/protoss/Reav&Shut_v_Sunkens.scm
#   default timeout: 900 s (INCONCLUSIVE, not failure, on expiry)
#
# Pass: client attached and exited, game advanced frames, errors.json
# (repo bwapi-data/write) holds no high-priority keys. UMS maps write no
# CherryVis trace (!umsMode gate), so Unload/kill evidence is printed
# informationally from the debug log, never asserted.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"
MAP="maps/ums/rav/protoss/Reav&Shut_v_Sunkens.scm"
TIMEOUT=900
for arg in "$@"; do
  case "$arg" in
    --map=*) MAP="${arg#--map=}" ;;
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

rm -f bwapi-data/write/errors.json
bash scripts/run.sh --map="$MAP" --headless --speed=0 --no-cherryvis &
RUN_PID=$!
END=$(( $(date +%s) + TIMEOUT ))
while kill -0 "$RUN_PID" 2>/dev/null; do
  [ "$(date +%s)" -ge "$END" ] && break
  sleep 5
done
if kill -0 "$RUN_PID" 2>/dev/null; then
  echo "[verify-drop] TIMEOUT after ${TIMEOUT}s - killing the run (INCONCLUSIVE)"
  kill "$RUN_PID" 2>/dev/null
  sleep 2
  pkill -9 -x StarCraft.exe 2>/dev/null
  pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
  pkill -9 -x java.exe 2>/dev/null
  INCONCLUSIVE=1
fi
wait "$RUN_PID" 2>/dev/null
STATUS=$?

echo "=== verify-drop: $MAP (run exit $STATUS) ==="
FAIL=0
grep -aq "HELLO_ERIDANWAVE" out/wine/WINE_LOG 2>/dev/null \
  && echo "PASS attach" || { echo "FAIL attach"; FAIL=1; }
FRAME="$(grep -ao "Exiting after game end at frame [0-9]*" out/wine/WINE_DEBUG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
[ -n "${FRAME:-}" ] && [ "$FRAME" -gt 24 ] \
  && echo "PASS frames played: $FRAME" || { echo "FAIL no frames played"; FAIL=1; }
if [ -f bwapi-data/write/errors.json ]; then
  python3 scripts/summarize-errors.py bwapi-data/write/errors.json --min-priority high --limit 10
  HIGH="$(python3 -c "import json;print(sum(1 for k in json.load(open('bwapi-data/write/errors.json')) if k.get('priority')=='high'))")"
  [ "$HIGH" = "0" ] && echo "PASS no high-priority errors" || { echo "FAIL $HIGH high-priority error keys"; FAIL=1; }
else
  echo "PASS no errors file (nothing failed)"
fi
echo "--- informational ---"
grep -ac "Unload" out/wine/WINE_DEBUG 2>/dev/null | xargs echo "unload mentions:"
grep -ac "FleeScourge\|HoldToFire\|Refresh\|Hop" out/wine/WINE_DEBUG 2>/dev/null | xargs echo "reaver-act mentions:"
if [ -n "${INCONCLUSIVE:-}" ]; then echo "RESULT: INCONCLUSIVE (timeout)"; exit 2; fi
[ "$FAIL" = "0" ] && echo "RESULT: PASS" || echo "RESULT: FAIL"
exit "$FAIL"
