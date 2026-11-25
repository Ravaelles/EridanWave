# PurpleWave vs Stardust: who gets to move the unit

Companion to `docs/stardust-combat.md` (that note is about *moving
together*: clusters, boids, arcs - read it first). This note is about
*authority*: what structurally stops two classes from ordering one unit
in one frame, and what we should steal to end the flap family
(VolleyHold/March, Retreat/helpScout/ForceScout, ZealotLeash/AdvanceGate,
Retreat/CorsairHunt/CorsairFleeRanged - same shape every time).

All Stardust paths below are relative to `/sc-ai/Stardust/src`.
All ours are relative to the repo root.

## 1. The two authority models

**Stardust: exclusive ownership, one activity, one hardware fuse.**
`StardustAIModule.cpp:238-272` runs a fixed pipeline per frame; combat
ends in `General::issueOrders` (`General/General.cpp:174-180`) which is
`for squad: squad->execute()`, and `Squad::execute`
(`General/Squad.cpp:304-315`) runs each cluster once. A unit belongs to
exactly one play/squad (`Strategist.cpp:189-241,527-589`) and one
`UnitCluster`; the cluster picks exactly one `Activity` and the micro
loop calls exactly one action per unit
(`General/UnitCluster/Tactics/Attack.cpp:102-135`). Every BWAPI call
in the codebase lives in `MyUnitImpl` wrappers
(`Units/MyUnit/MyUnit_Orders.cpp:6-352`); callers only say
`moveTo()/attack()/gather()`. `moveTo()` merely queues a `MoveCommand`
(`Units/MyUnit/MyUnit_Move.cpp:41-51`); the queue flushes once, at frame
end (`Units::issueOrders`, `Units/Units.cpp:1309-1315`).

**EridanWave: peer behaviors, first-claims chains, readiness.**
`Idle.actions` (`src/Micro/Actions/Idle.scala:32-59`) runs every action
in order; each `Action.act` (`src/Micro/Actions/Action.scala:49-64`)
checks `unit.ready` and runs `perform`, which may issue a BWAPI order
through `Commander.*` and flip the unit `unready`. Later actions then
skip - *if* they check readiness and *if* the earlier writer actually
issued. Nothing stops two writers from each voting a destination,
forceTo, or toAttack in one frame; `unit.agent.act(name)` credits
*accumulate*, so behavior.log shows `ForceScout/Retreat` and we argue
afterwards about who moved the unit. `Commander.move` dedups only
near-identical reissues (32 px); anything else re-issues.

## 2. The five Stardust mechanisms that make conflicts impossible

1. **The fuse.** `issuedOrderThisFrame`, set on every successful BWAPI
   call and checked at the top of every order method
   (`MyUnit_Orders.cpp:8,48,72,...`), reset in `MyUnit::update`
   (`Units/MyUnit/MyUnit.cpp:127`). A second order in the same frame is
   not a flap - it is a logged bug: `DUPLICATE ORDER: <unit>: <order>`
   in every method (`:10,50,74,102,147,...`). Our `ready/unready`
   (`CommanderCore.scala:43-64`) is the embryo, but it is advisory:
   writers can vote destinations and labels without issuing, and no log
   fires when two writers both act.
2. **Queued intent, single flush.** `moveTo()` overwrites one
   `moveCommand`; `issueMoveOrders` no-ops if the fuse is set
   (`MyUnit_Move.cpp:41-55`). Decision (where) and issuance (the BWAPI
   call) are separate moments. Our `forceTo` + `decision` + `move()`
   triple is the same idea split across three writers instead of one.
3. **Worker reservation.** Combat/scout/build consumers must
   `Workers::reserveWorker()` - the worker leaves the mineral/gas sets
   (`Workers/Workers.cpp:1162-1172`, e.g. `WorkerDefenseSquad.cpp:79`,
   `EarlyGameWorkerScout.cpp:82`) and the mining loop never touches a
   `Job::Reserved` worker (`:759-1036`); `releaseWorker` hands it back
   (`:1174-1180`). No gate flickers, because the two owners never meet
   on one frame. Our scout/helpScout/Retreat flap is exactly the
   missing reservation: three gates voting on one probe.
4. **One activity per cluster.** The cluster's `Activity`
   (`General/UnitCluster.h:13-35`: attack/regroup/move/flee/...) is
   chosen once; micro obeys it. Our squad has activities too, but the
   engage pipeline lets AdvanceGate (march) and Close (duel) both move
   one zealot on alternate frames.
5. **No holdPosition, no hope.** There is no `.holdPosition(` in the
   codebase; `stop()` is unstick-only (`MyUnit.cpp:267-295`,
   `MyUnit_Move.cpp:474-513`). Standing your ground is an explicit
   attack order. Our VolleyHold leans on BW auto-acquire (hold and pray
   the gun fires) - the PvZ zero-tiles non-attack is what that buys.
   Add dedup windows (no identical `Attack_Unit`/near-identical `Move`
   inside the latency window, `:14-28,54-61`) and `isReady` (never touch
   a unit mid-attack or on alternate frames at >250 supply, `:255-265`).

## 3. What to adopt, cheapest first

All five map onto rules we already claim (9: one writer, 11: one
verdict, 15: ledger diagnostics) - they are the enforcement we never
built.

1. **The fuse (P0, one afternoon).** A per-frame issued-order flag on
   the unit, checked in *every* `Commander.*` issuer
   (`CommanderMove/M CommanderEngage/M CommanderCore`), plus a
   `DUPLICATE ORDER` entry in the error ledger when a second issuer
   fires (their log line, our `ErrorLedger` + `FlapLedger` tripwire).
   Conflicts become impossible *and* visible in the same change; every
   later flap lands on one writer instead of a pile of labels.
2. **Explicit attack over hold-and-hope (P1, in flight).** Extend the
   `NearestShot.take` shape (already our shared tail) to every
   hold-to-shoot leg: a hull with something hittable holds an *attack*
   order, never a bare hold. Kills the PvZ zero-tiles class outright.
3. **Worker reservation (P2, one object).** A scout/builder/combat
   worker checks out of `Gather` (mirroring `reserveWorker`) instead of
   duelling it through `helpScout`-style gates; the job returns it on
   release. Deletes the whole Retreat/helpScout/ForceScout family at
   the root instead of gating each leg.
4. **One engage activity per frame (P3, grows MotionVerdict).** Let the
   cluster verdict own march-vs-duel the way it already owns
   hold-vs-kite: `AdvanceGate` and `Close` never both move one hull.
   Their `UnitCluster.h`-style activity is the model; our latch +
   release-band idiom is already the mechanism.
5. **isReady everywhere (P4, generalizes two embryos).** Our
   firing-solution veto and the 32 px kite dedup are special cases of
   "never touch a busy unit". Promote to the rule Stardust states
   outright: no re-issue while the shot is in flight, latency-scaled
   deadband on every re-issue, not just kites.

Explicitly *not* proposed: copying their squad/cluster membership
(per `stardust-combat.md` §7, our recruiter already answers that), or
their C++-shaped grid/pathing movers. Steal the authority, keep the
plumbing.
