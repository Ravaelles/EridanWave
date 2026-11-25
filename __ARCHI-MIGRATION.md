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

## Step 3 - rank decides across stages (C: SHADOW SHIPPED, FLIP IS AN OWNER CALL)

Status: the shadow instrument runs on every engage window (silent
except divergences), first-claims loop control RESTORED after a
revert-proven finding (see below). Live divergence data, 4+3Zeal:
exactly one contested pair - `divergeBid.DuelVsFormation` on
zealots, 34x/game, frames 0-2482 (a Formation slot-walk bid vs a
Close kite-move bid; rank would kite, order dresses). No other
pair diverges anywhere measured.
- Negative result with teeth: relaxing the loops to measure changed
  the game it measured (1Drag waste 117f band to 200-254f with three
  straight defeats; recovery to wins and 125-144f after revert).
  Cause: post-claim bodies carry load-bearing side effects (pushes,
  ride goals, decisions), so the shadow cannot run bodies to
  measure. Lesson: cross-stage arbitration cannot be observed for
  free - it needs per-pair flip experiments behind a revert, never
  a global relax.
- The DuelVsFormation flip is the first candidate: rank kites where
  order dresses, 34 frames/game. Flipping breaks "the hold owns
  outranged frames" - owner doctrine call with this count attached,
  not a blind flip.

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
