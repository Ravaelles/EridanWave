#!/usr/bin/env bash
# Dragoon-vs-dragoon benchmark: N fast headless games on MAP1, then M on MAP2.
#
# Usage: bash benchmarks/goon_v_goon.sh [N [M [MAP1 [MAP2]]]] [--timeout SECS] [--speed N]
#   defaults: N=6 M=6, ums/rav/protoss/14Drag_v_16Drag.scm,
#   ums/rav/protoss/15Drag_v_16Drag.scm, 600 s per game, speed 0 (fastest).
#
# Each game delegates to scripts/run.sh (Wine lifecycle, dashboard row); this
# script only loops, guards each game with timeout, and tabulates results
# into out/benchmarks/goon_v_goon.tsv (appended, one row per game). Games run
# one at a time - never parallelize Wine games. Defeats are data; only games
# that never attach or hit the timeout fail the run (exit 1).
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

N=6
M=6
MAP1="ums/rav/protoss/14Drag_v_16Drag.scm"
MAP2="ums/rav/protoss/15Drag_v_16Drag.scm"
TIMEOUT=600
SPEED=0
POS=0
while [ $# -gt 0 ]; do
  arg="$1"; shift
  case "$arg" in
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    --timeout) TIMEOUT="${1:?missing value for --timeout}"; shift ;;
    --speed=*) SPEED="${arg#--speed=}" ;;
    --speed) SPEED="${1:?missing value for --speed}"; shift ;;
    --help|-h) sed -n '2,8p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    --*) echo "[goon] unknown arg: $arg" >&2; exit 2 ;;
    *)
      POS=$((POS + 1))
      case "$POS" in
        1) N="$arg" ;;
        2) M="$arg" ;;
        3) MAP1="$arg" ;;
        4) MAP2="$arg" ;;
        *) echo "[goon] ignoring extra arg: $arg" >&2 ;;
      esac ;;
  esac
done
[[ "$N" =~ ^[0-9]+$ ]] || { echo "[goon] N must be a non-negative int, got '$N'" >&2; exit 2; }
[[ "$M" =~ ^[0-9]+$ ]] || { echo "[goon] M must be a non-negative int, got '$M'" >&2; exit 2; }
[ "$(( N + M ))" -gt 0 ] || { echo "[goon] N + M must be at least 1" >&2; exit 2; }

# Fail fast on a wrong map: run.sh would otherwise silently play the previous one.
GAME_ROOT="${GAME_ROOT:-${WINEPREFIX:-$HOME/.wine}/drive_c/sc}"
for MAP in "$MAP1" "$MAP2"; do
  [ -f "$GAME_ROOT/maps/$MAP" ] || { echo "[goon] map not found: $GAME_ROOT/maps/$MAP" >&2; exit 2; }
done

STAMP="$(date +%Y%m%d_%H%M%S)"
OUTDIR="$REPO/out/benchmarks/goon_v_goon-$STAMP"
RESULTS="$REPO/out/benchmarks/goon_v_goon.tsv"
mkdir -p "$OUTDIR" "$(dirname "$RESULTS")"
[ -f "$RESULTS" ] || echo -e "date\tbatch\tmap\tgame\tresult\tframes\tkilled\tlost\ttimes_hit\tsecs\terrors" >> "$RESULTS"

INFRA=0
run_set() { # $1 = map, $2 = count, $3 = tag
  local map="$1" count="$2" tag="$3" i
  i=0
  while [ "$i" -lt "$count" ]; do
    i=$((i + 1))
    echo "[goon] $tag game $i/$count ($map)"
    local log="$OUTDIR/$tag-game-$i.log"
    local start
    start=$(date +%s)
    if ! timeout "$TIMEOUT" bash scripts/run.sh --map="$map" --race=Protoss --headless --speed="$SPEED" --no-cherryvis > "$log" 2>&1; then
      echo "[goon] $tag game $i: runner failed or timed out after ${TIMEOUT}s, reaping leftovers (see $log)"
      pkill -9 -x StarCraft.exe 2>/dev/null
      pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
      pkill -9 -x java.exe 2>/dev/null
      sleep 2
      echo -e "$(date -u +%Y-%m-%dT%H:%M:%SZ)\t$STAMP\t$map\t$tag-$i\tTIMEOUT\t?\t?\t?\t?\t$(( $(date +%s) - start ))\t?" >> "$RESULTS"
      INFRA=$((INFRA + 1))
      continue
    fi
    local result="UNKNOWN" frame="?" killed="?" lost="?" hits="?" errors="?"
    grep -aq "HELLO_ERIDANWAVE" out/wine/WINE_LOG 2>/dev/null || {
      echo "[goon] $tag game $i: no attach"
      echo -e "$(date -u +%Y-%m-%dT%H:%M:%SZ)\t$STAMP\t$map\t$tag-$i\tNOATTACH\t?\t?\t?\t?\t$(( $(date +%s) - start ))\t?" >> "$RESULTS"
      INFRA=$((INFRA + 1))
      continue
    }
    if grep -aq "VICTORY!" out/wine/WINE_LOG 2>/dev/null; then result="VICTORY"
    elif grep -aq "Defeat" out/wine/WINE_LOG 2>/dev/null; then result="DEFEAT"; fi
    frame="$(grep -ao "Exiting after game end at frame [0-9]*" out/wine/WINE_DEBUG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
    killed="$(grep -ao "Killed: [0-9]*" out/wine/WINE_LOG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
    lost="$(grep -ao "Lost: [0-9]*" out/wine/WINE_LOG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
    hits="$(grep -ao "Times hit: [0-9]*" out/wine/WINE_LOG 2>/dev/null | grep -ao "[0-9]*$" | tail -1)"
    if [ -f bwapi-data/write/errors.json ]; then
      errors="$(python3 -c "import json;print(sum(1 for k in json.load(open('bwapi-data/write/errors.json')) if k.get('priority')=='high'))" 2>/dev/null || echo "?")"
    fi
    [ -n "$frame" ] || frame="?"; [ -n "$killed" ] || killed="?"; [ -n "$lost" ] || lost="?"
    [ -n "$hits" ] || hits="?"
    echo "[goon] $tag game $i: $result (killed ${killed:-?}/lost ${lost:-?}, frame ${frame:-?})"
    echo -e "$(date -u +%Y-%m-%dT%H:%M:%SZ)\t$STAMP\t$map\t$tag-$i\t$result\t$frame\t$killed\t$lost\t$hits\t$(( $(date +%s) - start ))\t$errors" >> "$RESULTS"
  done
}

run_set "$MAP1" "$N" "a"
run_set "$MAP2" "$M" "b"

echo "=== goon_v_goon RESULT (appended to out/benchmarks/goon_v_goon.tsv) ==="
python3 - "$RESULTS" "$STAMP" <<'PYEOF'
import sys
from collections import defaultdict
path, stamp = sys.argv[1], sys.argv[2]
rows = [l.rstrip("\n").split("\t") for l in open(path)
        if l.strip() and not l.startswith("date") and len(l.split("\t")) > 2 and l.split("\t")[1] == stamp]
maps = defaultdict(list)
for r in rows:
    maps[r[2]].append(r)
for m, rs in maps.items():
    wins = sum(1 for r in rs if r[4] == "VICTORY")
    losses = sum(1 for r in rs if r[4] == "DEFEAT")
    other = [r for r in rs if r[4] not in ("VICTORY", "DEFEAT")]
    def avg(col):
        xs = [int(r[col]) for r in rs if len(r) > col and r[col].isdigit()]
        return f"{sum(xs) / len(xs):.1f}" if xs else "?"
    print(f"{m}: {len(rs)} games, {wins}W/{losses}L" + (f", {len(other)} infra/other" if other else "") +
          f", avg killed {avg(6)}/lost {avg(7)}/hits {avg(8)}, avg frames {avg(5)}")
PYEOF
[ "$INFRA" = "0" ] || { echo "[goon] $INFRA game(s) never attached or timed out -> infra failure" >&2; exit 1; }
echo "[goon] done: $N x $MAP1 + $M x $MAP2"
