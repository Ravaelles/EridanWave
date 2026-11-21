# Sunkens / reaver pair

- Map fixed by the owner: the Reaver starts in range to fight. Lurker
  standoff (7.05 tiles with >= 1 scarab) and Reaver special picks
  (nearest detected Lurker vs Zerg, Dark Templar vs Protoss) built.
- Sunkens2 is an ammo wall, not micro: 5 starting scarabs, none built
  (0 minerals) - ~500 damage cannot clear ~970 HP of sunkens + lings.
  Needs a map fix. Do NOT chase in code.
- Sunkens3 is the winnable control: grinds 8-11 kills on foot but never
  ferries a progressing duel (no board trigger). Open: hold-at-range
  rule for picked lurkers (chase-into-cover dies 8/7), or a map-side
  answer - owner call.

Shipped (validate on Sunkens3):

- Static hold-to-fire: inside 7.25 with scarabs and the longer gun,
  hold every 8th frame and re-attack between (release 7.75, per-hull
  latch). Outranged guns keep the back-off; past 7.25 close through
  (E1).
- Fresh hulls (shieldWound <= 40, scarabs, 6s quiet) stand under
  statics: walk in, shoot, withdraw after - not before.
- Pair drop ring 8.5 out from the nearest static (never inside 7.5,
  4-tile mobile margin, nearest our side); unload fires on arrival.
  Baseless pairs bond blind; arrival legs use movePrecise.
- Stalled-duel fetch: 6s quiet while holding a static inside the gun
  boards despite the quiet-crowd pin (E2: foe static + ride within 5 +
  6s quiet boards the waiting ride). Closing hulls approach instead of
  boarding.
