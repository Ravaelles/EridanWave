#!/usr/bin/env bash
# Autonomous tuning loop: rounds of Wine batches plus gradient proposals.
#
# Usage:
#   bash scripts/run-tuning.sh --strategy=NAME [--map=PATH] [--rounds=R]
#       [--games-per-round=K] [--outdir=DIR] [--seed=N] [--radius=D]
#
# Example:
#   bash scripts/run-tuning.sh --strategy=KiteAwayDuel --rounds=3 --outdir=/tmp/opencode/tune1
#
# With no arguments this prints the help above and exits - nothing runs
# by accident. One game at a time, never parallelize Wine games.
#
# Each round plays K games (run-batch.sh) with the current proposal as
# KEY=VALUE overrides (round 1 has none, so the bot randomizes - cold
# start, Atlantis parity), then TuningDriver reads the hall of fame plus
# the round manifest and writes the next fragment. BEST=<fitness> on the
# driver output drives the schedule: halve the radius when the best stops
# improving.
#
# Silence is tuning-only and total: headless Xvfb :99 is forced (no
# --visible escape hatch), fastest speed, no CherryVis trace, and Wine
# audio drivers are nulled via WINEDLLOVERRIDES, so nothing maps, pops up
# or plays on the desktop. Normal runs are untouched.
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ "$#" -eq 0 ]; then
  sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  echo ""
  echo "nothing to do without arguments (see usage above)"
  exit 2
fi

STRATEGY=""
MAP="maps/ums/rav/protoss/15Drag_v_16Drag.scm"
ROUNDS=3
GAMES_PER_ROUND=5
OUTDIR="/tmp/opencode/tune-$(date +%Y%m%d-%H%M%S)"
SEED="$RANDOM"
RADIUS="0.10"
for arg in "$@"; do
  case "$arg" in
    --strategy=*) STRATEGY="${arg#--strategy=}" ;;
    --map=*) MAP="${arg#--map=}" ;;
    --rounds=*) ROUNDS="${arg#--rounds=}" ;;
    --games-per-round=*) GAMES_PER_ROUND="${arg#--games-per-round=}" ;;
    --outdir=*) OUTDIR="${arg#--outdir=}" ;;
    --seed=*) SEED="${arg#--seed=}" ;;
    --radius=*) RADIUS="${arg#--radius=}" ;;
    -h|--help) sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "[tune] ignoring extra arg: $arg" >&2 ;;
  esac
done
[ -n "$STRATEGY" ] || { echo "[tune] missing required --strategy=NAME" >&2; exit 2; }
[ "$ROUNDS" -gt 0 2>/dev/null ] || { echo "[tune] --rounds must be positive" >&2; exit 2; }
[ "$GAMES_PER_ROUND" -gt 0 2>/dev/null ] || { echo "[tune] --games-per-round must be positive" >&2; exit 2; }

# Tuning-only silence: no window, no trace, no sound. Scoped to this loop;
# interactive runs keep today's behavior.
export WINEDLLOVERRIDES="winealsa.drv=d;winepulse.drv=d"
export WINEDEBUG="-all"

cd "$REPO" || exit 1
TUNE_START=$(date +%s)
echo "[tune] building the bot jar once (batches reuse it with --no-build)"
./build-jar.sh > "$OUTDIR-build.log" 2>&1 || { echo "[tune] build failed (see OUTDIR-build.log)" >&2; exit 1; }
mkdir -p "$OUTDIR"

HOF="$OUTDIR/halloffame.txt"
FRAGMENT="$OUTDIR/fragment.env"
BEST_LAST=""
round=0
while [ "$round" -lt "$ROUNDS" ]; do
  round=$(( round + 1 ))
  ROUND_START=$(date +%s)
  echo "[tune] round $round/$ROUNDS (radius=$RADIUS)"
  ROUND_OUT="$OUTDIR/round-$round"
  if [ -f "$FRAGMENT" ]; then
    mapfile -t PROPOSAL < <(grep -v "^$" "$FRAGMENT")
  else
    PROPOSAL=()
  fi
  # shellcheck disable=SC2086
  bash scripts/run-batch.sh "$MAP" "$GAMES_PER_ROUND" "$ROUND_OUT" \
    --tuning-strategy="$STRATEGY" --headless --no-build \
    ${PROPOSAL[@]+"${PROPOSAL[@]}"} \
    > "$OUTDIR/round-$round-batch.log" 2>&1 || echo "[tune] round $round batch had failures (see round-$round-batch.log)"
  DRIVER_OUT="$(java -cp target/PurpleWave-1.0.jar Tuning.TuningDriver \
    --strategy="$STRATEGY" --hof="$HOF" --manifest="$ROUND_OUT/manifest.txt" \
    --out="$FRAGMENT" --radius="$RADIUS" --seed="$SEED" 2>&1)"
  echo "$DRIVER_OUT" | sed 's/^/[tune] driver: /'
  BEST="$(echo "$DRIVER_OUT" | grep -oE "^BEST=[^ ]+" | cut -d= -f2 | head -1)"
  if [ -n "$BEST" ] && [ "$BEST" = "$BEST_LAST" ] && [ "$BEST" != "unknown" ]; then
    RADIUS="$(awk -v r="$RADIUS" 'BEGIN { printf "%.4f", r / 2 }')"
    echo "[tune] best stagnant ($BEST), halving radius to $RADIUS"
  fi
  [ -n "$BEST" ] && BEST_LAST="$BEST"
  ROUND_SECS=$(( $(date +%s) - ROUND_START ))
  echo "[tune] round $round done in $(( ROUND_SECS / 60 ))m$(( ROUND_SECS % 60 ))s"
done

TUNE_SECS=$(( $(date +%s) - TUNE_START ))
echo "[tune] done: $ROUNDS rounds in $(( TUNE_SECS / 60 ))m$(( TUNE_SECS % 60 ))s, out $OUTDIR (hall of fame: $HOF)"
