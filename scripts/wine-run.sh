#!/usr/bin/env bash
# One command: attach EridanWave to real StarCraft under Wine and play.
#
# Verified mechanics (ported from the Atlantis Wine backend):
#   1. The bot JVM must run UNDER WINE with a WINDOWS JRE and
#      -Dos.name="Windows 10", so JBWAPI picks its ClientConnectionW32 backend.
#      A Linux JVM picks the POSIX backend, searches /dev/shm - which the
#      Wine-side BWAPI server never populates - and loops on
#      "Game table mapping not found" forever.
#   2. The CLIENT starts BEFORE the game. The BWAPI server registers a client
#      slot while the game starts; a client started after the match is running
#      attaches to a match that is already over.
#
# Usage: bash scripts/wine-run.sh [options]
#   --map=<name>        map file, e.g. "(3)TauCross.scx" (resolved under maps/)
#   --race=<race>       Protoss|Terran|Zerg|Random (written to bwapi.ini)
#   --enemy-race=<race> written to bwapi.ini
#   --game-type=<type>  MELEE|USE_MAP_SETTINGS (default: scenario content maps
#                         get USE_MAP_SETTINGS, everything else MELEE)
#   --speed=<0-1000>    default game speed (20 if absent; keyboard presets still work)
#   --cherryvis[=bool] write the CherryVis viewer trace, 1/true/yes or
#                       0/false/no (default: enabled). --no-cherryvis disables.
#   --tuning-strategy=<name> tuning strategy from Tuning.Strategies
#                       (absent = default play, no tuning)
#   --tune-param=NAME=VALUE (repeatable) per-game param override
#   --jar=<path>        bot jar (default: <repo>/target/EridanWave-1.0.jar)
#   --headless          Xvfb on :99, nothing ever maps on :0 (default)
#   --visible           run on the current X display instead of Xvfb
#   --no-build          play the jar already on disk instead of rebuilding it
#   -h|--help           this text
# Env overrides: WINEPREFIX (default ~/.wine), GAME_ROOT, JAVA_EXE, JAR,
# REPO (default: script dir/..), LOG_DIR (default: $REPO/out/wine).
#
# Exit code 0 = HELLO_ERIDANWAVE seen (bot attached and playing).
set -u

say() { echo "[wine-run] $*"; }

close_lock_fd() { eval "exec ${WINE_GAME_LOCK_FD:-9}>&-" 2>/dev/null || true; }
bgs() { ( close_lock_fd; exec setsid "$@" ) & }

REPO="${REPO:-$(cd "$(dirname "$0")/.." && pwd)}"
WINE_ROOT="${WINEPREFIX:-$HOME/.wine}"
GAME_ROOT="${GAME_ROOT:-$WINE_ROOT/drive_c/sc}"
JAVA_EXE="${JAVA_EXE:-$WINE_ROOT/drive_c/Java/bin/java.exe}"
JAR="${JAR:-$REPO/target/EridanWave-1.0.jar}"
LOG_DIR="${LOG_DIR:-$REPO/out/wine}"
WINE_RUNS_DIR="$REPO/out/wine_runs"
REPLAYS_DIR="${REPLAYS_DIR:-/sc-ai/REPLAYS}"

MAP=""; RACE=""; ENEMY_RACE=""; GAME_TYPE=""; SPEED=""; BUILD=1; CHERRYVIS="true"
TUNE_STRATEGY=""; TUNE_PARAMS=()
parse_bool() {
  case "$(echo "$1" | tr '[:upper:]' '[:lower:]')" in
    1|true|yes) echo true ;;
    0|false|no) echo false ;;
    *) echo "[wine-run] ignoring invalid boolean for $2: $1 (want 1/true/yes or 0/false/no)" >&2; echo "$3" ;;
  esac
}
for arg in "$@"; do
  case "$arg" in
    --map=*) MAP="${arg#--map=}" ;;
    --race=*) RACE="${arg#--race=}" ;;
    --enemy-race=*) ENEMY_RACE="${arg#--enemy-race=}" ;;
    --game-type=*) GAME_TYPE="${arg#--game-type=}" ;;
    --speed=*) SPEED="${arg#--speed=}" ;;
    --cherryvis) CHERRYVIS="true" ;;
    --no-cherryvis) CHERRYVIS="false" ;;
    --cherryvis=*) CHERRYVIS="$(parse_bool "${arg#--cherryvis=}" "--cherryvis" "$CHERRYVIS")" ;;
    --tuning-strategy=*) TUNE_STRATEGY="${arg#--tuning-strategy=}" ;;
    --tune-param=*) TUNE_PARAMS+=("${arg#--tune-param=}") ;;
    --jar=*) JAR="${arg#--jar=}" ;;
    --headless) HIDDEN=1 ;;
    --visible) HIDDEN=0 ;;
    --no-build) BUILD=0 ;;
    -h|--help) sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "[wine-run] ignoring extra arg: $arg" >&2 ;;
  esac
done

# The live bwapi.ini path (Unix side). Known before the client starts so the
# bot JVM gets it verbatim via -Deridanwave.bwapiIni (see client launch below)
# instead of guessing Wine home layouts at runtime.
INI="$GAME_ROOT/bwapi-data/bwapi.ini"
WIN_INI="$(winepath -w "$INI" 2>/dev/null || true)"
[ -n "$WIN_INI" ] || WIN_INI="Z:$(echo "$INI" | tr '/' '\\')"

# Headless by default: unattended runs never touch the desktop, and a visible
# StarCraft would steal focus and switch the resolution. Pass --visible to
# watch the game on the current X display instead, exactly like the Atlantis
# original (no extra checks there - a missing display fails loudly in Wine).
HIDDEN="${HIDDEN:-1}"

# Hidden session on a reused Xvfb (never killed at teardown).
if [ "$HIDDEN" = "1" ]; then
  export DISPLAY=":99"
  if [ -e /tmp/.X11-unix/X99 ] && ! pgrep -f "[X]vfb :99" >/dev/null; then
    rm -f /tmp/.X11-unix/X99 /tmp/.X99-lock
  fi
  if ! pgrep -f "[X]vfb :99" >/dev/null; then
    say "Hidden Wine desktop on :99 (Xvfb, invisible)"
    bgs Xvfb :99 -screen 0 1280x1024x24 >"$LOG_DIR/xvfb.log" 2>&1 < /dev/null
  fi
  for _ in $(seq 1 100); do [ -e /tmp/.X11-unix/X99 ] && break; sleep 0.1; done
  [ -e /tmp/.X11-unix/X99 ] || { echo "[wine-run] Xvfb :99 did not come up" >&2; exit 1; }
fi

# One Wine game per box: cleanup kills by process name, so a second game
# alongside a running one murders the first mid-play (and vice versa).
WINE_GAME_LOCK="/tmp/eridanwave-wine-game.lock"
exec {WINE_GAME_LOCK_FD}>"$WINE_GAME_LOCK" || { echo "[wine-run] cannot open lock $WINE_GAME_LOCK" >&2; exit 2; }
if ! flock -n "$WINE_GAME_LOCK_FD" 2>/dev/null; then
  echo "[wine-run] REFUSED: another Wine game holds the box lock; one game at a time" >&2
  exit 2
fi
bgs wineserver -p >/dev/null 2>&1 || true

mkdir -p "$LOG_DIR" "$(dirname "$JAR")"
CLIENT_LOG="$LOG_DIR/WINE_LOG"

# Standard BWAPI data folders, relative to the repo root (the client's cwd).
# Without bwapi-data/AI the bot runs on defaults; without read/write the
# logger flush and write-files fail. Seed a dev config (local metal: debugging
# + logstd) when AI/ is empty, but never clobber the owner's files.
mkdir -p "$REPO/bwapi-data/AI" "$REPO/bwapi-data/read" "$REPO/bwapi-data/write"
if [ -z "$(ls -A "$REPO/bwapi-data/AI" 2>/dev/null)" ] && [ -f "$REPO/configs/EridanWaveLocalMetal.config.json" ]; then
  cp -f "$REPO/configs/EridanWaveLocalMetal.config.json" "$REPO/bwapi-data/AI/"
  say "  Seeded bwapi-data/AI with EridanWaveLocalMetal.config.json"
fi

T0=$(date +%s)
since() { echo "[wine-run]   +$(( $(date +%s) - T0 ))s  $1"; }

cleanup() {
  disown -a 2>/dev/null
  pkill -9 -x StarCraft.exe 2>/dev/null
  pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
  pkill -9 -x java.exe 2>/dev/null
}
trap cleanup EXIT

# Rebuild the jar from source in the background: StarCraft takes seconds to
# come up anyway, so the compile hides inside that instead of adding to it.
if [ "$BUILD" = "1" ]; then
  say "Building the bot jar from source in the background (BUILD=0 to skip)"
  rm -f /tmp/eridanwave-build-jar.done /tmp/eridanwave-build-jar.failed
  (
    if timeout 600 bash "$REPO/build-jar.sh" >/tmp/eridanwave-build-jar.log 2>&1; then
      : > /tmp/eridanwave-build-jar.done
    else
      echo "failed" > /tmp/eridanwave-build-jar.failed
    fi
  ) &
fi

say "Killing leftovers"
pkill -9 -x StarCraft.exe 2>/dev/null
pkill -9 -f "[C]haoslauncher.exe" 2>/dev/null
pkill -9 -x java.exe 2>/dev/null
# Stale relay keys from a killed session would all fire at game start
# (e.g. a Pause or an Escape) - the client drains the file blindly.
# Drop them; fresh keys arrive live. Both the current rendezvous and the
# legacy repo-local path (pre-rendezvous builds may have left one behind).
rm -f "$GAME_ROOT/bwapi-data/write/keys.txt" "$REPO/out/wine/keys.txt"
for _ in $(seq 1 30); do
  if ! pgrep -x StarCraft.exe >/dev/null 2>&1 \
      && ! pgrep -f "[C]haoslauncher.exe" >/dev/null 2>&1; then break; fi
  sleep 0.1
done
if pgrep -x StarCraft.exe >/dev/null 2>&1 || pgrep -f "[C]haoslauncher.exe" >/dev/null 2>&1; then
  say "FATAL: a leftover game survived the kill - refusing to attach to a stale map."
  exit 1
fi

[ -f "$JAVA_EXE" ] || { say "Windows Java not found at $JAVA_EXE"; exit 1; }
[ -f "$JAR" ] || { say "Bot jar not found at $JAR (BUILD=0 with no jar on disk?)"; exit 1; }

if [ "$BUILD" = "1" ]; then
  waited=0
  while [ ! -f /tmp/eridanwave-build-jar.done ] && [ ! -f /tmp/eridanwave-build-jar.failed ]; do
    sleep 0.2; waited=$((waited + 1)); [ "$waited" -gt 1500 ] && break
  done
  if [ -f /tmp/eridanwave-build-jar.failed ]; then
    say "  FAILED to build the jar - see /tmp/eridanwave-build-jar.log"
    tail -20 /tmp/eridanwave-build-jar.log | sed 's/^/    /'
    exit 1
  fi
  since "jar rebuilt (parallel with game start)"
fi
[ -f "$JAR" ] || { say "Bot jar missing at $JAR"; exit 1; }
say "Step 1/3: jar: $JAR"

# The unix->windows jar translation is cached: winepath spawns a whole Wine
# process to print it (~0.4 s each run).
WINEPATH_CACHE="/tmp/eridanwave-winepath-cache"
WIN_JAR=""
if [ -f "$WINEPATH_CACHE" ] && [ "$(head -1 "$WINEPATH_CACHE" 2>/dev/null)" = "$JAR" ]; then
  WIN_JAR="$(tail -1 "$WINEPATH_CACHE" 2>/dev/null)"
fi
if [ -z "$WIN_JAR" ]; then
  WIN_JAR="$(winepath -w "$JAR" 2>/dev/null || true)"
  if [ -n "$WIN_JAR" ]; then { echo "$JAR"; echo "$WIN_JAR"; } > "$WINEPATH_CACHE" 2>/dev/null || true; fi
fi
[ -n "$WIN_JAR" ] || WIN_JAR="Z:$(echo "$JAR" | tr '/' '\\')"
say "  Windows path: $WIN_JAR"

# Key relay file shared by the supervisor (repo CWD) and the client
# (game-root CWD): the rendezvous is the live game root, valid in both
# filename namespaces without drive-mapping assumptions (no Z: needed).
# The client gets the absolute Windows path via -D (mirrors bwapiIni above).
mkdir -p "$GAME_ROOT/bwapi-data/write"
KEYS_WIN="$(winepath -w "$GAME_ROOT/bwapi-data/write/keys.txt" 2>/dev/null || true)"
[ -n "$KEYS_WIN" ] || KEYS_WIN="C:\\sc\\bwapi-data\\write\\keys.txt"
say "  Key relay: $GAME_ROOT/bwapi-data/write/keys.txt"

CLIENT_CWD="$(pwd)"
rm -f "$LOG_DIR/WINE_LOG" "$LOG_DIR/WINE_DEBUG" "$LOG_DIR/chaoslauncher.log"
LIVE_OUTPUT="${LIVE_OUTPUT:-1}"

# Client first: it loops on "Game table mapping not found" until the game is
# up, which is its normal idle state. -Dos.name="Windows 10" is what makes
# JBWAPI pick the W32 shared-memory backend under Wine.
# An explicit --speed= travels as ERIDANWAVE_SPEED (see GameCommands.initialSpeed).
# --cherryvis= travels as ERIDANWAVE_CHERRYVIS=1/0 (see ConfigurationLoader).
# Explicit tuning travels as -D flags on the client JVM (same channel as
# --speed/--cherryvis; no scattered env files - one flag per override).
TUNE_DEFS=()
[ -n "${TUNE_STRATEGY:-}" ] && TUNE_DEFS+=("-Deridanwave.tuningStrategy=$TUNE_STRATEGY")
for kv in ${TUNE_PARAMS[@]+"${TUNE_PARAMS[@]}"}; do TUNE_DEFS+=("-Deridanwave.tune.$kv"); done
SPEED_ENV=()
[ -n "${SPEED:-}" ] && SPEED_ENV=(ERIDANWAVE_SPEED="$SPEED")
CVIS_ENV=()
if [ "$CHERRYVIS" = "true" ]; then CVIS_ENV=(ERIDANWAVE_CHERRYVIS=1); else CVIS_ENV=(ERIDANWAVE_CHERRYVIS=0); fi
if [ "$LIVE_OUTPUT" = "1" ]; then
  bgs env WINEDEBUG=-all DISPLAY="${DISPLAY:-:0}" "${SPEED_ENV[@]}" "${CVIS_ENV[@]}" \
    wine "$JAVA_EXE" "-Dos.name=Windows 10" \
      -XX:MaxGCPauseMillis=15 -Xms1536m -Xmx2048m \
      "-Deridanwave.bwapiIni=$WIN_INI" \
      "-Deridanwave.keyRelay=$KEYS_WIN" \
      "-Deridanwave.wineClient=true" \
      ${TUNE_DEFS[@]+"${TUNE_DEFS[@]}"} \
      -jar "$WIN_JAR" \
      > >(close_lock_fd; exec tee "$CLIENT_LOG") 2> >(close_lock_fd; exec tee "$LOG_DIR/WINE_DEBUG" >&2) < /dev/null
  CLIENT_PID=$!
else
  bgs env WINEDEBUG=-all DISPLAY="${DISPLAY:-:0}" "${SPEED_ENV[@]}" "${CVIS_ENV[@]}" \
    wine "$JAVA_EXE" "-Dos.name=Windows 10" \
      -XX:MaxGCPauseMillis=15 -Xms1536m -Xmx2048m \
      "-Deridanwave.bwapiIni=$WIN_INI" \
      "-Deridanwave.keyRelay=$KEYS_WIN" \
      "-Deridanwave.wineClient=true" \
      ${TUNE_DEFS[@]+"${TUNE_DEFS[@]}"} \
      -jar "$WIN_JAR" \
      > >(close_lock_fd; exec tee "$CLIENT_LOG" > /dev/null) 2> >(close_lock_fd; exec tee "$LOG_DIR/WINE_DEBUG" > /dev/null) < /dev/null
  CLIENT_PID=$!
fi
say "  Client PID $CLIENT_PID, log: $CLIENT_LOG"
CLIENT_ALIVE=0
for _ in $(seq 1 15); do
  if kill -0 "$CLIENT_PID" 2>/dev/null; then CLIENT_ALIVE=1; break; fi
  sleep 0.2
done
if [ "$CLIENT_ALIVE" -eq 1 ]; then
  say "  Client is running (it will reach the table once the game starts)"
else
  say "  FAILED: client JVM died immediately. Log tail:"; tail -15 "$CLIENT_LOG" | sed 's/^/    /'; exit 1
fi

say "Step 2/3: GAME"
since "client started, launching the game"

# Resolve the map and patch the live bwapi.ini (read once at game start).
# Both spellings are matched: "map = X" and "map=X".
if [ -n "$MAP" ]; then
  RESOLVED_MAP=$(cd "$GAME_ROOT" && find -L maps -type f -iname "$(basename "$MAP")" 2>/dev/null | head -1)
  if [ -n "$RESOLVED_MAP" ]; then
    say "  Setting map in bwapi.ini to $RESOLVED_MAP"
    sed -i -E "s|^map *=.*|map=$RESOLVED_MAP|" "$INI"
    if [ -z "$GAME_TYPE" ]; then
      # Keep in sync with BwapiIni.java UMS_MARKERS: scenario content plays
      # USE_MAP_SETTINGS, everything else defaults to MELEE. Explicit
      # --game-type always wins.
      MAP_LOWER=$(echo "$RESOLVED_MAP" | tr '[:upper:]' '[:lower:]')
      GAME_TYPE="MELEE"
      for marker in ums micro minimap defense defence scenario bullet _v_ vsgosu exp_skilltest dragoon_v zeal_v goon_v wraith_v lings_v marines_v jim_v_ mar_v_ rebas hydraling m\&m sunken reav_v probe_run drag_run zealdrag wounded incorner tanks_v gosu 7th; do
        case "$MAP_LOWER" in *"$marker"*) GAME_TYPE="USE_MAP_SETTINGS" ;; esac
      done
    fi
  else
    say "  ERROR: map '$MAP' not found under $GAME_ROOT/maps."
    say "  Run 'bash scripts/list-maps.sh' for the exact paths. Refusing to play the previous map."
    cleanup; exit 1
  fi
fi
[ -n "${GAME_TYPE:-}" ] && sed -i -E "s|^game_type *=.*|game_type=$GAME_TYPE|" "$INI" \
  && say "  Setting game_type to $GAME_TYPE"
[ -n "${RACE:-}" ] && sed -i -E "s|^race *=.*|race=$RACE|" "$INI" && say "  Setting race to $RACE"
[ -n "${ENEMY_RACE:-}" ] && sed -i -E "s|^enemy_race *=.*|enemy_race=$ENEMY_RACE|" "$INI" \
  && say "  Setting enemy_race to $ENEMY_RACE"
sed -i -E "s|^windowed *=.*|windowed=OFF|" "$INI"
PRE_REP="$(ls -t "$REPLAYS_DIR"/*.rep 2>/dev/null | head -1 || true)"

cd "$GAME_ROOT"
bgs env WINEDEBUG=-all DISPLAY="${DISPLAY:-:0}" \
  wine chaoslauncher/Chaoslauncher.exe \
  > "$LOG_DIR/chaoslauncher.log" 2>&1 < /dev/null

SC_UP=0
for _ in $(seq 1 90); do
  if pgrep -x StarCraft.exe >/dev/null; then SC_UP=1; break; fi
  sleep 0.5
done
if [ "$SC_UP" -eq 1 ]; then say "  StarCraft is running"; since "StarCraft is up";
else say "  WARNING: StarCraft not running within 45 s (see $LOG_DIR/chaoslauncher.log)"; fi

say "Step 3/3: watching for HELLO_ERIDANWAVE (grace 60s)..."
END=$(( $(date +%s) + 60 )); ATTACHED=0
while [ "$(date +%s)" -lt "$END" ]; do
  if grep -qE "HELLO_(ERIDAN|PURPLE)WAVE" "$CLIENT_LOG" 2>/dev/null; then
    say "SUCCESS: BWAPI attached, EridanWave is playing!"; ATTACHED=1; break
  fi
  if ! kill -0 "$CLIENT_PID" 2>/dev/null; then
    say "FAILED: client JVM died. Log tail:"; tail -15 "$CLIENT_LOG" | sed 's/^/    /'
    cleanup; exit 1
  fi
  sleep 1
done
if [ "$ATTACHED" -eq 0 ]; then
  say "FAILED: no HELLO_ERIDANWAVE within the grace period. Client log tail:"
  tail -15 "$CLIENT_LOG" | sed 's/^/    /'
  cleanup; exit 1
fi

# The game ends when the client JVM exits (win or lose): tear the session down.
say "###############################################"
while kill -0 "$CLIENT_PID" 2>/dev/null; do sleep 2; done
for _ in $(seq 1 20); do
  CUR_REP="$(ls -t "$REPLAYS_DIR"/*.rep 2>/dev/null | head -1 || true)"
  if [ -n "${CUR_REP:-}" ] && [ "$CUR_REP" != "${PRE_REP:-}" ]; then break; fi
  sleep 1
done
ls -lt "$REPLAYS_DIR"/ 2>/dev/null | head -3
say "Client JVM exit, killing SC & ChaosLauncher."
cleanup
{
  RUN_STAMP="$(date +%Y%m%d_%H%M%S)"
  MAP_SLUG="$(basename "${MAP:-game}" | tr -c 'A-Za-z0-9._-' '_')"
  RUN_DIR="$WINE_RUNS_DIR/${RUN_STAMP}_${MAP_SLUG}"
  mkdir -p "$RUN_DIR" || exit 0
  for f in WINE_LOG WINE_DEBUG chaoslauncher.log; do
    [ -f "$LOG_DIR/$f" ] && cp -f "$LOG_DIR/$f" "$RUN_DIR/$f" 2>/dev/null || true
  done
  POST_REP="$(ls -t "$REPLAYS_DIR"/*.rep 2>/dev/null | head -1 || true)"
  if [ -n "${POST_REP:-}" ] && [ "$POST_REP" != "${PRE_REP:-}" ]; then
    cp -f "$POST_REP" "$RUN_DIR/" 2>/dev/null || true
    say "replay archived: $RUN_DIR/$(basename "$POST_REP")"
  fi
  ln -sfn "$RUN_DIR" "$WINE_RUNS_DIR/latest"
  say "run archived: $RUN_DIR"
} || true
exit 0
