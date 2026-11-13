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

## Awaiting a validation game (all committed, suite-green, jar synced)

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
shared Shuttle+Reaver status; StarTest harness for drop-loss <=5%.
StarTest sunkens scenario (`b6f207975`): Reaver+Shuttle vs 2 lings (0.5)
screening a Sunken (7) vs our 8 - lings die f63, sunken f246, reaver
8.3% (pure ling-melee tax, zero static damage), shuttle untouched.
Same test caught two live-game gaps: fresh-hull reboard band never
opened (`815dd4306`, +2.0 base) and no step-back inside outranged
statics (`b6f207975`, advantageBackoff + harness standoff rule). Scarab
rescue ported (`f99c81e0e`, THIS): pressured mid-shot boards while the
shot is flying (old first claim, pure gate + suite). Per user
2026-11-13: no more validation games, reproduce in StarTest. Refresh
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
stale hailers (macro game: no shuttle idling in the natural). March: StrongMarch (THIS) - banked stock or maxed strong army
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
