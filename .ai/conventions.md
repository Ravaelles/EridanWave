# Working conventions

Ordered most to least important. Each is one rule; keep them short.

1. **TDD, no exceptions.** Complex rules ship test-first: a failing
   suite, then code that greens it. Pure kernels beside their callers
   (`*Suite` mirrors `src/`, never co-located); game-coupled reads
   stay behind thin seams (`TestKit` seeds game-free geometry).
   Temporary repro suites are cheap; keepers live in
   `tests-starengine/` (tagged slow, never in the default suite).
   The legacy `tests/` folder is grandfathered - leave it alone.
2. **Every behavior states its unit context first.** A behavior
   that can fire says which units it ever applies to - race and class,
   ground or air, worker or warrior, seen or detected on both sides,
   whose weapon reaches whom - in one `appliesToThisUnit` gate beside
   its `logicApplies`, behind a single `shouldRun` the pipeline calls
   instead of `run`. New behaviors without the gate do not compile.
3. **Reproduce before fixing.** Every behavior change starts from a
   live game or a StarEngine repro - StarEngine first, add the missing
   mechanic if it lacks one. Never fix blind.
4. **Verify by execution.** Run the code, run the tests, read the
   evidence. State hypotheses and outcomes; distrust any claim the
   last run did not confirm.
5. **Green commits per unit, never push unsolicited.** A finished unit
   (green build, green tests, clean style gate) commits at once with
   an English message - including hunks you did not author, as long
   as they belong to the unit. Docs ride along, never alone. Never
   red, never pushed unless asked.
6. **English code, user's language out.** 100% of code, comments and
   commits in English; talk to the user in their language.
7. **One reason to change; extend, never rewrite.** SRP/OCP: new
   behavior arrives as a new object plus one dispatch line, never as
   a rewrite of untouched code.
8. **One writer per movement.** A behavior votes a posture OR moves a
   unit to its slot - never both, never a free map point. Unlisted
   writers fail the `decision-writers` gate until they answer: which
   posture, do slots stay, where is the hysteresis. Named exceptions:
   survival, boarding, pair logistics, slotless doctrines.
9. **Hysteresis lives in `Latch`; every edge ships entry and release.**
   Unit-keyed holds go through `Latch` (reset per game), never a fresh
   map. A threshold naming one side only is incomplete - ask for the
   other before coding it.
10. **One verdict per question.** Retreat, motion, production: a single
   object owns it and every reader obeys. A new vote-holder merges
   into the existing one instead.
11. **Every fix gets an architecture review.** One paragraph per solved
    problem: third instance of the shape? Shared gate, pure kernel,
    deleted special case? Log it in
    `.ai/architectural-challenges.md` with the fixing commit.
12. **Rebuild the bot jar when done.** Run `scripts/build-jar.sh`
    before the final message so the owner never waits on a cold build.
13. **The owner's tree changes are theirs.** A dirty working tree is
    the owner's hand: pre-analyze, adopt, split unrelated hunks,
    commit separately. Never drop, absorb, or leave past the session.
14. **Diagnostics through the ledger; stdout is the launcher's.**
    Per-frame noise goes to `ErrorLedger` with a greppable
    `area.Cause.at:line` key, never raw spam. In-game words go through
    the logger; `System.out` is for launchers only (DeathWatch death
    reports excepted).
15. **Tiles unless pixels.** New thresholds in tiles; convert once at
    the seam. Existing `pixel*` engine APIs stay.
16. **Blank lines around `return`, nothing else.** Gate only your own
    new violations; never reformat untouched legacy code.
17. **Protoss unit packages import races explicitly.** The package name
    shadows matchers - import `{Protoss, Terran, ...}` by name, never
    wildcard.
18. **Doctrine outranks `Target` and says so.** A pre-`Target` override
    is one target, fixed priority, documented in its header. No second
    scoring ladder for the same question; squad assignment belongs in
    `Target` as a filter instead.
19. **Disagree with a better idea; investigate cheapest-first.** Say so
    and propose it instead of executing blindly; pause for
    confirmation when the direction is materially different. Prefer
    the smallest investigation that unblocks a confident fix.
20. **UMS and StarEngine judge behavior; ladder validates.** Maps and
    suites are the source of truth, ladder games only confirm.
21. **Minimal diffs, stable line endings.** Default CRLF for new Scala
    unless the file says otherwise; no whitespace-only or
    line-ending-only edits.
