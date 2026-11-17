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
#                       A unique substring also works: --map=DT_v_Protoss
#                       (or even --map=DT_) resolves to the single match.
#                       Zero or several matches abort with an error here -
#                       BWAPI would otherwise silently replay the previous map.
# Optional parameters (sane defaults apply, see --help for the full list):
#   --race, --enemy-race, --game-type, --speed, --headless, --visible,
#   --cherryvis / --no-cherryvis,
#   --jar, --wine-prefix, --game-root, --java-exe, --bwapi-ini, --no-build
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

usage() {
  sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

MAP=""
GAME_ROOT_ARG=""
WINEPREFIX_ARG=""
PASSTHROUGH=()
for arg in "$@"; do
  case "$arg" in
    --map=*) MAP="${arg#--map=}" ;;
    --game-root=*) GAME_ROOT_ARG="${arg#--game-root=}" ;;
    --wine-prefix=*) WINEPREFIX_ARG="${arg#--wine-prefix=}" ;;
    -h|--help) usage; echo ""; bash scripts/wine-run.sh --help | tail -n +2; exit 0 ;;
    *) PASSTHROUGH+=("$arg") ;;
  esac
done

if [ -z "$MAP" ]; then
  echo "Missing required parameter." >&2
  usage >&2
  exit 2
fi

# Fuzzy map resolution: an exact relative path passes through untouched,
# anything else is a case-insensitive substring matched against maps/.
# Zero or several matches fail fast - a wrong/missing map silently plays
# the previous one, so guessing here beats debugging a phantom game.
WINE_ROOT_RESOLVE="${WINEPREFIX_ARG:-${WINEPREFIX:-$HOME/.wine}}"
RESOLVE_ROOT="${GAME_ROOT_ARG:-${GAME_ROOT:-$WINE_ROOT_RESOLVE/drive_c/sc}}"
if [ ! -f "$RESOLVE_ROOT/$MAP" ]; then
  if [ -d "$RESOLVE_ROOT/maps" ]; then
    MAP_GIVEN="$MAP"
    MAP_MATCHES=$(cd "$RESOLVE_ROOT" && find -L maps -type f \( -iname "*.scx" -o -iname "*.scm" \) | grep -iF -- "$MAP" | sort)
    if [ -z "$MAP_MATCHES" ]; then
      echo "Error: unknown map '$MAP_GIVEN'. No match under $RESOLVE_ROOT/maps." >&2
      echo "Try: bash scripts/list-maps.sh $MAP_GIVEN" >&2
      exit 2
    fi
    MAP_COUNT=$(printf '%s\n' "$MAP_MATCHES" | wc -l)
    if [ "$MAP_COUNT" -gt 1 ]; then
      echo "Error: ambiguous map '$MAP_GIVEN'. Found $MAP_COUNT:" >&2
      printf '%s\n' "$MAP_MATCHES" >&2
      exit 2
    fi
    MAP="$MAP_MATCHES"
    echo "[run] Resolved map '$MAP_GIVEN' -> $MAP"
  fi
fi

# Human-only maps (need a human player; idle forever headless) - keep in
# sync with the HUMAN_ONLY list in scripts/list-maps.sh.
case "$(basename "$MAP")" in
  *dragoons_v_map*)
    echo "[run] REFUSED: $MAP is human-only (see scripts/list-maps.sh)." >&2
    exit 2
    ;;
esac

JAR="$REPO/target/EridanWave-1.0.jar"
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
SUPERVISOR_JAR="$REPO/out/eridanwave-supervisor.jar"
mkdir -p "$(dirname "$SUPERVISOR_JAR")"
cp -f "$JAR" "$SUPERVISOR_JAR"

exec java -jar "$SUPERVISOR_JAR" --launcher=wine --map="$MAP" "${PASSTHROUGH[@]}"
