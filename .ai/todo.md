# HIGH PRIORITY: order-issuers review (SRP, one question per unit) - audit done 2026-11-24, fixes in progress on f/phase-j

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

Audit 2026-11-24 (three subagents, evidence with file:line in session):
 worst are Combat.scala (orchestrates target+verdicts+technique+dispatch,
 writes toAttack/perch/technique), CombatTechniques (ticks foreign
 verdict buses, decision.set(safety)), CombatEngagement (direct
 forceTo/move, perch.clear, breakFormationToAttack writes),
 KiteAwayBehavior (6 sub-duels, 3 order channels),
 ApproachBuildingFormation (nests HoldLineAdvance - documented as
 delegation, one order per frame by construction),
 BeDarkTemplar (7 behaviors, toAttack+move per leg),
 BeCarrier (verdict+destination+7-way order switch),
 CommanderEngage/CommanderMove (kitchen-sink movers),
 Target.choose (15 doctrines, writes dispatch-owned toAttack).
Done: leash/rejoin extracted to Behavior objects (a1d411612);
 decision-writers pinned (2 own lines; DT gun.pixel line left
 unlisted for owner review). Deferred as structural (needs owner):
 Combat.scala split, BeDarkTemplar/BeCarrier breakdowns,
 Commander* sink slimming, Target ladder ownership, Bust double
 toAttack write, Retreat.toAttack=None, perch.clear ownership,
 march-tick removal (load-bearing: marching-only units need a vote).
Done 2026-11-24: Retreating-label token (compiler-enforced, single
 construction site); incidental fix - the L1 rivalry tripwire compared
 a motion label against a march label and never fired.

# 1Drag_v_1Zeal kite duel (owner bar: max 1-3 hits, wins on both maps) - LANDED, bar UNMET on variance

Dragoon kites a lone Zealot from first sight (chase 7 tiles), never plants
or stands in contact (edge-only volleys when loaded), holds committed
6-tile strides, steers back toward friendly warriors past 6 tiles
(leashed, aligned, edge-capped), never flees across the map. All new
rules are guns-nearby-gated (melee within chase range counts as guns);
the mirror runs the old discipline untouched.
Formation (f/phase-j): FormationDuelStage before Close, lone duelist
duels from f0 (isLone decline), pack dresses (slot capped 2 tiles,
explicit HoldPosition, flee-only chase, seen enemy inside 15 (Terran
siege) / 12 (else) + not overwhelming; farther out the march owns
the walk), creep 0.15/frame for dressed line (melee too). Formation
freezes pre-contact verified per-frame (pos+order trace f0+: holds,
no overrider; f0-3 ghost twitch accepted per 6-B). Zealot no-kite
gate ADDED then REVERTED same night (mechanism: only cooling zealots
ever kite, and standing cooling zealots deal zero damage while
absorbing full contact - zealots died twice as fast with no gain).
Contact-kite bypass: dragoon + melee<=2 tiles + cd>=15 always kites
(1v1: 0 hits). ShouldChase ghost-gate proposed + reverted (3/9
correlation, mechanism unproven - f0 drift accepted as harmless).

Proven: StarEngine DragoonZealotKiteSuite - 0 hits, kill in 223f.
UMS mirror: VICTORIES 9/7 9/7 9/4 9/3 9/0 8/5 (one coin-flip 5/9 on
identical code), waste ~0-4f, no thrash, no silent-idle (Close
declines instead of silent-claiming; Reposition always signs).
UMS 1v1: VICTORIES 1/0 x7 (hits 0/2/4/5/6/8/13/18) plus 0/0 draws
and-teardown losses on identical code (23-hit draw, 0/1 routs).
Bar met 2/9. High run-to-run variance on identical code - glue
fights (v oscillates 0-8, ~21% full stops) decide the duel, not the
decision (kite acts every cooling frame; UMS shows KiteAway chains
throughout). Stuck-gun shape fires live (zealot 8f, dragoon 4-5f
per game) - the old Move-only ledger was blind to it.

Ledger upgraded 2026-11-24: waste counts stuck guns too (loaded +
near + standing + silent past a full period), judged by gun events
not orders. Split by shape 2026-11-24: 1v1 waste is move-past
(100-220f/duel, the priced-in cost of never-trading-contact), stuck
~0 (holds are clean). Mirror: dragoon waste ~0-6f, all stuck.

Formation (owner orders 2026-11-24, verified live): FormationDuelStage
before Close; outranged dragoons dress and HOLD (explicit
HoldPosition, verified f0+ per-frame pos+order trace - stillness
holds, no overrider found). Slot seek capped at 2 tiles, leash-gated
at 6, fleeing-only chase, lone blades duel, charge gated on
hittable-or-visible. Abuse votes only with hittable (no ghost
approach). 3-A implemented: seen enemy inside 15 tiles (Terran
siege) / 12 (else) + not overwhelmingly stronger, else march owns
the walk. Ghost-chase gate PROPOSED AND REVERTED same night (3/9
wipe on it; f0-2 ghost drift accepted as engine noise, Hunts per
6-B). Mirror 9/7 9/3 9/2 9/6 8/9 7/9 5/9 3/9 on identical code -
coin-flip variance, not discipline. 1v1: 7W + draws, hits 0-23.

Dance experiment + verdict 2026-11-24: original PurpleWave dance
math pinned as DragoonVsSlowlot kernels (suite-green); wiring it as
the slowlot leg LOST 4 straight 1v1 (0/1, 25-31 hits, 263-858f
move-waste, 0 kills) - reverted same night. Root cause: our
Commander dedups attack reissues (Stardust one-order) and skips
stationary holds, so the raw-order rhythm the dance was tuned for
never fires; kiteAway retreat glues in contact instead of opening
the gap. Judgment: keep our kite (proven wins), keep the kernels
as the specified matchup home; re-attempt only with per-frame
gun-event proof + threat-aware retreat. The fast 23s wins belong
to the whole old bot (raw orders), not portable alone. Trade-contact
rule added instead (plant loaded vs unoutrunnable pressers):
Drag_v_LingsUpg went 0 kills -> 21 kills (still DEFEAT, speedlings
trade up). Creep 0.15 restored per owner (melee dresses too).
8Zeal: Formation/Dress 1496 acts vs Brawl 462 (line dresses, then
brawls; 10/8 first measure).

Committed: lone-blade kernels, outer gate, HitsTaken ledger,
kite-gate (cooling kites contact), close-no-silent-claim,
designation 0.15, formation stage + ApproachBuildingFormation,
Abuse-hittable gate, loneBladeDuel kernel, slot-seek/hold/leash,
seen-range gate, waste split, leash/rejoin extraction, contact-kite
bypass (dragoon + melee <=2 tiles + cd>=15 = always kite).

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
