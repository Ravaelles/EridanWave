#!/usr/bin/env python3
"""Cluster disagreement audit (phase 1 of _COMBAT-REFACTOR.md).

Counts frames where units of one combat squad hold different verdicts:
FIGHT vs FLIGHT vs HOLD. A healthy cluster moves as one; every frame with
two verdict classes inside one squad is the half-flees-half-fights disease
made countable. Run against finished CherryVis traces:

  python3 scripts/disagreement-audit.py <trace.json|*.cvis dir> [...]

units_logs rows are change-only act chains, so each unit's last token is
forward-filled; task ownership comes from units_updates (also change-only).
Only combat squads (SquadAttack/DefendBase/Raze/Backstab/DarkTemplar and
BaseGuards) count, and only classifiable acts vote - economy noise stays out.
"""
import json
import os
import sys
from collections import defaultdict

FIGHT = {
    "Engage", "Approach", "Chase", "Abuse", "Brawl", "Attack", "Fight",
    "Charge", "ContainFire", "KiteAway", "Push", "StaticDefenseRepel",
    "Reposition", "CrucialAirSnipe", "TankHug", "Scavenge", "SuicideEngage",
    "IdleEngage", "Hunt", "Snipe", "StandShoot", "HoldToShoot", "Lurk",
    "Potshot",
}
FLIGHT = {
    "Retreat", "Flee", "Fallback", "Excuse", "Returning to slot", "Dodging",
    "Dodge", "AirFlee", "ObserverFlee", "StaticRepel",
}
HOLD = {
    "Waiting for pack", "Marching to formation slot",
    "Rallying to pack centroid", "Hold", "Move", "Slide", "Dress",
    "LoneMarch", "MainMarch", "PairRide", "StaticPush", "PackRegroup",
    "StrongMarch", "NoTankMarch", "ToothlessMarch", "MarchDress",
}

COMBAT_TASKS = {
    "SquadAttack", "SquadDefendBase", "SquadRazeExpansion",
    "SquadBackstabProxy", "SquadDarkTemplar", "SquadBaseGuards",
    "SquadCatchDTs",
}


def verdict_of(token):
    if token in FIGHT:
        return "FIGHT"
    if token in FLIGHT:
        return "FLIGHT"
    if token in HOLD:
        return "HOLD"
    return None


def load(arg):
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


def audit(data):
    tasks = data.get("tasks", {})
    task_name = {str(k): v.get("name", "?") for k, v in tasks.items()}
    logs = data.get("units_logs", {})
    updates = data.get("units_updates", {})

    workers = set()  # economy hands never vote: their potshots would drown
    for uid, rows in logs.items():  # the warrior verdict in defense squads
        for r in rows:
            if r["message"].split("/")[0] in (
                    "gather:Minerals", "gather:Gas", "returnCargo",
                    "moveToConstruct") or r["message"].startswith("issueBuild"):
                workers.add(uid)
                break

    act_events = defaultdict(list)   # uid -> [(frame, verdict)]
    for uid, rows in logs.items():
        if uid in workers:
            continue
        for r in rows:
            tok = r["message"].split("/")[-1]
            v = verdict_of(tok)
            if v is not None:
                act_events[uid].append((r["frame"], v))
    task_events = defaultdict(list)  # uid -> [(frame, task)]
    for uid, frames in updates.items():
        for fs, info in frames.items():
            task_events[uid].append((int(fs), str(info.get("task"))))
    for ev in list(act_events.values()) + list(task_events.values()):
        ev.sort()

    bounds = set()
    for ev in list(act_events.values()) + list(task_events.values()):
        for f, _ in ev:
            bounds.add(f)
    if not bounds:
        return {}, {}, 0
    end = max(bounds)

    cur_act, cur_task = {}, {}
    ai = {u: 0 for u in act_events}
    ti = {u: 0 for u in task_events}
    group_frames = defaultdict(int)   # (task_name) -> frames with >=3 voters
    group_split = defaultdict(int)    # -> frames with >=2 verdict classes
    worst = []                        # (frame, group, verdicts)
    flips = defaultdict(int)          # uid -> verdict changes
    prev_v = {}

    for f in sorted(bounds):
        for u, ev in act_events.items():
            while ai[u] < len(ev) and ev[ai[u]][0] <= f:
                cur_act[u] = ev[ai[u]][1]
                ai[u] += 1
        for u, ev in task_events.items():
            while ti[u] < len(ev) and ev[ti[u]][0] <= f:
                cur_task[u] = ev[ti[u]][1]
                ti[u] += 1
        groups = defaultdict(set)
        for u, v in cur_act.items():
            t = cur_task.get(u)
            name = task_name.get(t, "?") if t is not None else None
            if name in COMBAT_TASKS:
                if u in cur_act and (u not in prev_v or prev_v[u] != v):
                    flips[u] += 1
                    prev_v[u] = v
                groups[name].add(u)
        by_group = defaultdict(lambda: defaultdict(int))
        members = defaultdict(set)
        for u, v in cur_act.items():
            t = cur_task.get(u)
            name = task_name.get(t, "?") if t is not None else None
            if name in COMBAT_TASKS:
                by_group[name][v] += 1
                members[name].add(u)
        for name, counts in by_group.items():
            n = sum(counts.values())
            if n >= 3:
                group_frames[name] += 1
                if len(counts) >= 2:
                    group_split[name] += 1
                    if len(worst) < 12:
                        worst.append((f, name, dict(counts)))
    return group_frames, group_split, end, flips


def main(argv):
    args = [a for a in argv[1:] if not a.startswith("--")]
    if not args:
        print("usage: disagreement-audit.py <trace.json|dir> [...]")
        return 2
    for arg in args:
        data, game = load(arg)
        frames, split, end, flips = audit(data)
        secs = (game.get("game_duration_frames", end) or end) // 24
        print("== %s  map=%s frames=%d (~%dm%02ds)" % (
            arg, game.get("map", "?"),
            game.get("game_duration_frames", end) or end, secs // 60, secs % 60))
        total_f = total_s = 0
        for name in sorted(frames):
            f, s = frames[name], split.get(name, 0)
            total_f += f
            total_s += s
            print("  %-18s voted=%6d split=%6d rate=%5.1f%%" % (
                name, f, s, 100.0 * s / max(1, f)))
        print("  TOTAL voted=%d split=%d rate=%.1f%%" % (
            total_f, total_s, 100.0 * total_s / max(1, total_f)))
        top = sorted(flips.items(), key=lambda kv: -kv[1])[:8]
        print("  top flapping units: " + ", ".join(
            "%s(%dx)" % (u, n) for u, n in top))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
