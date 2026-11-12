#!/usr/bin/env bash
# UMS batch survey: run a curated subset of micro maps headless, one after
# another, and tabulate how the bot fares on each. A survey, not a gate:
# defeats are data (which matchups need work), only infra failures fail.
#
# Usage: bash scripts/ums-batch.sh [--timeout SECS] [--speed N] [--only SUB]
#   defaults: 100 s real time per map (INCONCLUSIVE on expiry), speed 0,
#   all maps. --only runs the subset whose path contains SUB.
#   Results append to out/ums-batch-results.tsv (map, result, frames, errors).
#
# Map notes live in MAPS below: one line per map. Mark dead ones BROKEN with
# the reason instead of deleting them, so reruns stay reproducible. New UMS
# maps (carriers welcome) get a line here when they arrive.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

TIMEOUT=100
SPEED=0
ONLY=""
for arg in "$@"; do
  case "$arg" in
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    --speed=*) SPEED="${arg#--speed=}" ;;
    --only=*) ONLY="${arg#--only=}" ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# path | note (BROKEN = skip with reason; empty = untested, will be graded)
MAPS=(
  "ums/rav/protoss/1Drag_v_1Drag.scm|symmetric warmup"
  "ums/rav/protoss/1Drag_v_1ZealZoo.scm|zealot zoo"
  "ums/rav/protoss/1Drag_v_1Zeal.scm|"
  "ums/rav/protoss/1Drag_v_1Ling.scm|"
  "ums/rav/protoss/1Zeal_v_2Ling.scm|"
  "ums/rav/protoss/1Drag_v_2Ling.scm|"
  "ums/rav/protoss/2Drag_v_2Drag.scm|"
  "ums/rav/protoss/3Drag_v_3Drag.scm|"
  "ums/rav/protoss/3Zeal_v_3Zeal.scm|"
  "ums/rav/protoss/1Drag_v_2Hydra.scm|"
  "ums/rav/protoss/1Drag_v_1Hydra.scm|"
  "ums/rav/protoss/14Drag_v_16Drag.scm|down-a-dragoon duel"
  "ums/rav/protoss/15Drag_v_16Drag.scm|down-a-dragoon duel"
  "ums/rav/protoss/16Drag_v_16Drag.scm|even duel"
  "ums/rav/protoss/10Zeal_v_Lings.scm|"
  "ums/rav/protoss/2Drag_v_3Zeal.scm|"
  "ums/rav/protoss/3Drag_v_Zealots.scm|"
  "ums/rav/protoss/1Drag_v_4Zeal.scm|"
  "ums/rav/protoss/1DragWounded_v_1Zeal.scm|wounded start"
  "ums/rav/protoss/1Zeal_v_LingUp.scm|upgraded lings"
  "ums/rav/protoss/2Zeal_v_LingUp.scm|"
  "ums/rav/protoss/1DragUpg_v_2HydraUpg.scm|upgraded both sides"
  "ums/rav/protoss/2DragUpg_v_3HydraUpg.scm|"
  "ums/rav/protoss/3Drag_v_HydraUpg.scm|"
)

RESULTS="$REPO/out/ums-batch-results.tsv"
mkdir -p "$(dirname "$RESULTS")"

WINS=0; LOSSES=0; SKIPPED=0; INCONCLUSIVE=0; NOATTACH=0
run_one() {
  local map="$1"
  echo "=== ums-batch: $map ==="
  START=$(date +%s)
  rm -f bwapi-data/write/errors.json
  bash scripts/run.sh --map="$map" --race=Protoss --headless --speed="$SPEED" --cherryvis &
  RUN_PID=$!
  END=$(( START + TIMEOUT ))
  while kill -0 "$RUN_PID" 2>/dev/null; do
    [ "$(date +%s)" -ge "$END" ] && break
    sleep 5
  done
  if kill -0 "$RUN_PID" 2>/dev/null; then
    echo "[ums-batch] TIMEOUT after ${TIMEOUT}s, killing (INCONCLUSIVE)"
    kill "$RUN_PID" 2>/dev/null
    sleep 2
    pkill -9 -x StarCraft.exe 2>/dev/null
    pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
    pkill -9 -x java.exe 2>/dev/null
    wait "$RUN_PID" 2>/dev/null
    echo -e "$map\tTIMEOUT\t?\t?" >> "$RESULTS"
    INCONCLUSIVE=$((INCONCLUSIVE + 1))
    return 0
  fi
  wait "$RUN_PID" 2>/dev/null
  local result="NOATTACH"
  grep -aq "HELLO_ERIDANWAVE" out/wine/WINE_LOG 2>/dev/null || {
    echo "FAIL attach (no game)"; echo -e "$map\tNOATTACH\t?\t?" >> "$RESULTS"; NOATTACH=$((NOATTACH + 1)); return 0
  }
  if grep -aq "VICTORY!" out/wine/WINE_LOG 2>/dev/null; then result="VICTORY"; WINS=$((WINS + 1));
  elif grep -aq "Defeat" out/wine/WINE_LOG 2>/dev/null; then result="DEFEAT"; LOSSES=$((LOSSES + 1));
  else result="UNKNOWN"; LOSSES=$((LOSSES + 1)); fi
  local frame
  frame="$(grep -ao "Exiting after game end at frame [0-9]*" out/wine/WINE_DEBUG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
  local high="?"
  if [ -f bwapi-data/write/errors.json ]; then
    high="$(python3 -c "import json;print(sum(1 for k in json.load(open('bwapi-data/write/errors.json')) if k.get('priority')=='high'))")"
  fi
  echo "fought to $result (frame ${frame:-?}, high-priority errors $high)"
  echo -e "$map\t$result\t${frame:-?}\t$high" >> "$RESULTS"
}

for entry in "${MAPS[@]}"; do
  map="${entry%%|*}"
  note="${entry#*|}"
  [ -n "$ONLY" ] && [[ "$map" != *"$ONLY"* ]] && continue
  case "$note" in BROKEN*) echo "=== ums-batch: $map (SKIP: $note) ==="; SKIPPED=$((SKIPPED + 1)); continue ;; esac
  run_one "$map"
done

echo "=== ums-batch RESULT: $WINS W / $LOSSES L, $SKIPPED skipped, $INCONCLUSIVE inconclusive, $NOATTACH no-attach ==="
echo "(appended to out/ums-batch-results.tsv)"
