# Kite doctrine (evidence + live read guide)

- Melee on top always kites while cooling (range abuse, not the
  volley race). Against ranged guns only the dying yield now
  (dedc29fdf): below 61 total HP (`KiteAway.kiteVsRangedHp`) the hull
  saves itself while cooling past 10, everything else holds its
  volleys. Mild-wound rotation is gone - in even mirrors it donated
  volleys (StarEngine `OutnumberedKiteSuite` 3/3: symmetric movement
  trades symmetric kills; focus-kite 12 kills vs focus-plant 14 down
  two guns).
- UMS death log (`DeathWatch.watches`, 68e8ab9f3): on micro maps
  every warrior death dumps frame, unit and last orders
  (`DEATHWATCH ... died f...`); ladder keeps the quiet set
  (Observer/Shuttle/DT/Reaver). First-dragoon-dead frame judges kite
  and dress on 14v16 (baseline f194 pre-61).
- Next: early formation dress (user plan) - dress far, wait ~5 tiles
  out, micro-nudges forward. PvP-only (vs Protoss reavers it feeds).
  NOT built yet.
