# Request queue (living list, newest last)

User requests that arrived faster than games could verify them. History
lives in git log; this file keeps only open items with enough context to
resume. Update on completion; delete lines that land.

Conventions: repo docs stay English; implementation follows
`.ai/conventions.md` (tests + blank-line gate per change, commit per unit).

## Open: krasi0 bot config for SCBW.Play

Goal: play vs the krasi0 files in `/sc-ai/BOTS/krasi0` through `scbw.play`,
the way `FETCH-NEW-BOT.sh` sets up downloadable bots.

Findings so far:
- krasi0 is an EXE bot (`krasi0AIClient.exe` + `BWAPI.dll` + gmp/mpfr DLLs +
  BWTA2 `.krasi0.bwta` blobs in `krasi0BotLatestVersion.zip`), not an AI
  module. scbw supports this: `BotPlayer` accepts `AI/*.exe`
  (`BotType.EXE`, exactly one exe, name must not contain "BWAPI"),
  `bot.json` with `"botType": "EXE"`, plus `BWAPI.dll`, `AI/`, `read/`
  (see `player.py: BotPlayer`, `_find_bot_filename`).
- A configured bot dir looks like `/sc-ai/BOTS/Crona`
  (`bot.json` + `BWAPI.dll` + `AI/` + `read/` + `write/`), played via
  `/sc-ai/BOTS/__PlayAsEridan/_Crona.sh` (`play-cvis.sh --bots Name:Race`).
- No EXE-bot precedent exists in `/sc-ai/BOTS` (all are AI_MODULE or
  JAVA_MIRROR); nothing to copy, but no blocker found either.

Still to do:
- Unzip the launcher archive into `/sc-ai/BOTS/krasi0/` as
  `BWAPI.dll`, `AI/krasi0AIClient.exe` (+ DLLs), `read/`, `write/`,
  `bot.json` (`name: krasi0`, race TBD - confirm Protoss, `botType: EXE`).
- Validate with `BotPlayer("/sc-ai/BOTS/krasi0")` (structure check), then
  one `scbw.play --bots krasi0:R` smoke game.
- Caveats: krasi0 readme says it is SaaS (needs internet) and officially
  supports only its `supportedMaps` pack + BWTA2 data; TauCross is likely
  unsupported (still plays, possibly weaker). LF 3 required for full
  strength.

## Open: Overlord targeting (DontAttackOverlords, refined)

User report (melee, vs Zerg): dragoons chased fleeing Overlords while a
freshly hatched Hydralisk stood among the army; the chase split the army
in half (see "Army split" below).

Wanted: never target Overlords while any other combat unit - or even
workers - are in our range; snipe a lone Overlord only when it is in our
range and nothing else is around. (Atlantis forbids Overlords outright
via `DontAttackOverlords.forbidden`; ours is the refined version above.)

Findings so far:
- Our ladder (`Micro/Targeting/Target.scala`) already prefers combat
  priority in range, but non-range strata (`requireInRange = false`) still
  pick fleeing Overlords, and `Commander.attack` then chases them out.
- A required filter (`TargetFilter`, runs in every stratum via
  `legalByFilters`) is the right seam; `canAttack` on the filter keeps
  Corsairs hunting while Dragoons hold fire.
- NOTE: `src/Micro/Targeting/ThreatPriority.scala` exists untracked in the
  tree (Defiler/Lurker/Muta tiers?) - coordinate with its author before
  touching targeting; do not duplicate the seam.

## Open: army split in half

User report: half the army stayed at the enemy natural, half pushed the
enemy main (`Squads A(2)` on the board - possibly stale rendering),
with 1-9-frame `Abuse`/`Returning to slot` and `Waiting for
pack`/`Engage` flapping throughout.

Two mechanisms, two fixes:
- Vote flicker (consensus flapping) -> `ConsensusVoteFilter` latch is
  COMMITTED (`501c42f4b`, fixed pass-through in `dee6339f3`) and
  VALIDATED in `GAME_A619B9B8`: 0 flapping units (was 47), 0 sub-24f
  flips (was 122). The 1-9-frame `Abuse`/`Returning to slot` flapping
  from the report cannot pass the fixed latch.
- Overlord-chasing splitting the pack -> `TargetFilterOverlord`
  COMMITTED (`a869e66d5`, suite green, no errors in game); awaits a
  natural Overlord-in-combat occurrence for end-to-end proof.

## Open: Dark Templar doctrine

User report (probe log): DTs flap `Waiting for pack` / `Engage/Approach`,
get march termini, and scout unseen areas. Wanted:
1. Never `Waiting for pack`, never formation/dress for DTs.
2. Send DTs only at visible or remembered (fogged, last-seen) contacts;
   never sweep unseen tiles or scout.
3. Double the wound flee margin vs detectors (recalled as +4 tiles max
   today -> 8 tiles).

Findings so far:
- `BeDarkTemplar` is mine-sniping only; march/order comes from
  `MarchOrders` + `Combat` on the shared pipeline. `Stealth` handles
  cloak flee but has NO wound scaling today (`nearMobileDetector` is a
  binary sight+8s-topSpeed check) - the +4-tile recall matches nothing
  exactly; closest analogues are `Spot` detector wound bonus and
  `Gather.threatenedWounded` (2.5 + 4 * wound).
- Likely shape: DT brain override (take targets first, hold when no
  contact, unready Combat), march bypass (plain vicinity, no wait/rally/
  slots), `Stealth` wound berth (max +8 tiles), all pinned by suites.
- DONE (`1556073e9`, jar rebuilt after): no DT formation slots,
  march bypass to plain vicinity with a `DarkTemplarHunt` label,
  `BeDarkTemplar` hunts visible hittable, then freshest fogged contact
  (<3 min), holds otherwise; `DarkTemplarBerth` wound berth (max +8)
  wired into `Stealth` mobile-detector check, DT-only. Validation:
  needs a game with DTs (no DT UMS map; awaits a natural melee game).

## Open: Reaver drops next to Sunken Colonies

User report: Reaver dropped right next to 2 Sunken Colonies. Wanted:
never unload within 8.1 tiles of anti-ground combat buildings.

Findings so far:
- `ReaverDropBravery.staticDefenseDist = 8.0` vetoes the brave-drop path,
  but arrival auto-unload (`CommanderMove`: within 8 px of destination)
  and engage unload (`CommanderEngage`) check nothing - the drop likely
  came through one of those.
- Fix direction: guard the actual unload (`Transport`/`requestUnload`
  path) against anti-ground combat buildings inside 8.1 tiles, so every
  path is covered - not another per-path check.
- DONE (`61bcb46eb`, jar rebuilt after): `DropSafety` veto inside
  `CommanderTransport.unload` (flying transports only, so bunkers are
  unaffected), suite-pinned. Validation: UMS reaver-vs-sunkens map
  attempted repeatedly, attach kept failing on that map; needs a run
  that actually starts.

## Open: Defiler > Lurker > Mutalisk targeting

Wanted: every unit with a visible Lurker in attack range attacks it;
Defiler outranks Lurker outranks Mutalisk; among those, the most wounded
in range unless it dies within the next second (already handled via
`acceleratesDemise` + `+4.0 * injury` in `TargetScoring`).

Findings so far: ladder structure mapped (`Target.best` combat-priority
strata, `defaultTargets` is diagnostics-only). Untested seam candidate:
per-tier predicates + in-range/visible gating, mirroring the
`DarkTemplarTanks` doctrine shape. NOTE: untracked
`ThreatPriority.scala` in the tree may already be this - coordinate.
- Not started by this thread.

## Open: melee validation games

- `ConsensusVoteFilter` latch (`dee6339f3`): VALIDATED in Crona
  `GAME_A619B9B8` (VICTORY 53/11, clean, no errors): 0 flapping units
  (baseline 47), 17 flips total, all with gaps >= 24 frames (baseline had
  122 sub-24f flips, 37%). Sustained retreats still work.
- Reaver +5-tile rally leash (`b9eeb500a`): indecisive so far - rally rows
  down in `GAME_A619B9B8` (673 vs 1747 baseline, shorter game) but units
  not identified as Reavers; needs a Reaver-heavy game.
- Worker flee (`6a153ceb`, `7426f32d5`, `252b6facd`): melee-validated
  twice (Crona VICTORY 217/165: Retreat-dominated death rows, no endless
  flap; Crona VICTORY 53/11: 1 probe lost, no flap chains). Lone-fighter
  rule (`<=30hp` + closing fighter) has suite coverage only.

## Untracked files in the tree (not this thread's - coordinate first)

- `scripts/kill-cleanup.sh` + `scripts/README.md` changes.
- `src/Micro/Targeting/ThreatPriority.scala`.
- Do not commit these from here without their author; the game-night box
  is shared - one Wine game at a time.

## Done and verified (keep for reference, prune when stale)

- Worker flee vs melee packs (`doomedVsMelee`, rusher opt-out,
  lone-fighter rule) - suite + Crona game, jar rebuilt + slot synced.
- Dark dashboard theme (`#ccc` on `#141414`), verified by screenshot.
- `benchmarks/goon_v_goon.sh` (N x MAP1 + M x MAP2, delegates to
  `run.sh`) - smoke-tested 1+1 (1W/1L, TSV + summary print).
- Formation harness (`--formation-audit`, tally/verdicts/modes/breaks/
  dress + `scripts/formation-audit.py` incl. assert flags).
- UMS chase of live contacts (`visibleEnemyChase`, validated VICTORY
  16/12 with the endgame timeline), break rows ignore formation-less
  frame-0 artifacts.
- `ConsensusVoteFilter`, Reaver rally leash - committed, awaiting games.
