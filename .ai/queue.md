# Request queue (living list, newest last)

User requests that arrived faster than games could verify them. History
lives in git log; this file keeps only open items with enough context to
resume. Update on completion; delete lines that land.

Conventions: repo docs stay English; implementation follows
`.ai/conventions.md` (tests + blank-line gate per change, commit per unit).

## Open: krasi0 bot config for SCBW.Play

Goal: play vs the krasi0 files in `/sc-ai/BOTS/krasi0` through `scbw.play`,
the way `FETCH-NEW-BOT.sh` sets up downloadable bots.

STAGED 2026-11-13: `/sc-ai/BOTS/krasi0/` holds `BWAPI.dll`,
`AI/krasi0AIClient.exe` (+ gmp/mpfr DLLs), `read/`, `write/`,
`bot.json` (`name: krasi0`, race Terran confirmed via Liquipedia -
Terran mech, `botType: EXE`). `BotPlayer` structure check passes
(TERRAN/EXE, single exe found).

Still to do: one `scbw.play --bots krasi0:R` smoke game (needs a free
Wine slot - one game at a time on this box).
Smoke 2026-11-13: bot launches under Wine and talks to BWAPI
(`gl & hf from krasi0 v2.781 (LF: 3)` in its log), then exits:
`Fatal Error: No TA cache file for map in bwapi-data/BWTA2/.
Pick from supportedMaps!`. Confirmed empirically: TauCross is
unplayable for it; unblocked only by the supportedMaps pack + BWTA2
cache, which has no reachable download (krasi0.com down, goo.gl
links dead). Parked until the pack shows up. Caveats: krasi0 readme says
it is SaaS (needs internet) and officially supports only its
`supportedMaps` pack + BWTA2 data; TauCross is likely unsupported
(still plays, possibly weaker). LF 3 required for full strength.

## Awaiting a validation game (all committed, suite-green, jar synced)

One line each; details live in git log by hash. Run a game, check the
named behavior in trace/`probe.txt`/`errors.json`, delete the line.

- `TargetFilterOverlord` + `OverlordSnipe` (`a869e66d6`): needs a natural
  Overlord-in-combat occurrence (never chase, snipe the lone one).
- DT doctrine (`1556073e9` + helper fold-in): needs a game with DTs
  (no DT UMS map; natural melee only).
- `DropSafety` veto (`61bcb46eb`): needs a reaver-vs-sunkens run that
  attaches. NOTE: `wine-run.sh` sed `&`-escape fix (`60564e111`)
  unblocks `Reav&Shut_*` names - retry those maps first.
- Defiler > Lurker > Mutalisk tiers (`139b97e5a`): needs a natural
  Zerg-comp game.
- Reaver +5-tile rally leash (`b9eeb500a`): rally rows down in
  `GAME_A619B9B8` (673 vs 1747) but units not identified as Reavers;
  needs a Reaver-heavy game.
- Worker lone-fighter rule (`<=30hp` + closing fighter): suite only.
- Wall-bust Focus hatch (`9b595bb18`, outrange guard added later):
  needs a Terran wall-in (idle guns break Depot/Barracks, Dragoons
  hold off outranging statics).
- Screened static +50% (`601e43dbd`): needs statics covered by 2+
  armed defenders in a judged battle.
- Base guards engage inside 30 ground tiles (`a5c7f3303`): needs an
  enemy near a guarded base.
- KillFrenzy latch (`78831687a`): needs 195 supply + (1000 min & 250
  gas, or 1500 min); releases below 170. Rare state - watch long macros.
- `ResearchBanked` float (`47089f1a2`): needs >700 min + >500 gas bank.
- Worker cap 63 (`47089f1a2`) and 4th miner only past 55 workers
  (`dd1ee3c70`): observable in any long macro game (miner counts).
- Fogged-memory drop (`216ead4de`) and corpse clear (`142f4282c`):
  needs zealots idling at an enemy base (the 7-time report).
- Worker gather/build trace rows (`4c2a7b0cf`): check any game trace
  for `gather:Minerals`, `moveToConstruct`, `issueBuild:X`.
- No-dressing rules (`c2af083ed` 20-tile enemy check,
  `782bbb938` 90-supply home column): watch Waiting/Marching rows dry
  up with no enemy near / column marches out of main.
- `MainAssault` (`706edf6e6`, fixed latch `dee6339f3`): needs our
  units inside the enemy main (vicinity + commit, no formation).
  Superseded by `SuicideMission` (uncommitted working tree): needs our
  warriors inside the enemy main (`SuicideMission` march label + commit,
  no retreat/flee, KiteAway still steps out while cooling, workers
  targeted before combat units).
- DT detector evade + ranged hide (`2ca28d0b5`): needs detectors or
  ranged fire on a DT (`DarkTemplarEvade`/`DarkTemplarHide` rows).
- Shuttle preemptive flee (`10c04fc8f`): needs Hydras walking up on a
  loaded Shuttle (AirFlee before 12-tile contact).
- Reaver stands vs workers-only above 70 shields (`8f84e9745`):
  needs worker-only pressure on a shielded Reaver (no Hop).
- `StaticDefenseRepel` (`3bc73c3bf`): needs friendlies inside 9 tiles
  of statics with no fight consensus (radial push-out rows).
- `SneakyWatch` trace lines (`8deb6aa30`): needs visible enemy
  Lurkers/DTs/Wraiths/Ghosts (`WeDetected:` rows).
- `ReaverStaticStandoff` (`be5dbb90d`): needs a Reaver inside static
  range on the sunkens map (back-off to max range, no 1.9-tile park).
- `WallBreach` (`fbd43ba36`): needs no-ground-path-to-goal (Terran
  wall game; nearest-blocker targeting).
- Storm friendly veto (`c25aa6dee`): needs a stormable pack with 3+
  own units in the box (no cast).
- Vulture duels (`0dac77449`): needs Vultures vs Dragoons
  (`PotshotVulture` rows, holds at 1.5x reach head-on).
- Carrier wound berth (`d09a80cf1`): needs a scratched-shield Carrier
  vs ranged (`CarrierKite` rows, margin grows to 15 tiles).
- Contain raze split (`0dac77449`, empty-map guard `75747c418`):
  needs contain + a live enemy expansion (10-warrior detail + return).
- Pylon rules (`1ae475b4a` 4 tiles, `1a95c9282` 5 tiles off main
  choke): check no base Pylon inside the discs in a macro game.
- Point-blank Focus hatch (`b3c664ed1`): needs friendlies touching
  enemy buildings (no more idle-at-hatchery).
- Detector spread priority + `cherryVisUnitLog` (`e7cb6aaee`):
  needs two Observers together (`DetectorSpread` rows, no clump).
- Sim work (`53d081038`): test-only, no game needed.
- `MarchOrders` MainMarch guard (`75747c418`): no `ourMain` crash
  on baseless UMS maps (watch `errors.json`).

## Coordination (not this thread - ask before touching)

- `scripts/kill-cleanup.sh` (untracked) + `scripts/README.md` changes:
  parallel thread's. The game-night box is shared - one Wine game
  at a time.
