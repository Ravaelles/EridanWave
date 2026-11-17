# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.

Conventions: docs stay English; `.ai/conventions.md` (tests +
blank-line gate per change, commit per unit).

## NEXT UP (highest priority, agreed 2026-11-17)

Unified retreat verdict (`RetreatVerdict`) - one verdict object per
unit per frame with hysteresis, read by the march arm, the technique
selection and the duel dispatch gates instead of re-deriving retreat
from stale/fresh/faction-split inputs in each place. Full write-up,
evidence (144 -> 8 -> 3 mixed frames per game, duplicates at 0),
rejected alternatives (god-object, capability types) and migration
contract live in `.ai/retreat-verdict.md` - this entry is only the
pointer. Steps, each a green unit on its own:

1. Shadow: compute the verdict alongside the current code, probe only
   the disagreements vs today's decisions (same pattern as the old
   shadow posture). No behavior change. Run a game, read the probe.
   (LANDED c44df2aa8: `RetreatVerdict.shouldHold` + `RetreatVerdictSuite`
   + `RetreatShadow` probe in `syncConsensus`; validating in Benzene PvP.)
2. Switch readers one by one behind the shadow data: march retreat
   arm, then `selectBaseTechnique` march arm, then the engage
   `advance` gate, then Abuse/dispatch gates. Each step its own
   commit, each revertable; full suite + one validation game each.
3. Delete the old predicates only after step 2 holds a full game
   with zero mixed frames (`scripts/retreat-audit.py`).

Open question the shadow data must answer: is a 24f unit-level hold
enough, or take the 96-frame cluster floor for the verdict too.

## Parked (explicit orders, do not touch)

- Sunkens2 baseless mission fallback (pair stalls after the first pack
  with a live ride nearby; needs a mission ferrying the pair at known
  enemies instead of micro-gating the handshake).
- Refresh hop 30f drop-cooldown reset (engine timing no suite can prove).
- Contain carve-out + mission-ordered unload (mission layer stays thin
  per architecture S4).
- C6 march-leg profiling (needs a live macro game; timers were in and
  out uncommitted).
- krasi0 bot config for SCBW.Play (`/sc-ai/BOTS/krasi0/` staged; needs
  one smoke on a free Wine slot; SaaS needs internet, LF 3).

## Open: Micro/Actions concept merges (from the edge-case review)

Single concept, one home - merge when touched (behavior-preserving,
suite-carried; risky ones need a game to prove). Assessed 2026-11-14:
Yamato/Lockdown pricing, mineral-clearance, FewShot stub and
`GetRepairedBuilding.perform` merged; Reaver boarding smuggle,
`KiteAwayBehavior.staticStandoff`, `ApproachBehavior.stepBack` stay
separate (distinct concepts). NEXT needs a game: kite-vs-approach
range axis; standoff-margin table (Corsair/Cluster); dress-the-line
governor; fight-forcer thresholds; cloak on/off policy;
Yamato/Lockdown-style value sharing for Maelstrom/Storm/Disruption/
Stasis; scout-harass soup to one table; worker-fight backup to one gate.

## Open: storm validation (needs a Templar game; rushes end too fast)

- Consensus walk (casters re-arm on fightConsensus).
- Overlord veto (lone Overlords need 190 energy).
- Lead moving victims (2.0-tile heading lead).
- Hunt inside cast range (8.9-tile search, no leash walk).
- Gap: HT run-downs (25/24/11 dead in storm games; casters march into
  contact - caster-standoff concept if it repeats).
- Gap: lurker wall (2064 storms killed 0/18 lurkers; need cast
  positions vs lurker sightings, detection upstream suspected).

## Open: air/observer validation

- Corsair 14-tile flee trigger (shared AirFlee stays 12).
- Shooting Corsairs hold the spread mid-duel.
- CorsairHunt march gate (macro corsairs vs Overlords).
- Observer standoff latch inconclusive (eye still flips 2079x with
  run-length-1 alternation; suspect detector-set/visibility flicker -
  StandOff probe landed (det id/dist/visibility/latch verdict per
  frame, BeDetector.keepOffDetectors): run a hunter game and read the
  flicker off the log).
- Observer hunter early warning macro confirmation (muta pressure).
- Observers stand off detectors (6 tiles, pack-spread ray).
- Arbiters flee like Corsairs (wound-scaled 2-to-11 margin).
- Arbiter spread cloak coverage (one Arbiter game; senior-arbiter
  attraction removed per review C2).

## Open: march validation (needs macro games)

- Toothless march (PvT columns walk with no tanks/vultures).
- StrongMarch (banked stock or maxed army skips rally/slots).
- Retreating warriors never rally or close ranks.
- Engage never overwrites a march home (frame-fresh intention flag).
- MarchDressSuite (StarEngine): scattered 4 dragoons + 4 zealots march dressed at an enemy 30 tiles out, modes March>Dress>Engage with no flap, reissues 14, pack tightens - green.
- PressedRideSuite (StarEngine): hydras press the line while scourge dive the ride - shuttle flees on the shipped penetration/latch/guard rule (1 clean latch cycle, 3 redirects), holds 15-tile corners, never takes a hit; reaver never leaves its anchor; pack dies - green.
- Expo punish vs Zerg at 140 (units-strength only).
- TankHug (cooling warriors close sieged tanks to 0.2 tiles).
- Sealed-breach wall chew (quiet 3s + sealed + 6-tile lane and reach, no
  shield term): needs a live PvT wall-in, zealots and dragoons both read
  it via BreakTerranWallIn.
- StaticDefense overwhelm (12 of ours push one static).
- Crowded leaders skip the dress wait (6 friendlies in 4 tiles).
- Lurker/DT avoid radii (8/4 tiles, above every formation and duel).
- Hunt-destination stabilization (UMS Reav&Shut 2026-11-17: squad
  vicinity alternates between hunt points every ~10 frames; the latched
  lone-march boundary is innocent - the *input* flaps. Reaver duels
  through it; ClusterCohesion rejoin runs before Hunt).
- HoldLine creep (from _COMBAT-REFACTOR.md, after the posture rewire):
  while attacking, hold formation and close the distance gradually (~0.1
  tile steps) instead of breaking or standing; needs per-unit hold
  positions advancing with the enemy + suite first.

## Open: posture-era validation (needs games that reach each rule)

New kernels ride suites; these need a live game that reaches them
(scout the named behavior in trace/probe/errors.json, delete the line):

- Scout static keep-off (ScoutStandoffScenarioSuite green; needs our
  Scouts vs enemy AA: hold outside reach+margin, shoot from the hold,
  never pose inside).
- Dragoon carrier hunt (kernel + suite green; needs Dragoons 20+ shields
  vs a Carrier: always attacks in range, closes a pathed one inside 11
  tiles or its halfway point).
- ForceGlobalAttack (kernel + suite green; needs our DT in PvP/PvT
  without enemy Observer/Vessel: global attack fires regardless, covered
  statics hold without advantage).
- CanProduceShuttle veto (kernel + suite green; proven holding by absence
  in early PvP - still wants an observer game: ShuttleVeto board must
  fire while production holds the ferry).
- Early mirror pressure penalty (-20% below 7 combat units vs Dragoons;
  needs an early PvP: DragoonMirrorSuite covers 3v5/4v2, live must show
  the break at the penalized line).
- FleeToMate (order proven in engine; needs a pressured pair with the
  mate on foot inside 15 tiles: ride converges instead of fleeing away).
- Raid status deadband (RaidStatusSuite pins no-flap inside 0.5-0.65;
  needs a raid-capable game: oldest pair, wounds crossing the band,
  one RaidCarry->FollowArmy transition, no flicker).
- Rearguard cover (needs a Withdraw with the table naming a rearguard:
  cover shoots while the line leaves, then falls through to Retreat).
- March-home bypass (Retreating label must own every retreat frame;
  no Waiting/Rallying/Marching-to-slot while the verdict says flee -
  watch the f16368 pattern stay dead).
- Roam suspension (needs an unseen PvP mirror: DragoonRoam march labels
  with zero Abuse acts).
- DefenseWhy board (needs any defense staffing: reason + triggering
  enemies by class/count + army total on the board).
- ClusterRejoin (0 acts so far across grinds: needs a drifter >14 tiles
  from its fighting cluster with no local contact).
- ZealotLeash (validating live: 918-1050 acts across PvP grinds; still
  wants one clean lone-zealot-vs-3-goons case).
- WorkerSpread (validating live: 65-773 dealt picks; wants one worker-
  harass game with the spread visible per unit).

## Open: micro validation (needs games that reach it)

- Spot retreat logic (sloppy catch-all today; doesn't handle the observer/
  detector/caster cases separately - needs a game per case).
- Carrier stop-hold micro (SH doubles Carrier DPS; needs a Carrier test
  map + visualizations first - links live in BeCarrier).
- Carrier post-attack answer (24f placeholder; needs a better answer
  than the flat wait).
- DT hunt bravery concept NOT BUILT (dither fixed, but 6 DTs died
  1-7 HP unconnected hunting alone - no duel without local edge).
- Dying Templars suicide-storm the median pack.
- ZealotSwarm pure case (warriors-only zealot-vs-ling +10% margin).
- ReaverBraveStand (stale shielded hull holds vs Terran in 8.5 tiles).
- Reaver vs workers (pressure, shields >70, no Hop).
- Scarab rescue (pressured mid-shot boards while the shot flies).
- No Reaver raids vs Zerg (pair supports the main army).
- Always-attack ladder (renamed from CrucialAirSnipe; grounded AA shoots
  Scourge/Carrier/Vessel/Wraith/BC in range, plus detected Observer,
  Reaver, DT, HT in that order - needs live confirms of the four new
  victims taking fire first).
- DT flees guns first (visible gun in 13 tiles outranks the detector).
- Workers run straight from visible enemy DTs (3.0 tiles, no pathing).
- Shuttle pair owns loaded hulls (no other pickups, no drop missions).
- Shuttle fetch glue diagnosed end to end (Reav&Shut_v_Sunkens): a TEMP
  Flow probe proved BeShuttle runs every 2nd frame but support never
  fires - adopt() returns true forever fetching a fighting reaver that
  never boards (stale reservation + live hail), starving escort/await
  while the mate dies 45px away. Fix: fetch only non-fighting Reavers
  (explicit hailers still fetch); first game with the fix shows
  EscortRide/AwaitRide from f178 and AirFlee under pressure - but they
  flap every 2nd frame (new dither: latch the escort/await choice).
- Shuttle await dither candidate (Returning/AwaitRide 2619x flips).
- Drop lifted targets (tasked building takes off, slot releases).
- StaticDefenseRepel shoots (weakest-first, retreat still outranks).
- Rich bank skips kiting (minerals >= 1000, expendables hold DPS).
- Dragoon closes fleeing workers (StarEngine ChaseAndKillSuite green;
  needs one live confirm).
- Vulture duels (PotshotVulture rows).
- Carrier berth (scratched shields vs ranged, CarrierKite rows).
- Screened static +50% (statics covered by 2+ defenders).
- Slow-alone watch (log stutter with no units nearby, max 10/game).

## Open: gameplan/economy

- ZealotIntoGoon unforced (TEMP flag removed; it competes as one branch
  among GateCore/reactives by match history - needs PvZ games, Monster
  included, to confirm dragoon-first branches get picked).
- ZealotIntoGoon early tech (deliberate, 2026-11-16): gas + Core ordered
  once the line stands (was: ten-warrior generic floor, flooding to ~18
  Zealots first) - needs PvZ games to confirm first Dragoon lands
  around a dozen hulls without breaking the ling hold.
- Crona 4-pool overrun (lings arrive before the first Zealot; early-pool
  scout reaction needs gameplan review).
- March-leg death tails (returning/marching units die mid-leg; probes
  lost late in grind losses).
- Production observer jam concept NOT BUILT (93 trainings, 91 died
  ~9f later under fire, jamming shuttle/reaver production; gate
  retraining when trainings die instantly).

## Open: viewer check

- CherryVis Task tab contract verified against the shipped viewer JS
  (`cvis_dbg_tasks.js` + unitslist: registry {owner,name,upc_id,
  creation_frame}, numeric per-unit `task` carried forward, None when
  missing). Task now means the MANAGING class, not the reason: every
  `intend()` mints/points the per-unit row at the manager task, reasons
  stay in units_logs. Proven in a traced Benzene VICTORY: 9 manager
  tasks (SquadAttack crew 36, Gathering 31), 78/78 managed units with
  rows, zero dangling refs (viewer logic replicated in python). The 13
  row-less units are unmanaged single-frame ephemera (no first_seen,
  viewer can't name them anyway) - None is correct there. UMS emits no
  traces by design; Owner/Action probes cover manager visibility there.

## Session: warnings + DT + rally + storm + ferry (2026-11-16, open)

- Warnings triage: fix `SimulationCheckpoint` hidden import (alias),
  `UmsMode`/`SpinWait` discarded returns, `IndexedSet` erasure
  (exists instead of cast), `BeReaver` non-exhaustive match
  (UnloadHere arm), `Polygons` Stack->List. LEFT AS-IS: pom
  `classpath` deprecation (offline build risk, revisit with vendor
  refresh), BWTA `readMap` deprecation (vendored lib), and the
  pre-existing launcher/IDE warnings the suite never sees.
- DT prey: `DarkTemplarTargets.isPrey` (combat buildings, combat
  units, bases - no masonry/workers) wired into hunt + remembered
  contacts; tier list dropped the worker tier. Needs: live PvT
  confirm (no depots chewed next to tanks).
- DT mine sidestep: `SpiderMineSniping.sidestepPixel` (lateral,
  fixed side per unit) + `BeDarkTemplar` leg after snipe; mines
  out of the detector evade. Needs: live minefield confirm.
- Rally latch: `SquadRally.isRallyingLatched` wired in
  `MarchOrders.rallyLeg` (+ base rows incl. hold-through-flap).
  StarEngine leg-level repro pending (march machinery is out of
  the engine); live mop-up confirm still wanted (Hao Pan f15721
  pattern).
- Storm stand: `StormStand.shouldHold` wired in
  `CombatTechniques`; `StormStandSuite` proves the cast under the
  retreat verdict. Needs: live PvZ storm confirm.
- DT ferry: `MissionDarkTemplarFerry` (pair straight to enemy main,
  ASAP drop, single trip) registered in Tactician. Needs: live
  game with DT+Shuttle (formation + drop + raid).
- Carrier AA ladder: `CarrierTargets` + BeCarrier pre-pick;
  `CarrierDoctrineSuite` proves turret-first. Needs: live PvT
  confirm (unconditional tank attack).
- OPEN question (owner, asked 2x): Carrier vs sieged tank with no
  other AA - "upewnijmy się ..." sentence cuts off. Implemented
  as unconditional attack via the ladder; confirm or restate.
- OPEN investigation: NoTankMarch-vs-Retreat flap (Hao Pan
  f6666) - march terminus vs combat retreat writers; unresolved,
  needs the exact game or a squad-level repro the engine lacks.

## Deferred: unified retreat verdict (no rewrite now)

- Full write-up lives in `.ai/retreat-verdict.md` (this entry is only
  the pointer). Problem: the retreat verdict is re-derived per phase
  (march reads stale `shouldFight`, combat reads fresh verdict plus
  posture, technique latches its own) so mixed march/retreat frames
  keep recurring per pair of readers - audited 144 -> 8 -> 7 mixed
  frames per game as each pair got its own gate. Proposal: one
  `RetreatVerdict` per unit per frame with hysteresis, read by the
  march arm, the technique arm and the duel dispatch gates; migrate
  behind a shadow probe like the posture rewire. Rejected: a
  god-object decider (kills the squad/unit split and OCP) and
  capability-typed permits (ceremony over readable ifs).
  Owner decision 2026-11-17: park it, keep per-site fixes coming.
