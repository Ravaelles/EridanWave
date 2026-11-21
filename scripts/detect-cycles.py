#!/usr/bin/env python3
"""Behavior-cycle detector for traceless UMS maps.

While PROBE Act rows log one line per CHANGE (repeats collapse, so a
two-behavior flap reads as a handful of rows), bwapi-data/write/
behavior.log carries every unit's decision chain every frame. This
script counts what each unit did and, more importantly, finds
ALTERNATION runs: A B C D C D C D C E F G C D C D C is not fifteen
decisions, it is C<->D fighting itself twice. That pair is the whole
bug class: two or three behaviors renegotiating one frame instead of
one owning it.

Layers: with --march <normal.log> each frame also carries the march
label (M:), forward-filled from change-only March rows, beside the act
chain (T:). A pair whose two signatures change BOTH layers at once is
flagged CROSS - two writers (squad WHERE vs unit HOW) fighting over
the frame. Same-layer pairs are technique-internal cadence and usually
fine. A hard mix counter tracks Retreating frames carrying duel acts.

Usage: python3 scripts/detect-cycles.py <behavior.log> [--march normal.log]
       [--unit ID] [--min N]
  --min N: report pair runs with at least N alternations (default 3);
  triple runs need at least 2 full periods (6 frames) regardless.
  Chains normalize past parens (Retreating(No 40vs60) -> Retreating)
  and sort tokens, so a drifting reason or insertion order never splits
  a real alternation; the histogram keeps raw.
"""
import re
import sys
from collections import Counter, defaultdict

PAREN = re.compile(r'\(.*\)')


def norm_chain(chain):
    return sorted(PAREN.sub('', chain).split('/'))


def norm_march(label):
    return PAREN.sub('', label).strip()


def load_behavior(path, only_unit=None):
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


def load_march(path, only_unit=None):
    by_unit = defaultdict(dict)
    if not path:
        return by_unit

    pat = re.compile(r'PROBE March (\d+) (\d+) (.*)')
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            m = pat.search(line)
            if not m:
                continue
            frame, uid, msg = int(m.group(1)), m.group(2), m.group(3)
            if only_unit is not None and uid != only_unit:
                continue
            label = msg.split(' terminus')[0]
            by_unit[uid][frame] = label
    return by_unit


DUEL_TOKENS = {
    'Engage', 'Approach', 'Chase', 'Abuse', 'Brawl', 'Charge',
    'Fight', 'Hunt', 'Push', 'Scavenge', 'ContainFire', 'Attack',
}


def signatures(rows, march_rows):
    """Merged per-frame signatures: 'M:<march> | <tchain>'. March is
    forward-filled (change-only); unknown before the first row."""
    march_frames = sorted(march_rows)
    out = []
    mi = 0
    current = None
    for frame, chain in sorted(rows):
        while mi < len(march_frames) and march_frames[mi] <= frame:
            current = march_rows[march_frames[mi]]
            mi += 1
        m = 'M:' + (norm_march(current) if current is not None else '?')
        t = 'T:' + '/'.join(norm_chain(chain))
        out.append((frame, m + ' | ' + t, m, set(norm_chain(chain))))
    return out


def pair_runs(seq, min_alts):
    """Maximal XYXY.. runs (X != Y). Yields (x, y, alts, f0, f1)."""
    runs = []
    n = len(seq)
    i = 0
    while i < n - 1:
        x = seq[i][1]
        y = seq[i + 1][1]
        if x == y:
            i += 1
            continue
        j = i + 2
        while j < n and seq[j][1] == (x if (j - i) % 2 == 0 else y):
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
        pat = [seq[i + k][1] for k in range(3)]
        if len(set(pat)) < 2:
            i += 1
            continue
        j = i + 3
        while j < n and seq[j][1] == pat[(j - i) % 3]:
            j += 1
        periods = (j - i) // 3
        if periods >= 2:
            runs.append((pat[0], pat[1], pat[2], periods, seq[i][0], seq[j - 1][0]))
        i = j - 2 if j - i > 3 else i + 1
    return runs


def is_cross(x, y):
    """Both layers change between the two signatures at once."""
    xm, xt = x.split(' | ', 1)
    ym, yt = y.split(' | ', 1)
    return xm != ym and xt != yt


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
    march_path = opts.get('march')

    by_unit = load_behavior(args[0], only_unit)
    march = load_march(march_path, only_unit)
    pair_totals = Counter()
    pair_best = {}
    triple_all = []
    hist_global = Counter()
    march_hist = Counter()
    mix_units = {}
    units_with_cycles = 0
    for uid in sorted(by_unit, key=int):
        seq = signatures(by_unit[uid], march.get(uid, {}))
        for _, sig, m, _ in seq:
            hist_global[sig] += 1
            march_hist[m] += 1
        mixes = [(f, sig) for f, sig, m, t in seq
                 if m.startswith('M:Retreating') and t & DUEL_TOKENS]
        if mixes:
            mix_units[uid] = mixes
        pairs = pair_runs(seq, min_alts)
        triples = triple_runs(seq)
        if pairs or triples:
            units_with_cycles += 1
            print(f'--- unit {uid} ({len(seq)} frames)')
            for x, y, alts, f0, f1 in sorted(pairs, key=lambda r: -r[2]):
                tag = ' CROSS' if is_cross(x, y) else ''
                print(f'  {x} <-> {y}: {alts} alternations, longest f{f0}-f{f1}{tag}')
                pair_totals[(x, y)] += alts
                if (x, y) not in pair_best or alts > pair_best[(x, y)][0]:
                    pair_best[(x, y)] = (alts, uid, f0, f1)
            for a, b, c, periods, f0, f1 in triples:
                print(f'  {a} -> {b} -> {c}: {periods} periods, f{f0}-f{f1}')
                triple_all.append((a, b, c, periods, uid, f0, f1))
    print(f'=== {units_with_cycles}/{len(by_unit)} units alternate (min {min_alts}) ===')
    cross = [(p, t) for p, t in pair_totals.items() if is_cross(*p)]
    if cross:
        print('--- CROSS-layer pairs (march and technique change together) ---')
        for (x, y), total in sorted(cross, key=lambda kv: -kv[1])[:10]:
            alts, uid, f0, f1 = pair_best[(x, y)]
            print(f'  {x} <-> {y}: {total} alternations total, worst unit {uid} f{f0}-f{f1}')
    if pair_totals:
        print('--- top alternating pairs (all units) ---')
        for (x, y), total in pair_totals.most_common(10):
            alts, uid, f0, f1 = pair_best[(x, y)]
            print(f'  {x} <-> {y}: {total} alternations total, worst unit {uid} f{f0}-f{f1}')
    if triple_all:
        print('--- triple cycles ---')
        for a, b, c, periods, uid, f0, f1 in sorted(triple_all, key=lambda r: -r[3])[:10]:
            print(f'  {a} -> {b} -> {c}: {periods} periods, unit {uid} f{f0}-f{f1}')
    if mix_units:
        print('--- HARD MIX: Retreating march with duel acts in the same frame ---')
        for uid, mixes in sorted(mix_units.items(), key=lambda kv: -len(kv[1])):
            f0 = mixes[0][0]
            f1 = mixes[-1][0]
            print(f'  unit {uid}: {len(mixes)} frames, f{f0}-f{f1}, e.g. {mixes[0][1]}')
    else:
        print('--- no Retreating+duel mixes ---')
    print('--- top march labels ---')
    for m, n in march_hist.most_common(8):
        print(f'  {n:7d}  {m}')
    print('--- top signatures (raw, top 10) ---')
    for sig, n in hist_global.most_common(10):
        print(f'  {n:7d}  {sig}')


if __name__ == '__main__':
    sys.exit(main(sys.argv))
