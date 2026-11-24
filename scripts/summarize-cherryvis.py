#!/usr/bin/env python3
"""Token-cheap CherryVis trace query: what each unit did, worst movers first.

Usage:
  python3 scripts/summarize-cherryvis.py <trace.json|*.cvis dir> [--unit ID]
      [--limit N] [--acts]

Reads units_logs rows ({frame, message}) plus the sibling game_summary.json.
Sections: game header, move-order counts per unit (the scout-smoothness
signal: orders per minute + dedup holds), acts (FleeScourge, Hop, ...),
engine orders (#...), kill rows with running balances. --unit restricts to
one unit id; --acts prints the act histogram only.
"""
import json
import os
import sys
from collections import Counter


def load_trace(arg):
    if os.path.isdir(arg):
        trace = os.path.join(arg, "trace.json")
        summary = os.path.join(arg, "game_summary.json")
    else:
        trace = arg
        summary = os.path.join(os.path.dirname(arg) or ".", "game_summary.json")
    with open(trace, encoding="utf-8") as f:
        data = json.load(f)
    game = {}
    if os.path.isfile(summary):
        with open(summary, encoding="utf-8") as f:
            game = json.load(f)
    return data, game


def main(argv):
    args = [a for a in argv[1:] if not a.startswith("--")]
    opts = {}
    for a in argv[1:]:
        if a.startswith("--"):
            k, _, v = a[2:].partition("=")
            opts[k] = v
    if not args:
        print("usage: summarize-cherryvis.py <trace.json|dir> [--unit ID] [--limit N] [--acts]")
        return 2
    data, game = load_trace(args[0])
    only_unit = opts.get("unit")
    limit = int(opts.get("limit", "15"))
    logs = data.get("units_logs", {})
    if only_unit is not None:
        logs = {k: v for k, v in logs.items() if k == str(only_unit)}

    if game:
        secs = game.get("game_duration_frames", 0) // 24
        print("map=%s frames=%d (~%dm%02ds) p0win=%s" % (
            game.get("map", "?"), game.get("game_duration_frames", 0),
            secs // 60, secs % 60, game.get("p0_win", "?")))

    moves, holds, acts, engine, kills = Counter(), Counter(), Counter(), Counter(), []
    for uid, rows in logs.items():
        for r in rows:
            m = r.get("message", "")
            if m.startswith("Moving to"):
                moves[uid] += 1
            elif m == "Holding move order":
                holds[uid] += 1
            elif m.startswith("Killed") or m.startswith("Died"):
                kills.append((r.get("frame", 0), uid, m))
            elif m.startswith("#"):
                engine[m.split("@")[0]] += 1
            else:
                acts[m] += 1

    if "acts" in opts:
        for m, n in acts.most_common(limit):
            print("%5dx %s" % (n, m[:100]))
        return 0

    print("units=%d movers=%d" % (len(logs), len(moves)))
    for uid, n in moves.most_common(limit):
        print("unit %4s: %5d move orders, %5d holds" % (uid, n, holds.get(uid, 0)))
    if acts:
        print("--- acts ---")
        for m, n in acts.most_common(limit):
            print("%5dx %s" % (n, m[:100]))
    if engine:
        print("--- engine orders ---")
        for m, n in engine.most_common(8):
            print("%5dx %s" % (n, m[:80]))
    if kills:
        print("--- kills ---")
        for frame, uid, m in sorted(kills)[:limit]:
            print("%6d unit %4s %s" % (frame, uid, m[:80]))
        if len(kills) > limit:
            print("... %d more" % (len(kills) - limit))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
