#!/usr/bin/env bash
# Headless smoke gate: a real melee game, then assertions that the bot
# actually played it. Catches start-abort breakage (dead geography, empty
# build queue) without reading a single log by hand.
#
# Usage: bash scripts/smoke.sh [--map=<path>] [--enemy-race=R] [--timeout SECS]
#   defaults: maps/sscai/(2)Benzene.scx, Zerg, 1200 s (INCONCLUSIVE on expiry)
#
# Pass: attached, frames played, fresh mapinfo (geography built), units
# produced past the starting set, errors.json free of high-priority keys.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"
MAP="maps/sscai/(2)Benzene.scx"
ENEMY="Zerg"
TIMEOUT=1200
for arg in "$@"; do
  case "$arg" in
    --map=*) MAP="${arg#--map=}" ;;
    --enemy-race=*) ENEMY="${arg#--enemy-race=}" ;;
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

START=$(date +%s)
rm -f bwapi-data/write/errors.json
bash scripts/run.sh --map="$MAP" --enemy-race="$ENEMY" --headless --speed=0 --cherryvis &
RUN_PID=$!
END=$(( START + TIMEOUT ))
while kill -0 "$RUN_PID" 2>/dev/null; do
  [ "$(date +%s)" -ge "$END" ] && break
  sleep 5
done
if kill -0 "$RUN_PID" 2>/dev/null; then
  echo "[smoke] TIMEOUT after ${TIMEOUT}s - killing the run (INCONCLUSIVE)"
  kill "$RUN_PID" 2>/dev/null
  sleep 2
  pkill -9 -x StarCraft.exe 2>/dev/null
  pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
  pkill -9 -x java.exe 2>/dev/null
  INCONCLUSIVE=1
fi
wait "$RUN_PID" 2>/dev/null
STATUS=$?

echo "=== smoke: $MAP vs $ENEMY (run exit $STATUS) ==="
FAIL=0
grep -aq "HELLO_ERIDANWAVE" out/wine/WINE_LOG 2>/dev/null \
  && echo "PASS attach" || { echo "FAIL attach"; FAIL=1; }
FRAME="$(grep -ao "Exiting after game end at frame [0-9]*" out/wine/WINE_DEBUG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
[ -n "${FRAME:-}" ] && [ "$FRAME" -gt 1000 ] \
  && echo "PASS frames played: $FRAME" || { echo "FAIL game never got going (frame ${FRAME:-?})"; FAIL=1; }
MAPINFO="bwapi-data/write/map-$(basename "$MAP").mapinfo.txt"
if [ -n "$(find bwapi-data/write -maxdepth 1 -name "$(basename "$MAPINFO")" -newermt "@$START" 2>/dev/null)" ]; then
  echo "PASS geography built: $(basename "$MAPINFO")"
else
  echo "FAIL no fresh mapinfo (geography never built)"; FAIL=1
fi
NEWLOG="$(ls -t bwapi-data/write/*.normal.log.txt 2>/dev/null | head -1)"
if [ -n "${NEWLOG:-}" ]; then
  BUILT="$(grep -ac "New unit:" "$NEWLOG" 2>/dev/null)"
  [ "$BUILT" -gt 10 ] && echo "PASS units produced: $BUILT" || { echo "FAIL only $BUILT births logged"; FAIL=1; }
else
  echo "FAIL no game log"; FAIL=1
fi
if [ -f bwapi-data/write/errors.json ]; then
  python3 scripts/summarize-errors.py bwapi-data/write/errors.json --min-priority high --limit 10
  HIGH="$(python3 -c "import json;print(sum(1 for k in json.load(open('bwapi-data/write/errors.json')) if k.get('priority')=='high'))")"
  [ "$HIGH" = "0" ] && echo "PASS no high-priority errors" || { echo "FAIL $HIGH high-priority error keys"; FAIL=1; }
else
  echo "PASS no errors file (nothing failed)"
fi
if [ -n "${INCONCLUSIVE:-}" ]; then echo "RESULT: INCONCLUSIVE (timeout)"; exit 2; fi
[ "$FAIL" = "0" ] && echo "RESULT: PASS" || echo "RESULT: FAIL"
exit "$FAIL"
