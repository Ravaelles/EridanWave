# Kite doctrine (evidence + live read guide)

- Melee on top always kites while cooling (range abuse, not the
  volley race). Against ranged guns only the dying yield now
  (dedc29fdf): below 61 total HP (`KiteAway.kiteVsRangedHp`) the hull
  saves itself while cooling past 10, everything else holds its
  volleys. Mild-wound rotation is gone - in even mirrors it donated
  volleys (StarEngine `OutnumberedKiteSuite` 3/3: symmetric movement
  trades symmetric kills; focus-kite 12 kills vs focus-plant 14 down
  two guns).
- Melee kites slow lings (27149e81c), and speedlings only as an
  intact pack (pack-gate): alone a blade cannot outrun them, so it
  stands; balled the line kites together (`MeleeKiteAway.packKiteTiles`
  1.0, seam reads nearest zealot mate). Validating on 2Zeal.
- UMS death log (`DeathWatch.watches`, 68e8ab9f3): on micro maps
  every warrior death dumps frame, unit and last orders
  (`DEATHWATCH ... died f...`); ladder keeps the quiet set
  (Observer/Shuttle/DT/Reaver). First-dragoon-dead frame judges kite
  and dress on 14v16 (baseline f194 pre-61, f213-257 post-61).
- 14v16 scoreboard: kite61 alone won 16/7 f1355 (first win ever)
  plus a 13/8 timeout; far-wait era 2W/5L but every fight
  competitive (11-16 kills vs 5-7 before) with the wait firing
  (up to 751 Waiting rows in the 16/11 win). Verdict: keep
  iterating, not reverting.
- Easy round green (slowling jar): 1DragZeal, 1DragLing, 1Zeal x3,
  1DragHydra, 3Zeal, 2DragUpg, 10Zeal, 1DragWounded all VICTORY;
  single losses re-ran to wins (variance, not the veto).
  1Drag post-kite61 won twice (was 0/4): the mild mill was the
  dither.
- Far dress-and-wait (d686ff703): `MarchDress.holdFarWait` latch
  (fires at five out, holds through the creep, releases on contact
  or past six; warriors only, UMS mirrors, never under reavers) +
  creep 0.15 tiles every 24f in `marchLegs`. Outranks memory duels
  (d5cdffa1d); race read off seen units (8055e88ed).
- Zealot ball (built 574d2bb11, REVERTED e71d56d09): zero fires in
  four games - packs already hold shoulder-to-shoulder.
- UMS endgame (`NoRetreat.umsEndgame`, fe2e62cae): past ten seconds
  a unit with nothing in range holds instead of withdrawing - duels
  keep the skim vote. Validated: LingsHydra timeouts gone (W f1619,
  W f1322); 14v16 recovered (W f1322) after the blanket version
  collapsed it 0-3 fast.
- 2Zeal parked hard (upgraded 2v6: kills 2-6 across runs, 1W/7L):
  pack-kite is the ceiling without a melee shared pick (declined).
