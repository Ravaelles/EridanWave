# HIGH PRIORITY: order-issuers review (SRP, one question per unit)

Review every class that issues orders to combat units
(Commander*/Combat*/Behaviors/*Behavior, Be* brains, march legs):
each answers exactly ONE question asked BY the unit (which posture,
do slots stay, where is the hysteresis - rule 9), owns exactly one
verdict (rule 11), and never sets another owner's state (no
cross-writes to marchHome/technique/verdict/labels). Where a class
does two things or answers two questions, split it: separate the
requirements, separate the responsibilities, keep the old behavior
behind the split. Enforcement where cheap (token/sealed paths like
the Retreating label); audit trail in commit messages otherwise.
Trigger: Close/Guard silent claims, holder-vs-mover thrash, split
march-vs-technique retreats - all three were multi-writer symptoms.

# 1Drag_v_1Zeal kite duel (IN PROGRESS - uncommitted, owner bar: max 1-3 hits, wins on both maps)

Dragoon kites a lone Zealot from first sight (chase 7 tiles), never plants
or stands in contact (edge-only volleys when loaded), holds committed
6-tile strides, steers back toward friendly warriors past 6 tiles
(leashed, aligned, edge-capped), never flees across the map. All new
rules are guns-nearby-gated (melee within chase range counts as guns);
the mirror runs the old discipline untouched.

Proven: StarEngine DragoonZealotKiteSuite (new, green) - 0 hits, zealot
dead in 223f, min gap 2.4 tiles, leash bounds separation (6.1 vs 26.0
tiles free). UMS ZealDrag_v_ZealDrag: clean wins (9/4, 9/0, 9/2),
no waste. UMS 1Drag_v_1Zeal: wins most games, never draws/stalemates
anymore.

NOT met yet: UMS hits 2-25 per duel (budget 1-3), waste 45-230f, high
run-to-run variance (wins, 0/0 draws, losses on identical code).

Root cause, measured not guessed (temporary KTRACE probe, remove before
commit): the dragoon kites every frame but its effective speed collapses
in contact (v oscillates 0-8, mean ~4 vs 5.0 max, ~21% full stops,
median gap 0-2 tiles, min 0.0). The zealot matches it and the gap never
opens. Ruled out: order reissues (Commander dedups within 128px),
Unstick stops (resetSticking on every kite frame), Reposition/dance
interference (outer gate forces Close on chase; dance stands down),
contact plants (edge-only for lone duels now).
Remaining suspects, in order: (1) per-frame heading recompute turns
the hull forever in contact - just added per-unit heading hold
(KiteAway.steadyHoldFrames=8, 45-degree cone, gunsOut-only); 2 measured
runs still glued (mean v 4.1, 21% stopped, minD 0). (2) shove-fights
with the glued zealot. (3) walls/corners (deaths at map edges, e.g.
tile (10,48)).
Next: verify the heading hold actually reduces turning (needs heading
trace, not just speed), else escalate to wall-aware kite direction.

Uncommitted in tree (do NOT land as-is): KTRACE Probe.note instrumentation
in KiteAwayBehavior (measurement only - REMOVE before commit);
KiteAway.scala + KiteAwayBehavior.scala + BeDragoonVsSlowlot.scala +
CombatEngagement.scala (chase/leash/escape/commit/steady/outer-gate),
KiteAwaySuite.scala, DragoonZealotKiteSuite.scala, HitsTaken.scala +
HitsTakenSuite.scala + EndOfGame hook (UMS hits ledger - the acceptance
metric; keep).

Watch: RetreatVerdictHoldSuite.skewed-readers fails deterministically
(flips=1, needs >60) on the clean tree too (stash-proven) - pre-existing,
not ours, do not fix here (someone else's pinned contract).

Pending ladder confirmation (rule 21): UmsHold paced advance, CatchUp
full-speed join, lone-blade kite, UMS-restricted Beast/Dragon maps.

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
ports (`TargetLadderTest`, `AcquireTargetTest`, `FocusFireTest`,
...) need per-stratum MatchupSnapshot views first.

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
