#!/usr/bin/env python3
"""One-page match debrief for a finished scbw game dir.

Usage: python3 scripts/analyze-match.py <GAME_DIR> [--limit=N] [--tag=NAME]

Prints: result, kills/lost, top acts (sorted), our deaths by class,
and for the worst-dying combat classes the last decision each victim
logged before dying (formation death vs combat death signal).
With --tag, also appends one TSV row to out/matches.tsv for the
campaign rollup (batch comparisons pre/post fix).
"""
import csv
import json
import os
import sys
from collections import Counter

def load_trace(game_dir):
    for base in ("write_0/cherryvis_dir.rep.cvis",):
        tj = os.path.join(game_dir, base, "trace.json")
        if os.path.exists(tj):
            with open(tj, encoding="utf-8", errors="replace") as f:
                return json.load(f).get("units_logs", {})

    return {}

def main(argv):
    args = [a for a in argv[1:] if not a.startswith("--")]
    limit = 40
    tag = None
    for a in argv[1:]:
        if a.startswith("--limit="):
            limit = int(a.split("=")[1])
        if a.startswith("--tag="):
            tag = a.split("=")[1]
    if not args:
        print(__doc__)

        return 2

    game = args[0]
    name = os.path.basename(game.rstrip("/"))

    res_path = os.path.join(game, "result.json")
    res = json.load(open(res_path)) if os.path.exists(res_path) else {}
    print("game=%s map=%s winner=%s frames=%s" % (
        name, res.get("map_name", "?"), res.get("winner", "?"),
        res.get("game_time", "?")))

    botlog = os.path.join(game, "logs_0", "bot.log")
    kl = "?"
    if os.path.exists(botlog):
        with open(botlog, encoding="utf-8", errors="replace") as f:
            for line in f:
                if "Units killed/lost" in line:
                    kl = line.strip().split("###")[1].strip()
    print("kills/lost:", kl)

    acts = Counter()
    logs = load_trace(game)
    last_before_death = {}
    for uid, rows in logs.items():
        for i, r in enumerate(rows):
            m = r.get("message", "")
            if m.startswith("Moving to") or m == "Holding move order":
                continue
            if m.startswith("Killed") or m.startswith("Died"):
                if m.startswith("Died") and i > 0:
                    prev = [x.get("message", "") for x in rows[max(0, i - 3):i]]
                    last_before_death[uid] = (r.get("frame", 0), prev)
                continue
            else:
                acts[m] += 1
            if m.startswith("Died") and i > 0:
                prev = [x.get("message", "") for x in rows[max(0, i - 3):i]]
                last_before_death[uid] = (r.get("frame", 0), prev)

    print("--- top acts ---")
    for m, n in acts.most_common(limit):
        print("%5dx %s" % (n, m[:90]))

    flap = Counter()
    flap_seq = {}
    for uid, rows in logs.items():
        seq = [r.get("message", "") for r in rows
               if not r.get("message", "").startswith(
                   ("#", "Moving to", "Holding", "Killed", "Died"))]
        changes = sum(1 for a, b in zip(seq, seq[1:]) if a != b)
        if changes > 0:
            flap[uid] = changes
            flap_seq[uid] = seq[:10]
    print("--- top flappers (decision changes per unit) ---")
    for uid, n in flap.most_common(12):
        print("%5dx id=%s :: %s" % (
            n, uid, " -> ".join(flap_seq[uid])[:160]))

    ue = os.path.join(game, "logs_0", "unit_events.csv")
    deaths = Counter()
    death_frame = {}
    if os.path.exists(ue):
        with open(ue, encoding="utf-8", errors="replace") as f:
            for row in csv.DictReader(f):
                if row["event_type"] == "unitDestroy" and row["player_owned"] == "true":
                    deaths[row["unit_type"]] += 1
                    death_frame[row["unit_id"]] = row["frame_number"]
    print("--- our deaths by class ---")
    for k, n in deaths.most_common():
        print("%5dx %s" % (n, k))

    print("--- last decision before death (worst classes) ---")
    worst = [k for k, _ in deaths.most_common(4)]
    shown = 0
    with open(ue, encoding="utf-8", errors="replace") as f:
        id2type = {r["unit_id"]: r["unit_type"] for r in csv.DictReader(f)
                   if r["player_owned"] == "true"}
    tails = Counter()
    for uid, (frame, prev) in sorted(last_before_death.items(),
                                      key=lambda kv: kv[1][0]):
        t = id2type.get(uid, "?")
        if t in worst:
            tails[(t, prev[-1] if prev else "?")] += 1
            if shown < 12:
                print("f%-6s %-22s id=%s <- %s" % (
                    frame, t, uid, " | ".join(prev)[-160:]))
                shown += 1
    print("--- death-tail rollup ---")
    for (t, last), n in tails.most_common(12):
        print("%5dx %-22s <- %s" % (n, t, last[:90]))
    if tag:
        log_campaign_row(name, game, tag, res, kl, acts, deaths)
    return 0

def log_campaign_row(name, game_dir, tag, res, kl, acts, deaths):
    tsv = os.path.join("out", "matches.tsv")
    header = "game\ttag\tenemy\twinner\tkills_lost\tstorms\tdt_hunt\tdt_evade\tcorsair_hunt\tcorsair_dance\tht_dead\tlurkers_killed\ttop_act\ttop_deaths\n"
    bots = res.get("bots") or []
    enemy = ",".join(b for b in bots if not b.startswith("EridanWave"))
    kills = ""
    import re
    m = re.search(r"(\d+)/(\d+)", kl)
    if m:
        kills = "%s/%s" % (m.group(1), m.group(2))
    acts_c = acts
    top_act = acts_c.most_common(1)[0][0] if acts_c else ""
    top_deaths = ",".join("%s:%d" % (k.split("_")[-1][:4], v)
                           for k, v in deaths.most_common(3))
    ht_dead = deaths.get("Protoss_High_Templar", 0)
    lurkers_killed = 0
    try:
        with open(os.path.join(game_dir, "logs_0", "unit_events.csv"),
                  encoding="utf-8", errors="replace") as f:
            for row in csv.DictReader(f):
                if (row["event_type"] == "unitDestroy"
                        and row["player_owned"] == "false"
                        and row["unit_type"] == "Zerg_Lurker"):
                    lurkers_killed += 1
    except OSError:
        pass
    row = "%s\t%s\t%s\t%s\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\t%s\n" % (
        name, tag, enemy, res.get("winner", "?"), kills,
        acts_c.get("PsionicStorm", 0),
        acts_c.get("DarkTemplarHunt", 0),
        acts_c.get("DarkTemplarEvade", 0),
        acts_c.get("CorsairHunt", 0),
        acts_c.get("CorsairDance", 0),
        ht_dead, lurkers_killed,
        top_act, top_deaths)
    new = not os.path.exists(tsv)
    with open(tsv, "a", encoding="utf-8") as f:
        if new:
            f.write(header)
        f.write(row)
    print("logged to %s [%s]" % (tsv, tag))

if __name__ == "__main__":
    sys.exit(main(sys.argv))
