#!/usr/bin/env bash
# Fast dev JAR build for PurpleWave -> target/PurpleWave-1.0.jar
# Default is incremental + offline + dev profile (no optimizer passes): seconds.
# Usage: ./build-jar.sh [--release] [--exe] [--clean]
#   --release  full optimizer flags (slower build, faster bot; use for the shipped artifact)
#   --exe      also build out/artifacts/PurpleSimViz.exe (needs a JRE under Wine)
#   --clean    wipe target/ first (forces a full rebuild; see below when it makes sense)
set -euo pipefail
cd "$(dirname "$0")"

PROFILE="-Pdev"
CLEAN=""
EXE=()
for arg in "$@"; do
  case "$arg" in
    --release) PROFILE="" ;;
    --exe) EXE=(-DbuildExe=true) ;;
    --clean) CLEAN="clean" ;;
    -h|--help) sed -n '2,8p' "$0"; exit 0 ;;
    *) echo "Unknown option: $arg (try --help)" >&2; exit 1 ;;
  esac
done

# Needed by launch4j/XStream on JDK 17; harmless for plain compile.
export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED"
# shellcheck disable=SC2086
mvn -o $PROFILE $CLEAN package "${EXE[@]}"
