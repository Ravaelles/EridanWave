# Kite doctrine (evidence + live read guide)

- Melee on top always kites while cooling (range abuse, not the
  volley race). Against ranged guns only the dying yield now
  (dedc29fdf): below 61 total HP (`KiteAway.kiteVsRangedHp`) the hull
  saves itself while cooling past 10, everything else holds its
  volleys. Mild-wound rotation is gone - in even mirrors it donated
  volleys (StarEngine `OutnumberedKiteSuite` 3/3: symmetric movement
  trades symmetric kills; focus-kite 12 kills vs focus-plant 14 down
  two guns).
- Melee kites only slow lings (27149e81c): speedlings are stood up
  to (`IsSlowling` seam, kernel param `hasSlowLingNearby`). 2Zeal
  kills 3->5 with dying-hull kites only; 1Zeal control still
  VICTORY. 2Zeal parked hard (2v6 upgraded needs a melee shared
  pick - unbuilt).
- UMS death log (`DeathWatch.watches`, 68e8ab9f3): on micro maps
  every warrior death dumps frame, unit and last orders
  (`DEATHWATCH ... died f...`); ladder keeps the quiet set
  (Observer/Shuttle/DT/Reaver). First-dragoon-dead frame judges kite
  and dress on 14v16 (baseline f194 pre-61, f213-257 post-61).
- 14v16 scoreboard: kite61 alone won 16/7 f1355 (first win ever)
  plus a 13/8 timeout; far-wait era 2W/4L but every fight
  competitive (11-14 kills vs 5-7 before) with the wait firing
  (up to 751 Waiting rows in the 16/11 win). Verdict: keep
  iterating, not reverting.
- Next: early formation dress (user plan) - dress far, wait ~5 tiles
  out, micro-nudges forward. PvP-only (vs Protoss reavers it feeds).
  BUILT (d686ff703): `MarchDress.holdFarWait` latch (fires at five
  out, holds through the creep, releases on contact or past six;
  warriors only, UMS mirrors, never under reavers) + creep 0.15
  tiles every 24f in `marchLegs`. Outranks memory duels (d5cdffa1d);
  race read off seen units (8055e88ed).
- 1Drag gun dither (open): Move/Attack orders alternate every ~10f,
  0 kills in 4 games. `AttackDelay.safeToIssueOrder` gates Agency
  orders, but the duel legs re-issue through `Commander`/maneuvers -
  verify the path before touching it.
