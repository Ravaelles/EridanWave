# Request queue (living list, newest last)

History lives in git log. Run a game, check the named behavior in
trace/`probe.txt`/`errors.json`, delete the line.
Details live in `details/` - the queue keeps one line per item.

## Now

- Ladder validation (S2 + audit + W1 jars, ~15 games: BetaStar, Crona,
  Terminus, Hao Pan series): every loss diagnosed as gameplan or
  composition gap (greedy expand into 9pool, flood vs 1-base, PvP
  mass-goon, carrier out-comp) or unwinnable forts - no mechanism
  found. Post-fix Crona shows zero ChokeHold and zero HoldingDuel
  rows: the lone holder no longer commits, the line marches, kites
  and retreats instead of donating.
- UMS: toggle-jar full batch 9W/13L/3NA (structural walls unchanged:
  Sunkens ammo, DT detection, mop-up timeouts); post-W1 duel maps
  green (1Drag/16Drag/ZealDrag/Reav_v_Drag wins, TanksVult kills
  rising).
- ZealDrag line doctrine (owner order, 2026-11-21): dress early,
  wait, inch forward - implemented via the existing dress machinery
  (far-wait + FormationApproach 0.1 steps): Dress outranks Duel while
  the far-wait holds, advance pauses while Dress is held, and healthy
  hulls with nothing inside two tiles never kite
  (`KiteAwayBehavior.shouldKite` gate: shieldWound==0 + no live threat
  within 2 tiles). Validated on ZealDrag_v_ZealDrag (line dresses from
  f28, first blood f203 vs f162, 3W/1L k9/l8 k9/l7 k9/l8 k4/l9 vs
  0W/2L before); no regression on 1DragWounded (V k1/l0) and 16Drag
  (V 16/12). Ladder validation pending.
- 1DragWounded MECHANISM FOUND + FIXED (2026-11-21): the losses were
  not a kite-tuning gap - the W1 duel legs never ran (legacy
  `BeDragoonVsSlowlot` issued every execute, unit never `ready`, Guard
  auto-acquire only). Fix: the legacy dance yields where the shipped
  kite kernel fires; chains show Close/KiteAway again, wins return
  (~1/3 in 15 post-fix games vs ~1/10 before; healthy 1Drag and 16Drag
  still win, ZealDrag mirror flips as before). Residual: fast early
  deaths + k0/l0 timeouts (kill too slow) - next is hold/approach
  tuning or accepting the map's variance.
- Jar: W1 + shouldRun unification (`strings`-verified, slot md5
  matches target).
- Kite discipline (todo #1, owner order: never yield ground for free):
  pre-gate plant + return-capped yield + melee-gated near-dead flight,
  judged by VolleyWaste. Validated on UMS: 16Drag waste 14-17f down to
  ~1-8f with wins held (16/12, 16/8, 16/10, 16/9, 16/6, 16/14);
  ZealDrag recovers to 8/5, 9/8, 9/6, 9/5, 9/1, 9/3 after the
  shared-margin fix (plant and cap read one squad-wide melee gate
  now); fresh loaded hulls hold fire, calm/pack hold for PvP, Zerg
  fresh shields hold, no TvT kite; the terminal shoots loaded hittable
  guns (no silent duels) and march moves never cancel volleys.
  All-time rates confirm no regression: ZealDrag 48W/99, 16Drag
  23W/41, 1Drag 25W/61, Wounded 42W/126 (its historic ~1/3),
  3Zeal 15W/29, 1Zeal_v_2Ling 23W/26;
  4+3Zeal always loses 0/5 (melee mirror, separate unit).
- Opening stand (owner order, ZealDrag_v_ZealDrag 5x reported: no
  formation, Retreat + AttackMove over units): the shipped 48f
  holdOpening never fired - the far-wait blind gate (no seen
  Protoss) sat before it, the f0 retreat verdict marched everything
  home past it, and RetreatStage walked (it never checked advance).
  Fixed three ways, verdict untouched (ladder-safe): the stand fires
  blind from frame zero, outranks the retreat MARCH (hold in place,
  WaitingForPack), RetreatStage stands down in the window, and the
  march credits DressHold so the overlay names the hold. Duel was
  already quiet (technique advance gate). StarEngine mirror pins it:
  zero displacement f0-47, all Hold at 1.5s, avg ground distance
  885->674->0, pack shape held (observers excluded). Live: zero
  Retreat/AttackMove acts f0-60, DressHold from f3, zero Formation
  rows f1-47 (the line stands), win 9/1 with zero waste.
- Opening hold, final proof (live positions): all 9 units
  pixel-identical f0-f48 under HoldPosition, zero Retreat/
  AttackMove/Hunt acts f0-60, DressHold from f3, wins 9/7 9/5
  9/4 with ~0 waste. The silent attacker was the Idle-list Attack
  action (acquireTarget picks without acting, closeIn attack-moves),
  gated for the window; the march holds with a forced HoldPosition
  (the skip-if-stationary Commander.hold never countermanded map
  orders); Follow and the dance footwork stand down for the window.
- VolleyDiscipline (owner order: one verdict, no conflicts):
  a single shared kernel (shouldVolley) now owns every loaded
  volley - dance rung, charge VolleyHold, terminal, scavenge,
  dress steps, dying volley all read it; validated green
  (StarEngine composition: shipped order kills both seats with
  zero wasted moves; UMS waste 0 on ZealDrag/1v1Zeal/1Zeal).
- Opening stand, final cut (owner: still no HoldLine, units move
  from frame one, Retreat+AttackMove overhead): the silent mover was
  the Idle-list Attack action - acquireTarget picks the far seen foe
  without acting, Commander.attack closes through closeIn as
  attackMove, and the march never countermands (all its movers are
  gated). Gated Attack on the window. Then the forced march hold did
  not stick: nothing in the unit phase countermands, so the last
  writer wins - added forced HoldPosition in the march hold plus a
  Fight-top hold for dragoons and zealots (reavers and other
  doctrines keep their openers). Live proof: all 9 units
  pixel-identical f0-f48 under HoldPosition orders, enemy closes
  15->12.7 tiles into the held line, then the duel advances
  normally (win 9/6, waste 5f).
  Autonomous night session (owner asleep, ~26 games): waste ZERO on
  every map the ledger can see (3Drag, 1v4, 2Zeal, 1v1Zeal streak,
  1v1Ling, 2v2Drag, 10Zeal, ZealDrag, 16Drag, 1v1Drag, Wounded).
  Remaining losses are numbers, not discipline: 3Drag 0W/13L (never
  won - unwinnable as set up, park), 1Drag_v_4Zeal 0W/3L (1v4,
  park), 2Zeal 5W/37L (5/2 timeouts - uncatchable speedlings, hunt
  tempo is the documented separate unit, park), LingsHydra 5W/16L
  (retreat verdict territory, park), 15Drag 12W/15L (near-parity
  coin), 10Zeal 14W/12L (coin - pack brawls together either way),
  1v1Drag 41% (f0 retreat symmetric, harmless delay, needs the
  ladder retreat sink for more).
- Coverage sweep (same night): new wins with clean ledgers -
  1v1Ling, 1v1Hydra, 1ZealDead, 1ZealZoo, 2v2Drag, 1v2LingUpg,
  2v3HydraUpg, ZealDrag_v_Lings (32/4), LingsHydra2 (43/14),
  Drag_v_Hydra (15/4), Drag_v_Hydra&Lurker (13/4), P_v_Lurkers2
  (3/6 objective win), Lings2_easy (26/3); close losses 1v2Hydra
  1/1, Hydra&Ling 1/1, HydraUpg 3Drag 1/3, Cann_v_HydraUpg 2/6
  (upgrade disadvantage + statics); ZealDrag2 8/9 (mirror coin).
  P_v_Lurkers (not 2) errors without fighting - detection-gated
  like the DT maps, park.
- 2v3 counterfactual (StarEngine, MirrorFocusSuite): forced focus and
  forced plant-and-chase BOTH wipe 3 zealots with 2 dragoons (3-0 and
  3-0 kills), while live contact-kiting times out at 2/0. Kiting for
  safety extends duels past the clock on timeout maps. PROPOSAL (no
  code before owner call): scope the contact kite to separable duels
  (kite when the pressers can be re-engaged), plant-and-focus when
  surrounded or outnumbered - stand, share the weakest, trade up.
  Dance spends loaded volleys before footwork (2026-11-22): the W1
  dance repositioned with a loaded gun and a hittable target (Wounded
  waste 28f, starvation with it); a loaded engine-confirmed shot now
  fires first, an aiming hull holds instead of walking. Verified live:
  Wounded waste 28f to none twice, 1Drag_v_1Zeal waste none; wins
  unchanged (coin-flip duels).
- Contact kite (owner order, 1Drag_v_1Zeal post-mortem): cooling
  ranged hulls yield melee inside 2.7 tiles, loaded guns always shoot
  first (every cooldown cycle, zero wasted frames). Fixes that landed
  it: the paced step capped by wound want stood in place (floor is now
  the wound-independent walk-back), and the standing attack order
  vetoed every kite move (contact kites interrupt explicitly). The
  ledger judges end-of-frame orders now - the pre-dispatch read
  counted every volley fired after a step as waste. Verified live:
  1Drag_v_1Zeal from 0/1, 0/0 to 5W/6 with movement on the census;
  ZealDrag wins hold (9/7, 9/3); no other map regressed.

## Needs a live game (code green, jar includes it)

- Wall-in doctrine. Needs a PvT wall-in (`details/wall-in.md`).
  PvP hold gates (9 tiles, shields>=20, no unit pressure, retreat
  priority) need the next ladder PvP wall-in - owner watches.
- Stalled-duel fetch. PARKED by owner (2026-11-22): the pair is broken
  beyond measure, no more Reaver testing. Sunkens3 stays a grind map;
  the BoardingVote agreement + BoardStalled leg stay in the tree
  suite-pinned (the vote fires live, the leg fired 6f once) but
  unproven - touch only with a new owner order. Terran maps
  (Vultures_v_Dragoons and the whole terran/ dir) out of scope:
  Protoss units only - Zealots and Dragoons.
- DT leash, vulture cannons, bank gates, weak-PvP hold,
  HT-vs-rejoin, artillery-vs-duels. Needs the triggering games
  (`details/pending-specs.md`).
- 2Zeal shared pick shipped: weakest enemy two pack blades can both
  reach outranks individual picks (`MeleeFocus`, rule 19). Validated
  firing live (synchronized picks on the same frames), kills up;
  2Zeal_v_LingUp wins still need hunt-side coordination (separate
  unit).

## Owner decisions (no code before the call)

- Mirror focus gate taken (4->3 everywhere, `FocusFire.minShooters`):
  validated on drag mirrors. If small lines still spread, the next
  suspect is reach (wings out of shared-target range), not the count.
- Progressing-duel ferry question (`details/sunkens.md`).
- Carrier vs sieged tank, natural crescent hold
  (`details/pending-specs.md`).

## Parked (do not touch)

- Contain carve-out, C6 profiling, krasi0, Sunkens2 mission fallback,
  refresh-hop timing. Goon_v_DT + DT_v_Protoss unwinnable without
  detection: leave alone.
- Audit 2026-11 follow-ups needing a game (reference-frame sweep;
  fixed: HoldToShoot own-range + loaded-hold, TankHold tank-range
  + reply-range leg, ArtilleryKeepOff 13.1, TankHug proactive leave,
  kite/abuse/carrier/minedefuse ghost gates, SlotDiscipline warrior
  feed, ArmedCordon edge feed, nearestArmed canAttack filter,
  Scout hold edge, ZealotPack edge feed, Reposition urgency,
  KiteTo air frame, Suicide ghost charge): KiteAway squad-mate margin
  gating (pack coherence vs back-line kite), dogfight 0.25 band,
  carrier-hunt and minedefuse release bands, escaping-toggle
  smoothing, SuicideMission path/euclidean + squadInZone latch,
  FormationHold Reaver hold, MeleeKiteAway dragoon-as-mate counting.
