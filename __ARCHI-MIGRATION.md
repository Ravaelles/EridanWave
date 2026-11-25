# Architecture migration: one Commander, reason-ranked bids

Owner-ordered program (2026-11-25): replace peer-behavior convention
(first-claims chains plus sleep discipline) with mechanism - behaviors
post `UnitIntention` bids carrying a reason, one Commander-owned rank
table decides, one resolve issues once per overlap. Stardust P0-P3
(`docs/PURPLE_VS_STARDUST.md`); rules 9/11/23 (one writer, one
verdict, grow the existing logic).

## Vocabulary (fixed)

- `UnitIntentionReason`: Survival > Dodge > Duel > Formation > March >
  Macro. Open by owner decision; a new reason earns its rank through
  one table row plus one suite row, never by hand-tuning call sites.
- `OrderIssuer`: every Commander BWAPI door, compile-checked, dotted
  ledger keys unchanged (`CommanderMove.step`, ...).
- `UnitIntention`: reason plus issuer plus deferred order thunk. The
  thunk runs once, only if the bid wins. Everything else (votes,
  pushes, probes, destinations) runs once at posting, exactly like
  before.
- `CommanderCore.issueOrder`: the only BWAPI door. Direct mode claims
  the fuse and issues (Step-0 semantics). Capture mode posts a bid
  under the entering stage's reason. Unready frames post nothing.
- `CommanderCore.resolveBids`: ends a window. Highest rank wins,
  first posted wins ties (today's first-claims, kept as the tie-break).
  Losers count `outrankedBid.loserVsWinner.at.Class` (Low). A direct
  issuance that already claimed the frame keeps today's semantics -
  bids stand down and name it (`bidStoodDown.directIssuer.at.Class`).
- Loop control stays first-claims until Step 3 proves otherwise per
  pair (rule 4). Traffic pushes stay advisory (never orders).

## Step 0 - fuse, log-only (DONE, `b915f779c`)

Every `Commander*` issuance claims the frame; a second issuance in
the same frame lands in ErrorLedger as
`duplicateOrder.firstVsSecond.at.Class` (Medium) instead of failing
silently. Callers still issue. First blood: 10x
`ApproachEnemyFormation.holdVsCommanderMove.hold` on 4+3Zeal.

## Step 1 - bids in technique plus engage (DONE, `4f4c99652`)

Rank kernel plus suite, bid queue on the Agent, resolve in
`CombatEngagement.engage` and `dispatchByTechnique`. Every
`BehaviorStage` declares its reason beside name and band; the shared
template stamps it. 35 sites plus the formation bypass run through
the one door. Live: 39 same-rank ties arbitrated on 4+3Zeal, zero
duplicates, tripwires silent; 1Drag VICTORY clean; 1DragWounded
inside its variance band.

## Step 1b - doors plus wider window (DONE, `f597a6175`)

`OrderIssuer` enum (typo'd doors do not compile), capture window
opened in `dispatchByTechnique` so technique bids (Retreat Survival,
Avoid Dodge) resolve together with duel bids. Live: 48 ties, zero
duplicates, 1DragWounded kite chains healthy.

## Step 3 - rank decides across stages (C: ANALYSIS DONE, OWNER GATE)

Status: the mechanism is ready (rank kernel, doors, windows,
resolve); the flip is NOT shipped, on purpose. Worked pair proving
a naive relaxation breaks doctrine:

- Formation-vs-Close, outranged frame with a seen enemy and no
  volley: `FormationDuelStage` claims (dress the line - the header
  says the hold owns outranged frames), and `CloseDuelStage.outer`
  is simultaneously true through `noVolley`. Rank says Duel(3) >
  Formation(2), so the line would charge from ten tiles out instead
  of dressing. The pipeline ORDER encodes context (outranged goes
  to the line, in-reach to the duel) that a context-free rank does
  not see.
- Conclusion: cross-stage arbitration needs contextual rank (rank
  conditioned on contact/reach, or split reasons such as
  DuelInReach) - a doctrine redesign, not a mechanical flip.
  Rule 20: owner doctrine call required before any loop-control
  change. Until then loop control stays first-claims and the rank
  arbitrates true within-stage overlaps only (proven live).

## Step 2 - every governor bids (A plus B)

A. Local windows (DONE): `tryDodgeBid` (Dodge), `Travel` (March),
   `Attack` (Duel) open capture, stamp the reason, resolve at once.
   Single-bid frames issue exactly like before; the reasons are live
   for the future joint window. Live: 1Drag V/V/D/V/V - the defeat
   inside the documented glue-fight variance (only arbitration that
   game was outcome-equivalent), victories clean with zero ledger
   notes.
B. Parked, still direct under the fuse, each needing its own UMS map
   plus owner call: Gather plus economy, Be* legacy brains,
   transport missions, scouts. They migrate never opportunistically.

## Step 3 - rank decides across stages (C)

Relax first-claims loop control under capture (keep running stages
after a claim), so the rank table - not pipeline order - picks the
winner. The table mirrors the band doctrine by construction
(HIGH survival/kills, MEDIUM duel, LOW march), so most frames
reproduce today; every differing pair needs a UMS game (rule 4)
before it ships. The fuse stays as the enforcing tripwire; direct
mode disappears only when its last issuer migrates, never by flag
day. Ladder games validate the whole (rule 21).
