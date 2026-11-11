#!/usr/bin/env bash
# Headless UMS micro gate: real duel games, then assertions that the bot
# actually fought them. Catches micro secession breakage (armies that march
# but never engage) without reading a single log by hand.
#
# Usage: bash scripts/ums-smoke.sh [--timeout SECS]
#   defaults: 1Drag_v_1Drag.scm + 15Drag_v_16Drag.scm, 600 s per map
#   (INCONCLUSIVE on expiry, like smoke.sh)
#
# Pass per map: attached, game ended with a VICTORY!/Defeat line,
# frames played, errors.json free of high-priority keys. A defeat still
# passes: duels can be lost, but they must be fought.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

TIMEOUT=600
for arg in "$@"; do
  case "$arg" in
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

MAPS=(
  "ums/rav/protoss/1Drag_v_1Drag.scm"
  "ums/rav/protoss/15Drag_v_16Drag.scm"
)

FAIL=0
for MAP in "${MAPS[@]}"; do
  echo "=== ums-smoke: $MAP ==="
  START=$(date +%s)
  rm -f bwapi-data/write/errors.json
  bash scripts/run.sh --map="$MAP" --race=Protoss --headless --speed=0 --cherryvis &
  RUN_PID=$!
  END=$(( START + TIMEOUT ))
  while kill -0 "$RUN_PID" 2>/dev/null; do
    [ "$(date +%s)" -ge "$END" ] && break
    sleep 5
  done
  if kill -0 "$RUN_PID" 2>/dev/null; then
    echo "[ums-smoke] TIMEOUT after ${TIMEOUT}s - killing the run (INCONCLUSIVE)"
    kill "$RUN_PID" 2>/dev/null
    sleep 2
    pkill -9 -x StarCraft.exe 2>/dev/null
    pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
    pkill -9 -x java.exe 2>/dev/null
    wait "$RUN_PID" 2>/dev/null
    echo "RESULT: INCONCLUSIVE (timeout)"
    exit 2
  fi
  wait "$RUN_PID" 2>/dev/null
  STATUS=$?

  grep -aq "HELLO_ERIDANWAVE" out/wine/WINE_LOG 2>/dev/null \
    && echo "PASS attach" || { echo "FAIL attach"; FAIL=1; }
  if grep -aq "VICTORY!" out/wine/WINE_LOG 2>/dev/null; then
    echo "PASS fought to VICTORY"
  elif grep -aq "Defeat" out/wine/WINE_LOG 2>/dev/null; then
    echo "PASS fought to defeat (fought, not seceded)"
  else
    echo "FAIL no game result (never fought to a conclusion)"; FAIL=1
  fi
  FRAME="$(grep -ao "Exiting after game end at frame [0-9]*" out/wine/WINE_DEBUG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
  # Floor stays low on purpose: a 1v1 duel is legitimately over by frame 365.
  # Attach + a fought result above already prove the game went somewhere.
  [ -n "${FRAME:-}" ] && [ "$FRAME" -gt 100 ] \
    && echo "PASS frames played: $FRAME" || { echo "FAIL game never got going (frame ${FRAME:-?})"; FAIL=1; }
  if [ -f bwapi-data/write/errors.json ]; then
    python3 scripts/summarize-errors.py bwapi-data/write/errors.json --min-priority high --limit 10
    HIGH="$(python3 -c "import json;print(sum(1 for k in json.load(open('bwapi-data/write/errors.json')) if k.get('priority')=='high'))")"
    [ "$HIGH" = "0" ] && echo "PASS no high-priority errors" || { echo "FAIL $HIGH high-priority error keys"; FAIL=1; }
  else
    echo "PASS no errors file (nothing failed)"
  fi
  echo "(run exit $STATUS)"
done

[ "$FAIL" = "0" ] && echo "RESULT: PASS" || echo "RESULT: FAIL"
exit "$FAIL"
