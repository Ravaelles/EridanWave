#!/usr/bin/env bash
# Tuning batch runner: N sequential Wine games, one override vector per game.
#
# Usage:
#   bash scripts/run-batch.sh "<map>" N OUTDIR [--tuning-strategy=NAME] [KEY=VALUE ...] [--headless|--visible] [--speed=N] [--timeout=SEC] [--no-build]
#
# Each game runs scripts/wine-run.sh with --tuning-strategy= plus one
# --tune-param KEY=VALUE per KEY=VALUE arg (no scattered env files - the
# bot randomizes any registered param without an override). Default is
# headless (Xvfb :99, nothing on :0); pass --visible to watch.
# --speed=0 (fastest) is the default; --no-build plays the jar on disk
# instead of rebuilding it for every game. The bot exits after game end,
# so one game at a time - never parallelize Wine games.
#
# After each game the fixed-name tuning row (bwapi-data/write/tuning/last.csv,
# overwritten every game) is copied into OUTDIR before the next game wipes it.
# Staleness guard: a failed start leaves the previous game's file behind -
# only a row newer than the run counts, otherwise the game is INVALID.
#
# At the end a manifest.txt mirrors the batch (map, jar md5, per-game verdict
# plus param digest and vector) for the gradient loop to read.
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"

MAP=""
N=""
OUTDIR=""
STRATEGY=""
declare -a OVERRIDES=()
VISIBLE="--headless"
SPEED="0"
TIMEOUT=750
NO_BUILD=""
for arg in "$@"; do
  case "$arg" in
    --tuning-strategy=*) STRATEGY="${arg#--tuning-strategy=}" ;;
    --headless) VISIBLE="--headless" ;;
    --visible) VISIBLE="--visible" ;;
    --speed=*) SPEED="${arg#--speed=}" ;;
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    --no-build) NO_BUILD="--no-build" ;;
    *=*) OVERRIDES+=("${arg}") ;;
    *)
      if [ -z "$MAP" ]; then MAP="$arg";
      elif [ -z "$N" ]; then N="$arg";
      elif [ -z "$OUTDIR" ]; then OUTDIR="$arg";
      else echo "[batch] ignoring extra arg: $arg" >&2; fi ;;
  esac
done
[ -n "$MAP" ] && [ "$N" -gt 0 2>/dev/null ] && [ -n "$OUTDIR" ] || { echo "usage: run-batch.sh \"<map>\" N OUTDIR [--tuning-strategy=NAME] [KEY=VALUE ...] [--headless|--visible] [--speed=N] [--timeout=SEC] [--no-build]" >&2; exit 1; }

WINEPREFIX="${WINEPREFIX:-$HOME/.wine}"
GAME_ROOT="${GAME_ROOT:-$WINEPREFIX/drive_c/sc}"
TUNING_ROW="$GAME_ROOT/bwapi-data/write/tuning/last.csv"

mkdir -p "$OUTDIR"
CSV="$OUTDIR/batch.csv"
[ -f "$CSV" ] || echo "game,result,frames,killed,lost,killedRes,lostRes,digest" > "$CSV"

TUNE_FLAGS=()
[ -n "$STRATEGY" ] && TUNE_FLAGS+=("--tuning-strategy=$STRATEGY")
for kv in ${OVERRIDES[@]+"${OVERRIDES[@]}"}; do TUNE_FLAGS+=("--tune-param=$kv"); done

cd "$REPO" || exit 1
i=0
while [ "$i" -lt "$N" ]; do
  i=$(( i + 1 ))
  echo "[batch] game $i/$N ($MAP)"
  GDIR="$OUTDIR/game-$i"
  mkdir -p "$GDIR"
  touch "$GDIR/.started"
  # shellcheck disable=SC2086
  if ! timeout "$TIMEOUT" bash scripts/wine-run.sh --map="$MAP" $VISIBLE --speed="$SPEED" --no-cherryvis $NO_BUILD "${TUNE_FLAGS[@]+"${TUNE_FLAGS[@]}"}" > "$GDIR/game-$i.log" 2>&1; then
    echo "[batch] game $i: runner failed (see game-$i.log), continuing"
  fi
  if [ -f "$TUNING_ROW" ] && [ "$TUNING_ROW" -nt "$GDIR/.started" ]; then
    cp -f "$TUNING_ROW" "$GDIR/tuning.csv"
    tail -n +2 "$GDIR/tuning.csv" | head -1 | awk -F';' -v g="$i" '{print g","$2","$3","$4","$5","$6","$7","$8}' >> "$CSV" 2>/dev/null || true
  else
    echo "[batch] game $i: INVALID (no fresh tuning row), skipped" | tee -a "$OUTDIR/batch.log"
    rm -f "$GDIR/tuning.csv"
  fi
  echo "[batch] game $i done (see $GDIR)"
done

MANIFEST="$OUTDIR/manifest.txt"
JAR_FP="$(md5sum "target/PurpleWave-1.0.jar" 2>/dev/null | cut -d' ' -f1)"
[ -z "$JAR_FP" ] && JAR_FP="unknown"
{
  echo "map=$MAP"
  echo "jar=$JAR_FP"
  echo "games=$(($(wc -l < "$CSV") - 1))"
  tail -n +2 "$CSV" 2>/dev/null | while IFS=, read -r g result frames killed lost killedRes lostRes digest; do
    TW="$OUTDIR/game-$g/tuning.csv"
    [ -f "$TW" ] || continue
    case "$result" in W) V=WIN;; *) V=LOSS;; esac
    PARAMS="$(tail -1 "$TW" | cut -d';' -f9-)"
    [ -z "$PARAMS" ] && continue
    VEC="$(paste -d'|' <(head -1 "$TW" | tr ';' '\n' | tail -n +9) <(printf '%s' "$PARAMS" | tr ';' '\n') | awk -F'|' '{printf "%s=%s,", $1, $2}' | sed 's/,$//')"
    echo "game-$g|$V|$killed|$lost|$frames|$digest|$VEC"
  done
} > "$MANIFEST"

echo "[batch] done: $N games, summary in $CSV"
column -t -s, "$CSV" 2>/dev/null | head -35 || cat "$CSV"
