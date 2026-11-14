# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.

Conventions: docs stay English; `.ai/conventions.md` (tests +
blank-line gate per change, commit per unit).

## Open: krasi0 bot config for SCBW.Play

`/sc-ai/BOTS/krasi0/` staged (EXE, Terran mech, `BotPlayer` OK). Needs
one `scbw.play --bots krasi0:R` smoke on a free Wine slot. 2026-11-13:
launches, talks BWAPI, then exits - no TA cache for TauCross
(`Pick from supportedMaps!`); pack + BWTA2 unreachable (site down).
Parked. Caveats: SaaS (needs internet), LF 3 for full strength.

## Open: Micro/Actions edge-case review (no game needed)

Audit `src/Micro/Actions` for edge cases that smuggle in whole new
behaviors: three classes for one case (attack always, never retreat,
never flee - suicide-because-in-enemy-base vs fighting-in-defense vs
whatever else). For each class write down its intent (one line in the
header: what it is for, what it yields to); where the intents match,
merge toward one concept. Group unit functionality into shared
concepts, not 300 edge cases. Suspects from the field: suicide
targeting (workers-only, buildings vetoed), KiteTo-vs-kiteaway overlap,
reboard/board/park/await chains in the pair doctrine.

## Open: Micro/Actions concept merges (from the edge-case review)

Single concept, one home - merge when touched (behavior-preserving,
suite-carried; the risky ones need a game to prove):
DONE (`9f36c2e82`): Yamato/Lockdown reach pricing, mineral-clearance
rule, dead FewShot stub. DONE (`2dc4c43ae`): empty
`GetRepairedBuilding.perform` removed (buildings cannot seek repair;
SCV-side Repair already owns healing via toHeal). WONT-MERGE (assessed
2026-11-14, distinct concepts, merging would change behavior):
`CombatManeuvers.move` Reaver boarding smuggle (travel-boarding vs
transport primitives belong in different layers; the pair brain owns
boarding decisions), `KiteAwayBehavior.staticStandoff` to
Assemble/Push (idle-under-fire step-out vs pre-fight depth hold),
`ApproachBehavior.stepBack` to kite (formation correction cadence vs
cooldown kiting). NEXT - safe: (none left open). NEXT - needs a game:
kite-vs-approach range axis; standoff-margin table (Corsair/Cluster);
dress-the-line governor; fight-forcer thresholds; cloak on/off policy;
Yamato/Lockdown-style value sharing for Maelstrom/Storm/Disruption/Stasis;
scout-harass soup to one table; worker-fight backup question to one gate.

## Awaiting a validation game (all committed, suite-green, jar synced)

Night 2026-11-14 (all on the jar, suite-green): contain-leave
confirmation - Contain/Deprox alternation 160 down to 39 on the
sunkens map, the Reaver lives, 6-7 becomes 1-1. Pair duelist loop -
`DuelFerry`, `BoardShotless` (x5), `EscortRide` (x4) all fire live on
Reav_v_Drag + Reav_v_DragB, both VICTORY. Sunkens2 loop proven end
to end (DuelWalk, board, ferry, unload, CloseStatic, escort) - the
wall is ammo, not doctrine: 5 scarabs then a dry hold to timeout.
Adopt fetches hailing, endangered, travelling or Scarab-targeted
Reavers only (was: any Reaver, gluing the ride and starving escort);
foot Reavers duel-walk at known enemies on baseless maps. Storm consensus walk -
casters re-arm on squad fightConsensus (unproven, needs a Storm
game). Still open: the sunkens duel stays DEFEAT (the Reaver trades
14-16 for its life when ammo runs dry with the ride late); two scbw
long games crashed server-side at 8-12 min (wine path stable);
production Storm proof still needs a game that reaches Templars
(rushes keep ending them first). Sunkens2 pair stalls after the
first pack with a live ride nearby: the reaver holds (contacts in
matchups but out of reach), the ride holds (no need, no pressure) -
needs a baseless mission fallback that ferries the pair at known
enemies instead of micro-gating the handshake. Adopt-vs-AirFlee
flap on pressured pickups wants a flee latch next. Crona 4-pool overrun (scbw,
11/36 at 5:47): lings arrive before the first Zealot, probes brawl
and die, no Search acts in trace - early-pool scout reaction needs
gameplan review. UMS batch 2026-11-14: 12V/12D over 24 maps.
Confirmed fine: all 1v1 duels, up-duels, mass drag fights, upgraded
fights, ZealDrag mixes. Watch, probably map-hard, not doctrine:
3Zeal_v_3Zeal 2/3 mirror (focus suspected), 10Zeal_v_Lings 17/10
(out-killed but wiped by the stream), 2Zeal_v_LingUp 3/2 in 169f
(upgraded stream), 1DragWounded_v_1Zeal 0/1 (wounded start),
14Drag_v_16Drag lost (down two), Reaver_v_Sunkens 3/8 (shuttle-less,
ammo economy). Pair maps need the baseless mission fallback (Sunkens
1-4 all DEFEAT/timeout, Drag flips VICTORY/DEFEAT run to run).
DT_v_Protoss timeouts 1/1 (mill vs kiting dragoons).

Storm consensus walk (Templars follow the attack: a personal retreat
no longer blocks the approach; leash, ladder and veto unchanged) -
needs a Templar game (rushes end too fast, we go carriers; Archives
only ~11:18). Observer escort (`a727a0b3a`, ranged flee on a 6-12
tile margin; an f89 death becomes a 1-1 timeout) - the DT on
DT_v_Protoss mills without connecting against kiting Dragoons until
timeout (open). Shuttle-less Reaver_v_Sunkens (THIS): the foot Reaver
survives the whole game on StandShoot/CloseStatic, but 8 scarabs make
3 kills against the ling stream - a stalemate. Two long scbw games
crashed server-side at ~8-12 min (wine path stable, including an
11:38) - infra, not the bot.
Targeting/micro: `OverlordSnipe` (`a869e66d6`, chase never, snipe
lone) - natural Overlord-in-combat. DT doctrine (`1556073e9`) -
natural melee with DTs. Defiler>Lurker>Mutalisk (`139b97e5a`) -
Zerg-comp game. Storm friendly veto (`c25aa6dee`, 3+ own in box, no
cast). Storm Overlord veto (`35c59bc1b`, HT<=190 never storms
Overlord). Storm intent + Atlantis pack (`850a7bba`) - HT approaches
with spell intent, casts only a reconfirmed in-range spot;
energy-scaled pack (195:1, 180:2, 110:base-1), crucial
Reaver/sieged-Tank bypass, never out of range. Wall-bust Focus (`9b595bb18`) - Terran wall-in, idle guns
break Depot/Barracks. Point-blank Focus (`b3c664ed1`) - friendlies
touching enemy buildings. `RepairerSnipe` (`2d4521365`) - SCV
repairing a tasked Terran building gets sniped. `ShuttleDitch/Adopt`
(`c00ef3c3a`) - chain stops after a ditch/adopt (no quest on top).
Detector spread
(`e7cb6aaee`, two Observers, `DetectorSpread` rows). `SneakyWatch`
(`8deb6aa30`, visible Lurkers/DTs/Wraiths/Ghosts). Rich bank skips kiting (THIS) - minerals >= 1000 buys the trade:
expendable units hold DPS instead of yielding ground
(`KiteAway.skipKiting`, macro game with banked stock). Vulture duels
(`0dac77449` vs Dragoons, `PotshotVulture` rows). Dragoon closes
fleeing workers (`1b425a873`, THIS): no more kite-away vs what cannot
hit back - `KiteTo` takes `shieldWound` (was full-health
only), closes grounded helpless at <=15 shield damage, same as
buildings always did. StarEngine chase-and-kill scenario when the
engine is ready. Carrier berth
(`d09a80cf1`, scratched shields vs ranged, `CarrierKite` rows).

March/stance: `NoRetreat` tanks (`c4bb6ed62`) - siege tanks within 18
of our natural always attacked, no retreat (`TanksAtNatural` rows).
`SuicideMission` workers-first (`7c75e98eb`) - needs Dragoons meeting
enemy workers in the main (assault itself verified GAME_54CCDB32:
labels from f11600, commit, kills, VICTORY 77/7). Suicide razes the base
(THIS): unassigned buildings got no Focus vote, so suicide squads stood
on Pylons forever - `TargetFilterFocus.allowsBuilding` now votes yes for
suicide attackers, StarEngine suicide-raze scenario green (workers first
blood, then the Pylon, no dragoon losses). `LoneMarch`
(`ea28a943e`) - lone unit walks straight at goal. LoneMarch latch
(`739d419ff`) - alone past 10.5, back below 9.5, per-unit, reset/game;
re-run Reav&Shut to confirm the +-300px final stops flapping. Order continuation
(`92564a364`) - far shifted goals keep cruising (Move hold-vs-issue counts
vs GAME_54CCDB32). Slow-alone watch (THIS) - log stutter with no units
nearby, max 10 per game. No-dressing
(`c2af083ed`, `782bbb938`) - Waiting/Marching dry with no enemy
near, column out of main. Base guards 30 tiles (`a5c7f3303`) -
enemy near guarded base. Screened static +50% (`601e43dbd`) -
statics covered by 2+ defenders. Shuttle flee (`10c04fc8f`,
`07d372772`) - Hydras on loaded Shuttle, AirFlee before 12 tiles;
single 9-tile wound curve keeps fresh behavior, caps dying. Reaver vs workers (`8f84e9745`) - worker-only pressure, shields
>70, no Hop. Reaver leash (`b9eeb500a`) - Reaver-heavy game, rally
rows down. Reaver standoff (`be5dbb90d`) - sunkens map, back off to
max range. GAME_181312 Reaver_v_Sunkens DEFEAT 13/11: shuttle-less map,
so no shuttle/unload acts possible (correct silence); on-foot
StandShoot x4 + CloseStatic x2, BackOff x0 (nothing outranges us).
Force (5 drag + reaver + obs) traded into ~30 Zerg, scarabs dry after
f522, wipe ~f650, observer evaded to f4820. SlowAlone x4 = post-shot
accel, benign; same window exposed destinationFinal +-300px flap
(44-65% alternation, all units) -> latched LoneMarch (THIS). `ReaverBraveStand` (`cb7903db4`) - stale shielded Reaver holds vs
Terran army within 8.5 tiles, never under sieged tanks (`BraveStand`
rows, no Hop/retreat flap). Reaver+Shuttle doctrine rebuild (research) -
baseline in `.ai/reaver-shuttle-baseline.md`, then a fresh doctrine on a
shared Shuttle+Reaver status; StarEngine harness for drop-loss <=5%.
StarEngine sunkens scenario (THIS): Reaver+Shuttle vs 2 lings screening
a Sunken, all real UnitType numbers (ling 5.49 px/f outruns the 1.78
Reaver, Sunken 40dmg/32f) plus the 15-tile UMS leash with teleport -
lings die first, then the static, reaver under 15% (melee tax only),
shuttle untouched. Same test caught two live-game gaps: fresh-hull
reboard band never opened (`815dd4306`, +2.0 base) and no step-back
inside outranged statics (`b6f207975`, advantageBackoff + standoff
rule). Scarab
rescue ported (`f99c81e0e`, THIS): pressured mid-shot boards while the
shot is flying (old first claim, pure gate + suite). Per user
2026-11-13: no more validation games, reproduce in StarEngine. Refresh
hop PARKED: the 30f drop-cooldown reset is engine timing no suite can
prove; porting it blind would be guessing. Contain carve-out +
mission-ordered unload PARKED: mission layer stays thin per
architecture S4, UnloadOrdered already covers the ordered path. DropSafety (`61bcb46eb`) - reaver-vs-sunkens attach
(`Reav&Shut_*` unblocked by `60564e111`). `WallBreach`
(`fbd43ba36`) - no-ground-path Terran wall. DT evade/hide
(`2ca28d0b5`) - detectors/ranged on DT. DT lone hold (`3319f24ae`) - full
shields + no attacker in 10 tiles ignores the detector, keeps hunting. Choke contain (`80e9e0f1a`)
- parked on enemy natural choke vs anti-ground statics holds.
Contain raze split (`0dac77449`, `75747c418`) - contain + live
enemy expo. Weak raid (`fc06f634b`) - 2-4 warriors detach for a detected expo
while too weak to contain (`R` squad rows). MainMarch guard (`75747c418`) - no `ourMain` crash on
baseless UMS (`errors.json`). C6 march-leg profiling - PARKED per user
2026-11-13 (needs a live macro game; temporary timers were in and out
of the tree uncommitted, nothing measured). DeathWatch: mechanism proven
in melee (Nov 11 log has full Shuttle+Reaver death chains) and the
watched-set membership is suite-green (THIS); TauCross silence is
probably correct (3 losses, likely unwatched classes); the two UMS
Reaver silences stay unexplained and need a game to resolve. Eject-scout UMS guard (`00e5e63fd`) -
GAME_182054 Reav&Shut DEFEAT 2/5: TacticEjectScout->scoutCleared->ourMain
threw empty.minBy every frame f31-f4820 (4790 notes), Tactician dead all
game; zonesToLookForEnemyScouts now answers empty with no bases, shuttle
flee falls back to map home. GAME_182857 re-run (fixed jar, clean
errors.json): full pair chain f0-f100 (Adopt, BoardRide x3, RaidFerry,
UnloadStatic), then reaver fought on foot next to its ride and died
~f480 with 1 kill - fresh-hull reboard band was 0 (fixed `815dd4306`)
and no step-back inside outranged statics (fixed THIS).
DeathWatch silent on both UMS losses despite Reaver deaths - hook never
fired, telemetry gap to verify next game.

March: PairRide (THIS) - loaded Shuttles never march the legs
(rally/slot), the pair owns them; park centers on live cargo, not
stale hailers (macro game: no shuttle idling in the natural). Micro: drop lifted targets (THIS) - tasked buildings that take
off release the slot for whoever cannot hit air instead of chewing
vacuum; needs Terran liftoff in a game. March: ArbiterSpread (THIS) - Arbiters separate exactly like
Observers (nine tiles, newer moves off the ray, `DetectorSpread`
kernel); macro game with two Arbiter hulls. March: StaticDefenseRepel shoots (THIS) - repelling units no longer
hold fire with a hittable enemy in range; weakest-first (lowest total
health, Tickle precedent), retreat still outranks shooting under fire
(macro game: dragoons vs sunken+lings trade). Micro: Storm walks short, storms cheap - approach leash
12 tiles vs Terran / 10 otherwise plus the 75/100/180 pack ladder
(base at 75, pairs at 100, singles at 180, hold fire below 75).
Proven: Terminus PvT (1013 storm acts, bio deaths 2.4x enriched
after casts, 299/202) plus StarEngine lead (0.0px miss) and range
(6 tiles: storm f1, wipe f5; 10 tiles: walk 35px, wipe f16; 15
tiles: hold, 0 storms) scenarios. Micro: crucial air victims first (THIS) - grounded AA shoots
Scourge nearest, then weakest Carrier/Vessel/Battlecruiser/Wraith in
range, ahead of kiting, dressing and retreat; needs air-heavy games.
Micro: DT flees guns first (THIS) - visible enemy that can hit the
Templar inside 13 tiles outranks the detector; Overlords alone never
move the hunt. March: Shuttle pair owns loaded hulls (THIS) - a Shuttle carrying
a Reaver never takes other passengers and no drop mission assigns it
new pickups; needs a Reaver-drop game. Micro: workers run straight from Dark Templars (THIS) - any
worker within 3.0 tiles of a visible enemy DT flees directly away, no
pathing detours; needs DT pressure on the mineral line. Micro: Arbiters flee like Corsairs (THIS) - wound-scaled 2-to-11
tile margin against anything that can hit them, on top of the
entanglement check. Micro: DT kills weak first (THIS) - no-cooldown Templar tiers
in-range contacts: weaklings, then tanks, warriors, workers, nearest
inside each tier; StarEngine DT scenario proves it end to end. Micro: returning scouts walk home (THIS) - no re-issued gather
orders; same target keeps cruising instead of crawling to stops
(macro scout-return smoothness). Micro: suicide forces the attack after ten idle seconds (THIS) -
past the generic 150-frame rule, a suicide unit charges the nearest
enemy regardless of advantage; macro suicide games to verify. Micro: no Reaver raids vs Zerg (THIS) - every pair supports the
main army, never flies into the main; needs a PvZ macro game with
Reavers. Micro: contain fires when ready (THIS) - containing weapons shoot
the nearest hittable target instead of dressing past it; gated on the
contain so volley discipline survives elsewhere. Micro: storms lead
moving victims (THIS) - 2.0-tile lead along the heading, standing
victims keep the spot. Micro: Observers stand off detectors (THIS) - never closer than
6 tiles to an enemy detector, same ray geometry as the pack spread;
needs an Observer-vs-Overlord game. Micro: observer hunter early
warning - a detector inside 11 tiles means seen, so Scourge/Corsair/
Wraith/Mutalisk trigger the flee from 20 tiles; StarEngine proves the
differential (breaks at f11 vs f34), macro game with muta pressure
still needed. March: StaticDefense overwhelm
(THIS) - twelve of ours against one static defender pushes instead of
repelling; needs a contain break. March: TankHug (THIS) -
cooling ground warriors close sieged tanks to 0.2 tiles instead of
kiting; needs a Terran mech game with tanks in range. March: march straight at toothless Terrans (THIS) - dragoons skip
Waiting/Rallying with no tanks around; combat units skip every
formation dance (Waiting, Rallying, slots, trip home) with no Vultures
and no Tanks; nobody dresses inside static reach; no LoneMarch while
retreating. Macro PvT games to confirm the columns just walk. March: CorsairHunt - corsairs never
take formation legs (march straight at the goal instead); needs a
macro game with corsairs vs Overlords (StarEngine proves the hunt+flee,
not the march gate). March: StrongMarch (THIS) - banked stock or maxed strong army
marches straight at the goal, never rallies to the pack centroid or
walks to a formation slot (`StrongMarchSuite`, macro game with bank).
Micro: idle attackers force fire after 150 frames (THIS) - a
combat unit holding a hittable enemy with nobody able to hit it back
stops dithering and shoots; never fleeing/retreating-gated
(`IdleAttackSuite`, StarEngine chase-and-kill when ready). Macro: KillFrenzy (`78831687a`, 195 supply + bank, releases <170).
`ResearchBanked` (`47089f1a2`, >700min+>500gas). Worker cap 63 +
4th miner past 55 (`47089f1a2`, `dd1ee3c70`) - long macro. Pylon
discs (`1ae475b4a`, `1a95c9282`) - no base Pylon inside. Main choke
clear 6 (`20e928ee2`) - nothing but wall-ins within 6 tiles. Fogged drop
(`216ead4de`) + corpse clear (`142f4282c`) - zealots idling at enemy
base. Worker gather/build rows (`4c2a7b0cf`) -
`gather:Minerals`/`moveToConstruct`/`issueBuild:X` in trace.
Micro: ZealotSwarm +10% (THIS, `485ab632d`) - warriors-only zealot-vs-ling battles demand a ten percent target margin; any other warrior (Dragoon, Hydra) or spectator-only sides lift it (`JudgmentModifiersSuite`, 4 cases). Needs a macro game that stays pure zealots-vs-lings to confirm the caution fires. Vs Monster 2026-11-14: DEFEAT 110/163, 106 zealots + 26 dragoons vs ling/hydra/muta mix - not the pure case, modifier correctly silent; no CorsairDance acts in the log.
Micro: DT detector dither latched (THIS, `c19db7528`) - evade releases two tiles past the band (LoneMarch shape) and the hunt skips targets under likely detectors instead of re-approaching them; StarEngine duel pins sharp at 1181 band crossings with zero damage vs fixed at 0 (`DarkTemplarDitherSuite`, real Overlord sight/speeds). Field: Monster Hunt/Evade ~150 flips per DT, deaths at 1-4 HP unconnected. Needs a DT game on the new jar. Batch: 5x Monster running on the pre-DT slot jar (jar rebuild deferred to keep the batch on one build). Engine determinism: StarEngine core has no RNG (no Random/Math.random in non-suite sources) - the Reav_v_Drag VICTORY/DEFEAT flip is game-side, not engine-side. Analyzer: `scripts/analyze-match.py` now also prints a flap rollup (decision changes per unit) beside acts/deaths/death tails.
Batch1 game 1 (GAME_E0DCCAC4, pre-DT-fix jar): Monster DEFEAT 287/283, 25:10. First storm-heavy Monster game (2064 PsionicStorm acts). Split: pre-storm 31/15 (2.07), storm-era 250/268 (0.93) - storms mulch chaff (169 lings, 44 hydras) but we bleed the expensive half (25 HTs, 90 zealots, 38 goons, 41 probes). HT tails: PsionicStorm/Retreat/StrongMarch alternation f28-32k, run down after casting. Hypothesis (needs batch repeat): casters march into contact with the column instead of standoffing at storm range - a caster-standoff fix if games 2-5 repeat it. Also: DT Hunt/Evade dither again pre-fix (DT #245/#318), observer #543 StandOff/Escort 1414 flips but lived 33k frames (harmless, skip). Lategame lurker wall: dragoons die in Scavenge/Snipe/Lurk + Retreat chains f33k+.
Proven in GAME_E0DCCAC4 (Monster, 287/283, pre-DT-fix jar): DetectorSpread 438 rows with 2 observers; observer hunter early warning (ObserverFlee 461 + Hunters 30 vs 13 mutas, eye lived to f33598); PairRide 146 + ShuttleAdopt 28 + evac chain; ContainFire 215; storm consensus (2064 PsionicStorm acts in a Templar macro game, HT-survival split still open per above); DT doctrine fires (Hunt 422 / Evade 206, quality fixed by the latch next batch); FleeScourge 3 (shuttles still died f18973/f31457/f31760 - all in the endgame collapse, watch whether the flee fires too late vs 12 scourge).
Proven in StarEngine (`ChaseAndKillSuite`): closing helpless connects (kill f337), kiting watches it escape (untouched at 1500f). Closes the "Dragoon closes fleeing workers" validation row pending since `1b425a873`.
StarEngine idle-force-fire scenario DROPPED (not a behavior gap): the engine shoots position-based (nearest hittable in range fires regardless of order), so hold-fire-then-force has no contrast to pin - the kernel stays pinned by `IdleAttackSuite` alone. A scenario would re-prove chase-and-kill, which `ChaseAndKillSuite` already covers.
Storm-lurker gap (GAME_E0DCCAC4): lurker stormValue is 1.0 (top, 5x a ling) yet 2064 storm acts killed 0 of 18 lurkers (2 died to other causes) while mulching 198 lings + 45 hydras. Weights are right, so the miss is upstream: detection at the wall, HTs dying before 9-tile range, or boxes centering on chaff. Needs cast positions (CastStorm rows in trace) vs lurker sightings in the next lurker-wall game before touching targeting.
Corsair fix field proof (GAME_22DF1B1C, older Monster loss 25/61): corsair #217 flipped CorsairHunt/CorsairDance 969 times then died - the exact pre-fix dither the dueling + keepsAttacking gates close. Batch2 (post-fix jar) must show the Hunt/Dance alternation gone. Same game: 32 probes + 15 zealots lost f7-8k with march-leg death tails (Marching/Rallying/Returning) - early-pool overrun shape, gameplan watch, not micro.
Micro: observer standoff latched (THIS) - yield releases at eight tiles; StarEngine duel pins sharp at 542 flips/372f inside vs latched 1 flip/1 entry frame (`ObserverStandOffSuite`). Field: GAME_D184D970 eye #382 flipped 2220 times, both observers died; GAME_E0DCCAC4 eye #543 flipped 1414 times, lived. Needs a post-fix game with hunters to prove survival.
Batch1 game 4 (GAME_544F98D2, pre-fix jar): Monster DEFEAT 137/188, 20:33. Returning to slot dominates (10441x, top act), march-leg death tails again, observer #376 flipped 1296 times (died), DT Hunt 562 / Evade 460 pre-fix, 41 probes lost late. No storm path this game (Archives up, zero HTs). Four of five batch1 games show the same grind-loss shape: even early, bleed in the ling/hydra/lurker era, collapse. Batch2 (post-fix jar: DT latch, observer latch, ZealotSwarm margin, corsair gates) must move the trade.
Batch1 game 5 (GAME_48D40933, pre-fix jar): Monster DEFEAT 297/328, 27:10. Second storm game: 1264 PsionicStorm acts, 24 HTs dead (game 1 had 2064/25) - the HT run-down pattern repeats twice now, and both storm games lost despite chaff-mulching. 79 probes + 3 observers + 4 DTs lost. Observer #534 flipped StandOff/Escort 1648 times (third instance: 1414, 2220, 1648 - the latch ships in batch2). Batch1 totals: 5 games, 0 wins (287/283, 179/190, timeout, 137/188, 297/328), all pre-fix. Rebuilding the jar with the session fixes (corsair gates, ZealotSwarm, DT latch, observer latch) and running batch2 post-fix next.
