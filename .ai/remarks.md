# Owner remarks (living list, newest last; history in git log)

## Infra notes (not a bug in the bot)

- 2026-11-23 SCBW.play failures (3x, EOF in startGame after the
  opening/gameplan print, "No server proc ID", client JVM Killed):
  the game server died before frame 1 on the docker path, for two
  different enemies - not bot logic (the bot reaches gameplan
  selection fine) and not the jar (the same jar plays full games).
  Box showed no memory pressure; my own Wine path kept working.
  A diagnostic SCBW game right after played normally (frame 8760+
  and climbing), so this was transient game-server flakiness
  (possibly load contention from parallel Wine batches). If it
  recurs: grab the GAME_* dir immediately (logs_0/game.log shows
  how far it got), check `docker ps`, and avoid heavy parallel
  builds during ladder games.
- 2026-11-23 Wine path NOATTACH/pipe failures: wedged Xvfb
  (BadWindow errors since session start) - kill and restart Xvfb,
  then one smoke game before batches.

## Game-gated watch (needs a game, not code)

- Retreat 160 deadband: kept; revisit on game evidence.
- Long regroup verdicts: 130-gate scoped to 6+ warriors; melee
  boundary flap still unwatched.
- 14-vs-16 dragoon ball (`ums/rav/protoss/14Drag_v_16Drag.scm`):
  formation + focus trades better but wins unstable; suspects:
  mop-up, partial focus (wings out of shared-target range).
- Single unexplained suite flake before three 1208/1208 greens: watch.

## Parked / blocked
- Reaver ferry (owner order 2026-11-22): broken beyond measure, no
  more Reaver testing. Vote agreement + BoardStalled leg stay
  suite-pinned but unproven live.
- Terran maps out of scope (owner order 2026-11-22): Protoss units
  only - Zealots and Dragoons (ZealDrag/1Drag/16Drag/1Zeal/2Zeal/
  Wounded/3Zeal mirrors + StarEngine mirrors below).
- Sim-minus-statics (no contain code in tree), Atlantis suite ports
  (need per-stratum seams), replay reader (opaque engines, screp
  offsets fail; OpenBW re-sim later), krasi0 (needs supportMaps +
  BWTA2 pack).
