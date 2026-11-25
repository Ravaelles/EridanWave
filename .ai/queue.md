# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- 1DragWounded flee-first (owner order: survive first, shoot from the
  edge): bleeding hulls never start a volley inside 2.8 tiles, kite far
  instead of edging to zero, hold-to-shoot/volley/plant gated the same
  way, firing solution expires past aim range, tight kite reissue.
  StarEngine: flee-first outlives kiting both seats. Live: 68f deaths
  to 200-860f fighting retreats, no flap. NOT passing yet: needs
  winning volley cadence or corner awareness. Next: re-run the map.
  2026-11-25 reps: VICTORY 895f + DEFEAT 676f (1-1 variance, not a
  regression). Death came fleeing (Close/KiteAway straight from f546)
  with Guard/Move alternating the last 14f - the ledger does not count
  Guard, so its all-clear hid the death flap. Gap stuck 126px/final 0
  at the end: push/pathing anomaly, not a plant.
- Large-fleer resource clearance (owner order 2026-11-25, amended:
  fleeing large hulls keep 1.2 tiles off minerals and 3.1 off geysers
  - the 1.2 hug designated a dead spot against the geyser while the
  duel wanted to go behind it; the wide ring strides past instead):
  shared kernel `FleeMineralClearance` (mineral/geyser rings, push
  onto the away-ray with a bounded pass, settles), wired into the
  duel `kiteAway` flee spot and the `Retreat` waypoint veto (was:
  minerals-only adjacency). Live: clean volley cadence kept, deaths
  still land in the (12,4x) pocket - cornering, needs the exact
  geyser-hug game pointed out to verify the push there; corner
  awareness still open, no new code until the owner scopes it
  (pocket exit needs pathing, not a push).
- PvP anti-DT cannon placement (owner order: first cannon at the main
  choke, closest powered slot wins): choke pylon first (unlocks
  near-choke slots), then the choke cannon by proximity to the
  five-tile anchor, then the hall cannon at the front. Needs a ladder
  PvP with DTs: check tile distance to the choke + no ChokeCannonNoSlot.
- PvT wall chew (owner order: always attack the wall, never WallStand;
  under tanks only fresh shields trade): dragoons and melee (incl. DTs)
  take the shared depot when wound <= 13, spent hulls yield the frame
  to the retreat. Needs a ladder PvT: depot chewed instead of
  WallStand/Rallying flap, no spent-hull donations.
- PvZ SunkenTrap (owner order: a blade inside 7.1 tiles of a Sunken
  walks home): lone-melee cover gap in staticOutgunned never fired, so
  the zealot planted volleys against masonry till death - new
  FightRules leg retreats it before contact, plant and march both
  stand down. Needs a ladder PvZ: no KiteHold/March flap inside the
  guns, hull walks home instead.
- Cluster ForceRejoin (owner order: past 8 tiles from the centroid the
  hull walks home, victim or not): the drift band needs an empty slot,
  so a chase victim walked blades out alone to die - new latched leg
  (8 in, 7 out, assumed mirror of the leash band - confirm) clears the
  stale pick and forces the return; brawls, retreats, duels and
  explicit orders keep their frames. Needs a ladder game: no lone
  chase past 8 tiles, no ZealotLeash/AdvanceGate flap on the way out.
- Citadel-sweep sacrifice (owner order: the probe dies confirming the
  Citadel rather than fleeing blind): latched `CitadelSweep` (PvP
  ladder, scout probe in the enemy metro, Core known, Citadel chain
  unseen, base unswept) vetoes PreserveScout and the helpScout
  pull-home - the Search sweep owns every frame until answered,
  swept, walked out or dead. Needs a ladder PvP: probe must sweep
  instead of flap-retreating (no Retreat/helpScout interleaves in
  behavior.log), then Forge follows the confirm.
- Zealot rally vs rush flap (owner order: no stop-start crawl):
  BeRushingZealot yields to the latched rally - one owner per frame.
  Needs a ladder PvP opening to confirm full-speed rejoins.
- Suicide mission latch (owner: mission has priority, no split army):
  done, needs a ladder game where the ball breaks into an enemy main.
- DT unfinished-static pick (owner order): morphing cannon/turret
  first, always. Needs a ladder PvP vs forge expand.
- TargetPick kernel: the ladder's atom as data (row pick + stickiness),
  bestInStratum delegates, dead bestUn () removed. Next on demand:
  port ladder-shape tests onto rows.
- 4Drag_v_Zeal endgame (2026-11-25, 0-2, both timeouts at 4820f):
  NOT flee-gate DPS loss - Walk<->Hunt/Walk flips 5-6k per game from
  ~f1700 with zero visible enemies (hunt pulls to the memory, Walk
  pulls back, nobody commits). Early KiteHold<->KiteAway flicker every
  2-3f at first contact (f62-74 both games, ledger 12-24x) is the
  smaller tax. Fix direction (latch the hunt target / hunt over Walk
  while the memory stays huntable) borders parked stalled-duel fetch:
  proposal only, no code until the owner calls it.

## Needs a live game (code green, jar includes it)

- Wall-in doctrine (`details/wall-in.md`).
- Stalled-duel fetch: PARKED, touch only with a new owner order.
- DT leash, vulture cannons, bank gates, weak PvP hold, HT-vs-rejoin,
  artillery-vs-duels, carrier-vs-siege, natural crescent hold:
  specs in `details/pending-specs.md`, each needs its triggering game.
- 2Zeal hunt-side coordination (separate unit from the shared pick).
- 2v3 contact-kite scope: analysis delivered, awaiting owner call -
  plant-and-focus when surrounded/outnumbered, kite only separable
  duels (StarEngine: plant-and-focus wipes 3-0, contact-kite times out
  at 2/0).
- Progressing-duel ferry question (`details/sunkens.md`).

## Parked (do not touch)

- 3Drag, 1v4, 2Zeal lings, LingsHydra, P_v_Lurkers maps (unwinnable
  setups or separate units).
- Audit leftovers needing a game: KiteAway squad-mate gating, dogfight
  0.25 band, carrier-hunt and minedefuse release bands,
  escaping-toggle smoothing, SuicideMission path/euclidean +
  squadInZone latch, FormationHold Reaver hold, MeleeKiteAway
  dragoon-as-mate counting.
- Contain carve-out, C6 profiling, krasi0, Sunkens2 mission fallback,
  refresh-hop timing. Goon_v_DT + DT_v_Protoss unwinnable without
  detection: leave alone.
