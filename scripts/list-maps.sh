#!/usr/bin/env bash
# List StarCraft maps available to the Wine game, grouped by folder.
# Prints the exact relative path to pass as --map= to wine-run.sh
# (or to java -jar PurpleWave.jar --launcher=wine --map=...).
#
# Usage: bash scripts/list-maps.sh [--filter=<substring>|<substring>]
#   --filter protoss   list only maps matching "protoss" (case-insensitive),
#                      same as: bash scripts/list-maps.sh protoss
#   Output stays grouped by folder and sorted alphabetically.
# Env: WINEPREFIX (default ~/.wine), GAME_ROOT (default $WINEPREFIX/drive_c/sc).
set -u

WINE_ROOT="${WINEPREFIX:-$HOME/.wine}"
GAME_ROOT="${GAME_ROOT:-$WINE_ROOT/drive_c/sc}"
MAPS_DIR="$GAME_ROOT/maps"

[ -d "$MAPS_DIR" ] || { echo "No maps dir: $MAPS_DIR (set GAME_ROOT?)" >&2; exit 1; }

FILTER=""
EXPECT_VALUE=0
for arg in "$@"; do
  if [ "$EXPECT_VALUE" = 1 ]; then FILTER="$arg"; EXPECT_VALUE=0; continue; fi
  case "$arg" in
    --filter=*) FILTER="${arg#--filter=}" ;;
    --filter) EXPECT_VALUE=1 ;;
    -h|--help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    --*) echo "Unknown option: $arg (try --help)" >&2; exit 2 ;;
    *) FILTER="$arg" ;;
  esac
done
if [ "$EXPECT_VALUE" = 1 ]; then echo "--filter needs a value" >&2; exit 2; fi
TOTAL=0
CURRENT_DIR=""

while IFS= read -r map; do
  rel="${map#$GAME_ROOT/}"
  if [ -n "$FILTER" ] && ! echo "$rel" | grep -qi "$FILTER"; then
    continue
  fi
  dir=$(dirname "$rel")
  if [ "$dir" != "$CURRENT_DIR" ]; then
    CURRENT_DIR="$dir"
    echo ""
    echo "$dir/"
  fi
  echo "  $rel"
  TOTAL=$((TOTAL + 1))
done < <(cd "$GAME_ROOT" && find -L maps -type f \( -iname "*.scx" -o -iname "*.scm" \) | sort)

echo ""
echo "$TOTAL map(s)."
