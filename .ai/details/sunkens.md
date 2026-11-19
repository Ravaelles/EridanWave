# Sunkens / reaver pair

- Sunkens2 is an ammo wall, not micro: the hull starts 5 scarabs,
  fires exactly 5, builds none (0 minerals, no income). ~500 damage
  cannot clear 3 sunkens plus lings (~970 HP). Needs a map fix
  (starting scarabs or minerals). Do NOT chase in code.
- Sunkens3 is the winnable control: grinds 8-11 kills on foot but
  never ferries a progressing duel (no board trigger: E2 needs a
  stall and the pin bypass needs one too).

Shipped (validate on Sunkens3):

- Static hold-to-fire: inside 7.25 with scarabs and the longer gun,
  hold every 8th frame and re-attack between holds (release 7.75,
  per-hull latch). Outranged guns keep the back-off.
- Fresh hulls (shieldWound <= 40, scarabs, 6s quiet) stand under
  statics: walk in, shoot, withdraw after - not before.
- Pair drop ring 8.5 out from the nearest static (never inside 7.5,
  4-tile mobile margin, nearest our side); the unload fires on
  arrival.
- Baseless pairs bond blind (nearest hull regardless of squad);
  arrival legs use movePrecise (no 288px flyer overshoot).
- Stalled-duel fetch: 6s quiet while holding a static inside the gun
  boards despite the quiet-crowd pin. Closing hulls approach instead
  of boarding (in-gun added).
- E1: past 7.25 with scarabs, close through instead of standing
  (standing at engine range is how hulls die unfired).
- E2: foe static plus ride within 5 plus 6s quiet boards the waiting
  ride. Mobiles keep duel-first; no ride in reach keeps dueling.

OWNER DECISION pending: ferry a winning-but-slow duel, or keep
duel-first and win by grinding? No code before the call.
