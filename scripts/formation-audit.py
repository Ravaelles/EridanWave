#!/usr/bin/env python3
"""Read a formation-audit file (UMS micro has no CherryVis trace,
so this is the formation channel there).

Usage: python3 scripts/formation-audit.py [path] [--assert-mean lo-hi] [--assert-breaks n]
       [--assert-dress-changes n]

Prints the behavior tally, the per-class slot-leave distances, the dress
verdicts and the per-unit oscillation ranking (order changes between
consecutive dress frames). With --assert-* it also gates a future harness:
exit 1 when the overall mean break distance leaves [lo, hi], fewer than n
breaks were observed, or any unit changes dress order more than n times.
Default path is the live bwapi-data/write/formation-audit.txt; the game
only writes it with --formation-audit (see scripts/wine-run.sh).
"""
import re
import statistics
import sys
from collections import Counter, defaultdict

DEFAULT_PATH = "bwapi-data/write/formation-audit.txt"


def parse(path):
    header = {}
    tally = Counter()
    rows = []
    dress_counts = Counter()
    dress_rows = []
    section = None
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip("\n")
            if line.startswith("# FormationAudit"):
                for m in re.finditer(r"(\w+)=(\S+)", line):
                    header[m.group(1)] = m.group(2)
            elif line.startswith("[behaviors]"):
                section = "tally"
            elif line.startswith("[verdicts]"):
                section = "verdicts"
            elif line.startswith("[breaks-by-class]"):
                section = "classes"
            elif line.startswith("[break-rows]"):
                section = "rows"
            elif line.startswith("[dress-orders]"):
                section = "dress_counts"
            elif line.startswith("[dress-rows]"):
                section = "dress"
            elif section == "tally":
                m = re.match(r"\s*(\d+)\s+(.*)", line)
                if m:
                    tally[m.group(2)] = int(m.group(1))
            elif section == "verdicts":
                m = re.match(r"\s*(\d+)\s+(.*)", line)
                if m:
                    verdicts[m.group(2)] = int(m.group(1))
            elif section == "dress_counts":
                m = re.match(r"\s*(\d+)\s+(.*)", line)
                if m:
                    dress_counts[m.group(2)] = int(m.group(1))
            elif section == "rows" and line.strip():
                parts = line.split()
                if len(parts) >= 5 and parts[0] != "frame":
                    rows.append({
                        "frame": int(parts[0]),
                        "unit": parts[1],
                        "own": parts[2],
                        "target": parts[3],
                        "dist": float(parts[4]),
                    })
            elif section == "dress" and line.strip():
                parts = line.split()
                if len(parts) >= 9 and parts[0] != "frame":
                    dress_rows.append({
                        "frame": int(parts[0]),
                        "unit": parts[1],
                        "own": parts[2],
                        "beyond": float(parts[3]),
                        "mean": float(parts[4]),
                        "std": float(parts[5]),
                        "count": int(parts[6]),
                        "posture": parts[7],
                        "order": parts[8],
                    })
    return header, tally, verdicts, rows, dress_counts, dress_rows


def oscillation(dress_rows):
    """Order changes between consecutive dress frames, per unit."""
    by_unit = defaultdict(list)
    for r in dress_rows:
        by_unit[r["unit"]].append((r["frame"], r["order"]))
    changes = {}
    for unit, frames in by_unit.items():
        frames.sort()
        changes[unit] = (len(frames),
                         sum(1 for a, b in zip(frames, frames[1:]) if a[1] != b[1]))
    return changes


def main():
    args = sys.argv[1:]
    path = DEFAULT_PATH
    assert_mean = None
    assert_breaks = None
    assert_dress = None
    i = 0
    positional = []
    while i < len(args):
        if args[i] == "--assert-mean" and i + 1 < len(args):
            lo, hi = args[i + 1].split("-")
            assert_mean = (float(lo), float(hi))
            i += 2
        elif args[i] == "--assert-breaks" and i + 1 < len(args):
            assert_breaks = int(args[i + 1])
            i += 2
        elif args[i] == "--assert-dress-changes" and i + 1 < len(args):
            assert_dress = int(args[i + 1])
            i += 2
        elif args[i].startswith("--"):
            print(f"unknown arg: {args[i]}", file=sys.stderr)
            return 2
        else:
            positional.append(args[i])
            i += 1
    if positional:
        path = positional[0]
    try:
        header, tally, verdicts, rows, dress_counts, dress_rows = parse(path)
    except FileNotFoundError:
        print(f"no audit file at {path} - run a game with --formation-audit first",
              file=sys.stderr)
        return 2

    print(f"map={header.get('map', '?')} frames={header.get('frames', '?')} "
          f"behaviors={sum(tally.values())} breaks={len(rows)} "
          f"dress={len(dress_rows)}")
    print("=== behaviors (leaf decisions per unit-frame) ===")
    for label, n in tally.most_common(20):
        print(f"{n:8}  {label}")
    print("=== verdicts (fight votes per unit-frame) ===")
    for label, n in verdicts.most_common(20):
        print(f"{n:8}  {label}")
    print("=== breaks by class (slot-leave range in tiles) ===")
    by_class = {}
    for r in rows:
        by_class.setdefault(r["own"], []).append(r["dist"])
    for cls in sorted(by_class, key=lambda c: -len(by_class[c])):
        ds = by_class[cls]
        mean = statistics.fmean(ds)
        std = statistics.pstdev(ds) if len(ds) > 1 else 0.0
        print(f"{cls:24} n={len(ds):4} mean={mean:5.2f} "
              f"std={std:5.2f} min={min(ds):5.2f} max={max(ds):5.2f}")
    print("=== dress orders ===")
    for order, n in dress_counts.most_common():
        print(f"{n:8}  {order}")
    changes = oscillation(dress_rows)
    if changes:
        print("=== oscillation (dress order changes per unit) ===")
        for unit, (n, c) in sorted(changes.items(), key=lambda kv: -kv[1][1])[:15]:
            print(f"unit {unit:>5}: {n:5} verdicts, {c:4} order changes")
    failures = []
    if rows:
        ds = [r["dist"] for r in rows]
        mean = statistics.fmean(ds)
        std = statistics.pstdev(ds) if len(ds) > 1 else 0.0
        print(f"=== overall: n={len(ds)} mean={mean:.2f} std={std:.2f} "
              f"min={min(ds):.2f} max={max(ds):.2f} ===")
        if assert_mean is not None:
            lo, hi = assert_mean
            ok = lo <= mean <= hi
            print(f"{'PASS' if ok else 'FAIL'}: overall mean {mean:.2f} "
                  f"{'inside' if ok else 'outside'} [{lo}, {hi}]")
            if not ok:
                failures.append("mean")
    if assert_breaks is not None:
        ok = len(rows) >= assert_breaks
        print(f"{'PASS' if ok else 'FAIL'}: {len(rows)} breaks "
              f"{'reaches' if ok else 'below'} {assert_breaks}")
        if not ok:
            failures.append("breaks")
    if assert_dress is not None:
        worst = max((c for _, c in changes.values()), default=0)
        ok = worst <= assert_dress
        print(f"{'PASS' if ok else 'FAIL'}: worst unit changes dress order "
              f"{worst}x {'within' if ok else 'above'} {assert_dress}")
        if not ok:
            failures.append("dress")
    if not rows and not dress_rows:
        print("(empty audit: flag off or no engagements)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
