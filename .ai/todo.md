# Targeting parity with Atlantis — test roadmap

Our picker is `Micro/Targeting/Target.scala` (strata + `TargetScoring`);
Atlantis picks through the ordered ladder in
`Atlantis/src/atlantis/combat/targeting/TargetLadder.java`
(chewer, keepCurrent, weakestInRange, sticky, shared, cover, nearest,
breach) with the tiers in `src-old/.../battle/targeting/generic/
ATargetingImportant.java`. No `UnitInfo` stub harness exists, so the
strata themselves stay untested; what we pin instead, per rule:

- Pure kernels beside the caller with a `*Suite` next to code
  (`TargetScoring.damageForSize`, `creepColonyBonus` — done).
- Game-coupled strata stay thin and delegate to the kernels.

## Atlantis tests to port (owner direction NEXT #184/#185)

- `src/tests/unit/TargetLadderTest.java` — ladder order is explicit and
  stable; chewer beats keep-current beats weakest; no silent reordering.
  Ours: pin any reorder of `Target.best`/`defaultTargets` strata the
  same way once a stub harness allows it.
- `src/tests/unit/AcquireTargetTest.java` — top-of-ladder behavior.
- `src/tests/unit/MeleeChewerTargetTest.java` — weakest-in-range over
  the committed target when the shot is ready; keep-current while
  cooling. Ours: `acceleratesDemise` strata in `Target.best`.
- `src/tests/unit/TargetPlacementNearTest.java`,
  `UndetectedTargetTest.java` — geometry and cloaked handling.
- `ChaseVetoTest.java`, `ChaseVetoFollowsDecidedPostureTest.java`,
  `FocusFireTest.java` — chase discipline and focus.

## Creep Colony acceptance (the repeated miss)

Atlantis ranks Creep Colonies as their own tier (`Targ_CB_CreepC`:
in 8 radius, `C5d`: buildings in 7) between combat buildings and
ordinary buildings. Ours lifts them in `TargetScoring.apply` via
`creepColonyBonus` (morphing 14, idle 8, below fighters at combat
12). Verify in a game:

- Army finishing other buildings then engages a standing Creep Colony
  instead of idling past it.
- A morphing colony (future Sunken) is picked over an idle one.
- Fighters still outrank colonies; Reavers still skip non-combat
  buildings entirely (see `TargetFilterReaver`).
