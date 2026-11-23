#!/usr/bin/env bash
# StarEngine live view: runs scenario suites with the Swing visualizer.
# The window shows every live body plus its current order - the movement
# arrow points at the order destination, labels carry kind, order and
# hit points, the same active-behavior readout run.sh --visible paints.
# Keys: space pauses, +/- steps the frame delay, 0 runs full speed,
# q closes the window and lets the run finish headless.
# Usage: scripts/watch.sh [--filter tag] [Suite [Suite...]]
#   scripts/watch.sh DragoonZealotKiteSuite     # one scenario, watch it live
#   scripts/watch.sh --filter dragoon           # every scenario tagged dragoon
#   scripts/watch.sh                            # checklist picker, then watch in order
# Scenarios run one after another in a single JVM; the window stays up
# and retitles to the active game. Needs an X display (the Wine Xvfb on
# :99 works); without one the suites run headless as usual.
set -euo pipefail
cd "$(dirname "$0")/.."

pick_suites() {
  local all=()
  while IFS= read -r f; do all+=("$(basename "$f" .scala)"); done < <(ls tests-starengine/scala/starengine/*Suite.scala | sort)
  echo "StarEngine scenarios (space-separated numbers, 'all', or empty to quit):" >&2
  local i=1
  for s in "${all[@]}"; do printf '  %2d) %s\n' "$i" "$s" >&2; i=$((i + 1)); done
  local picked=""
  read -rp "Watch> " picked
  if [ -z "$picked" ]; then exit 0; fi

  if [ "$picked" = "all" ]; then printf '%s\n' "${all[@]}"; return 0; fi

  local n
  for n in $picked; do
    if [[ "$n" =~ ^[0-9]+$ ]] && [ "$n" -ge 1 ] && [ "$n" -le "${#all[@]}" ]; then
      echo "${all[$((n - 1))]}"
    else
      echo "watch: ignoring '$n' (1-${#all[@]} or 'all')" >&2
    fi
  done
}

args=()
while [ $# -gt 0 ]; do
  case "$1" in
    --filter) args+=("--filter" "$2"); shift 2;;
    *) args+=("$1"); shift;;
  esac
done

if [ "${#args[@]}" -eq 0 ] && [ -t 0 ]; then
  mapfile -t chosen < <(pick_suites)
  if [ "${#chosen[@]}" -eq 0 ]; then exit 0; fi
  args=("${chosen[@]}")
fi

if [ -z "${DISPLAY:-}" ]; then
  echo "watch: no DISPLAY set - suites run headless, the window is skipped." >&2
  echo "watch: point DISPLAY at an X server (the Wine Xvfb listens on :99) to watch." >&2
fi

export STARENGINE_VISUALIZE=1
exec scripts/starengine.sh "${args[@]}"
