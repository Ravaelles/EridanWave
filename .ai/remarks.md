# Owner remarks (living list, newest last; history in git log)

## Infra notes (not a bug in the bot)

(none open - 2026-11-23 SCBW/Wine transient failures retired to git
history 2026-11-24: servers dying in the same second is a host-side
event, not bot logic; grab GAME_* immediately if it recurs)

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
