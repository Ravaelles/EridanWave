# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.

Conventions: docs stay English; `.ai/conventions.md` (tests +
blank-line gate per change, commit per unit).

## Done 2026-11-19 night (validate on next games)

- Kite A/B (7 Crona games, ON 2-1 vs OFF 1-2): kite fired in exactly
  one game (ON#2, 6 acts, one dragoon) - too rare to move results in
  these comps. Mechanism confirmed separately (33x suicide zealots,
  6x dragoon here); OFF arm correctly shows zero kite acts (kill-switch
  works). Verdict: keep the behavior, do not re-run the A/B without
  frozen code. Confound note: working tree evolved mid-series (hunt
  assign, AlwaysAttack split, coordination, standoff), so ON#1 ran
  older code than the rest - mechanism analysis only, no causal claims.
- Tank hold (dragoons never close sieged/unsieged tanks; hug keeps its
  point-blank exception): needs a live PvT with tanks walking at dragoons.
- Contain-expand (natural while containing if a safe path exists):
  needs a live containing PvT (Terminus game running).
- Static standoff 9.1 + holdsLine hook: needs a live HoldLine game vs
  statics. Terminus running may cover it.
- Pack regroup in-contact hold + warrior anchor + mayFight HP>20:
  needs a live early-PvZ defense (the E_10:27 pattern).
- Sunken break (Robo at 3, Bay+2 Reavers at 4): needs a live PvZ with
  a 3-4 sunken wall. Cancel guarded while static >= 3.
- Builder shift (re-seat town hall up to 5 tiles on unseen blockers):
  needs a live blocked-base game; inProgressNearby guard unproven live.
- Duel-owns-hold (march legs yield while the duel has a target or the
  unit is engaged): needs any live fight - fixes the per-frame
  Approach/Rallying flap on dragoons and the PackRegroup yo-yo.

## Done 2026-11-19 midday (validate live)

- Overwhelming-line latch release: a held personal retreat drops
  immediately when the unit's own squad votes Fight at >=1.5 skim -
  ten hulls stop routing off four. Needs the E_10:27-style grind
  (posture Fight + outnumbered duel + retreat flap).
- EnemyWallInDetected: zealots chew the depot nearest the cluster
  (hp>=31, yield on tanks+shields<=10), dragoons hold outside 8 tiles
  instead of marching in. Needs a live PvT wall-in.
- PvP reaver gate: no Bay/Reavers below 15 dragoons without a scouted
  enemy Cannon; gas stays on gates and goons. Needs a live PvP.
- AlwaysAttack picks always, shoots only when the duel will not run
  (retreating or no advance): the kite starvation fix. Needs any live
  fight with an air victim in margin.

## Done 2026-11-19 DT defense (validate live)

- Worker Templar flee owns the frame: a visible blade within 3 tiles
  moves a worker straight out ten (release at 4, per-worker latch)
  through WorkerDarkTemplarFlee, never through Retreat - the Retreat DT
  branch is deleted. Needs a live DT-at-the-line game confirming
  WorkerDarkTemplarFlee acts.
- Live on this build: Terminus DEFEAT 45/135 with TankHold 215x +
  HugTank 41x (first live TankHold fires, errors absent; no wall-in on
  TauCross so WallStand/EnemyWallIn correctly 0); Monster DEFEAT 90/125
  with PackRegroup 1474x (errors absent). The overwhelming-line release
  rode along - still needs the E_10:27-style grind to confirm the
  394-skim flap is gone.
- Senior observer sticks to the blade: the first eye follows the enemy
  Dark Templar nearest our main inside 50 ground tiles (release 56),
  juniors keep their duties, survival still outranks. Needs a live game
  with enemy DT plus observer confirming ObserverTemplarFollow acts.
- Small armies answer the blade: below 30 warriors the main-base hunt
  reaches 50 ground tiles instead of 40; large armies hold the old line.
  Needs a live DT defense confirming the extended walk.
- Fogged blades are not targets: rememberedNear never recalls an
  invisible Dark Templar (stale HP), other recalls untouched. Needs a
  Goon_v_DT re-run confirming the ladder stays quiet without detection.

## Done 2026-11-19 reaver static break (validate live)

- Static hold-to-fire vs Sunkens/Cannons (Atlantis band, 8-frame
  cadence): inside 7.25 tiles with scarabs and the longer gun the hull
  holds every eighth frame and re-attacks between holds (release 7.75,
  per-hull latch); outranged guns keep the back-off. Needs a live
  Reav&Shut_v_Sunkens2 run confirming StaticHoldFire acts and a scarab
  launches.
- Designated pair drop (baseless duels): the loaded ride holds a ring
  spot 8.5 out from the nearest static (never inside 7.5, 4-tile mobile
  margin, nearest our side) instead of flying at the static and
  circling; the unload fires on arrival. Needs the same Sunkens2 run
  confirming DuelDropSpot acts and the ride never parks over the colony.
- Fresh hulls stand under statics (user gate): shieldWound <= 40 with
  scarabs and 6s quiet skips the keep-off floor - walk in, shoot,
  withdraw after, not before. Needs the same Sunkens2 run confirming
  the hull fires instead of stepping out.
- Baseless pairs bond blind (Sunkens2 diagnosis): the lenient squad
  gate bonds the nearest hull regardless of staffing - the 13:24 run
  watched the ride await while its mate died on foot at f336, neither
  new behavior ever reached. Strict based games untouched. Needs the
  same Sunkens2 run confirming board plus ferry.
- Pair arrival legs park precisely (Sunkens2 root cause): the flyer
  fast-transit projects destinations 288px past, so rejoin/fetch/
  escort/drop legs orbited at 285px for three minutes instead of
  closing within the 5-tile rejoin reach. They opt out of XY
  correction for one move now. Needs the same Sunkens2 run confirming
  the ride closes and boards.

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
- Gap: lurker wall (2064 storms killed 0/18 lurkers; need cast
  positions vs lurker sightings, detection upstream suspected).

## Open: air/observer validation

- Corsair dance latch (2026-11-17: dance holds only under spore
  cover, per-corsair Latch spans the cooldown cycle; needs a live
  overlord hunt confirming no Hunt/Dance flap).

- Corsair 14-tile flee trigger (shared AirFlee stays 12).
- Shooting Corsairs hold the spread mid-duel.
- CorsairHunt march gate (macro corsairs vs Overlords).
- Observer hunter early warning macro confirmation (muta pressure).
- Arbiters flee like Corsairs (wound-scaled 2-to-11 margin).
- Arbiter spread cloak coverage (one Arbiter game; senior-arbiter
  attraction removed per review C2).

## Open: march validation (needs macro games)

- StrongMarch (banked stock or maxed army skips rally/slots).
- Retreating warriors never rally or close ranks.
- Expo punish vs Zerg at 140 (units-strength only).
- TankHug (cooling warriors close sieged tanks to 0.2 tiles;
  Terminus 2026-11-18: HugTank act fired 129x in three late clusters
  f21434-30516, 13 enemy tanks died - act-level proven, 0.2-tile
  arrival still unproven from trace acts).
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
- HoldLine creep (per-unit advancing anchors landed + scenario;
  0 fires in 4 macro games incl. 25-min Terminus grind - gate review
  2026-11-18, no game needed from here: creep sits in the terminal
  Reposition stage behind volley-hold (Close outer=false), while
  resolveBreakFormation:248 breaks whenever the target sits inside
  128*confidence px past range. The creep window (Fight posture +
  dressed 0.8 + cold weapon + beyond range + unbroken) looks
  unsatisfiable in contact; needs an owner decision: relax the
  readiness/break terms, re-scope, or delete the behavior).

## Open: posture-era validation (needs games that reach each rule)

New kernels ride suites; these need a live game that reaches them
(scout the named behavior in trace/probe/errors.json, delete the line):

- Middle-choke guard (2026-11-17: past 13 warriors the standing guard
  moves to the middle choke; needs a mid-size army game).
- Reaver leash REVERTED same night (foot reaver past 3 tiles walks
  back): UMS evidence shows the pair lives in SquadAttack, so the
  with-army gate stays open while boarding/fetching and the leash
  turns arbiter flap into movement flap. Retry needs proximity
  anchoring plus the fetch-glue latch, not squad identity.
- No reavers in the PvP goon mirror (2026-11-17: three-/four-gate
  goons no longer force the Bay/Reaver detour; needs a PvP mirror).
- Dragoon bravery vs pure melee (2026-11-17: facesRush needs ranged
  backup in the pressuring pack; needs a ling/zealot flood game).

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
- DefenseWhy board (needs any defense staffing: reason + triggering
  enemies by class/count + army total on the board).
- ClusterRejoin (0 acts across 3 macro grinds; engine suite pins the
  behavior and grinds lose their drifters in contact, so live may
  never trigger - validated in StarEngine, watch only).
- ZealotLeash (validating live: 918-1050 acts across PvP grinds; still
  wants one clean lone-zealot-vs-3-goons case).
- WorkerSpread (validating live: 65-773 dealt picks; wants one worker-
  harass game with the spread visible per unit).

## Open: micro validation (needs games that reach it)

- ZealotLeash latch (2026-11-18: slot/Leash alternation every
  3-6f on the natural choke - raw 5.5 boundary, no hysteresis;
  latched 5.5/4.5 same shape as pursuit. Needs a live choke game
  confirming one commit per crossing).
- Attack floor 180 with hard-counter escape (2026-11-18: 238-strength
  Withdraw via fighting<0.35 with RetreatAllowed open under 250;
  floor forbids retreat at >=180 unless lurkers/DT/carriers/siege
  known, dying hulls keep their out, proxy still forbids. Needs a
  live 180+ macro game confirming band instead of Withdraw).
- Terminus grind 2026-11-18 (DEFEAT 82/144 f24120): DT hunt
  correctly idle (0 HuntEnemyTemplar, no enemy DTs, no errors);
  PotshotVulture 102x (2nd live confirm); HugTank 129-133x in
  clusters (3rd firing data point, 0.2-tile arrival still unproven);
  MineSidestep 22x vs 38 mines (fired, quality unproven);
  posture 2 flips in 16 min with retreats flowing under sieged
  tanks (floor binds, escape opens - no cycling); NoTankMarch 0
  same-tick Retreat combos.
- Terminus grind 2026-11-18 (VICTORY 642/371 f40848): NoTankMarch
  and Toothless end exactly at first siege f18138 (gates honest);
  HugTank 472x f18185-38827 (4th firing data); DT hunt idle again
  (no enemy DTs); posture stable, retreats flow vs siege (escape
  opens); leash alternation p50 11 vs pre-fix 5 (damped, phase
  instrument inconclusive); MineSidestep N/A (no enemy mines).
- Sunkens mineral wall (Reav&Shut 1/2: 0 scarabs built all game,
  0/0 timeout defeats; Reav&Shut 3 builds 3, fights 6/3; Sunkens4
  ends f257 by map trigger): ReloadScarabs gates on minerals>=price
  and baseless UMS has no income, so a 0-mineral start permanently
  disarms the reaver - nothing to shoot with, nothing to ferry for.
  Sunkens3 is the positive control (armed pair fights). No code fix;
  map resources, not behavior.
- Crona rematch post-fix (VICTORY 47/3 f11038): leash latch runs
  (208 acts), floor holds (posture 1 row all game, 28 Retreating vs
  1388 pre-fix - no cycling), DT hunt correctly idle. Caveat: stomp
  (24-zealot flood), never behind - validates no-regression, not the
  grind case. A grind-case PvZ still wanted for the choke-hold
  confirmation.
- Monster sample #2 (2026-11-18, DEFEAT 29/50 f11376: 2-gate
  zealot flood into 88-ling flood + sunken, right comp, out-massed;
  1 Forge 0 cannons built. Zealot-first again appropriate here;
  still needs a game where dragoon-first branches get picked).
- Endgame chase (ZealDrag re-measured post-hunt-fix: DEFEAT k26,
  hunt pursues to f4800 with march updating throughout - no more
  f2852 freeze; remaining enemies kite forever at equal speed while
  the pack switches targets together (43/212/216 synchronised).
  Closing a kiter needs surrounds, not stickiness - contain carve-out
  stays parked; StarEngine ChaseAndKill covers workers, live needs
  the same proof vs kiters).
- Re-baseline UMS batch post-all-fixes (9W/13L/3NA): mirror
  VICTORY 398 with f0 Retreat flicker releasing to duel (bus 24f
  hold absorbs, latch never engages committed hunts - as designed);
  ZealDrag VICTORY 1091, errors clean (crash fix holds 4th game);
  15Drag/3Zeal flip both ways across identical-code batches
  (variance, not regression - stop chasing single-map flips);
  Reaver_v_Sunkens DEFEAT 513 (wall stands 7th game);
  ReavDrag L/W/L coin-flip with escort working, boarding by design.
- Baseless pair unite (Reav_v_Drag: lenient gate escorts f61-641 with
  one clean transition, no escort/await flap; batch re-run
  2026-11-18 VICTORY 596 - the pair unites and wins, gate validated.
  Open doctrine remainder: should an escorting ride hail a dueling
  mate, or is duel-first correct?).
- DT maps wall (2026-11-18: DT_v_Protoss DEFEAT f497 k0/l2 -
  DT hit-and-runs vs 8 goons + cannon, obs caught f306, no isolated
  prey so no kills; Goon_v_DT DEFEAT f827 k0/l4 - 5 goons chase one
  kiting zealot while 2 invisible DTs carve them, hunt correctly
  idle with no detection). Both numbers/kit, not targeting: no
  detection vs permanent cloak is unwinnable, 1+1+obs vs 8+cannon
  needs picks that never split off. Re-run when the escort toolkit
  changes; the batch covers both maps now.
- Main-base DT hunt (engine-pinned: nearest-to-main takes fire
  first among revealed Templars; still needs live: detected enemy DT
  within 40 ground tiles of our main - already-dueling keeps;
  Reavers and our DTs keep their disciplines).
- Reaver pack anchor (2026-11-18: on-foot hull never stands further
  from home than the squad centroid line, minus woundBonus(0,4);
  BackToPack outranks cool-walk/stand/close. Baseless UMS skips it.
  Needs a live pair game confirming no ahead-of-army deaths).
- MeleeKiteAway reach (2026-11-18: zero fires in 4 zealot games -
  UMS commits every warrior (SquadAttack:359), and the behavior
  skips committed hulls by design, so no UMS map can reach the
  branch; the formation gate is additionally lifted for blades
  (requiresFormation kernel + suite - melee never holds dress
  slots). LIVE 2026-11-18 (Crona VICTORY 44/4 f12744): fired 33x on
  suicide-mission zealots 148 (28x) and 163 (5x) inside the enemy
  base, interleaved with Engage/Chase plus a drone kill - the branch
  works in a real duel. Open-field line hold still unproven.
  2Zeal_v_LingUp stays DEFEAT (rerun 4/2 f332) as the UMS baseline).
- Reaver_v_Sunkens wall STANDS (2026-11-18 batch VICTORY 37/45 f4028
  re-run to DEFEAT f506 - outlier on early chaos, now 1W/9;
  no new labels in either game, baseless skips anchor/home-hold).
- Reav_v_Drag pair win (2026-11-18: VICTORY f497 twice in a row,
  StandShoot/CoolWalk duel, 7/9 kills - escort/board/glue era holds).
- UMS variance re-confirmed (2026-11-18: 3DragHydra VICTORY->DEFEAT
  ->VICTORY, 15Drag VICTORY->DEFEAT->VICTORY, 3Zeal VICTORY 3-0->
  DEFEAT across identical-code runs; 3DragHydra timeout kills 4-0 -
  mop-up, not combat. No new labels anywhere - the formation-hold
  change does not touch these matchups).
- Goon_v_DT hunt fires (2026-11-18 rerun: 16x HuntEnemyTemplar from
  f512 on remembered contacts, kills 0->3 across runs, both games
  DEFEAT at f3203 map timeout. Anchor validated; the map has no
  detection so the DTs stay unkillable - unwinnable by design, as
  triaged. 2026-11-18 3rd run: 3/3 f3203, hunt 15x - kills now
  consistent, timeout unchanged).
- UMS walls re-confirmed 2026-11-18 (Sunkens1 instant f1 trigger;
  Sunkens4 f257 trigger; Sunkens2/3 f4820 timeouts on Approach/Move
  milling with 4-10 CloseStatic/StandShoot - the hunt-destination
  input flap, already queued; DT_v_Protoss 0/2 f497 - wall;
  ZealDragLings 24/10 f4820 - wins the fight, loses the mop-up).
- Home-hold lock (2026-11-18: inside the choke margin with a hot gun
  the technique can be neither Fallback nor Flee - latch broken,
  all four Fallback transitions gated, fallthrough forced to Fight.
  Same line the HomeStand verdict votes on, same cooldown the dodge
  holds on. Crona 2026-11-18 (VICTORY 44/4 f12744) never pressured
  home - 3 forward zealot deaths + 1 probe, no natural defense.
  Still needs a live defensive game confirming no
  Retreat/Fallback/Potshot at home with a ready gun).
- NoTankMarch inverted to posture (2026-11-19: the gate skipped slots
  to make Dragoons attack tankless Terran; now NoTankAttack votes
  posture Fight and the walk stays on the slots, duel releases through
  the break conditions. Needs a live macro PvT vs tankless Terran
  confirming Dragoons attack in formation instead of posing).
- Formation-first refactor (2026-11-19, five moves, suites green):
  (1) slot discipline binds in contact - only a firing gun, fresh
  wounds or a held retreat vetoes the legs; (2) Strong/Toothless/
  Roam/Static gates deleted, their intent votes posture Fight/Fight/
  Regroup/Fight in updatePosture, travel keys on posture (Regroup
  marches out dressed); Main/LoneMarch stay as pure travel overrides;
  (3) kite steps back off the slot pin, Hunt assigns the target and
  yields to the duel instead of attack-moving; (4) never-retreat is a
  posture floor (Withdraw->HoldLine) plus the shared technique lock,
  homeHold replaced by NoRetreat.holdsAny; (5) standing guards form
  Guard at the choke post, not the main. Needs: macro PvT (posture
  forces + slot line), Goon_v_DT (Hunt assign), macro defense game
  (choke guard + posture floor).
- HuntContact marches at parity, sweep never ends (2026-11-19 night:
  Hunt assigned the target and yielded, but posing at an invisible
  ghost froze Goon_v_DT, and marching 14 Dragoons into 16 wiped twice
  identically at f827. Now: visible prey goes to the duel slot-bound,
  invisible memory keeps the attack-move; the hunt arc marches out only
  at/above parity (withdrawBelowAdvantage), below it the pack holds home
  slots; the unseen sweep is oldest-first perpetual. Results: 14Drag back
  to variance (VICTORY 16/8 + close DEFEAT 12/7), ZealDragLings VICTORY x2
  (31/8, 31/10 ~f1025, mop-up timeout gone), 3DragHydra VICTORY x2 (5/2 f695,
  unfrozen). ZealDrag_v_ZealDrag stable 5-6/9 loss with verified focus fire
  (74 picks on one target) - duel quality, not targeting; future. Goon_v_DT
  unwinnable without detection (3rd confirm); leave the map alone.
- Pack-work coordination at the main choke (E_10:27:25 Crona analysis:
  2 zealots + ~10 probes fight at the choke but PackRegroup yo-yos the
  blades home every ~30f to a home-weighted centroid while probes die
  alone. Fix, post-freeze: (1) PackRegroup never fires in contact
  (inRangeToAttack or engagedUpon) - the rule stops lone charges, it
  must not pull fighters out; (2) detachment staffing waives the 5-warrior
  minimum when a defense squad has zero warriors, so the blades join the
  fight's squad instead of milling home; (3) WorkerCombatSupport.mayFight
  gains an HP>20 gate - wounded probes rotate to the minerals, continuous
  recruitWorkers backfills. Rejected: workers into the attack squad (no
  consensus marches home - abandons the post). The leash emerges: fight
  only within 10 of a fighting warrior, chase further and withdraw.
- PvZ sunken break (user spec, post-freeze): unconditional go(Reaver,
  static >= 4) independent of composition caps, Robo cancel forbidden
  while static >= 4, max 2 Reavers (techReaver already does get(2)).
  Caveat to verify live: were the 4-6 sunkens scouted at all - if not,
  the trigger needs scout survival first, not just weights.
- Formation holdable (2026-11-18: line forms only vs what shoots
  ground - Turret/Spore marched down, Sunken/Cannon still hold;
  kernel isHoldableTarget + suite. Needs a live PvT confirming
  dragoons march a Turret instead of posing).
- Storm stand siege-gated (2026-11-18: stand holds vs unsieged Terran
  too - walking Marines down is allowed danger, siege shells still
  forbid; the cast itself keeps vetoing tank-covered boxes. Needs a
  live PvT with charged HTs vs Marines confirming the cast lands).
- Observer Vessel ring (2026-11-18: eleven-tile keep-off vs Science
  Vessel, latched 11/13; detected eye holds four off the gun centroid.
  Kernel + suite. Needs a live PvT confirming no inside-sight skims).
- Ghost lockdown avoid (2026-11-18: Carrier/Arbiter/Shuttle never
  inside 9.6 tiles of a 98+ Ghost, last in the avoid order;
  AvoidDangerousUnits kernel + suite, wired at the top of all three
  brains. Needs a live PvT vs Ghosts confirming no freezes).
- UMS batch triage 2026-11-18 (8W/15L/2NA, post all fixes):
  ZealDrag flips to VICTORY 9/6 errors-clean (baseless guards hold);
  15Drag ends 3v1 chase-timeout (surrounds unbuilt, contain parked);
  3Zeal pack focuses together but trades 1-3 (variance, no stuck
  retreats); Reav_v_Drag L/W/L coin-flip with escort working and no
  boardings by design; Goon_v_DT + DT_v_Protoss walls recorded
  separately. Regressions to re-measure: 15Drag(V->D), 3Zeal(V->D),
  3Drag_HydraUpg(V->D). Next full batch after the next behavior
  change, not before.
- AttackPosture flap (Crona PvZ 2026-11-18: 20 Fight<->Withdraw flips
  f5205-7154, ~100f period): entry needs only fighting<0.35, the 96f
  floor gates only the exit - leave after 96f, re-enter the moment
  fighting dips. Strong armies cycled home instead of holding; the
  180 floor (band instead of Withdraw) plus the leash latch target
  exactly this. Needs a live PvZ choke game post-fix to confirm.
- Reaver_v_Sunkens wall (6 live games pre-retreat-fix, best DEFEAT 10/7;
  StarEngine ReaverSunkensScaleSuite pins it stage by stage: packed
  and column screens clear with 8-10 shots, a 5-scarab hull leaves
  a sunken standing, and the shipped KiteAway kernel does not save
  the escort against faster melee on an open plane. CAUTION 2026-11-18:
  all six ran with the baseless Retreat NPE (fixed same day) - the
  goon-survival half needs one post-fix re-measure before the wall
  is final; the scarab arithmetic (8-10 needed, ~5 carried) stands
  regardless. Re-measured 2026-11-18 (DEFEAT 3/6, errors clean):
  wall stands, zero victories in 7; losses run lower post-fix
  (7, 6 vs 8-10 - retreats function), kills swing 3-10 on early
  chaos, sunkens still never die).
- UMS map-side endings (2026-11-18: Reav&Shut_v_Sunkens instant
  DEFEAT f1 0/0, Sunkens4 DEFEAT f257 0/0): BWAPI onEnd(false) with
  no decisions from the bot - triggers, not behavior. Re-run single
  maps that start properly; do not chase these in code.
- Baseless geography crashes FIXED 2026-11-18 (ZealDrag_v_ZealDrag:
  2292 notes/game - ourMain empty.minBy via DefendFFE, ourMetro NPE
  via the rush-retreat arm - both guarded on ourBases.isEmpty;
  post-fix errors.json absent and the map flips DEFEAT 4/9 to
  VICTORY 9/2). Implication: every UMS result before this fix ran
  with per-frame Tactician/Agency deaths - re-measure walls
  (Reaver_v_Sunkens first) before trusting old numbers.
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
- ReaverBraveStand (stale shielded hull holds vs Terran in 8.5 tiles;
  Terminus 2026-11-18: 0 acts with 15 reavers built - conditions
  never met, still unvalidated).
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
- Shuttle fetch glue (2026-11-18: adopt gated on no-duel for the
  whole pickup branch - fighting hulls board only on explicit hail
  or Scarab; kernel + suite. UMS pair maps validating now).
- Shuttle await dither candidate (Returning/AwaitRide 2619x flips).
- Drop lifted targets (tasked building takes off, slot releases).
- StaticDefenseRepel shoots (weakest-first, retreat still outranks).
- Rich bank skips kiting (minerals >= 1000, expendables hold DPS).
- Dragoon closes fleeing workers (StarEngine ChaseAndKillSuite green;
  needs one live confirm).
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
- Monster sample #1 (2026-11-18, DEFEAT 5/38 f11856: zealot-first
  into a blind hydra switch - Den complete 3:36, first hydra seen
  5:11 already marching, collapse from 5:26; worker scout abandons
  vs lings by design, corsair explore arrives too late). Sample
  count is one; needs more PvZ before any branch verdict.
- Crona 4-pool overrun (lings arrive before the first Zealot; early-pool
  scout reaction needs gameplan review).
- March-leg death tails (returning/marching units die mid-leg; probes
  lost late in grind losses).
- Production observer jam concept NOT BUILT (93 trainings, 91 died
  ~9f later under fire, jamming shuttle/reaver production; gate
  retraining when trainings die instantly).

## Session: warnings + DT + rally + ferry (2026-11-16, open)

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
  TWIN seen live 2026-11-17 (Benzene PvT 194253, u=179 f7353-7391):
  six-squad bounce (Attack/Ferry/Defend/Scout/ClearBlockers) with a
  stale NoTankMarch label while technique retreated on a fresh
  -OutOfContact; single-writer skips leave the label behind when the
  squad changes under it. Post-migration the dispatch reads the bus,
  so the stale label cannot dispatch - the twin cost 4 smear frames.
  Terminus 2026-11-18 data point (25-min macro PvT): 1274 NoTankMarch
  rows, zero same-tick Retreat combinations - march and duel never
  share a frame; neighbors within +-10f are sequencing, not flap.
