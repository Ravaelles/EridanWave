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

# Formation advance (owner order 2026-11-24: 0.25 tiles every 10 frames)

The dressed line inches toward the nearest known warrior on a metronome
(`ApproachBuildingFormation`: all warriors step, staggered by id; duel
takes over on hittable/contact so steps never walk a gun into blades).
History: 20f/0.25 all-hulls -> 15f/0.3 melee-only (froze ranged mirrors:
dragoon lines held forever facing each other) -> 10f/0.25 all warriors.
Live validation pending (needs a UMS mirror: line must dress, then
shrink the gap instead of parking).

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
