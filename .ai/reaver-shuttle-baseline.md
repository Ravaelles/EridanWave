# Reaver + Shuttle — current behavior baseline (research for the rebuild)

What the tree does today, per layer. Verified 2026-11-13 against
`f/phase-b` plus the R9/R10/R11/NoRetreat landings.

## Shuttle brain (`BeShuttle`, first claim wins, one owner of every move)

- Ditch hitchhikers: drop loaded non-Reaver/non-Templar without our squad; returns acted.
- Adopt passenger: pick up the neediest hailer (Reaver x1000, targeted-by-Scarab x100, same-squad x10, endangered x10, sojourning x2); plus newborn Reavers for attack/defense squads; returns acted.
- Scourge rear flee: loaded + Scourge within 12 tiles runs to the rear third of the cluster (nearest our base), never the median.
- Air-threat flee: anything that can hit us within 12 tiles, or a ranged threat whose reach plus the wound band (4 tiles fresh, up to 9 dying) touches us, pushes straight out (single) or off their centroid (many); survival outranks pickup and quests.
- Emergency pickup, then best quest (`ShuttleQuestCost`): a trip must save 24+ frames (plus drop cooldown for shooters); fighting passengers cost x1, safe ones x10.
- Park: with grounded passengers aboard, hover near their centroid offset from the nearest threat; retreat if 12+ tiles out with no safety frames.
- Await: empty Shuttle waits at the Robotics corner; retreats if unsafe unless a Reaver finishes sooner than the ride back.
- Wound geometry (`ShuttleFleeGeometry`): safety margin 64 skating / 160 standing, scaled tenfold by wounds; preemptive trigger when reach + band touches.

## Transport layer (`Transport.perform`, every frame per transport)

- Mission cargo accounting + abandon unsafe pickups for drop missions.
- Round-robin unload: one passenger per frame max, only on request (`wantsUnload` or mission `requestsUnload`), only onto walkable tiles.
- `Evacuate`: transport about to die (frames-to-live < 24 per loaded head) unloads one and retreats.
- `BeShuttle` drives; `BeFlier` skips transports entirely.

## Reaver brain (`ReaverAtlantisBrain`, first claim wins)

- Rescue a flying Scarab; never fight without the ride nearby.
- `ReaverEvacTanks`: vs Terran, fired within 72 frames, shields down 35+ or a sieged tank aiming within 10 tiles — hop back aboard (trades the shot, not the Reaver).
- `ReaverBraveStand`: shields 50+, unfired 96+ frames, foe within 8.5, armed, no sieged-tank cover, no static in face, not doomed/Scarab-targeted — hold and shoot instead of boarding.
- `ReaverBoardPressure`: board when doomed, Scarab-targeted, cooling+outranged+shotless under pressure, shields down 20+ with close pressure (38+ or shotless), freshly hit with shields down 18+, hiking far, or cooling with nothing to answer — unless `ReaverFireDiscipline.shotReady` bans boarding or only workers are near with shields above 70.
- `ReaverUnloadToFight`: ordered to reave with a scarab path and a clean veto — unload onto safe spots, brave drops, or contain+combat-near.
- `ReaverLandWithLead`: land next to the fighting lead passenger when the shot (or trainee) is near-ready and the spot is safe.
- `ReaverRefreshCooldown`: cooling under pressure hops to reset the 30-frame drop cooldown.
- `ReaverBackOffStatic`: step out of static reach instead of holding inside it.
- `ReaverHoldToFire`: loaded scarab + hittable target within 7.25 tiles holds position.
- Threat facts (`ReaverThreats`, single table `ReaverThreatRanges` + `ReaverTanks`): pressured <4, hold <7.25, engage <=8.5, tank aim <10, tank cover range+1, worker-only <8, foe in reach, scouting frames-since-fired/hit.

## Missions and contain

- `MissionReaverDrop`: sweep-ordered itinerary of enemy bases (clockwise latch), states Assembling/Travelling/Landing/Raiding/Evacuating/Escaping, per-transport raid progress, drop only with `DropSafety` (no unload within 8.1 tiles of an enemy anti-ground static).
- Unload gates: vetoed when 3+ ranged ground-hitters cover the landing (`ReaverUnloadVeto`); unsafe inside a wound-scaled enemy radius (`ReaverUnloadSafety`); brave drops override on rings (foes <=1 within 6, ranged <=2 within 8, tanks <=2 in theater 20, no statics within 8, both hulls 50+ shields, affordable shot) or on staleness (10 s dry, shields up, no melee within 3, tank gate).
- `ContainStatus` (tactic layer, per-frame memo): main cluster plus defensive buildings, or parked on the enemy natural choke within 20 tiles against anti-ground statics without overwhelming force (score <300) — holds the contain spot and roster.
- Drop passengers fight through (`commitWhileRaiding`); UMS duelists commit; suicide missions commit in the enemy base.

## Known gaps driving the rebuild

- Two brains, two owners, no shared status: the Shuttle does not know whether the pair raids a base or supports the army, and the Reaver does not know either — each decides alone every frame.
- Reavers join the army too rarely; no oldest-raids-base / rest-follow-army split exists.
- Boarding is allowed too eagerly (any pressure + cooldown), including right after landing.
- Landing vs Zerg statics is timid: no explicit "land and kill it" rule inside 12 tiles with a safety margin.
- No shared danger rule: below 50% life on either hull the pair should default to the army, not the raid.
