#!/usr/bin/env python3
"""Retreat-discipline audit: mixed march/duel frames in one Wine run.

The contract: a frame whose march label says home (Retreating, Marching
home, Returning to slot) must never carry a duel act (Abuse, Engage,
Chase, Approach that closes). Retreating + Retreat* is the consistent
pair and does not count. Also counts duplicate same-unit same-frame
March notes (two writers = re-squadding double-send).

The mixed count splits by label: the verdict arm owns `Retreating`
(hard retreat - must be zero, same-tick contradictions only), while
`Marching home` is the soft fallback (walk home, no fight) where a
fight-verdict duel while marching through contact is marching, not
flapping. Sleep-merged windows (a pre-flip duel act flushed together
with fresh retreat acts) and terminus re-notes inflate both buckets;
read the residual with scripts/shadow-join.py, not raw.

Usage: python3 scripts/retreat-audit.py out/wine_runs/<run>/WINE_DEBUG
"""
import re
import sys
from collections import Counter, defaultdict

DUEL_ACTS = ("Abuse", "Engage", "Chase", "Approach")


def main(path):
    march = defaultdict(dict)
    acts = defaultdict(dict)
    pat = re.compile(r'PROBE (March|Act) (\d+) (\d+) (.*)')
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            m = pat.search(line)
            if not m:
                continue
            kind, frame, uid, msg = m.group(1), int(m.group(2)), m.group(3), m.group(4)
            if kind == 'March':
                march[uid][frame] = msg.split(' terminus')[0]
            else:
                acts[uid].setdefault(frame, []).append(msg.split()[0] if msg else '')

    mixed = Counter()
    hard_mixed = Counter()
    retreat_frames = 0
    for uid, frames in march.items():
        for frame, label in frames.items():
            if 'Retreat' in label or label == 'Marching home':
                retreat_frames += 1
                chain = ' '.join(acts[uid].get(frame, []))
                first = chain.split('/')[0] if chain else '(none)'
                if 'Abuse' in chain or first in DUEL_ACTS or chain.startswith('Engage'):
                    mixed[(label, first if first != '(none)' else chain)] += 1
                    if 'Retreat' in label:
                        hard_mixed[(label, first if first != '(none)' else chain)] += 1

    dup_pat = re.compile(r'PROBE March (\d+) (\d+) ')
    seen = defaultdict(Counter)
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            m = dup_pat.search(line)
            if m:
                seen[m.group(2)][int(m.group(1))] += 1
    dups = sum(1 for u in seen.values() for n in u.values() if n > 1)

    print(f'retreat-march frames: {retreat_frames} mixed: {sum(mixed.values())} (hard Retreating: {sum(hard_mixed.values())})')
    for (label, act), n in mixed.most_common(15):
        print(f'  {n:5d}  {label} + {act}')
    print(f'duplicate same-unit same-frame March notes: {dups}')


if __name__ == '__main__':
    main(sys.argv[1])
