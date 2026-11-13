# Owner remarks (living list, newest last)
#
# History lives in git log; this file keeps only open items.

## Game-gated watch (needs a game, not code)
- Retreat 160 deadband: kept with rationale; revisit on game evidence.
- Long regroup verdicts: Sim probe built; 1v1 flap observed, 130-gate
  scoped to 6+ warriors; melee boundary flap still unwatched.
- Creep Colony acceptance (see todo.md): needs game verification.
- 4th/5th base progression: expected solved by shifted base, watch.
- Single unexplained suite flake before three 290/290 greens: watch.
- Drag mass fight (14 vs 16 on `ums/rav/protoss/14Drag_v_16Drag.scm`):
  formation + shared focus fire now trade far better (16/6, 16/8, 16/9,
  15/8 vs 6-14 kills before), but wins are not stable yet: roughly even,
  one 200 s game killed 15 of 16 and lost the scenario clock hunting the
  last enemy. Next suspects: last-enemy mop-up on UMS, and partial focus
  (wings out of range of the shared target fall back to their own pick).

## Parked / blocked
- Sim-minus-statics: no contain code in tree, parked.
- Atlantis suite ports (see todo.md): per-stratum seams, no full harness.
- Replay reader Tier 1 (see replay-reader.md): planned, unbuilt.

## Owner issue log (reported -> status, newest last)

- [ ] Pylon within 4 tiles of a choke keeps appearing (GAME_1CD49FB9:
  first Pylon (9,35) sits inside a gateway wall with its Gateways).
  Fix `1ae475b4a` is live in the jar but exempts wall/defense Pylons.
  Open question: extend the 4-tile ban to wall Pylons (risk: unpowered
  walls) or was it a different Pylon?
- [ ] Dragoon vs Vulture: if cooldown <= 5 and a Vulture is in range,
  attack it; approaching head-on (moving, facing us) engage already at
  1.5x range. Investigated (HoldToShoot/PotshotBehavior exist) - NOT
  implemented, no tests yet.
- [ ] Carrier under 100 HP keeps 14 tiles from the nearest ranged
  attacker that can hit it (Carrier KiteAway). NOT implemented.
- [ ] ContainStatus + detected enemy expansion -> split the main army,
  new 8-10-unit squad razes the base. NOT implemented.
- [x] Wall-in bounce (zealots forward/back at Terran walls): Stardust
  reachability rule ported - `WallBreach` (`fbd43ba36`, jar synced).
  Needs a wall-in game to verify.
- [x] Storms hitting our own army: `vetoBox` through TargetedSpell/AOE,
  veto above 2 own units (`c25aa6dee`, jar NOT rebuilt after).
- [x] MainAssault zealots ignoring adjacent base (stale fogged toAttack):
  corpse clear (`142f4282c`) + fogged-memory drop (`216ead4de`), jar
  synced. Needs natural game proof.
- [x] Stop-and-go march (Waiting/Marching/Returning cycle): dress only
  with an enemy inside 20 tiles + compact-strong-unopposed skip
  (`c2af083ed`, jar NOT rebuilt after).
- [x] Worker CherryVis logs (gather Gas/Minerals, returnCargo, build
  stages assigned/moveToConstruct/waiting/issueBuild): un-gated
  (`4c2a7b0cf`, jar NOT rebuilt after).
- [x] KillFrenzy latch (>=195 supply + bank, release <170)
  (`78831687a`, jar synced). Needs macro-game proof.
- [x] Research the >700/>500 float for owned lines (`ResearchBanked`,
  `47089f1a2`, jar synced). Needs macro-game proof.
- [x] Worker cap 63 (`47089f1a2`, jar synced).
- [x] Base guards engage inside 30 ground tiles (`a5c7f3303`, jar
  synced). Needs defense-game proof.
- [x] Screened statics +50% (`601e43dbd`, jar synced).
- [x] Wall-bust Focus hatch (`9b595bb18`, jar synced).
- [x] DT detector evade + ranged hide (`2ca28d0b5`, jar synced).
- [x] Shuttle preemptive flee (`10c04fc8f`, jar synced).
- [x] Reaver stands vs workers-only above 70 shields (`8f84e9745`).
- [x] StaticDefenseRepel inside 9 tiles (`3bc73c3bf`, jar NOT rebuilt).
- [x] SneakyWatch trace lines (`8deb6aa30`, jar NOT rebuilt).
- [x] krasi0 staged (`/sc-ai/BOTS/krasi0`, BotPlayer OK Terran/EXE);
  smoke game pending (needs free Wine slot).
- [?] Two fast Crona DEFEATs (DBE354C5, ACAB92A0: overrun ~7k frames,
  9pool vs PvZ1BaseZealotIntoGoon). Unclear if retarget change
  regressed focus; needs a controlled look.
