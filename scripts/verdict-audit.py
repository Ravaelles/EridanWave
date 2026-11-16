#!/usr/bin/env python3
"""Verdict-level disagreement audit (the true half-flees-half-fights ruler).

Act chains mix execution noise (cooldowns, positions); verdicts (+/- with
the FightRules reason) are the actual vote. Parses PROBE Verdict lines from
a Wine debug log (key must be armed via bwapi-data/write/probe.txt) and
joins squad membership from the CherryVis trace's units_updates, then
counts frames where SquadAttack members voted both ways - plus which
reasons drove each side of the split.

  python3 scripts/verdict-audit.py <trace.json|*.cvis dir> <wine-debug-log>
"""
import json
import os
import re
import sys
from collections import Counter, defaultdict

VERDICT = re.compile(r"PROBE Verdict ([0-9]+) ([0-9]+) ([+-][^ ]+)")


def load_trace(arg):
    if os.path.isdir(arg):
        trace = os.path.join(arg, "trace.json")
    else:
        trace = arg
    with open(trace, encoding="utf-8") as f:
        return json.load(f)


def main(argv):
    if len(argv) != 3:
        print("usage: verdict-audit.py <trace.json|dir> <wine-debug-log>")
        return 2
    data = load_trace(argv[1])
    tasks = data.get("tasks", {})
    task_name = {str(k): v.get("name", "?") for k, v in tasks.items()}
    updates = data.get("units_updates", {})
    task_events = defaultdict(list)
    for uid, frames in updates.items():
        for fs, info in frames.items():
            task_events[uid].append((int(fs), str(info.get("task"))))
    for ev in task_events.values():
        ev.sort()

    verdict_events = defaultdict(list)
    with open(argv[2], encoding="utf-8", errors="replace") as f:
        for line in f:
            m = VERDICT.search(line)
            if m:
                verdict_events[m.group(2)].append((int(m.group(1)), m.group(3)))
    for ev in verdict_events.values():
        ev.sort()
    if not verdict_events:
        print("no PROBE Verdict lines - arm the Verdict key in probe.txt")
        return 1

    bounds = set()
    for ev in list(task_events.values()) + list(verdict_events.values()):
        for fr, _ in ev:
            bounds.add(fr)
    cur_task, cur_verdict = {}, {}
    ti = {u: 0 for u in task_events}
    vi = {u: 0 for u in verdict_events}
    voted = split = 0
    split_reasons = Counter()
    for fr in sorted(bounds):
        for u, ev in task_events.items():
            while ti[u] < len(ev) and ev[ti[u]][0] <= fr:
                cur_task[u] = ev[ti[u]][1]
                ti[u] += 1
        for u, ev in verdict_events.items():
            while vi[u] < len(ev) and ev[vi[u]][0] <= fr:
                cur_verdict[u] = ev[vi[u]][1]
                vi[u] += 1
        for_votes, against_votes = Counter(), Counter()
        n = 0
        for u, v in cur_verdict.items():
            if task_name.get(cur_task.get(u), "?") == "SquadAttack":
                n += 1
                (for_votes if v.startswith("+") else against_votes)[v] += 1
        if n >= 3 and for_votes and against_votes:
            voted += 1
            split += 1
            split_reasons[(for_votes.most_common(1)[0][0],
                           against_votes.most_common(1)[0][0])] += 1
        elif n >= 3:
            voted += 1
    print("SquadAttack verdict moments=%d split=%d rate=%.1f%%" % (
        voted, split, 100.0 * split / max(1, voted)))
    print("top split reason pairs (for / against):")
    for (f, a), c in split_reasons.most_common(10):
        print("  %6d  %-16s vs %-16s" % (c, f, a))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
