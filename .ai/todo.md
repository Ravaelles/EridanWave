# HIGH PRIORITY: order-issuers review (SRP, one question per unit) - audit done 2026-11-24, fixes in progress on f/phase-j

Review every class that issues orders to combat units
(Commander*/Combat*/Behaviors/*Behavior, Be* brains, march legs):
each answers exactly ONE question, owns exactly one verdict
(rules 9/11), never sets another owner's state. Split where a class
does two things; keep the old behavior behind the split. Enforcement
where cheap (token/sealed paths), audit trail in commits otherwise.

Done: leash/rejoin extracted to Behaviors; Retreating-label token
(compiler-enforced); incidental fix - the L1 rivalry tripwire compared
a motion label against a march label and never fired.
Deferred as structural (needs owner): Combat.scala split,
BeDarkTemplar/BeCarrier breakdowns, Commander* sink slimming, Target
ladder ownership, Bust double toAttack write, perch.clear ownership,
march-tick removal (load-bearing: marching-only units need a vote).

# 1Drag_v_1Zeal kite duel (owner bar: max 1-3 hits, wins on both maps) - bar UNMET on variance

Mechanism shipped: dragoon kites a lone Zealot from first sight
(chase 7 tiles), contact-kite bypass (dragoon + melee<=2 tiles + cd>=15
always kites), guards leashed/aligned/edge-capped. StarEngine: 0 hits,
kill in 223f. UMS 1v1: wins + draws on identical code, hits 0-23 across
reps (glue fights decide, not the decision). Bar met ~2/9.
Watch: RetreatVerdictHoldSuite.skewed-readers fails on the clean tree
too (stash-proven) - pre-existing, not ours.
Pending ladder confirmation (rule 21): UmsHold paced advance, CatchUp
full-speed join, lone-blade kite, UMS-restricted Beast/Dragon maps.

# Formation advance (owner order 2026-11-24: 0.25 tiles, frames%20<=8 window)

The dressed line inches toward the nearest known warrior inside a move
window (`ApproachEnemyFormation`: frames 0-8 of every 20-frame cycle,
all warriors step 0.25 tiles, staggered by id; the window re-issues
until the hull moves - a single step after holds never executes).
History: 20f/0.25 all-hulls -> 15f/0.3 melee-only (froze ranged mirrors)
-> 10f/0.25 all warriors (single ticks swallowed by the one-order skip,
back units held past the 2-tile slot cap) -> 20f-cycle/9f-window.
Live 2026-11-24 Drag_v_LingsUpg: DressAdvance on 10 dragoon ids f0-100
(whole line, front and back), 713 steps on 553 frames, VICTORY 40/3.

Pack brawls together (owner order 2026-11-24, 8Zeal_v_LingUp): one
fighting blade releases the dress for the whole melee line - firing,
engaged upon, hittable, inside an enemy gun, or enemy inside 2 tiles of
any mate; release only once every blade stands clear (2.0/3.5 band).
Blades only: ranged lines hold their arc while individuals kite.
Live: 22 Dress before contact, ZERO after (was: 3 dressed while 5
brawled); 13/8 -> 17/8.

Hunt is blind mop-up now (owner order 2026-11-24, 8Zeal_v_LingUp
11/1 timeout at 4820 frames, visible=0 from f456): deleting Hunt
paralyzed the endgame - memories expired, nobody walked, both sides
milled to the clock. Restored with a blind gate (zero visible enemy
warriors): any live contact owns the frame through duel+formation,
memories only walk when nothing live is in sight. Live: 3 mop-up acts
in blind windows, zero hunts beside visible enemies.
BeRushingZealot gated to ladder (2026-11-24, same map): the
rush-response brain has no natural to be past on baseless UMS - it
forceTo'd lone zealots at the enemy main from f0 ahead of the
technique (the legacy-starves-pipeline shape, third instance).
`allowed` takes `! umsMode` now. Live: 151 rejoins the pack instead of
marching alone, first brawl f265 (pack together), VICTORY 18/7 (was
three straight defeats).
Formation receives as a line (owner order 2026-11-24, same map -
"follow the slot"): the line dissolved in the first seconds for a
3-ling screen (audit: 6 breaks at 4.6-12 tiles, f4-f26) and the
slotless vanguard charged solo from f0. Four gates, one doctrine -
melee breaks the line only for the pack fight, never for sight alone
((c) arrival); a 3:1 skim breaks only at contact range, workers-only
still break anywhere ((f) prey band); a slotless warrior holds while
its formed pack stands calm instead of walking out alone ((a)
slotless hold); pursuit waits until every warrior holds a slot and
never chases a pack-pressing enemy ((pursuit) unplaced + centroid
gates). Live: f0 Dress/Move only (zero f0 charges), breaks only at
contact or pack fight, VICTORY 18/2 with first brawl f445 as a pack.
Watch: vanguard still pursues retreating screens past 12 (f4 breaks at
13-18, won this time) - needs a game lost on it.
Pending ladder confirmation (rule 21): melee (c) arrival, prey band,
slotless hold, blind mop-up (ladder never hunted, gate keeps it so).

# Kite discipline (owner direction: never yield ground for free)

Kiting must not leave shooting range when it can fight from it -
especially against ranged guns, where the yielded ground buys nothing.
Refactor the kite so a loaded-enough hull holds its volleys inside its
own reach and only yields what the cooldown forces. Exceptions allowed:
near-dead hulls (shields <= 5 and falling - be very careful, kite
early and far). The wasted-frames ledger (shots forgone with a hittable
enemy in range) judges the refactor: it must fall, kills must not.

# Distance hygiene (owner direction: tiles everywhere)

Convert `.pixelDistance` to tiles where possible
(`tileDistanceEdge/Center`, `Pixel.tx/ty`, `Maff.div32`). Exceptions:
pixel construction, mixed range math, hot sim loops. Count with
`grep -rn "\.pixelDistance" src --include=*.scala | wc -l`; keep
suites green, no behavior change.

# Targeting parity with Atlantis

Pin pure kernels beside the caller with a `*Suite`. Game-coupled
strata stay thin and delegate. `testkit.TestKit` covers geometry;
matchups/battles stay game-only - next seam on demand. Atlantis
ports (`TargetLadderTest`, `AcquireTargetTest`, `FocusFireTest`, ...)
need per-stratum MatchupSnapshot views first.

# Open ends (each needs a game to prove)

- Creep Colony: morphing colony over idle; fighters still outrank
  colonies; Reavers skip non-combat buildings. Neither combat state
  may idle past a standing colony.
- 4th/5th base progression: watch (shifted base may have fixed it).
- Spot retreat: catch-all today, needs one game per case
  (observer/detector/caster).
- Carrier stop-hold micro (SH doubles DPS; links in BeCarrier) and
  the 24f post-attack wait: need a Carrier map + visualizations.

# Code health

- `docs/longest-files.md`: cut cheapest first (`WallFinder`,
  `SimulationGrid`, painters); controllers need a game per cut.
- `Tactic.Squads.SquadAutomation` is a forwarder shell over
  `SquadTargeting`/`SquadForming`/`MarchOrders`: migrate callers to
  the halves and delete it. New code calls the halves, never the shell.
