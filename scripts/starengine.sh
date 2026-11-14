#!/usr/bin/env bash
# StarEngine scenarios, ad hoc only - never in the default suite.
# Usage: scripts/starengine.sh [Suite [Suite...]]
#   scripts/starengine.sh                        # every scenario (slow)
#   scripts/starengine.sh SunkensScenarioSuite    # one scenario (fast)
# Suite names take an optional starengine. prefix. Sources live in
# tests-starengine/scala; the default mvn test tree never sees them.
set -euo pipefail
cd "$(dirname "$0")/.."

export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xms2g -Xmx4g"

if [ "$#" -eq 0 ]; then
  exec mvn -o -Pstarengine test
fi

suites=""
for name in "$@"; do
  short="${name#starengine.}"
  if [ -n "$suites" ]; then suites="$suites,"; fi
  suites="${suites}starengine.${short}"
done
exec mvn -o -Pstarengine test -Dsuites="$suites"
