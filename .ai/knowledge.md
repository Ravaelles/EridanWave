# Knowledge base — submodule removal migration (2026-11-09)

Branch: `f/remove-git-submodules-use-direct-libs` (from `develop` ~2a6dfd405).
Goal: no git submodules; jbwapi + mjson vendored as plain files; green build.

## What was done
- Unlinked submodules: `git rm --cached 3rdparty/jbwapi 3rdparty/mjson`, deleted
  `.gitmodules`, removed `.git/modules/3rdparty/*`. Deleted unused
  `3rdparty/javajps`, `3rdparty/jbweb` entirely.
- Staged `3rdparty/jbwapi` + `3rdparty/mjson` as plain files.
- Trimmed vendor fat: removed `jbwapi/src/test` (1.5 MB), `jbwapi/.github`,
  `mjson/src/test`, `jbwapi/it/`, `bwapistructs/`, `.mvn/`, `mvnw*`.
  Policy: `src/main` (+`src/java`) + LICENSE/README only.
- Dropped deps: `commons-lang3` (0 imports), javajps+jbweb source roots from
  `pom.xml`; deleted `src/Placement/JBWEBWrapper.scala` + 5 call sites
  (`EridanWave.scala`: `Walls.logInfo`, `onStart/onUnitDestroy/onUnitDiscover/onUnitMorph`;
  `ShowBWEB.draw`), `jbweb.Walls` import, apache `*-javadoc/-sources` jars.
- Updated `.idea/vcs.xml` (single Git root), `docs/Architecture.md`,
  `readme.md` (no `submodule update --init`). `.idea/workspace.xml` edits stay
  local (file is git-ignored).

## Lost pin d6003b0 — reconstruction (bytecode-verified)
- Upstream `d6003b0` object destroyed with `.git/modules` deletion, unreachable
  upstream. Fresh clone `/tmp/opencode/jbwapi-fetch` HEAD=`68871f1` lacks
  `*Unsafe` too; `git log -S isVisibleUnsafe` finds nothing.
- Truth source: slot jar `/sc-ai/BOTS/EridanWave/AI/EridanWave.jar`
  (`javap -p -c bwapi/Game.class`): `isVisibleUnsafe`, `isExploredUnsafe`,
  `hasCreepUnsafe` are no-guard direct `gameData()` reads.
- Reconstructed as documented vendor patches:
  - `Game.java:~1012`: 3 `*Unsafe` methods (no `isValidTile` guard).
  - `BWClientConfiguration.java:23`: `private` → `public` ctor (so EridanWave
    doesn't mutate shared `DEFAULT`; upstream keeps it private).
- Remaining `with*` gap solved via upstream `Builder`, NOT patching:
  `Main.scala:22-28` uses `new BWClientConfiguration.Builder()...build()`.
  Note: `Builder.withAsyncFrameBufferCapacity` rejects `<1`, but
  `framesToBuffer` is 0 when buffering is off → pass
  `Math.max(framesToBuffer, 1)`.

## Verify
- `git submodule status` must show no `+` prefix (empty = clean).
- `grep -c isVisibleUnsafe 3rdparty/jbwapi/src/main/java/bwapi/Game.java` ≥ 2.
- `grep -c "commons-lang3\|javajps\|jbweb" pom.xml` = 0;
  `grep -rn "jbweb\|JBWEBWrapper" src/ --include=*.scala` empty.
- Build: `./build-jar.sh --clean` (auto-syncs slot jar). Tests:
  `mvn -o -Ptests test` (needs `MAVEN_OPTS` add-opens flags, see `build-jar.sh:30`).
  State 2026-11-09: `BUILD SUCCESS`, jar 18.8 MB, 141/141 tests green.
- Style gate: `python3 scripts/check-blank-lines.py` on touched files only;
  blank line inside continued Scala expression breaks compilation.
  47 violations inside vendored `Game.java`/`BWClientConfiguration.java` are
  pre-existing upstream style, outside our patch regions — left alone per
  convention (fix only your own violations).

## JNA 5.1.0 → 5.18.1 (why the bump)
- Vendored `ClientConnectionPosix.java` (OpenBW macOS/Linux transport, new in
  upstream vs our old checkout) imports `com.sun.jna.platform.linux.Fcntl`,
  `LibRT`, `Mman.*` — absent from jna-platform 5.1.0 (only `unix/LibC`).
  Upstream jbwapi's own pom uses JNA 5.18.1, so we bumped `jna`+`jna-platform`
  to 5.18.1 (both offline in `~/.m2`). Our `src/` uses zero JNA directly
  (only vendored jbwapi does), so blast radius is the already-upstream-tested path.
- `no.fiken.oss:junixsocket-common:1.0.2` added with `provided` scope
  (compile-only for Posix; old coords because the offline repo has them).
  Provided = not shaded into the fat jar; Posix class never loads at runtime
  (bot runs Windows/Wine, `Client` picks W32).
- Shade `<filters>` for jna/jnativehook natives ported from `f/gameplan-tweaks`
  (build-only, ~3 MB dead natives out).
- Legacy: `.idea/artifacts/EridanWave_jar.xml` still extracts
  `3rdparty/apache/jna-5.1.0.jar` (IntelliJ artifact path). Maven is source of
  truth; IntelliJ path would also miss junixsocket — update it if anyone still
  builds through the IDE.

## Warnings for next thread
- **Branch divergence:** feature work lives on `f/gameplan-tweaks` (tip
  `0b00c9733`); this branch lacks it. 2026-11-09 18:54 `build-jar.sh` auto-synced
  the slot (`/sc-ai/BOTS/EridanWave/AI/EridanWave.jar` is now the 18.8 MB
  migration build, previous 18.2 MB feature build overwritten — reproducible by
  rebuilding from `f/gameplan-tweaks` after merge). Merge direction undecided.
- **Do not touch:** uncommitted `KiteAway.scala` method move (user's).
- **Worktree mystery (solved):** parallel user activity switched branches at
  18:34–18:39 (`f/gameplan-tweaks` → `develop` → this branch), which made
  applied fixes "disappear". Proven by mtimes stuck at 18:34:48 + reflog.
  Fix: re-apply + verify in same command; never assume the worktree is stable.
- **Out-of-git:** `/sc-ai/BOTS/__PlayAsEridan/__GENERIC-RUN-ERIDAN.sh` +
  6 wrappers (`_Steamhammer`, `_Bereaver`, `_Ecgberht`, `_PurpleWave`,
  `_ZealotHell`, `_ZergHell`) play `EridanWave:P vs <enemy>` (old copies used
  `AtlantisP:P` + `play-cvis.sh`). `ZealotHell`/`ZergHell` dirs don't exist yet.
- Gotchas: `woundPercent()` is 0.0–1.0 (not 0–100 like Atlantis);
  `Maff.minBy` returns Option; `file` check after edits (edit tool can strip
  CRLF); CherryVis lib must stay (our `Debugging/CherryVis` imports it);
  keep bwem/bwta (pinned jbwapi `bwta` needs `bwem`, our geography uses `bwta`).
- Never commit without being asked (convention: commit-per-feature, but user
  gate stands).
