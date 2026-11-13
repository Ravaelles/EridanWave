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
(`8deb6aa30`, visible Lurkers/DTs/Wraiths/Ghosts). Vulture duels
(`0dac77449` vs Dragoons, `PotshotVulture` rows). Carrier berth
(`d09a80cf1`, scratched shields vs ranged, `CarrierKite` rows).

March/stance: `NoRetreat` tanks (`c4bb6ed62`) - siege tanks within 18
of our natural always attacked, no retreat (`TanksAtNatural` rows).
`SuicideMission` workers-first (`7c75e98eb`) - needs Dragoons meeting
enemy workers in the main (assault itself verified GAME_54CCDB32:
labels from f11600, commit, kills, VICTORY 77/7). `LoneMarch`
(`ea28a943e`) - lone unit walks straight at goal. No-dressing
(`c2af083ed`, `782bbb938`) - Waiting/Marching dry with no enemy
near, column out of main. Base guards 30 tiles (`a5c7f3303`) -
enemy near guarded base. Screened static +50% (`601e43dbd`) -
statics covered by 2+ defenders. Shuttle flee
(`10c04fc8f`) - Hydras on loaded Shuttle, AirFlee before 12 tiles.
Reaver vs workers (`8f84e9745`) - worker-only pressure, shields
>70, no Hop. Reaver leash (`b9eeb500a`) - Reaver-heavy game, rally
rows down. Reaver standoff (`be5dbb90d`) - sunkens map, back off to
max range. `ReaverBraveStand` (`cb7903db4`) - stale shielded Reaver holds vs
Terran army within 8.5 tiles, never under sieged tanks (`BraveStand`
rows, no Hop/retreat flap). Reaver+Shuttle doctrine rebuild (research) -
baseline in `.ai/reaver-shuttle-baseline.md`, then a fresh doctrine on a
shared Shuttle+Reaver status; StarTest harness for drop-loss <=5%. DropSafety (`61bcb46eb`) - reaver-vs-sunkens attach
(`Reav&Shut_*` unblocked by `60564e111`). `WallBreach`
(`fbd43ba36`) - no-ground-path Terran wall. DT evade/hide
(`2ca28d0b5`) - detectors/ranged on DT. DT lone hold (HASH) - full
shields + no attacker in 10 tiles ignores the detector, keeps hunting. Choke contain (`80e9e0f1a`)
- parked on enemy natural choke vs anti-ground statics holds.
Contain raze split (`0dac77449`, `75747c418`) - contain + live
enemy expo. Weak raid (`fc06f634b`) - 2-4 warriors detach for a detected expo
while too weak to contain (`R` squad rows). MainMarch guard (`75747c418`) - no `ourMain` crash on
baseless UMS (`errors.json`).

Macro: KillFrenzy (`78831687a`, 195 supply + bank, releases <170).
`ResearchBanked` (`47089f1a2`, >700min+>500gas). Worker cap 63 +
4th miner past 55 (`47089f1a2`, `dd1ee3c70`) - long macro. Pylon
discs (`1ae475b4a`, `1a95c9282`) - no base Pylon inside. Main choke
clear 6 (HASH) - nothing but wall-ins within 6 tiles. Fogged drop
(`216ead4de`) + corpse clear (`142f4282c`) - zealots idling at enemy
base. Worker gather/build rows (`4c2a7b0cf`) -
`gather:Minerals`/`moveToConstruct`/`issueBuild:X` in trace.
