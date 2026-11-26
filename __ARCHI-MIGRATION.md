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

## Step 2 - every governor bids (A plus B, then consolidated in Step 4)

A. Local windows (SHIPPED then CUT as vacuous in Step 4):
   `tryDodgeBid` (Dodge), `Travel` (March), `Attack` (Duel) opened
   capture, stamped the reason, resolved at once.
   Single-bid frames issue exactly like before; the reasons are live
   for the future joint window. Live: 1Drag V/V/D/V/V - the defeat
   inside the documented glue-fight variance (only arbitration that
   game was outcome-equivalent), victories clean with zero ledger
   notes.
B. Parked, still direct under the fuse, each needing its own UMS map
   plus owner call: Gather plus economy, Be* legacy brains,
   transport missions, scouts. They migrate never opportunistically.

## Step 3 - rank decides across stages (C: LIVE, OWNER-VOTED FLIP)

Status: every window resolves live by rank (ties go to the first
bid, i.e. today's order) - the flip the shadow below measured.
The shadow instrument has since been deleted (Step 4); the live
divergence data that motivated the flip: exactly one contested
pair - `divergeBid.DuelVsFormation` on zealots, 34x/game,
frames 0-2482 (a Formation slot-walk bid vs a Close kite-move bid).
No other pair diverged anywhere measured.
- Negative result with teeth: relaxing the loops to measure changed
  the game it measured (1Drag waste 117f band to 200-254f with three
  straight defeats; recovery to wins and 125-144f after revert).
  Cause: post-claim bodies carry load-bearing side effects (pushes,
  ride goals, decisions), so cross-stage arbitration cannot be
  observed for free - it needs per-pair flip experiments behind a
  revert, never a global relax.
- Flip sample on 4+3: 5/7, 3/7, 1/7, 4/7 (mean 3.25 vs ~2.2 shadow
  era), tripwires/flap/waste silent - not worse anywhere, mildly
  better. Kept per owner vote; the remaining 4+3 gap is the focus
  quorum, not arbitration.

## Step 4 - consolidation: what the audit kept vs cut (DONE)

An audit of the whole authority mechanism (every seam, window and
ledger after Steps 0-3) kept what measures and cut what does not:
- CUT: the shadow resolve path (resolveShadow), the divergeKey
  ledger key and its suite row, the shadow flag on every resolve
  call. The flip it was built to measure shipped live instead; the
  instrument served, then left. Re-adding it is one commit from
  git history if a future pair needs counting first.
- CUT: the single-bid local windows (Travel/March, Attack/Duel,
  tryDodge/Dodge). Proven vacuous by construction: one posted bid
  always wins, the act gate already skips unready frames, so the
  stood-down path could never fire - the resolve issued exactly
  what the direct door would. A future joint window stamps reasons
  at the Action level if it ever comes; these ad-hoc lines would
  be replaced by it, not reused.
- KEPT: the fuse (log-only tripwire - it caught the hold double
  and still watches every direct issuer: economy, Be* legacy,
  transports, scouts), the engage+technique joint window (the only
  place multi-bid frames actually occur), outranked/stood-down
  ledgers, FocusMemory, StickyAnchor, OrderIssuer doors.
- DELIBERATELY NOT MERGED: rank table vs pipeline order stay two
  artifacts on purpose. Phase order (technique before duel) and
  within-window rank answer different questions; unifying them
  would need contextual rank (reach/contact), which is doctrine,
  not plumbing.
- DELIBERATELY NOT FLIPPED: the fuse stays log-only. Blocking a
  duplicate drops intent that no dedup owns today (unload-then-move
  has no sleep between them); same-issuer skips are unsafe too,
  because sites are shared across actions. Blocking needs per-pair
  evidence that does not exist yet.
Live: 1Drag + Wounded VICTORY 1/0 (waste in band), 4+3 in band,
zero tripwires, suite green.
- Post-pass cleanup (no behavior change, verified green + UMS):
  dead shadow path deleted outright (resolveShadow, divergeKey and
  its suite row), single-bid local windows reverted to direct
  Commander calls (Travel/Attack/tryDodge proved vacuous), shared
  FocusPack trace plus one-call expeditionScore extracted so the
  two focus seams and the three retreat arms cannot drift apart,
  UnitIntentionReason imports shortened beside the stages they
  document, claimOrder simplified to Unit (nobody read the flag),
  FocusMemory writes fenced behind tracePack, PostureHoldSuite
  fixed to the moved RetreatExtraPenalties home.
