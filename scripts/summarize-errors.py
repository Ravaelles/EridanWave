#!/usr/bin/env python3
"""Token-cheap error ledger query: one line per key, worst first.

Usage:
  python3 scripts/summarize-errors.py <errors.json> [--min-priority high|medium|low]
      [--key SUBSTRING] [--limit N] [--samples]

Defaults print every key with its count, frame span and priority; pass
--samples to also show the kept sample messages (capped at write time).
"""
import json
import sys

RANK = {"high": 3, "medium": 2, "low": 1}


def main(argv):
    args = [a for a in argv[1:] if not a.startswith("--")]
    opts = {}
    for a in argv[1:]:
        if a.startswith("--"):
            k, _, v = a[2:].partition("=")
            opts[k] = v
    if not args:
        print(__doc__.strip().splitlines()[-4])
        return 2

    with open(args[0], encoding="utf-8") as f:
        notes = json.load(f)
    minimum = opts.get("min-priority", "low")
    key_filter = opts.get("key", "")
    limit = int(opts.get("limit", "50"))
    show_samples = "samples" in opts
    rows = [n for n in notes
            if RANK.get(n.get("priority", ""), 0) >= RANK.get(minimum, 0)
            and key_filter in n.get("key", "")]
    rows.sort(key=lambda n: (-RANK.get(n.get("priority", ""), 0), -n.get("count", 0)))
    print("keys=%d notes=%d shown=%d" % (
        len(notes), sum(n.get("count", 0) for n in notes), min(len(rows), limit)))
    for n in rows[:limit]:
        print("%5dx [%s-%s] %-6s %s" % (
            n.get("count", 0), n.get("firstFrame", 0), n.get("lastFrame", 0),
            n.get("priority", "?"), n.get("key", "?")))
        if show_samples:
            for s in n.get("samples", [])[:3]:
                print("      | " + s.splitlines()[0][:160])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
