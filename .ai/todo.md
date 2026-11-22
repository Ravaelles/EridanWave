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
