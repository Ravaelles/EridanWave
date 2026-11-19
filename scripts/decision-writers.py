#!/usr/bin/env python3
"""Static single-writer audit: every frame a unit obeys one destination.
March slots own the walk; anything else that writes a destination must
be a known exception (survival, duel chase, pair logistics, slotless
doctrine) or it breaks the line PurpleWave holds.

Usage: python3 scripts/decision-writers.py [--update]
  Scans forceTo(/decision.set(/Commander.(move|attackMove|attack)(
  call sites, classifies the destination and compares against
  scripts/decision-writers.allowlist (one normalized key per line).
  Unknown writer  -> printed + exit 1 (review it, then pin it).
  --update        -> rewrite the allowlist from the current tree.

Destination classes (first match wins):
  SLOT  formation placement / station / slot / march terminus machinery
  PACK  centroid / ally / mate / ride / armyGoal / home / battery / leader
  ENEMY target / foe / remembered / contact / chase pixel
  FREE  projection / fallback / away-from / guard( / computed point
  VOTE  no destination (decideFight, posture, intent flags) - skipped
"""
import re
import subprocess
import sys

ROOT = subprocess.check_output(
    ["git", "rev-parse", "--show-toplevel"], text=True).strip() + "/"
ALLOW = ROOT + "scripts/decision-writers.allowlist"

CALL = re.compile(r"\.(forceTo|decision\.set)\(\s*\S")
CMDR = re.compile(r"Commander\.(move|attackMove|attack)\(\s*\w+\s*,")

RULES = [
    ("SLOT", re.compile(
        r"placement|station|slot|formation|terminus|redoubt|getTravel|getReturn|marchLegs|finalTravel|finalMarch")),
    ("PACK", re.compile(
        r"centroid|ally|allies|mate|ride|armyGoal|home|battery|recharge|leader|waypoint|standoff|baitGoal|park|safety|homeConsensus|veterans")),
    ("STAY", re.compile(
        r"^unit\.pixel(\.walkablePixel)?$")),
    ("ENEMY", re.compile(
        r"foe|target|enemy|remember|contact|chase|hunt|snipe")),
    ("FREE", re.compile(r".")),
]


def classify(expr):
    for name, rx in RULES:
        if rx.search(expr):
            return name
    return "FREE"


def balanced(text, start):
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return text[start:]


def writers():
    out = []
    files = [f for f in subprocess.check_output(
        ["git", "ls-files", "*.scala"], text=True,
        cwd=ROOT.rstrip("/")).split()
        if f.startswith("src/") and "/test/" not in f
        and "test/scala" not in f and not f.startswith("src-old/")]
    for rel in files:
        if "/test/" in rel or "test/scala" in rel:
            continue
        try:
            with open(ROOT + rel, encoding="utf-8") as f:
                lines = f.readlines()
        except OSError:
            continue
        blob = "".join(lines)
        for m in list(CALL.finditer(blob)) + list(CMDR.finditer(blob)):
            paren = blob.index("(", m.start())
            arg = balanced(blob, paren)[1:-1].strip()
            arg = re.sub(r"\s+", " ", arg)
            if len(arg) > 140:
                arg = arg[:140] + "..."
            line = blob.count("\n", 0, m.start()) + 1
            out.append((rel, line, "." + m.group(1), arg))
    return out


def main():
    rows = writers()
    keyed = [(f"{rel} :: {call}({arg})", rel, line, call, arg,
              classify(call + "(" + arg)) for rel, line, call, arg in rows]
    if "--update" in sys.argv:
        with open(ALLOW, "w", encoding="utf-8") as f:
            for key, _, _, _, _, _ in sorted(keyed):
                f.write(key + "\n")
        print(f"allowlist: {len(keyed)} writers pinned")
        return 0
    try:
        with open(ALLOW, encoding="utf-8") as f:
            known = {ln.strip() for ln in f if ln.strip()}
    except OSError:
        known = set()
    unknown = [(k, rel, line, cls) for k, rel, line, _, _, cls in keyed
               if k not in known]
    by_cls = {}
    for k, rel, line, _, _, cls in keyed:
        by_cls.setdefault(cls, []).append((rel, line))
    print(f"writers: {len(keyed)} "
          + " ".join(f"{c}={len(v)}" for c, v in sorted(by_cls.items())))
    if unknown:
        print(f"UNLISTED writers ({len(unknown)}) - review, then pin:")
        for k, rel, line, cls in unknown:
            print(f"  [{cls}] {rel}:{line}: {k.split(' :: ', 1)[1]}")
        return 1
    print("all writers pinned in scripts/decision-writers.allowlist")
    return 0


if __name__ == "__main__":
    sys.exit(main())
