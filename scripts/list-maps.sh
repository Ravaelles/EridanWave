#!/usr/bin/env bash
# List StarCraft maps available to the Wine game, grouped by folder.
# Prints the exact relative path to pass as --map= to wine-run.sh
# (or to java -jar PurpleWave.jar --launcher=wine --map=...).
#
# Usage: bash scripts/list-maps.sh [filter]
#   filter  optional case-insensitive substring, e.g. "taucross"
# Env: WINEPREFIX (default ~/.wine), GAME_ROOT (default $WINEPREFIX/drive_c/sc).
set -u

WINE_ROOT="${WINEPREFIX:-$HOME/.wine}"
GAME_ROOT="${GAME_ROOT:-$WINE_ROOT/drive_c/sc}"
MAPS_DIR="$GAME_ROOT/maps"

[ -d "$MAPS_DIR" ] || { echo "No maps dir: $MAPS_DIR (set GAME_ROOT?)" >&2; exit 1; }

FILTER="${1:-}"
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
