#!/usr/bin/env bash
# Deploy EridanWave into the scbw bot slot (/sc-ai/BOTS/EridanWave).
#
# Usage: bash scripts/deploy-eridan-slot.sh [--no-build]
#
# Copies the freshly built fat jar as AI/EridanWave.jar (a REAL file, never
# a link - a linked jar once broke Atlantis games with no main manifest
# attribute), writes bot.json, and ensures BWAPI.dll (known-good copy from
# the AtlantisP slot under this harness) plus read/ and write/ exist.
# Rerunnable and deterministic: same inputs, same slot.
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
SLOT="${SCBW_BOTS_DIR:-/sc-ai/BOTS}/EridanWave"
JAR="$REPO/target/EridanWave-1.0.jar"
REF_DLL="/sc-ai/BOTS/AtlantisP/BWAPI.dll"

cd "$REPO" || exit 1
if [ "${1:-}" != "--no-build" ]; then
  ./build-jar.sh || exit 1
fi
[ -f "$JAR" ] || { echo "[deploy] no jar at $JAR" >&2; exit 1; }

mkdir -p "$SLOT/AI" "$SLOT/read" "$SLOT/write"
cp -f "$JAR" "$SLOT/AI/EridanWave.jar"
if ! unzip -p "$SLOT/AI/EridanWave.jar" META-INF/MANIFEST.MF 2>/dev/null | grep -q "Main-Class: Lifecycle.Main"; then
  echo "[deploy] jar has no Main-Class: Lifecycle.Main manifest - refusing to deploy a dead slot" >&2
  exit 1
fi
if [ -f "$REF_DLL" ]; then
  cp -f "$REF_DLL" "$SLOT/BWAPI.dll"
else
  echo "[deploy] WARNING: no reference BWAPI.dll at $REF_DLL; slot needs one for scbw.play" >&2
fi
cat > "$SLOT/bot.json" <<'EOF'
{"name": "EridanWave", "race": "Protoss", "botType": "JAVA_MIRROR", "description": "EridanWave (Ravaelles) - real jar rebuilt by scripts/deploy-eridan-slot.sh, never a link"}
EOF

echo "[deploy] slot ready: $SLOT"
ls -la "$SLOT" "$SLOT/AI" | sed 's/^/[deploy] /'
echo "[deploy] play it with: time bash /sc-ai/BOTS/__PlayAsEridan/_Steamhammer.sh"
