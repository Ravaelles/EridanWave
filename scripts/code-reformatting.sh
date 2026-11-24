#!/usr/bin/env bash
# Style gate for EridanWave (the Atlantis checkstyle.sh equivalent).
#
# Usage:
#   bash scripts/code-reformatting.sh               # check all of src/
#   bash scripts/code-reformatting.sh <path...>     # check only these (must be under src/)
#   bash scripts/code-reformatting.sh --apply [<path...>]  # auto-fix: insert the
#     missing blank lines around `return`, then re-check. The fixer only ever
#     ADDS blank lines (semantics-preserving) and covers both .scala and .java
#     (Checkstyle itself is Java-only, so it cannot serve the mixed tree).
#
# Scope is src/ ONLY, always: production code and tests. Every other
# directory is never touched (owner rule).
#
# This is an ON-DEMAND script, not part of the test run: run it on files you
# touched before committing, not on every change. A bare --apply covers the
# whole tree including legacy files - prefer passing the paths you touched.
#
# How it works: everything is implemented in scripts/check-blank-lines.py
# (rules in tools/blank-lines.xml, ported from Atlantis tools/checkstyle.xml).
# This script only resolves targets, enforces the src/ scope and maps exit
# codes. No downloads, no network, no dependencies beyond python3.
#
# Exit codes: 0 = clean, 1 = violations found, 2 = tooling problem.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHECKER="${REPO_DIR}/scripts/check-blank-lines.py"
CONFIG="${REPO_DIR}/tools/blank-lines.xml"
SRC_DIR="${REPO_DIR}/src"

# --- Flags ---------------------------------------------------------------------
# --apply: after reporting, fix exactly the reported return-blank-line
# violations by inserting the missing blank lines, then re-check.
APPLY=0
ARGS=()
for t in "$@"; do
  case "$t" in
    --apply) APPLY=1 ;;
    *) ARGS+=("$t") ;;
  esac
done
if [ "${#ARGS[@]}" -gt 0 ]; then
  set -- "${ARGS[@]}"
else
  set --
fi

say()  { echo "[code-reformatting] $*"; }
fail() { echo "[code-reformatting] FATAL: $*" >&2; exit 2; }

[ -f "$CHECKER" ] || fail "checker not found: $CHECKER"
[ -f "$CONFIG" ] || fail "config not found: $CONFIG"
[ -d "$SRC_DIR" ] || fail "src/ not found: $SRC_DIR"
command -v python3 >/dev/null 2>&1 || fail "python3 not found"

# --- Resolve targets (src/ only, .scala/.java files only) -----------------------
# The checker is handed explicit source files, never a raw directory: like the
# Atlantis original this keeps fixtures and stray files out of both reporting
# and fixing.
targets=()
collect() {
  while IFS= read -r -d '' f; do targets+=("$f"); done \
    < <(find "$1" \( -name '*.scala' -o -name '*.java' \) -print0)
}
if [ "$#" -eq 0 ]; then
  collect "$SRC_DIR"
else
  for t in "$@"; do
    case "$t" in
      /*) abs="$t" ;;
      *)  abs="$(cd "$REPO_DIR" && cd "$(dirname "$t")" && pwd)/$(basename "$t")" ;;
    esac
    case "$abs" in
      "$SRC_DIR"/*) ;;
      *) fail "refusing path outside src/: $t" ;;
    esac
    [ -e "$abs" ] || fail "no such path: $t"
    if [ -d "$abs" ]; then
      collect "$abs"
    elif [[ "$abs" == *.scala || "$abs" == *.java ]]; then
      targets+=("$abs")
    else
      fail "not a .scala/.java file (never touch fixtures): $t"
    fi
  done
fi
[ "${#targets[@]}" -gt 0 ] || fail "no .scala/.java files selected"

# --- Run ------------------------------------------------------------------------
if [ "$APPLY" -eq 0 ]; then
  say "checking ${#targets[@]} file(s) ($CONFIG)"
  python3 "$CHECKER" "${targets[@]}"
  rc=$?
  if [ "$rc" -eq 0 ]; then
    say "OK: no violations"
    exit 0
  fi
  say "violations found (exit $rc)"
  say "note: the checker only reports - it never modifies files; re-run with the paths you touched (bare run covers the whole tree, including legacy files); use --apply to auto-insert the missing blank lines"
  exit 1
fi

say "applying return-blank-line fixes to ${#targets[@]} file(s)"
python3 "$CHECKER" --apply "${targets[@]}"
rc=$?
if [ "$rc" -eq 0 ]; then
  say "OK: no violations"
  exit 0
else
  say "violations remain after --apply (exit $rc) - fix the rest by hand"
  exit 1
fi
