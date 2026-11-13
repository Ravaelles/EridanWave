#!/usr/bin/env bash
# Broom for stale Wine games: after a crashed run or a manually exited match
# (Esc in the window, killed terminal) the teardown never runs, and a
# lingering wineserver + winedevice keeps the next game from attaching -
# the client loops on "Game table mapping not found" until the grace period
# dies. wine-run.sh only reaps by pidfile/name (StarCraft, ChaosLauncher,
# java.exe); the Wine server itself survives those. This kills the server
# and the strays. Xvfb :99 is intentionally left alone (reused desktop).
#
# Usage: bash scripts/kill-cleanup.sh [--force]
#   Refuses while the box lock shows a live game; --force kills anyway.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

WINE_GAME_LOCK="/tmp/eridanwave-wine-game.lock"
FORCE=0
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ;;
    *) echo "unknown arg: $arg (want --force)" >&2; exit 2 ;;
  esac
done

exec {LOCK_FD}>"$WINE_GAME_LOCK"
if ! flock -n "$LOCK_FD" 2>/dev/null; then
  if [ "$FORCE" = "0" ]; then
    echo "[kill-cleanup] REFUSED: the box lock is held - a game looks live. Re-run with --force to kill it anyway." >&2
    exit 1
  fi
  echo "[kill-cleanup] --force: killing despite the held box lock."
fi

killed=0
snuff() { # exact-name or [b]racket pattern, then count the taken
  local label="$1"; shift
  local before
  before="$(pgrep -c "$@" 2>/dev/null || true)"
  [ -n "$before" ] && [ "$before" -gt 0 ] 2>/dev/null || return 0
  # shellcheck disable=SC2086
  pkill -9 "$@" 2>/dev/null || true
  echo "[kill-cleanup] killed $before x $label"
  killed=$((killed + before))
}

snuff StarCraft.exe -x StarCraft.exe
snuff Chaoslauncher.exe -f "[C]haoslauncher.exe"
snuff java.exe -x java.exe
snuff winedevice.exe -f "[w]inedevice.exe"

# The server outlives every name above (nothing ever pkills it) and owns the
# prefix: a clean shutdown takes its strays with it.
if pgrep -f "[w]ineserver" >/dev/null 2>&1; then
  (WINEPREFIX="${WINEPREFIX:-$HOME/.wine}" wineserver -k 2>/dev/null) || true
  sleep 2
  snuff wineserver -f "[w]ineserver"
fi

if [ "$killed" = "0" ]; then
  echo "[kill-cleanup] nothing stale found."
else
  echo "[kill-cleanup] reaped $killed process(es)."
fi
