#!/usr/bin/env bash
# Base test suite: the routine build gate, StarEngine scenarios excluded
# (they carry the starengine.Slow tag, ad hoc only via scripts/starengine.sh).
# Usage: scripts/tests.sh [Suite [Suite...]]
#   scripts/tests.sh                         # all 600+ base tests
#   scripts/tests.sh Micro.Corsair.CorsairRelocateSuite   # one suite
set -euo pipefail
cd "$(dirname "$0")/.."

export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xms2g -Xmx4g"

if [ "$#" -eq 0 ]; then
  exec mvn -o -Ptests test
fi

suites=""
for name in "$@"; do
  if [ -n "$suites" ]; then suites="$suites,"; fi
  suites="${suites}${name}"
done
exec mvn -o -Ptests test -Dsuites="$suites"
