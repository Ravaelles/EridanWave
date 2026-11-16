#!/usr/bin/env bash
# StarEngine scenarios, ad hoc only - never in the default suite.
# Usage: scripts/starengine.sh [--filter tag[,tag...]] [Suite [Suite...]]
#   scripts/starengine.sh                         # every scenario (slow)
#   scripts/starengine.sh SunkensScenarioSuite     # one scenario (fast)
#   scripts/starengine.sh --filter reaver          # every scenario tagged reaver
#   scripts/starengine.sh --filter reaver,shuttle  # scenarios tagged either
# Suite names take an optional starengine. prefix. Tags take an optional
# starengine. prefix too (reaver == starengine.reaver); topics live in
# tests-starengine/scala/starengine/StarEngineTags.scala. Suites and the
# filter intersect when both are given.
# Sources live in tests-starengine/scala; the default mvn test tree never sees them.
set -euo pipefail
cd "$(dirname "$0")/.."

export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xms2g -Xmx4g"

filter=""
suites=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --filter=*)
      filter="${1#--filter=}"
      shift
      ;;
    --filter)
      filter="${2:?--filter needs a value}"
      shift 2
      ;;
    --*)
      echo "unknown flag: $1 (did you mean --filter?)" >&2
      exit 2
      ;;
    *)
      short="${1#starengine.}"
      suites+=("starengine.${short}")
      shift
      ;;
  esac
done

qualify() {
  local out=""
  local item
  IFS=',' read -ra items <<< "$1"
  for item in "${items[@]}"; do
    case "$item" in
      *.*) ;;
      *) item="starengine.${item}" ;;
    esac
    if [ -n "$out" ]; then out="$out,"; fi
    out="${out}${item}"
  done
  printf '%s' "$out"
}

# Suites declaring any of the requested tags (union, like ScalaTest's
# -n with a comma list). Declaration form is fixed by convention:
# StarEngineTags.<Name> inside a test(...) call. Narrows discovery so
# the runner does not load all 213 suites; -DtagsToInclude below still
# decides which tests inside them run.
suites_for_tags() {
  local names="$1"
  local short
  short="$(printf '%s' "$names" | sed 's/starengine\.//g')"
  # Declaration form is StarEngineTags.<Name>; filter values are lowercase.
  local capped=""
  local item
  IFS=',' read -ra items <<< "$short"
  for item in "${items[@]}"; do
    item="$(tr '[:lower:]' '[:upper:]' <<< "${item:0:1}")${item:1}"
    if [ -n "$capped" ]; then capped="$capped|"; fi
    capped="${capped}${item}"
  done
  grep -rlE "StarEngineTags\.(${capped})" tests-starengine/scala/starengine/*Suite.scala 2>/dev/null \
    | sed 's#.*/##; s#\.scala$##' \
    | sed 's#^#starengine.#' \
    | paste -sd, - || true
}

args=()
if [ "${#suites[@]}" -gt 0 ]; then
  args+=("-Dsuites=$(IFS=,; echo "${suites[*]}")")
fi
if [ -n "$filter" ]; then
  if [ "${#suites[@]}" -eq 0 ]; then
    tagged="$(suites_for_tags "$filter")"
    if [ -n "$tagged" ]; then
      args+=("-Dsuites=$tagged")
    fi
  fi
  args+=("-DtagsToInclude=$(qualify "$filter")")
fi

if [ "${#args[@]}" -eq 0 ]; then
  exec mvn -o -Pstarengine test
fi
exec mvn -o -Pstarengine test "${args[@]}"
