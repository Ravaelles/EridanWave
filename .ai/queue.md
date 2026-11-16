# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.

Conventions: docs stay English; `.ai/conventions.md` (tests +
blank-line gate per change, commit per unit).

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
  needs per-frame latch-input probe in a hunter game).
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
- Crucial air victims first (grounded AA shoots Scourge/Carrier/
  Vessel/Battlecruiser/Wraith in range).
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
- Larva/Egg exclusion (SquadAttack lists carry no Larva/Egg; needs a Zerg
  game: Target probe never picks them, no Engage/Approach on chaff).
- Slow-alone watch (log stutter with no units nearby, max 10/game).

## Open: gameplan/economy

- ZealotIntoGoon unforced (TEMP flag removed; it competes as one branch
  among GateCore/reactives by match history - needs PvZ games, Monster
  included, to confirm dragoon-first branches get picked).
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

## Session: UMS Reaver freeze (validating now)

- DragC hunt step (target-less duelists attack-move toward the nearest
  known foe; hold-cap + pursuit latch already proven - do NOT
  re-litigate). Mechanism pinned by Path + Engage probes (since
  reverted): paths exist, no wall - the line holds vs an invisible
  remembered ghost 6-8 tiles out (hold=true, never approaches, never
  fires) while toAttack sits pinned on the far pylon, then dresses
  against the ghost and attacks the pin with a stale perch (Stop/
  AttackUnit flap, zero shots). Hunt now covers useless targets too:
  far pins past the pursuit horizon and ghosts past sight + 1 tile
  (sight-edge flicker keeps the line). Hunt remembers: mobiles three
  minutes (squad HuntContact parity), seen-once buildings until they
  die - a fogged pin no longer strands the line on an empty list. Hook
  rides two paths: engage (Fight-technique target-less) and consensus
  walk (no battle parks the ignorant technique in formation slots -
  audit showed Hunt 1172 + Abuse 560 frames then empty chains,
  +Committed, Contain-on-pylon while the line sits 700px short).
  Validating on the hunt jar.
- Re-run remaining Reaver UMS maps with pauses between games (Wine
  box lock aborts concurrent launches): Sunkens3/4, Reaver_v_Sunkens.
- Owner probe (intend-client changes ride `Probe Owner`, traces or not -
  UMS manager tracking per unit). Pair batch running on it: Shuttle
  must flee properly, Reaver must attack and fight most frames.
- LoneMarch flap re-run (Reav&Shut; confirm the +-300px terminus flap
  is gone on the latched jar).

## Session: PairArbiter (one decider per pair, 2026-11-16)

- Kernel (`PairArbiter.decide`) + game-free bond resolution over
  plain views (`resolveShuttle`/`resolveReaver`) + `PairArbiterSuite`
  (decide rows, resolve rows, WalkToArmy regression, landing band).
  Same resolve path runs in production and in StarEngine pilots.
- `PairOrdersSuite` (fetch-the-fighting, pressured flee) +
  `PairLifecycleSuite` (12 Sunkens3 stages: bond, ferry, unload,
  shoot, fight, reboard, evacuate, fetch, walk, alone, retarget,
  full run). Pair UMS iteration replaced by these; live games stay
  for what the engine lacks (squad march, fog).
- Live reads: Sunkens 11/6 (Bait fired on first contact), Sunkens3
  2/1 (LoneMarch terminus still flaps +-300px to f4810, army never
  arrives - squad level, not pair; needs its own repro).
- Wired: `BeShuttle`/`BeReaver` execute arbiter orders (incl.
  UnloadHere through the existing landing gates); `landingReady`
  moved to the arbiter so order and executor share one read.
