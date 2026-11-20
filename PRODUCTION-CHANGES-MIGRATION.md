# Production changes migration plan

Status: Step 0 done (`f1f102aa0`, audit verdicts in the table below);
Step 1 done (`9c05a9f1c`, `starengine.production` harness +
`ProductionParitySuite` 6/6 green); Step 2 done (`6698526c8` sink +
`ProductionPolicySuite` 6/6 green, `a96460149` emergence scenarios
3/3 green, full base suite 1122 green, jar rebuilt with the sink).
Next: Step 3 live proof (ladder PvP Robo game). UMS no-regression
check on the sink jar done: full batch 7W/17L, then all 5 re-run
losses (3Zeal, 1Zeal, 16Drag, ZealDrag, 3Drag) flipped to VICTORY
on the identical jar - batch variance on known flippers, not a sink
regression (production is inert on baseless UMS maps; vetoes can
only suppress requests that cannot execute there). 10Zeal sits
inside its historical flip distribution (7W/5L all-time).

Goal: every production decision passes through one policy sink that
communicates intent and reason, so a tech gate cannot be bypassed by a
sibling production path the way the first-Reaver gate was
(`DragoonsBeforeReavers` lived in one gameplan method while
`trainRoboUnits`, `PvPOpeningVsFE` and `PvPLateGame` produced around
it). Same shape as the retreat verdict bus, for production. Veto by
*what*, not by *who*.

Precedent: the Shuttle veto already lives in the mechanical sink
(`MacroActions.once`/`pump` early-return through `canProduceShuttle`,
pure kernel `CanProduceShuttle` + suite). This plan generalizes that
shape, it does not invent a new one.

## Step 0 - audit the existing policy classes

Map every `CanProduce*` / `Should*` / `*Gas*` / `skip*` object to one
of three dispositions before moving anything. No renames for their
own sake; working names stay.

| Class | Shape | Disposition |
|---|---|---|
| `Macro.Actions.CanProduceShuttle` | `CanProduceX` (absolute veto: observers, short line, score) | Register as-is; the template |
| `Gameplans.Protoss.PvP.DragoonsBeforeReavers` | `ShouldProduceYBeforeX` (backbone before first hull) | First registered rule; its four call-site reads dissolve |
| `Gameplans.Protoss.PvP.MirrorGoonTech.skipReavers` | `ShouldProduceYBeforeX` (gates/goons/range before reactive Bay) | Register; reactive-tech reaction stays a separate rule from committed play |
| `Gameplans.Protoss.PvZ.policy.ReaverGas.allowsReaver` | Gas-paced veto (1st always, 2nd at 250, rest at 350) | Register; same shape as `GasBank`, different counter |
| `Gameplans.Protoss.PvZ.policy.GasBank.allowsDragoon` | Gas-paced veto (10th at 250, past 16 at 360, big army at 210) | Register |
| `Gameplans.Protoss.PvP.PvPWeakHold.holdNatural` | Expand veto (no second Nexus while weak) | Register the veto half only; the hold itself stays a posture rule |
| `Gameplans.Protoss.PvZ.policy.TemplarArchivesOrRoboticsFacility` | `ShouldProduceYBeforeX` (DTs before first Robo) | Register; ordering between techs, not counts |
| `Gameplans.Protoss.PvZ.policy.StormResearch.shouldResearch` | Research veto (committed casters + bank) | Register second wave; policy covers `Buildable`, units first |
| `Gameplans.Protoss.PvZ.policy.DefenseWithoutDragoons` | Posture (defend without Dragoons) | Out: answers fight-or-hold, never build-or-not |
| `Gameplans.Protoss.PvZ.policy.EarlyPressure.mayPush` | March (push at 3 warriors) | Out: answers push-or-hold, never build-or-not |
| `Gameplans.Protoss.PvZ.policy.RecoveryGas.shouldGas` | Allocation (take gas after failed all-in) | Out: moves workers, never vetoes a request |
| `Macro.Actions.ShouldCancelBase` | Cancel axis (kill the morphing base for detection) | Out: kills in-flight, the sink only gates new requests |

Rule of the audit: the sink answers "may this be requested at all".
How many, in what order, and when to cancel stay with the owners
(`pumpShuttleAndReavers` counts, `PvPIdeas` cancels, harass timing).
A class that mixes veto with counts or timing is split, not moved.

## Step 1 - working production in StarEngine (first)

Charter guard: StarEngine stays micro-only by design (no economy in
shipped code). The harness lives in `tests-starengine` only, as an
isolated `starengine.production` module the fight engine never sees:
pilots issue build orders, the engine advances busy timers, bodies
emerge. No workers, no income sim - the scenario sets stockpiles.

Model the three facts the owner named, nothing more:

- free gateway: an idle production structure of the right class;
  a busy one queues, it never overlaps;
- powered: Protoss structures produce only under power;
- resources: mineral/gas stockpiles cover cost, else the order waits.

Numbers come from the real `UnitClass` tables (cost, build frames),
the way `Body` already reads range and damage. Semantics mirror
today's requests: `once` (`BuildOnce`: exactly N ever), `pump`
(continuous up to its maximum), `get(N)` (want N total).

Parity scenarios (each must pass before and after Step 2):

- funded + free + powered gateway with a standing pump order emits
  units at table rate;
- busy gateway queues instead of overlapping;
- unpowered gateway emits nothing;
- broke stockpile waits instead of emitting;
- `BuildOnce(1)` emits exactly one hull ever.

These scenarios are the migration's safety net: after Step 2 they
must produce byte-identical order streams with the policy allowing,
and silence with the policy vetoing.

## Step 2 - the ProductionPolicy sink (second)

One policy object consulted inside the existing mechanical sink, next
to the Shuttle lines:

```scala
object ProductionPolicy {
  sealed trait VetoReason // logged centrally, the enum is the taxonomy
  def allow(unit: UnitClass): Option[VetoReason] // None = produce
}
```

- `allow` folds the registered rules; the first `Some(reason)`
  wins and rides the existing status/probe channel (the reason
  channel already exists - `ShuttleVeto`, `DragoonsBeforeReavers`
  - the enum only names it).
- Rules read game state internally (matchup included, precedent:
  `enemyIsProtoss` inside `CanProduceShuttle`), so per-matchup
  doctrines stay whole instead of splitting by caller.
- Committed play bypasses explicitly: vetoes gate the FIRST hull
  or structure; reinforcement (`have` ever) flows. The scattered
  `||= have(...)` ratchets move into the policy as one stated rule.
- Gate new requests only; in-flight cancellation stays with the
  owners (today's `PvPIdeas` cancels). The sink never kills.
- `once`/`pump`/`get(RequestUnit)` consult it centrally (the
  Shuttle `if (... ) return` lines are the template). Call sites
  go back to dumb pumps; `DragoonsBeforeReavers` becomes rule #1
  and its four call-site reads dissolve.

Migration rhythm, one rule per commit: base suite pins the kernel,
a StarEngine emergence scenario pins the veto and the resume
(14 goons + no Cannon: silence; 15th completes: Bay + Reaver flow),
full suite green, jar rebuilt.

## Step 3 - validation

- `scripts/tests.sh` green, writers gate pinned, StarEngine parity
  scenarios green.
- Live proof is a ladder game (production is macro - UMS micro maps
  cannot prove it): a PvP Robo game must show the first Reaver only
  behind the backbone or a scouted Cannon, in trace and in
  `probe.txt`. DT-wall maps stay structurally red and are not
  validation targets.

## Risks

- Per-matchup doctrines differ (PvP backbone vs PvZ gas vs PvR
  ferry): the policy reads the matchup, it does not average rules.
- Scheduler interplay: the sink sees requests, not construction;
  a veto that flips mid-build must not strand resources - gate
  requests, never kill in-flight.
- Supply blocks, autosupply and `RequireEssentials` are untouched;
  the policy answers permission, never logistics.
