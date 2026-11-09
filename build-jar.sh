#!/usr/bin/env bash
# Fast dev JAR build for EridanWave -> target/EridanWave-1.0.jar
# Default is incremental + offline + dev profile (no optimizer passes): seconds.
# Every successful build also syncs /sc-ai/BOTS/EridanWave/AI/EridanWave.jar
# when the slot directory exists, so the harness can never play a stale jar.
# Usage: ./build-jar.sh [--release] [--exe] [--clean]
#   --release  full optimizer flags (slower build, faster bot; use for the shipped artifact)
#   --exe      also build out/artifacts/EridanSimViz.exe (needs a JRE under Wine)
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
# 4 GB heap: a cold scalac over ~700 files GC-thrashes on the Maven default;
# single-module Scala compilation is single-threaded, so cores don't help,
# but heap does (measured: ~27 s cold either way, warm zinc stays ~3 s).
export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xms2g -Xmx4g"
JAR="target/EridanWave-1.0.jar"
FINGERPRINT_FILE="target/.build-fingerprint"

# The scbw harness plays whatever sits in the bot slot, so a build that
# never reaches the slot is a stale game waiting to happen (Atlantis parity:
# its build-bot-jar.sh writes straight to the slot path; Maven owns target/,
# so we sync the slot at the end of every build instead). Skipped silently
# when the slot directory does not exist (dev machines without the harness).
SLOT_JAR="/sc-ai/BOTS/EridanWave/AI/EridanWave.jar"
sync_slot() {
  [ -d "$(dirname "$SLOT_JAR")" ] || return 0
  if [ -f "$SLOT_JAR" ] && cmp -s "$JAR" "$SLOT_JAR"; then return 0; fi
  if ! unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null | grep -q "Main-Class: Lifecycle.Main"; then
    echo "Refusing to sync slot: $JAR has no Main-Class: Lifecycle.Main manifest" >&2
    return 1
  fi
  cp -f "$JAR" "$SLOT_JAR"
  echo "Slot synced: $SLOT_JAR"
}

# Skip the build when nothing changed: hash all build inputs and compare with
# the last successful build (same trick as Atlantis build-bot-jar.sh).
# wine-run.sh rebuilds in the background on every run; without edits this
# turns seconds of waiting into a fraction of one.
if [ -z "$CLEAN" ]; then
  FINGERPRINT="$(find src src-macros 3rdparty pom.xml build-jar.sh -type f -exec md5sum {} + 2>/dev/null | sort -k2 | md5sum | cut -d' ' -f1)-$PROFILE-${EXE[*]:-}"
  if [ -f "$JAR" ] && [ -f "$FINGERPRINT_FILE" ] && [ "$(cat "$FINGERPRINT_FILE")" = "$FINGERPRINT" ]; then
    echo "Sources unchanged since last build; reusing $JAR"
    sync_slot
    exit 0
  fi
fi
# shellcheck disable=SC2086
mvn -o $PROFILE $CLEAN package "${EXE[@]}"
sync_slot
if [ -z "$CLEAN" ]; then
  echo "$FINGERPRINT" > "$FINGERPRINT_FILE"
fi
