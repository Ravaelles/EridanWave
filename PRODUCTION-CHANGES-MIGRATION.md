# Production changes migration plan

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
| `Gameplans.Protoss.PvZ.policy.ReaverGas.allowsReaver` | Gas gate (reavers vs gas stockpile) | Audit: fold if it is a veto, leave if it is allocation |
| `Gameplans.Protoss.PvP.PvPWeakHold` | Expand gate (hold natural, no second base) | Audit: production-adjacent, register only the veto half |
| `Gameplans.Protoss.PvZ.policy.{GasBank, RecoveryGas, ...}` | Allocation, not veto | Out of scope; the sink vetoes, it never budgets |
| `Macro.Actions.ShouldCancelBase` | Cancel axis, not produce axis | Out of scope; stays with its owner |

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
