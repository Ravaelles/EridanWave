#!/usr/bin/env python3
"""Join shadow-verdict disagreements with mixed march/duel frames.

Usage: python3 shadow-join.py <WINE_DEBUG>
Reads PROBE March/Act/Verdict/Technique/RetreatShadow/Owner lines and prints:
 1. mixed frames (march home-ish + duel acts), each with technique + shadow
 2. shadow disagreements NOT covered by mixed frames (silent splits)
 3. vintage analysis: at each mixed frame, was the march-time shouldFight
    (last Verdict note strictly before the frame) different from the
    frame's own verdict? I.e. pure 1-frame vintage flips vs input splits.
"""
import re, collections, sys

D = sys.argv[1]
march, acts, verd, tech, shadow = (collections.defaultdict(dict) for _ in range(5))
own = {}
pat = re.compile(r'PROBE (March|Act|Verdict|Technique|RetreatShadow|Owner) (\d+) (\d+) (.*)')
for line in open(D, encoding='utf-8', errors='replace'):
    m = pat.search(line)
    if not m:
        continue
    kind, fr, uid, msg = m.group(1), int(m.group(2)), m.group(3), m.group(4).strip()
    if kind == 'March':
        march[uid][fr] = msg.split(' terminus')[0]
    elif kind == 'Act':
        acts[uid].setdefault(fr, []).append(msg.split()[0] if msg else '')
    elif kind == 'Verdict':
        verd[uid][fr] = msg
    elif kind == 'Technique':
        tech[uid][fr] = msg
    elif kind == 'RetreatShadow':
        shadow[uid][fr] = msg
    else:
        own[uid] = msg.split()[-1][:30]

def homeish(label):
    return 'Retreat' in label or label == 'Marching home'

def last_before(d, uid, fr):
    best = None
    for f, v in d.get(uid, {}).items():
        if f < fr and (best is None or f > best[0]):
            best = (f, v)
    return best

mixed = 0
vintage_flip = 0
print('=== mixed frames (march home-ish + duel acts) ===')
for uid in sorted(march, key=int):
    for fr in sorted(march[uid]):
        label = march[uid][fr]
        if not homeish(label):
            continue
        a = ' '.join(acts[uid].get(fr, []))
        first = a.split('/')[0] if a else '(none)'
        if not ('Abuse' in a or first in ('Engage', 'Chase', 'Approach') or a.startswith('Engage')):
            continue
        mixed += 1
        t = last_before(tech, uid, fr + 1)
        s = last_before(shadow, uid, fr + 1)
        v_here = verd[uid].get(fr, '?')
        v_prev = last_before(verd, uid, fr)

        # vintage flip: fight-ness changed between march time (prev) and now
        def fights(v):
            return v is not None and v != '?' and v.startswith('+')

        flip = False
        if v_here != '?' and v_prev is not None and fights(v_here) != fights(v_prev[1]):
            flip = True
            vintage_flip += 1
        print(f"u={uid} {own.get(uid,'?')} f={fr} march={label} act={a} "
              f"tech={t[1] if t else None} shadow={s[1] if s else None} "
              f"verdictNow=[{v_here}] verdictPrev={v_prev} {'VINTAGE-FLIP' if flip else ''}")
print(f'mixed: {mixed}, of which vintage flips: {vintage_flip}')

print('=== shadow disagreements outside mixed frames ===')
n = 0
for uid in sorted(shadow, key=int):
    for fr in sorted(shadow[uid]):
        label = [v for f, v in march.get(uid, {}).items() if abs(f - fr) <= 1]
        a = ' '.join(acts[uid].get(fr, []))
        first = a.split('/')[0] if a else '(none)'
        is_mixed = any(homeish(l) for l in label) and (
            'Abuse' in a or first in ('Engage', 'Chase', 'Approach') or a.startswith('Engage'))
        if not is_mixed:
            n += 1
            if n <= 30:
                print(f"u={uid} {own.get(uid,'?')} f={fr} shadow=[{shadow[uid][fr]}] march~{label} act={a or '(none)'}")
print(f'silent shadow disagreements: {n} (first 30 shown)')
