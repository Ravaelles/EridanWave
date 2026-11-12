#!/usr/bin/env bash
# Test profiler: runs the suite once and prints every test sorted by
# duration, slowest first, so a sluggish test stands out against the
# average. Times come from the JUnit XML the scalatest plugin writes to
# target/scalatest-reports/junit-reports (see the tests profile in pom.xml).
#
# Usage: bash scripts/profile-tests.sh [--top N] [--skip-run]
#   --top N     show only the N slowest tests (default: all)
#   --skip-run  skip mvn, render the last target/scalatest-reports/junit-reports as-is
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

TOP=0
RUN=1
while [ $# -gt 0 ]; do
  case "$1" in
    --top=*) TOP="${1#--top=}"; shift ;;
    --top) TOP="$2"; shift 2 ;;
    --skip-run) RUN=0; shift ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

STATUS=0
if [ "$RUN" = "1" ]; then
  export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -Xms2g -Xmx4g"
  mvn -o -Ptests test -q 2>&1 | tail -8
  STATUS=${PIPESTATUS[0]}
fi

python3 - "$TOP" <<'EOF'
import glob
import sys
import xml.etree.ElementTree as ET

top = int(sys.argv[1])
files = sorted(glob.glob('target/scalatest-reports/junit-reports/TEST-*.xml'))
if not files:
    print('no JUnit reports in target/scalatest-reports/junit-reports - run without --skip-run first', file=sys.stderr)
    sys.exit(2)

rows = []
for path in files:
    root = ET.parse(path).getroot()
    suites = [root] if root.tag == 'testsuite' else root.findall('testsuite')
    for suite in suites:
        for case in suite.findall('testcase'):
            rows.append((
                float(case.get('time', 0)),
                case.get('classname', '?'),
                case.get('name', '?')))
rows.sort(reverse=True)

total = sum(t for t, _, _ in rows)
avg = total / len(rows)
print(f'{len(rows)} tests, total {total:.1f}s, average {avg * 1000:.1f}ms')
shown = rows[:top] if top > 0 else rows
for t, cls, name in shown:
    flag = '  <-- SLOW' if t > 5 * avg and t > 0.5 else ''
    print(f'{t * 1000:9.1f}ms  {t / max(avg, 1e-9):5.1f}x  {cls}.{name}{flag}')

by_suite = {}
for t, cls, _ in rows:
    by_suite[cls] = by_suite.get(cls, 0.0) + t
print('\nSlowest suites by total time:')
for cls, st in sorted(by_suite.items(), key=lambda kv: -kv[1])[:8]:
    print(f'{st * 1000:9.1f}ms  {cls}')

slow = [r for r in rows if r[0] > 5 * avg and r[0] > 0.5]
print(f'\n{len(slow)} test(s) above 5x average and 0.5s - worth a look' if slow
      else '\nNo test above 5x average and 0.5s - the suite is evenly fast')
EOF

exit "$STATUS"
