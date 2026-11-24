#!/usr/bin/env python3
"""Formation audit for traceless UMS duels: do we form up before fighting?

Reads PROBE Formation snapshots from a debug log (arm the key first:
add a `Formation` line to bwapi-data/write/probe.txt before the game):

  PROBE Formation <frame> <uid> <x> <y> <foeTiles> <order>

Reports, per snapshot: average distance to the nearest foe, pack cohesion
(average distance to our own centroid), and the orders of scattered units.
Verdict: the frame of the first Attack order against cohesion at that time -
a formed ball attacks together; lone Attack orders while scattered mean we
fed the fight piecemeal.

Usage: python3 scripts/formation-check.py <normal.log> [--straggler 6.0]
"""
import math
import re
import sys

SNAP = re.compile(r"PROBE Formation (\d+) (\d+) (\d+) (\d+) ([\d.]+) (\S+)")


def centroid(units):
    xs = [u[0] for u in units.values()]
    ys = [u[1] for u in units.values()]
    return (sum(xs) / len(xs), sum(ys) / len(ys))


def main(path, straggler_tiles):
    snaps = {}
    for line in open(path, encoding="utf-8", errors="replace"):
        m = SNAP.search(line)
        if not m:
            continue
        frame, uid = int(m.group(1)), int(m.group(2))
        snaps.setdefault(frame, {})[uid] = (
            int(m.group(3)), int(m.group(4)), float(m.group(5)), m.group(6))
    if not snaps:
        print("no PROBE Formation snapshots (arm the Formation key and replay)")
        return 2
    frames = sorted(snaps)
    print(f"snapshots: {len(frames)} from f{frames[0]} to f{frames[-1]}")

    first = snaps[frames[0]]
    print(f"--- start (f{frames[0]}, {len(first)} units) ---")
    for uid in sorted(first):
        print(f"  unit {uid}: nearest foe {first[uid][2]:.1f} tiles [{first[uid][3]}]")
    print(f"  AVG nearest foe: {sum(u[2] for u in first.values()) / len(first):.1f} tiles")

    first_attack = None
    print("--- trajectory (avg foe-dist / cohesion / attackers) ---")
    for f in frames:
        units = snaps[f]
        cx, cy = centroid(units)
        cohes = [math.hypot(u[0] - cx, u[1] - cy) / 32.0 for u in units.values()]
        attackers = sorted(uid for uid, u in units.items()
                           if u[3].startswith("Attack"))
        if attackers and first_attack is None:
            first_attack = (f, sum(cohes) / len(cohes), attackers)
        print(f"  f{f}: foe {sum(u[2] for u in units.values()) / len(units):.1f} "
              f"cohesion {sum(cohes) / len(cohes):.1f} (max {max(cohes):.1f}) "
              f"attacking {len(attackers)}")

    print("--- scattered units (order while adrift) ---")
    shown = 0
    for f in frames:
        units = snaps[f]
        cx, cy = centroid(units)
        for uid in sorted(units):
            x, y, foe, order = units[uid]
            drift = math.hypot(x - cx, y - cy) / 32.0
            if drift > straggler_tiles:
                print(f"  f{f} unit {uid}: {drift:.1f} tiles off centroid, "
                      f"foe {foe:.1f}, order {order}")
                shown += 1
                if shown >= 15:
                    break
        if shown >= 15:
            break
    if shown == 0:
        print("  none: the pack held together in every snapshot")

    if first_attack:
        f, cohes, attackers = first_attack
        print(f"first Attack orders at f{f} with cohesion {cohes:.1f} "
              f"(units {attackers})")
    else:
        print("no Attack orders in any snapshot: the army never engaged")
    return 0


if __name__ == "__main__":
    straggler = 6.0
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for a in sys.argv[1:]:
        if a.startswith("--straggler="):
            straggler = float(a.split("=", 1)[1])
    if not args:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(args[0], straggler))
