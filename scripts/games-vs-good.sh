#!/usr/bin/env bash
# Crona + Terminus back-to-back: the second game starts 4 s after the first,
# so both scbw.play instances run concurrently (isolated Docker games).
# Each launcher enforces its own timeout; this script waits for both.
set -u

cd ~/.scbw/bots/__PlayAsEridan || { echo "no __PlayAsEridan dir" >&2; exit 1; }

./_Crona.sh > /tmp/opencode/games-crona.log 2>&1 &
sleep 4
./_Terminus.sh > /tmp/opencode/games-terminus.log 2>&1 &
wait
echo "both games done (logs: /tmp/opencode/games-crona.log, games-terminus.log)"
