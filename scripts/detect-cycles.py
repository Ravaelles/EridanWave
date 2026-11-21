#!/usr/bin/env python3
"""Behavior-cycle detector for traceless UMS maps.

While PROBE Act rows log one line per CHANGE (repeats collapse, so a
two-behavior flap reads as a handful of rows), bwapi-data/write/
behavior.log (armed via behavior.txt, see Debugging.BehaviorLog)
carries every unit's decision chain every frame. This script counts
what each unit did and, more importantly, finds ALTERNATION runs:
A B C D C D C D C E F G C D C D C is not fifteen decisions, it is C<->D
fighting itself twice. That pair is the whole bug class: two or three
behaviors renegotiating one frame instead of one owning it.

Usage: python3 scripts/detect-cycles.py <behavior.log> [--unit ID] [--min N]
  --min N: report pair runs with at least N alternations (default 3);
           triple runs need at least 2 full periods (6 frames) regardless.
  Chains normalize past parens (Retreating(No 40vs60) -> Retreating) so a
  drifting reason never splits a real alternation; the histogram keeps raw.
"""
import re
import sys
from collections import Counter, defaultdict

PAREN = re.compile(r'\(.*\)')


def norm(chain):
    """Canonical behavior set: parens stripped (a drifting reason never
    splits a real alternation) and tokens sorted (insertion order of
    the same set is not an alternation)."""
    return '/'.join(sorted(PAREN.sub('', chain).split('/')))


def load(path, only_unit=None):
    by_unit = defaultdict(list)
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            parts = line.split(None, 2)
            if len(parts) < 3:
                continue
            try:
                frame = int(parts[0])
            except ValueError:
                continue
            uid = parts[1]
            if only_unit is not None and uid != only_unit:
                continue
            by_unit[uid].append((frame, parts[2].strip()))
    return by_unit


def compress(rows):
    out = []
    for frame, chain in rows:
        if not out or out[-1][1] != chain:
            out.append((frame, chain))
    return out


def pair_runs(seq, min_alts):
    """Maximal XYXY.. runs (X != Y). Yields (x, y, alts, f0, f1)."""
    runs = []
    n = len(seq)
    i = 0
    while i < n - 1:
        x = norm(seq[i][1])
        y = norm(seq[i + 1][1])
        if x == y:
            i += 1
            continue
        j = i + 2
        while j < n and norm(seq[j][1]) == (x if (j - i) % 2 == 0 else y):
            j += 1
        alts = (j - i) - 1
        if alts >= min_alts:
            runs.append((x, y, alts, seq[i][0], seq[j - 1][0]))
        i = j - 1 if j - i > 2 else i + 1
    return runs


def triple_runs(seq):
    """Maximal ABCABC.. runs (period 3). Yields (a, b, c, periods, f0, f1)."""
    runs = []
    n = len(seq)
    i = 0
    while i < n - 2:
        pat = [norm(seq[i + k][1]) for k in range(3)]
        if len(set(pat)) < 2:
            i += 1
            continue
        j = i + 3
        while j < n and norm(seq[j][1]) == pat[(j - i) % 3]:
            j += 1
        periods = (j - i) // 3
        if periods >= 2:
            runs.append((pat[0], pat[1], pat[2], periods, seq[i][0], seq[j - 1][0]))
        i = j - 2 if j - i > 3 else i + 1
    return runs


def main(argv):
    args = []
    opts = {}
    rest = argv[1:]
    i = 0
    while i < len(rest):
        a = rest[i]
        if a.startswith('--'):
            k, _, v = a[2:].partition('=')
            if not v and i + 1 < len(rest) and not rest[i + 1].startswith('--'):
                v = rest[i + 1]
                i += 1
            opts[k] = v
        else:
            args.append(a)
        i += 1

    if not args:
        print(__doc__)
        return 2

    only_unit = opts.get('unit')
    min_alts = int(opts.get('min', '3'))

    by_unit = load(args[0], only_unit)
    pair_totals = Counter()
    pair_best = {}
    triple_all = []
    hist_global = Counter()
    units_with_cycles = 0
    for uid in sorted(by_unit, key=int):
        rows = sorted(by_unit[uid])
        for _, chain in rows:
            hist_global[chain] += 1
        seq = compress(rows)
        pairs = pair_runs(seq, min_alts)
        triples = triple_runs(seq)
        if pairs or triples:
            units_with_cycles += 1
            print(f'--- unit {uid} ({len(rows)} frames, {len(seq)} runs)')
            for x, y, alts, f0, f1 in sorted(pairs, key=lambda r: -r[2]):
                print(f'  {x} <-> {y}: {alts} alternations, longest f{f0}-f{f1}')
                pair_totals[(x, y)] += alts
                if (x, y) not in pair_best or alts > pair_best[(x, y)][0]:
                    pair_best[(x, y)] = (alts, uid, f0, f1)
            for a, b, c, periods, f0, f1 in triples:
                print(f'  {a} -> {b} -> {c}: {periods} periods, f{f0}-f{f1}')
                triple_all.append((a, b, c, periods, uid, f0, f1))
    print(f'=== {units_with_cycles}/{len(by_unit)} units alternate (min {min_alts}) ===')
    if pair_totals:
        print('--- top alternating pairs (all units) ---')
        for (x, y), total in pair_totals.most_common(10):
            alts, uid, f0, f1 = pair_best[(x, y)]
            print(f'  {x} <-> {y}: {total} alternations total, worst unit {uid} f{f0}-f{f1}')
    if triple_all:
        print('--- triple cycles ---')
        for a, b, c, periods, uid, f0, f1 in sorted(triple_all, key=lambda r: -r[3])[:10]:
            print(f'  {a} -> {b} -> {c}: {periods} periods, unit {uid} f{f0}-f{f1}')
    print('--- act histogram (raw, top 15) ---')
    for act, n in hist_global.most_common(15):
        print(f'  {n:7d}  {act}')


if __name__ == '__main__':
    sys.exit(main(sys.argv))
