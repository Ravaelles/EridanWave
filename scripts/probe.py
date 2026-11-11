#!/usr/bin/env python3
"""Read Probe decision lines from a bot debug log (UMS micro games have no
CherryVis trace, so this is the micro channel there).

Usage: python3 scripts/probe.py <normal.log.txt> [--unit ID] [--key KEY]

Prints per-unit decision timelines plus a count rollup, same spirit as
scripts/summarize-cherryvis.py but for PROBE lines. Arm keys mid-game by
writing them (one per line) to bwapi-data/write/probe.txt.
"""
import re
import sys
from collections import Counter

PAT = re.compile(r"PROBE (\S+) (\d+) (\d+) (.*)")

def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return
    path = args[0]
    only_unit = None
    only_key = None
    for i, a in enumerate(args[1:]):
        if a == "--unit":
            only_unit = args[1:][i + 1]
        if a == "--key":
            only_key = args[1:][i + 1]
    rows = []
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = PAT.search(line)
            if m:
                rows.append(m.groups())
    if only_key:
        rows = [r for r in rows if r[0] == only_key]
    if only_unit:
        rows = [r for r in rows if r[2] == only_unit]
    by_unit = {}
    for key, frame, unit, msg in rows:
        by_unit.setdefault(unit, []).append((int(frame), key, msg))
    for unit in sorted(by_unit, key=int):
        print(f"--- unit {unit} ({len(by_unit[unit])} rows)")
        for frame, key, msg in by_unit[unit][:60]:
            print(f"  f{frame} [{key}] {msg}")
        if len(by_unit[unit]) > 60:
            print(f"  ... +{len(by_unit[unit]) - 60} more")
    counts = Counter((r[0], r[3]) for r in rows)
    print("=== counts (key, decision) ===")
    for (key, msg), n in counts.most_common(25):
        print(f"{n:6}  [{key}] {msg}")

if __name__ == "__main__":
    main()
