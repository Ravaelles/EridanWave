#!/usr/bin/env bash
# Main entry point for local Wine games: real StarCraft under Wine + the bot.
# The map is mandatory - BWAPI loads it from bwapi.ini at game start, and a
# wrong/missing map silently plays the previous one, so failing fast here
# beats debugging a game that never started.
#
# Usage: ./scripts/run.sh --map=<map> [options]
#   Example: ./scripts/run.sh --map=maps/sscai/\(3\)TauCross.scx --race=Protoss
#   Maps:    bash scripts/list-maps.sh [filter]
#
# Required parameters:
#   --map=<path>        map file, relative to the game's maps/ folder
#                       e.g. maps/ums/rav/protoss/1Drag_v_1Zeal.scm
#                       (find names with: bash scripts/list-maps.sh [filter])
# Optional parameters (sane defaults apply, see --help for the full list):
#   --race, --enemy-race, --game-type, --speed, --headless, --visible,
#   --jar, --wine-prefix, --game-root, --java-exe, --bwapi-ini, --no-build
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

usage() {
  sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

MAP=""
PASSTHROUGH=()
for arg in "$@"; do
  case "$arg" in
    --map=*) MAP="${arg#--map=}" ;;
    -h|--help) usage; echo ""; bash scripts/wine-run.sh --help | tail -n +2; exit 0 ;;
    *) PASSTHROUGH+=("$arg") ;;
  esac
done

if [ -z "$MAP" ]; then
  echo "Missing required parameter." >&2
  usage >&2
  exit 2
fi

JAR="$REPO/target/PurpleWave-1.0.jar"
if [ ! -f "$JAR" ]; then
  echo "[run] No bot jar at $JAR - building it first (once)."
  ./build-jar.sh || exit 1
fi

# The supervisor JVM must run from an immutable classpath: the game script
# rebuilds target/*.jar in the background while the supervisor is already
# running from it, and any class loaded lazily afterwards reads a truncated
# jar and dies with NoClassDefFoundError (measured with JNativeHook's mouse
# listener). A cheap copy keeps it stable; the game client always starts
# from the freshly built jar instead.
SUPERVISOR_JAR="$REPO/out/purplewave-supervisor.jar"
mkdir -p "$(dirname "$SUPERVISOR_JAR")"
cp -f "$JAR" "$SUPERVISOR_JAR"

exec java -jar "$SUPERVISOR_JAR" --launcher=wine --map="$MAP" "${PASSTHROUGH[@]}"
